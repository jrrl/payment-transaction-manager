package ph.safibank.paymenttransactionmanager.core.usecase.impl

import ph.safibank.paymenttransactionmanager.core.event.EventService
import ph.safibank.paymenttransactionmanager.core.exception.TransactionNotFoundException
import ph.safibank.paymenttransactionmanager.core.exception.ValidationError
import ph.safibank.paymenttransactionmanager.core.exception.ValidationException
import ph.safibank.paymenttransactionmanager.core.fee.CustomerFee
import ph.safibank.paymenttransactionmanager.core.fee.VendorFee
import ph.safibank.paymenttransactionmanager.core.posting.PostingService
import ph.safibank.paymenttransactionmanager.core.provider.ProviderStatus
import ph.safibank.paymenttransactionmanager.core.transaction.PostingStatus
import ph.safibank.paymenttransactionmanager.core.transaction.Transaction
import ph.safibank.paymenttransactionmanager.core.transaction.TransactionRepo
import ph.safibank.paymenttransactionmanager.core.transaction.TransactionStatus
import ph.safibank.paymenttransactionmanager.core.usecase.HandleFailedPaymentUseCase
import java.util.UUID

class HandleFailedPaymentUseCaseImpl(
    private val transactionRepository: TransactionRepo,
    private val eventService: EventService,
    private val postingService: PostingService
) : HandleFailedPaymentUseCase {
    override suspend fun invoke(transactionID: UUID) {
        val transaction = transactionRepository.getTransaction(transactionID) ?: throw TransactionNotFoundException()

        validateTransaction(transaction)

        val updatedTransaction = transaction.copy(
            status = TransactionStatus.PENDING_RELEASE,
            customerFee = CustomerFee.ZERO_PHP,
            vendorFee = VendorFee.ZERO_PHP,
            providerDetails = transaction.providerDetails.copy(providerStatus = ProviderStatus.FAILED),
        )

        val postingResult = postingService.releaseAmount(updatedTransaction)

        saveAndSendEvent(
            updatedTransaction.copy(
                postingDetails = transaction.postingDetails!!.copy(
                    batchId = postingResult.batchId,
                    status = PostingStatus.PENDING_RELEASE,
                )
            )
        )
    }

    private fun validateTransaction(transaction: Transaction) {
        val errors = listOfNotNull(
            when (transaction.status) {
                TransactionStatus.SENT_TO_PROVIDER -> null
                else -> ValidationError("Transaction (${transaction.id}) Status should be ${TransactionStatus.SENT_TO_PROVIDER}")
            },
            if (transaction.postingDetails == null) {
                ValidationError("Transaction (${transaction.id}) posting details should not be null")
            } else null,
        )

        if (errors.isNotEmpty()) {
            throw ValidationException(errors)
        }
    }

    private suspend fun saveAndSendEvent(transaction: Transaction): Transaction {
        val updatedTransaction = transactionRepository.updateTransaction(transaction)
        eventService.sendTransactionFailedEvent(updatedTransaction)
        return updatedTransaction
    }
}
