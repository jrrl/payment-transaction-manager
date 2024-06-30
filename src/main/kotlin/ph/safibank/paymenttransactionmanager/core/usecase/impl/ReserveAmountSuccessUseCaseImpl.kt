package ph.safibank.paymenttransactionmanager.core.usecase.impl

import jakarta.inject.Singleton
import ph.safibank.paymenttransactionmanager.core.event.EventService
import ph.safibank.paymenttransactionmanager.core.exception.TransactionNotFoundException
import ph.safibank.paymenttransactionmanager.core.exception.ValidationError
import ph.safibank.paymenttransactionmanager.core.exception.ValidationException
import ph.safibank.paymenttransactionmanager.core.provider.ProviderResult
import ph.safibank.paymenttransactionmanager.core.provider.ProviderService
import ph.safibank.paymenttransactionmanager.core.transaction.PostingStatus
import ph.safibank.paymenttransactionmanager.core.transaction.ReserveTransactionRequest
import ph.safibank.paymenttransactionmanager.core.transaction.Transaction
import ph.safibank.paymenttransactionmanager.core.transaction.TransactionRepo
import ph.safibank.paymenttransactionmanager.core.transaction.TransactionStatus
import ph.safibank.paymenttransactionmanager.core.usecase.ReserveAmountSuccessUseCase
import ph.safibank.paymenttransactionmanager.util.getLogger
import java.time.Instant

@Singleton
class ReserveAmountSuccessUseCaseImpl(
    private val transactionRepo: TransactionRepo,
    private val providerServices: List<ProviderService>,
    private val eventService: EventService,
) : ReserveAmountSuccessUseCase {

    private val log = getLogger()

    override suspend fun invoke(request: ReserveTransactionRequest) {
        log.info("invoke: $request")
        val transaction = transactionRepo.getTransaction(request.transactionId) ?: throw TransactionNotFoundException("Transaction: ${request.transactionId} not found")
        validateTransaction(transaction)

        val updatedTransaction = transactionRepo.updateTransaction(
            transaction.copy(
                postingDetails = transaction.postingDetails!!.copy(
                    postingId = request.postingId,
                    postedAt = Instant.now(),
                    status = PostingStatus.RESERVED
                )
            )
        )
        log.info("send transaction reserved event $updatedTransaction")
        eventService.sendTransactionReservedEvent(updatedTransaction)

        var providerResult: ProviderResult? = null
        val transactionStatus = if (updatedTransaction.status == TransactionStatus.PENDING) {
            val providerService = providerServices.first { it.getName() == updatedTransaction.providerDetails.provider }
            log.info("Initiate Provider Service: ${providerService.getName()}")
            providerResult = providerService.initiatePayment(updatedTransaction)
            TransactionStatus.SENT_TO_PROVIDER
        } else updatedTransaction.status
        log.info("Provider Result: $providerResult")

        transactionRepo.updateTransaction(
            updatedTransaction.copy(
                status = transactionStatus,
                providerDetails = if (providerResult != null) {
                    updatedTransaction.providerDetails.copy(
                        providerId = providerResult.providerId,
                        providerStatus = providerResult.status
                    )
                } else updatedTransaction.providerDetails
            )
        ).also {
            log.info("send transaction sent to provider event $updatedTransaction")
            eventService.sendTransactionSentToProviderEvent(it)
        }
        log.info("end invoke")
    }

    private fun validateTransaction(transaction: Transaction) {
        log.info("Validate Transaction")
        val errors = listOfNotNull(
            when (transaction.status) {
                TransactionStatus.PENDING, TransactionStatus.WAITING_FOR_APPROVAL -> null
                else -> ValidationError("Transaction (${transaction.id}) Status should be ${TransactionStatus.PENDING.name} or ${TransactionStatus.WAITING_FOR_APPROVAL.name}")
            },
            if (transaction.postingDetails == null) {
                ValidationError("Transaction (${transaction.id}) should have posting details")
            } else null,
        )

        if (errors.isNotEmpty()) {
            log.error("Validation Errors: $errors")
            throw ValidationException(errors)
        }
    }
}
