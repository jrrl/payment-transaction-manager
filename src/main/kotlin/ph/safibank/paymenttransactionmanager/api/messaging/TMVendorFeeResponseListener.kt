package ph.safibank.paymenttransactionmanager.api.messaging

import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import ph.safibank.common.idempotency.core.IdempotencyService
import ph.safibank.paymenttransactionmanager.core.transaction.VendorFeeUpdateResult
import ph.safibank.paymenttransactionmanager.core.usecase.HandleVendorFeeUpdateUseCase
import ph.safibank.tm.listener.handler.BillPaymentVendorFeeECPayHandler
import ph.safibank.tm.listener.handler.BillPaymentVendorFeeECPayReversalHandler
import ph.safibank.tm.listener.handler.BillPaymentVendorFeePaynamicsHandler
import ph.safibank.tm.listener.handler.BillPaymentVendorFeePaynamicsReversalHandler
import ph.safibank.tm.model.InstructionMetadata
import ph.safibank.tm.model.transaction.BillPaymentVendorFeeECPay
import ph.safibank.tm.model.transaction.BillPaymentVendorFeeECPayReversal
import ph.safibank.tm.model.transaction.BillPaymentVendorFeePaynamics
import ph.safibank.tm.model.transaction.BillPaymentVendorFeePaynamicsReversal
import ph.safibank.tm.model.transaction.CoreTransaction

@Singleton
class TMVendorFeeResponseHandler(
    private val handleVendorFeeUpdateUseCase: HandleVendorFeeUpdateUseCase,
    private val idempotencyService: IdempotencyService,
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    fun process(
        instructionMetadata: InstructionMetadata,
        transaction: CoreTransaction,
    ): Unit = runBlocking {
        val postingBatchId = instructionMetadata.batchId

        val vendorFeeUpdateResult = VendorFeeUpdateResult(
            id = transaction.toPostingInstruction().clientTransactionId,
            postingId = instructionMetadata.requestId, // TODO: or instructionMetadata.instructionId
            batchId = postingBatchId,
        )

        log.info("Received vendor fee posting update $postingBatchId: $vendorFeeUpdateResult")

        idempotencyService.runFlow("tm-response-listener-flow", instructionMetadata.requestId) { ctx ->
            ctx.phase("payment-vendor-fee-update") {
                runBlocking {
                    handleVendorFeeUpdateUseCase.invoke(vendorFeeUpdateResult)
                }
            }
        }
    }
}

@Singleton
class BillPaymentVendorFeeECPayListener(
    private val handler: TMVendorFeeResponseHandler,
) : BillPaymentVendorFeeECPayHandler() {
    override fun process(
        instructionMetadata: InstructionMetadata,
        transaction: BillPaymentVendorFeeECPay
    ) {
        handler.process(instructionMetadata, transaction)
    }
}

@Singleton
class BillPaymentVendorFeePaynamicsListener(
    private val handler: TMVendorFeeResponseHandler,
) : BillPaymentVendorFeePaynamicsHandler() {
    override fun process(
        instructionMetadata: InstructionMetadata,
        transaction: BillPaymentVendorFeePaynamics
    ) {
        handler.process(instructionMetadata, transaction)
    }
}

@Singleton
class BillPaymentVendorFeeECPayReversalListener(
    private val handler: TMVendorFeeResponseHandler,
) : BillPaymentVendorFeeECPayReversalHandler() {
    override fun process(
        instructionMetadata: InstructionMetadata,
        transaction: BillPaymentVendorFeeECPayReversal
    ) {
        handler.process(instructionMetadata, transaction)
    }
}

@Singleton
class BillPaymentVendorFeePaynamicsReversalListener(
    private val handler: TMVendorFeeResponseHandler,
) : BillPaymentVendorFeePaynamicsReversalHandler() {
    override fun process(
        instructionMetadata: InstructionMetadata,
        transaction: BillPaymentVendorFeePaynamicsReversal
    ) {
        handler.process(instructionMetadata, transaction)
    }
}
