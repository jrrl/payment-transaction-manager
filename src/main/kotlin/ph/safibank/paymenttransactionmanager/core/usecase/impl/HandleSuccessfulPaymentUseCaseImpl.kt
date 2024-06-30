package ph.safibank.paymenttransactionmanager.core.usecase.impl

import ph.safibank.paymenttransactionmanager.core.event.EventService
import ph.safibank.paymenttransactionmanager.core.exception.TransactionNotFoundException
import ph.safibank.paymenttransactionmanager.core.exception.ValidationError
import ph.safibank.paymenttransactionmanager.core.exception.ValidationException
import ph.safibank.paymenttransactionmanager.core.posting.PostingService
import ph.safibank.paymenttransactionmanager.core.provider.ProviderStatus
import ph.safibank.paymenttransactionmanager.core.transaction.PostingStatus
import ph.safibank.paymenttransactionmanager.core.transaction.Transaction
import ph.safibank.paymenttransactionmanager.core.transaction.TransactionRepo
import ph.safibank.paymenttransactionmanager.core.transaction.TransactionStatus
import ph.safibank.paymenttransactionmanager.core.usecase.HandleSuccessfulPaymentUseCase
import java.util.UUID

class HandleSuccessfulPaymentUseCaseImpl(
    private val transactionRepository: TransactionRepo,
    private val eventService: EventService,
    private val postingService: PostingService
) : HandleSuccessfulPaymentUseCase {
    override suspend fun invoke(transactionID: UUID) {
        val transaction = transactionRepository.getTransaction(transactionID) ?: throw TransactionNotFoundException()

        validateTransaction(transaction)

        val updatedTransaction = transaction.copy(
            status = TransactionStatus.PENDING_SETTLEMENT,
            providerDetails = transaction.providerDetails.copy(providerStatus = ProviderStatus.SUCCESS),
        )

        val postingResult = postingService.settleAmount(updatedTransaction)

        saveAndSendEvent(
            updatedTransaction.copy(
                postingDetails = transaction.postingDetails!!.copy(
                    batchId = postingResult.batchId,
                    status = PostingStatus.PENDING_SETTLEMENT,
                    postedAt = null,
                    postingId = null,
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
        eventService.sendTransactionPendingSettlementEvent(transaction)
        return updatedTransaction
    }
}
