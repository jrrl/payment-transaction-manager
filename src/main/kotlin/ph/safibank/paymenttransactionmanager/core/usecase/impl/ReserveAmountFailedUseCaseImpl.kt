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
import ph.safibank.paymenttransactionmanager.core.transaction.ReserveTransactionRequest
import ph.safibank.paymenttransactionmanager.core.transaction.Transaction
import ph.safibank.paymenttransactionmanager.core.transaction.TransactionRepo
import ph.safibank.paymenttransactionmanager.core.transaction.TransactionStatus.FAILED
import ph.safibank.paymenttransactionmanager.core.transaction.TransactionStatus.PENDING
import ph.safibank.paymenttransactionmanager.core.transaction.TransactionStatus.WAITING_FOR_APPROVAL
import ph.safibank.paymenttransactionmanager.core.usecase.ReserveAmountFailedUseCase

@Singleton
class ReserveAmountFailedUseCaseImpl(
    private val transactionRepo: TransactionRepo,
    private val eventService: EventService,
    private val feeService: FeeService,
) : ReserveAmountFailedUseCase {

    override suspend fun invoke(request: ReserveTransactionRequest) {
        val transaction =
            transactionRepo.getTransaction(request.transactionId) ?: throw TransactionNotFoundException()

        validateTransaction(transaction)
        revertFees(transaction)
        updateAndSendEvent(transaction)
    }

    private suspend fun revertFees(transaction: Transaction) {
        feeService.revertCustomerFee(transaction.customerFee)
    }

    private suspend fun updateAndSendEvent(transaction: Transaction) {
        eventService.sendTransactionFailedEvent(
            transactionRepo.updateTransaction(
                transaction.copy(
                    status = FAILED,
                    customerFee = CustomerFee.ZERO_PHP,
                    vendorFee = VendorFee.ZERO_PHP,
                    postingDetails = transaction.postingDetails!!.copy(status = PostingStatus.FAILED),
                )
            )
        )
    }

    private fun validateTransaction(transaction: Transaction) {
        val errors = listOfNotNull(
            when (transaction.status) {
                PENDING, WAITING_FOR_APPROVAL -> null
                else -> ValidationError("Transaction (${transaction.id}) Status should be pending")
            },
            if (transaction.postingDetails == null) {
                ValidationError("Transaction (${transaction.id}) posting details should not be null")
            } else null,
        )

        if (errors.isNotEmpty()) {
            throw ValidationException(errors)
        }
    }
}
