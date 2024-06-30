package ph.safibank.paymenttransactionmanager.core.usecase.impl

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import ph.safibank.paymenttransactionmanager.core.account.AccountDetails
import ph.safibank.paymenttransactionmanager.core.account.AccountService
import ph.safibank.paymenttransactionmanager.core.event.EventService
import ph.safibank.paymenttransactionmanager.core.exception.FraudException
import ph.safibank.paymenttransactionmanager.core.exception.InvalidProviderException
import ph.safibank.paymenttransactionmanager.core.exception.StepUpException
import ph.safibank.paymenttransactionmanager.core.exception.UnknownStatusException
import ph.safibank.paymenttransactionmanager.core.exception.ValidationError
import ph.safibank.paymenttransactionmanager.core.exception.ValidationException
import ph.safibank.paymenttransactionmanager.core.fee.CustomerFee
import ph.safibank.paymenttransactionmanager.core.fee.FeeService
import ph.safibank.paymenttransactionmanager.core.fee.VendorFee
import ph.safibank.paymenttransactionmanager.core.fraud.FraudService
import ph.safibank.paymenttransactionmanager.core.fraud.FraudStatus
import ph.safibank.paymenttransactionmanager.core.merchant.MerchantService
import ph.safibank.paymenttransactionmanager.core.merchant.ProductDetails
import ph.safibank.paymenttransactionmanager.core.posting.PostingService
import ph.safibank.paymenttransactionmanager.core.provider.ProviderService
import ph.safibank.paymenttransactionmanager.core.transaction.PostingDetails
import ph.safibank.paymenttransactionmanager.core.transaction.ProviderDetails
import ph.safibank.paymenttransactionmanager.core.transaction.SenderDetails
import ph.safibank.paymenttransactionmanager.core.transaction.Transaction
import ph.safibank.paymenttransactionmanager.core.transaction.TransactionRepo
import ph.safibank.paymenttransactionmanager.core.transaction.TransactionResult
import ph.safibank.paymenttransactionmanager.core.transaction.TransactionStatus
import ph.safibank.paymenttransactionmanager.core.transaction.TransactionType
import ph.safibank.paymenttransactionmanager.core.usecase.AirtimeLoadRequest
import ph.safibank.paymenttransactionmanager.core.usecase.CreateAirtimeLoadUseCase

class CreateAirtimeLoadUseCaseImpl(
    private val accountService: AccountService,
    private val merchantService: MerchantService,
    private val providers: List<ProviderService>,
    private val fraudService: FraudService,
    private val transactionRepo: TransactionRepo,
    private val eventService: EventService,
    private val feeService: FeeService,
    private val postingService: PostingService
) : CreateAirtimeLoadUseCase {

    override suspend fun invoke(request: AirtimeLoadRequest): TransactionResult = coroutineScope {
        val accountDetailsCall = async {
            accountService.getAccountDetails(request.accountNumber)
                ?: throw ValidationException(listOf(ValidationError("Account ${request.accountNumber} does not exist")))
        }

        val providerService = providers.filter {
            it.usableForTransaction(TransactionType.AIRTIME_LOAD, request.product)
        }

        if (providerService.size != 1) {
            throw InvalidProviderException(request.product)
        }

        val productDetails = merchantService.getProduct(request.product)
            ?: throw ValidationException(listOf(ValidationError("Product ${request.product} does not exist")))
        val accountDetails = accountDetailsCall.await()

        validateRequest(request, accountDetails, productDetails)

        val newTransaction = createNewTransaction(
            request = request,
            accountDetails = accountDetails,
            productDetails = productDetails,
            providerService = providerService.first()
        )

        val savedTransaction = when (val fraudStatus = fraudService.determineFraudStatus(newTransaction)) {
            FraudStatus.REJECTED -> {
                saveFailedTransaction(newTransaction, fraudStatus)
                throw FraudException()
            }
            FraudStatus.STEP_UP_LEVEL1,
            FraudStatus.STEP_UP_LEVEL2,
            FraudStatus.STEP_UP_LEVEL3,
            FraudStatus.STEP_UP_LEVEL4 -> {
                saveFailedTransaction(newTransaction, fraudStatus)
                throw StepUpException(fraudStatus)
            }
            FraudStatus.MANUAL_APPROVAL,
            FraudStatus.APPROVED -> {
                val vendorFeeCall = async { feeService.calculateVendorFee(newTransaction) }
                val customerFeeCall = async { feeService.calculateCustomerFee(newTransaction) }

                val updatedTransaction = saveNewTransaction(
                    newTransaction.copy(
                        fraudStatus = fraudStatus,
                        status = TransactionStatus.PENDING,
                        customerFee = customerFeeCall.await(),
                        vendorFee = vendorFeeCall.await()
                    )
                )

                launch {
                    val postingResult = postingService.reserveTransactionAmount(updatedTransaction)
                    val savedTransaction = transactionRepo.updateTransaction(
                        updatedTransaction.copy(
                            postingDetails = PostingDetails(postingResult.batchId)
                        )
                    )
                    eventService.sendTransactionApprovedEvent(savedTransaction)
                }

                updatedTransaction
            }
            FraudStatus.UNKNOWN -> {
                throw UnknownStatusException("Unknown fraud status for transaction ${newTransaction.id}")
            }
        }
        TransactionResult(
            transactionId = savedTransaction.id,
            transactionStatus = savedTransaction.status
        )
    }

    private suspend fun saveFailedTransaction(newTransaction: Transaction, fraudStatus: FraudStatus) {
        saveNewTransaction(
            newTransaction.copy(
                status = TransactionStatus.FAILED,
                fraudStatus = fraudStatus
            )
        )
    }

    private fun validateRequest(
        request: AirtimeLoadRequest,
        accountDetails: AccountDetails,
        productDetails: ProductDetails
    ) {
        val errors = listOfNotNull(
            if (accountDetails.balance < request.amount) {
                ValidationError("Account ${accountDetails.accountNumber} has insufficient balance")
            } else null,
            if (!accountDetails.active) {
                ValidationError("Account ${accountDetails.accountNumber} is not active")
            } else null,
            if (productDetails.sellingPrice != request.amount) {
                ValidationError("Request amount ${request.amount} is not equal to product selling price ${productDetails.sellingPrice}")
            } else null
        )
        if (errors.isNotEmpty()) {
            throw ValidationException(errors)
        }
    }

    private fun createNewTransaction(
        request: AirtimeLoadRequest,
        accountDetails: AccountDetails,
        productDetails: ProductDetails,
        providerService: ProviderService
    ) = Transaction(
        id = request.id,
        amount = request.amount,
        currency = request.currency,
        type = TransactionType.BILL_PAYMENT,
        status = TransactionStatus.INITIATED,
        senderDetails = SenderDetails(
            accountId = accountDetails.id,
            customerId = accountDetails.customerId,
            accountNumber = accountDetails.accountNumber
        ),
        fraudStatus = FraudStatus.UNKNOWN,
        customerFee = CustomerFee.ZERO_PHP,
        vendorFee = VendorFee.ZERO_PHP,
        providerDetails = ProviderDetails(
            provider = providerService.getName(),
            merchantCode = productDetails.code,
            merchantName = productDetails.name
        )
    )

    private suspend fun saveNewTransaction(transaction: Transaction): Transaction {
        val savedTransaction = transactionRepo.saveTransaction(transaction)
        eventService.sendTransactionCreatedEvent(transaction)
        return savedTransaction
    }
}
