package ph.safibank.paymenttransactionmanager.api.messaging

import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import ph.safibank.common.idempotency.core.IdempotencyService
import ph.safibank.paymenttransactionmanager.core.transaction.ReleaseTransactionResult
import ph.safibank.paymenttransactionmanager.core.usecase.ReleaseTransactionUseCase
import ph.safibank.tm.listener.handler.BillPaymentOutboundAuthorisationECPayReleaseHandler
import ph.safibank.tm.listener.handler.BillPaymentOutboundAuthorisationPaynamicsReleaseHandler
import ph.safibank.tm.listener.handler.DigitalGoodsOutboundAuthorisationPaynamicsReleaseHandler
import ph.safibank.tm.model.InstructionMetadata
import ph.safibank.tm.model.transaction.BillPaymentOutboundAuthorisationECPayRelease
import ph.safibank.tm.model.transaction.BillPaymentOutboundAuthorisationPaynamicsRelease
import ph.safibank.tm.model.transaction.DigitalGoodsOutboundAuthorisationPaynamicsRelease
import ph.safibank.tm.model.transaction.Release
import java.util.UUID

@Singleton
class TMReleaseResponseHandler(
    private val releaseTransactionUseCase: ReleaseTransactionUseCase,
    private val idempotencyService: IdempotencyService,
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    fun process(
        instructionMetadata: InstructionMetadata,
        transaction: Release,
    ): Unit = runBlocking {
        val transactionId = transaction.toPostingInstruction().clientTransactionId

        val releaseTransactionResult = ReleaseTransactionResult(
            postingId = UUID.fromString(instructionMetadata.requestId),
            transactionId = UUID.fromString(transactionId),
        )

        log.info("Received release transaction response $transactionId: $releaseTransactionResult")

        idempotencyService.runFlow("tm-response-listener-flow", instructionMetadata.requestId) { ctx ->
            ctx.phase("payment-release-amount") {
                runBlocking { // todo do i need context here?
                    releaseTransactionUseCase.invoke(releaseTransactionResult)
                }
            }
        }
    }
}

@Singleton
class BillPaymentOutboundAuthorisationECPayReleaseListener(
    private val handler: TMReleaseResponseHandler,
) : BillPaymentOutboundAuthorisationECPayReleaseHandler() {
    override fun process(
        instructionMetadata: InstructionMetadata,
        transaction: BillPaymentOutboundAuthorisationECPayRelease
    ) {
        handler.process(instructionMetadata, transaction)
    }
}

@Singleton
class BillPaymentOutboundAuthorisationPaynamicsReleaseListener(
    private val handler: TMReleaseResponseHandler,
) : BillPaymentOutboundAuthorisationPaynamicsReleaseHandler() {
    override fun process(
        instructionMetadata: InstructionMetadata,
        transaction: BillPaymentOutboundAuthorisationPaynamicsRelease
    ) {
        handler.process(instructionMetadata, transaction)
    }
}

@Singleton
class DigitalGoodsOutboundAuthorisationPaynamicsReleaseListener(
    private val handler: TMReleaseResponseHandler,
) : DigitalGoodsOutboundAuthorisationPaynamicsReleaseHandler() {
    override fun process(
        instructionMetadata: InstructionMetadata,
        transaction: DigitalGoodsOutboundAuthorisationPaynamicsRelease
    ) {
        handler.process(instructionMetadata, transaction)
    }
}
