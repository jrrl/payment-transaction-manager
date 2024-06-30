package ph.safibank.paymenttransactionmanager.core.usecase.impl

import jakarta.inject.Singleton
import ph.safibank.paymenttransactionmanager.core.event.EventService
import ph.safibank.paymenttransactionmanager.core.exception.TransactionNotFoundException
import ph.safibank.paymenttransactionmanager.core.exception.ValidationError
import ph.safibank.paymenttransactionmanager.core.exception.ValidationException
import ph.safibank.paymenttransactionmanager.core.transaction.PostingStatus
import ph.safibank.paymenttransactionmanager.core.transaction.SettleTransactionResult
import ph.safibank.paymenttransactionmanager.core.transaction.Transaction
import ph.safibank.paymenttransactionmanager.core.transaction.TransactionRepo
import ph.safibank.paymenttransactionmanager.core.transaction.TransactionStatus
import ph.safibank.paymenttransactionmanager.core.usecase.SettleTransactionUseCase
import java.time.Instant

@Singleton
class SettleTransactionUseCaseImpl(
    private val transactionRepo: TransactionRepo,
    private val eventService: EventService,
) : SettleTransactionUseCase {
    override suspend fun invoke(result: SettleTransactionResult) {
        val transaction = transactionRepo.getTransaction(result.transactionId) ?: throw TransactionNotFoundException()
        validateTransaction(transaction)
        saveAndSendEvent(
            transaction.copy(
                status = TransactionStatus.SUCCESS,
                postingDetails = transaction.postingDetails!!.copy(
                    postingId = result.postingId,
                    postedAt = Instant.now(),
                    status = PostingStatus.SETTLED
                )
            )
        )
    }

    private suspend fun saveAndSendEvent(transaction: Transaction) {
        val updatedTransaction = transactionRepo.updateTransaction(transaction)
        eventService.sendTransactionSuccessEvent(updatedTransaction)
    }

    private fun validateTransaction(transaction: Transaction) {
        val errors = listOfNotNull(
            when (transaction.status) {
                TransactionStatus.PENDING_SETTLEMENT -> null
                else -> ValidationError("Transaction (${transaction.id}) Status should be pending settlement")
            },
            if (transaction.postingDetails == null)
                ValidationError("Transaction (${transaction.id}) posting details should not be null") else null,
        )

        if (errors.isNotEmpty()) {
            throw ValidationException(errors)
        }
    }
}
