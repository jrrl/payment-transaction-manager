package ph.safibank.paymenttransactionmanager.core.usecase.impl

import jakarta.inject.Singleton
import ph.safibank.paymenttransactionmanager.core.event.EventService
import ph.safibank.paymenttransactionmanager.core.exception.TransactionNotFoundException
import ph.safibank.paymenttransactionmanager.core.exception.ValidationError
import ph.safibank.paymenttransactionmanager.core.exception.ValidationException
import ph.safibank.paymenttransactionmanager.core.fee.CustomerFee
import ph.safibank.paymenttransactionmanager.core.fee.FeeService
import ph.safibank.paymenttransactionmanager.core.fee.VendorFee
import ph.safibank.paymenttransactionmanager.core.transaction.PostingStatus
import ph.safibank.paymenttransactionmanager.core.transaction.ReleaseTransactionResult
import ph.safibank.paymenttransactionmanager.core.transaction.Transaction
import ph.safibank.paymenttransactionmanager.core.transaction.TransactionRepo
import ph.safibank.paymenttransactionmanager.core.transaction.TransactionStatus
import ph.safibank.paymenttransactionmanager.core.usecase.ReleaseTransactionUseCase
import java.time.Instant

@Singleton
class RelaseTransactionUseCaseImpl(
    private val transactionRepo: TransactionRepo,
    private val eventService: EventService,
    private val feeService: FeeService
) : ReleaseTransactionUseCase {
    override suspend fun invoke(result: ReleaseTransactionResult) {
        val transaction = transactionRepo.getTransaction(result.transactionId) ?: throw TransactionNotFoundException()

        validateTransaction(transaction)

        feeService.revertCustomerFee(transaction.customerFee)

        saveAndSendEvent(
            transaction.copy(
                status = TransactionStatus.FAILED,
                postingDetails = transaction.postingDetails!!.copy(
                    postingId = result.postingId,
                    postedAt = Instant.now(),
                    status = PostingStatus.RELEASED,
                ),
                customerFee = CustomerFee.ZERO_PHP,
                vendorFee = VendorFee.ZERO_PHP,
            )
        )
    }

    private fun validateTransaction(transaction: Transaction) {
        val errors = listOfNotNull(
            when (transaction.status) {
                TransactionStatus.PENDING_RELEASE -> null
                else -> ValidationError("Transaction (${transaction.id}) Status should be ${TransactionStatus.PENDING_RELEASE}")
            },
            if (transaction.postingDetails == null)
                ValidationError("Transaction (${transaction.id}) posting details should not be null") else null,
        )

        if (errors.isNotEmpty()) {
            throw ValidationException(errors)
        }
    }

    private suspend fun saveAndSendEvent(transaction: Transaction) {
        val updatedTransaction = transactionRepo.updateTransaction(transaction)
        eventService.sendTransactionReleasedEvent(updatedTransaction)
    }
}
