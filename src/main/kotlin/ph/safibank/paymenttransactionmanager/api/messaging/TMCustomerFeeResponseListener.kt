package ph.safibank.paymenttransactionmanager.api.messaging

import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import ph.safibank.common.idempotency.core.IdempotencyService
import ph.safibank.paymenttransactionmanager.core.transaction.CustomerFeeUpdateResult
import ph.safibank.paymenttransactionmanager.core.usecase.HandleCustomerFeeUpdateUseCase
import ph.safibank.tm.listener.handler.BillPaymentCustomerFeeAdjustmentByReplacementHandler
import ph.safibank.tm.listener.handler.BillPaymentCustomerFeeAdjustmentHandler
import ph.safibank.tm.listener.handler.BillPaymentCustomerFeeHandler
import ph.safibank.tm.listener.handler.BillPaymentCustomerFeeReleaseHandler
import ph.safibank.tm.listener.handler.BillPaymentCustomerFeeSettlementHandler
import ph.safibank.tm.listener.handler.DigitalGoodsCustomerFeeAdjustmentByReplacementHandler
import ph.safibank.tm.listener.handler.DigitalGoodsCustomerFeeAdjustmentHandler
import ph.safibank.tm.listener.handler.DigitalGoodsCustomerFeeHandler
import ph.safibank.tm.listener.handler.DigitalGoodsCustomerFeeReleaseHandler
import ph.safibank.tm.listener.handler.DigitalGoodsCustomerFeeSettlementHandler
import ph.safibank.tm.model.InstructionMetadata
import ph.safibank.tm.model.transaction.BillPaymentCustomerFee
import ph.safibank.tm.model.transaction.BillPaymentCustomerFeeAdjustment
import ph.safibank.tm.model.transaction.BillPaymentCustomerFeeAdjustmentByReplacement
import ph.safibank.tm.model.transaction.BillPaymentCustomerFeeRelease
import ph.safibank.tm.model.transaction.BillPaymentCustomerFeeSettlement
import ph.safibank.tm.model.transaction.CoreTransaction
import ph.safibank.tm.model.transaction.DigitalGoodsCustomerFee
import ph.safibank.tm.model.transaction.DigitalGoodsCustomerFeeAdjustment
import ph.safibank.tm.model.transaction.DigitalGoodsCustomerFeeAdjustmentByReplacement
import ph.safibank.tm.model.transaction.DigitalGoodsCustomerFeeRelease
import ph.safibank.tm.model.transaction.DigitalGoodsCustomerFeeSettlement

@Singleton
class TMCustomerFeeResponseHandler(
    private val handleCustomerFeeUpdateUseCase: HandleCustomerFeeUpdateUseCase,
    private val idempotencyService: IdempotencyService,
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    fun process(
        instructionMetadata: InstructionMetadata,
        transaction: CoreTransaction,
    ): Unit = runBlocking {
        val postingBatchId = instructionMetadata.batchId

        val customerFeeUpdateResult = CustomerFeeUpdateResult(
            id = transaction.toPostingInstruction().clientTransactionId,
            postingId = instructionMetadata.requestId, // TODO: or instructionMetadata.instructionId
            batchId = postingBatchId,
        )

        log.info("Received customer fee posting update $postingBatchId: $customerFeeUpdateResult")

        idempotencyService.runFlow("tm-response-listener-flow", instructionMetadata.requestId) { ctx ->
            ctx.phase("payment-customer-fee-update") {
                runBlocking {
                    handleCustomerFeeUpdateUseCase.invoke(customerFeeUpdateResult)
                }
            }
        }
    }
}

@Singleton
class BillPaymentCustomerFeeListener(
    private val handler: TMCustomerFeeResponseHandler,
) : BillPaymentCustomerFeeHandler() {
    override fun process(
        instructionMetadata: InstructionMetadata,
        transaction: BillPaymentCustomerFee
    ) {
        handler.process(instructionMetadata, transaction)
    }
}

@Singleton
class BillPaymentCustomerFeeAdjustmentListener(
    private val handler: TMCustomerFeeResponseHandler,
) : BillPaymentCustomerFeeAdjustmentHandler() {
    override fun process(
        instructionMetadata: InstructionMetadata,
        transaction: BillPaymentCustomerFeeAdjustment
    ) {
        handler.process(instructionMetadata, transaction)
    }
}

@Singleton
class BillPaymentCustomerFeeAdjustmentByReplacementListener(
    private val handler: TMCustomerFeeResponseHandler,
) : BillPaymentCustomerFeeAdjustmentByReplacementHandler() {
    override fun process(
        instructionMetadata: InstructionMetadata,
        transaction: BillPaymentCustomerFeeAdjustmentByReplacement
    ) {
        handler.process(instructionMetadata, transaction)
    }
}

@Singleton
class BillPaymentCustomerFeeReleaseListener(
    private val handler: TMCustomerFeeResponseHandler,
) : BillPaymentCustomerFeeReleaseHandler() {
    override fun process(
        instructionMetadata: InstructionMetadata,
        transaction: BillPaymentCustomerFeeRelease
    ) {
        handler.process(instructionMetadata, transaction)
    }
}

@Singleton
class BillPaymentCustomerFeeSettlementListener(
    private val handler: TMCustomerFeeResponseHandler,
) : BillPaymentCustomerFeeSettlementHandler() {
    override fun process(
        instructionMetadata: InstructionMetadata,
        transaction: BillPaymentCustomerFeeSettlement
    ) {
        handler.process(instructionMetadata, transaction)
    }
}

@Singleton
class DigitalGoodsCustomerFeeListener(
    private val handler: TMCustomerFeeResponseHandler,
) : DigitalGoodsCustomerFeeHandler() {
    override fun process(
        instructionMetadata: InstructionMetadata,
        transaction: DigitalGoodsCustomerFee
    ) {
        handler.process(instructionMetadata, transaction)
    }
}

@Singleton
class DigitalGoodsCustomerFeeAdjustmentListener(
    private val handler: TMCustomerFeeResponseHandler,
) : DigitalGoodsCustomerFeeAdjustmentHandler() {
    override fun process(
        instructionMetadata: InstructionMetadata,
        transaction: DigitalGoodsCustomerFeeAdjustment
    ) {
        handler.process(instructionMetadata, transaction)
    }
}

@Singleton
class DigitalGoodsCustomerFeeAdjustmentByReplacementListener(
    private val handler: TMCustomerFeeResponseHandler,
) : DigitalGoodsCustomerFeeAdjustmentByReplacementHandler() {
    override fun process(
        instructionMetadata: InstructionMetadata,
        transaction: DigitalGoodsCustomerFeeAdjustmentByReplacement
    ) {
        handler.process(instructionMetadata, transaction)
    }
}

@Singleton
class DigitalGoodsCustomerFeeReleaseListener(
    private val handler: TMCustomerFeeResponseHandler,
) : DigitalGoodsCustomerFeeReleaseHandler() {
    override fun process(
        instructionMetadata: InstructionMetadata,
        transaction: DigitalGoodsCustomerFeeRelease
    ) {
        handler.process(instructionMetadata, transaction)
    }
}

@Singleton
class DigitalGoodsCustomerFeeSettlementListener(
    private val handler: TMCustomerFeeResponseHandler,
) : DigitalGoodsCustomerFeeSettlementHandler() {
    override fun process(
        instructionMetadata: InstructionMetadata,
        transaction: DigitalGoodsCustomerFeeSettlement
    ) {
        handler.process(instructionMetadata, transaction)
    }
}
