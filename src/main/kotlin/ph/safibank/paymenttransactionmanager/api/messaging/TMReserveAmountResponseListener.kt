package ph.safibank.paymenttransactionmanager.api.messaging

import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import ph.safibank.common.idempotency.core.IdempotencyService
import ph.safibank.paymenttransactionmanager.core.exception.ValidationError
import ph.safibank.paymenttransactionmanager.core.exception.ValidationException
import ph.safibank.paymenttransactionmanager.core.transaction.ReserveTransactionRequest
import ph.safibank.paymenttransactionmanager.core.usecase.ReserveAmountFailedUseCase
import ph.safibank.paymenttransactionmanager.core.usecase.ReserveAmountSuccessUseCase
import ph.safibank.tm.listener.handler.BillPaymentOutboundAuthorisationECPayHandler
import ph.safibank.tm.listener.handler.BillPaymentOutboundAuthorisationPaynamicsHandler
import ph.safibank.tm.listener.handler.DigitalGoodsOutboundAuthorisationPaynamicsHandler
import ph.safibank.tm.model.InstructionMetadata
import ph.safibank.tm.model.PostingBatchStatus
import ph.safibank.tm.model.transaction.BillPaymentOutboundAuthorisationECPay
import ph.safibank.tm.model.transaction.BillPaymentOutboundAuthorisationPaynamics
import ph.safibank.tm.model.transaction.DigitalGoodsOutboundAuthorisationPaynamics
import ph.safibank.tm.model.transaction.OutboundAuthorisation
import java.util.UUID

@Singleton
class TMReserveAmountResponseHandler(
    private val reserveAmountSuccessUseCase: ReserveAmountSuccessUseCase,
    private val reserveAmountFailedUseCase: ReserveAmountFailedUseCase,
    private val idempotencyService: IdempotencyService,
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    fun process(
        instructionMetadata: InstructionMetadata,
        transaction: OutboundAuthorisation,
    ): Unit = runBlocking {
        val transactionId = transaction.toPostingInstruction().clientTransactionId

        val reserveTransactionResult = ReserveTransactionRequest(
            postingId = UUID.fromString(instructionMetadata.requestId),
            transactionId = UUID.fromString(transactionId),
        )

        log.info("Received reserve transaction response $transactionId: $reserveTransactionResult")

        idempotencyService.runFlow("tm-response-listener-flow", instructionMetadata.requestId) { ctx ->
            ctx.phase("payment-reserve-amount") {
                runBlocking { // todo do i need context here?
                    when (instructionMetadata.batchStatus) {
                        PostingBatchStatus.ACCEPTED -> reserveAmountSuccessUseCase.invoke(reserveTransactionResult)
                        PostingBatchStatus.REJECTED -> reserveAmountFailedUseCase.invoke(reserveTransactionResult)
                        else -> throw ValidationException(
                            listOf(ValidationError("Invalid batch status for transaction $transactionId"))
                        )
                    }
                }
            }
        }
    }
}

@Singleton
class BillPaymentOutboundAuthorisationECPayListener(
    private val handler: TMReserveAmountResponseHandler,
) : BillPaymentOutboundAuthorisationECPayHandler() {
    override fun process(
        instructionMetadata: InstructionMetadata,
        transaction: BillPaymentOutboundAuthorisationECPay
    ) {
        handler.process(instructionMetadata, transaction)
    }
}

@Singleton
class BillPaymentOutboundAuthorisationPaynamicsListener(
    private val handler: TMReserveAmountResponseHandler,
) : BillPaymentOutboundAuthorisationPaynamicsHandler() {
    override fun process(
        instructionMetadata: InstructionMetadata,
        transaction: BillPaymentOutboundAuthorisationPaynamics
    ) {
        handler.process(instructionMetadata, transaction)
    }
}

@Singleton
class DigitalGoodsOutboundAuthorisationPaynamicsListener(
    private val handler: TMReserveAmountResponseHandler,
) : DigitalGoodsOutboundAuthorisationPaynamicsHandler() {
    override fun process(
        instructionMetadata: InstructionMetadata,
        transaction: DigitalGoodsOutboundAuthorisationPaynamics
    ) {
        handler.process(instructionMetadata, transaction)
    }
}
