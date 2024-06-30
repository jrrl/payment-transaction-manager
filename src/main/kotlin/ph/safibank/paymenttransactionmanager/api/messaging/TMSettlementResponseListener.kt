package ph.safibank.paymenttransactionmanager.api.messaging

import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import ph.safibank.common.idempotency.core.IdempotencyService
import ph.safibank.paymenttransactionmanager.core.transaction.SettleTransactionResult
import ph.safibank.paymenttransactionmanager.core.usecase.SettleTransactionUseCase
import ph.safibank.tm.listener.handler.BillPaymentOutboundAuthorisationECPaySettlementHandler
import ph.safibank.tm.listener.handler.BillPaymentOutboundAuthorisationPaynamicsSettlementHandler
import ph.safibank.tm.listener.handler.DigitalGoodsOutboundAuthorisationPaynamicsSettlementHandler
import ph.safibank.tm.model.InstructionMetadata
import ph.safibank.tm.model.transaction.BillPaymentOutboundAuthorisationECPaySettlement
import ph.safibank.tm.model.transaction.BillPaymentOutboundAuthorisationPaynamicsSettlement
import ph.safibank.tm.model.transaction.DigitalGoodsOutboundAuthorisationPaynamicsSettlement
import ph.safibank.tm.model.transaction.Settlement
import java.util.UUID

@Singleton
class TMSettlementResponseHandler(
    private val settleTransactionUseCase: SettleTransactionUseCase,
    private val idempotencyService: IdempotencyService,
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    fun process(
        instructionMetadata: InstructionMetadata,
        transaction: Settlement,
    ): Unit = runBlocking {
        val transactionId = transaction.toPostingInstruction().clientTransactionId

        val settleTransactionResult = SettleTransactionResult(
            postingId = UUID.fromString(instructionMetadata.requestId),
            transactionId = UUID.fromString(transactionId),
        )

        log.info("Received settlement transaction response $transactionId: $settleTransactionResult")

        idempotencyService.runFlow("tm-response-listener-flow", instructionMetadata.requestId) { ctx ->
            ctx.phase("payment-settle-amount") {
                runBlocking { // todo do i need context here?
                    settleTransactionUseCase.invoke(settleTransactionResult)
                }
            }
        }
    }
}

@Singleton
class BillPaymentOutboundAuthorisationECPaySettlementListener(
    private val handler: TMSettlementResponseHandler,
) : BillPaymentOutboundAuthorisationECPaySettlementHandler() {
    override fun process(
        instructionMetadata: InstructionMetadata,
        transaction: BillPaymentOutboundAuthorisationECPaySettlement
    ) {
        handler.process(instructionMetadata, transaction)
    }
}

@Singleton
class BillPaymentOutboundAuthorisationPaynamicsSettlementListener(
    private val handler: TMSettlementResponseHandler,
) : BillPaymentOutboundAuthorisationPaynamicsSettlementHandler() {
    override fun process(
        instructionMetadata: InstructionMetadata,
        transaction: BillPaymentOutboundAuthorisationPaynamicsSettlement
    ) {
        handler.process(instructionMetadata, transaction)
    }
}

@Singleton
class DigitalGoodsOutboundAuthorisationPaynamicsSettlementListener(
    private val handler: TMSettlementResponseHandler,
) : DigitalGoodsOutboundAuthorisationPaynamicsSettlementHandler() {
    override fun process(
        instructionMetadata: InstructionMetadata,
        transaction: DigitalGoodsOutboundAuthorisationPaynamicsSettlement
    ) {
        handler.process(instructionMetadata, transaction)
    }
}
