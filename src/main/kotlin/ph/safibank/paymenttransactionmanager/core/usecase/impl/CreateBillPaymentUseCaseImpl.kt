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
import ph.safibank.paymenttransactionmanager.core.merchant.MerchantDetails
import ph.safibank.paymenttransactionmanager.core.merchant.MerchantService
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
import ph.safibank.paymenttransactionmanager.core.usecase.BillPaymentRequest
import ph.safibank.paymenttransactionmanager.core.usecase.CreateBillPaymentUseCase

class CreateBillPaymentUseCaseImpl(
    private val accountService: AccountService,
    private val merchantService: MerchantService,
    private val fraudService: FraudService,
    private val transactionRepo: TransactionRepo,
    private val feeService: FeeService,
    private val postingService: PostingService,
    private val providers: List<ProviderService>,
    private val eventService: EventService
) : CreateBillPaymentUseCase {

    override suspend fun invoke(request: BillPaymentRequest): TransactionResult = coroutineScope {
        val accountDetailsCall = async {
            accountService.getAccountDetails(request.accountNumber)
                ?: throw ValidationException(listOf(ValidationError("Account ${request.accountNumber} does not exist")))
        }
        val merchantDetails = merchantService.getMerchantDetails(request.billerCode)
            ?: throw ValidationException(listOf(ValidationError("Biller ${request.billerCode} does not exist")))
        val accountDetails = accountDetailsCall.await()

        validateRequest(request, accountDetails, merchantDetails)

        val providerService = providers.filter {
            it.usableForTransaction(TransactionType.BILL_PAYMENT, request.billerCode)
        }
        if (providerService.size != 1) {
            throw InvalidProviderException(request.billerCode)
        }

        val newTransaction = createNewTransaction(
            request = request,
            accountDetails = accountDetails,
            merchantDetails = merchantDetails,
            providerService = providerService.first()
        )

        val savedTransaction = when (val fraudStatus = fraudService.determineFraudStatus(newTransaction)) {
            FraudStatus.REJECTED -> {
                saveNewTransaction(
                    newTransaction.copy(
                        status = TransactionStatus.FAILED,
                        fraudStatus = fraudStatus
                    )
                )
                throw FraudException()
            }
            FraudStatus.STEP_UP_LEVEL1,
            FraudStatus.STEP_UP_LEVEL2,
            FraudStatus.STEP_UP_LEVEL3,
            FraudStatus.STEP_UP_LEVEL4 -> {
                saveNewTransaction(
                    newTransaction.copy(
                        status = TransactionStatus.FAILED,
                        fraudStatus = fraudStatus
                    )
                )
                throw StepUpException(fraudStatus)
            }
            FraudStatus.MANUAL_APPROVAL,
            FraudStatus.APPROVED -> {
                val vendorFee = feeService.calculateVendorFee(newTransaction)
                val customerFee = feeService.calculateCustomerFee(newTransaction)

                val updatedTransaction = saveNewTransaction(
                    newTransaction.copy(
                        fraudStatus = fraudStatus,
                        status = TransactionStatus.PENDING,
                        customerFee = customerFee,
                        vendorFee = vendorFee
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

    private fun createNewTransaction(
        request: BillPaymentRequest,
        accountDetails: AccountDetails,
        merchantDetails: MerchantDetails,
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
            merchantCode = merchantDetails.code,
            merchantName = merchantDetails.name
        )
    )

    private suspend fun saveNewTransaction(transaction: Transaction): Transaction {
        val savedTransaction = transactionRepo.saveTransaction(transaction)
        eventService.sendTransactionCreatedEvent(transaction)
        return savedTransaction
    }

    private fun validateRequest(request: BillPaymentRequest, accountDetails: AccountDetails, merchatDetails: MerchantDetails) {
        val errors = listOfNotNull(
            if (accountDetails.balance < request.amount) {
                ValidationError("Account ${accountDetails.accountNumber} has insufficient balance")
            } else null,
            if (!accountDetails.active) {
                ValidationError("Account ${accountDetails.accountNumber} is not active")
            } else null,
            if (request.amount < merchatDetails.minimumLimit) {
                ValidationError("Payment amount is less than Biller ${merchatDetails.code} minimumLimit")
            } else null,
            if (merchatDetails.maximumLimit != null && request.amount > merchatDetails.maximumLimit) {
                ValidationError("Payment amount is greater than Biller ${merchatDetails.code} maximumLimit")
            } else null
        )

        if (errors.isNotEmpty()) {
            throw ValidationException(errors)
        }
    }
}
