package ph.safibank.paymenttransactionmanager.api.messaging

import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.clearAllMocks
import io.mockk.coJustRun
import io.mockk.mockk
import jakarta.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import ph.safibank.common.testutils.kafka.UseKafka
import ph.safibank.paymenttransactionmanager.TestUtils
import ph.safibank.paymenttransactionmanager.core.usecase.HandleCustomerFeeUpdateUseCase
import ph.safibank.tm.model.transaction.AuthorisationAdjustmentByReplacementRuntimeParams
import ph.safibank.tm.model.transaction.AuthorisationAdjustmentRuntimeParams
import ph.safibank.tm.model.transaction.BillPaymentCustomerFee
import ph.safibank.tm.model.transaction.BillPaymentCustomerFeeAdjustment
import ph.safibank.tm.model.transaction.BillPaymentCustomerFeeAdjustmentByReplacement
import ph.safibank.tm.model.transaction.BillPaymentCustomerFeeRelease
import ph.safibank.tm.model.transaction.BillPaymentCustomerFeeSettlement
import ph.safibank.tm.model.transaction.DigitalGoodsCustomerFee
import ph.safibank.tm.model.transaction.DigitalGoodsCustomerFeeAdjustment
import ph.safibank.tm.model.transaction.DigitalGoodsCustomerFeeAdjustmentByReplacement
import ph.safibank.tm.model.transaction.DigitalGoodsCustomerFeeRelease
import ph.safibank.tm.model.transaction.DigitalGoodsCustomerFeeSettlement
import ph.safibank.tm.model.transaction.OutboundAuthorisationRuntimeParams
import ph.safibank.tm.model.transaction.ReleaseRuntimeParams
import ph.safibank.tm.model.transaction.SettlementRuntimeParams
import java.util.UUID
import kotlin.reflect.KClass
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.primaryConstructor

@Suppress("UNUSED")
@ExperimentalCoroutinesApi
@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@UseKafka
// @DBRider
internal class TMCustomerFeeResponseHandlerTest {
    @MockBean(HandleCustomerFeeUpdateUseCase::class)
    fun handleCustomerFeeUpdateUseCaseMock() = mockk<HandleCustomerFeeUpdateUseCase>()

    @Inject
    private lateinit var handleCustomerFeeUpdateUseCaseMock: HandleCustomerFeeUpdateUseCase

    @Inject
    private lateinit var listenerHandler: TMCustomerFeeResponseHandler

    private val transactionId = UUID.fromString("c4103d99-c601-4ccc-a1c4-10a7c5d6d817")

    enum class CustomerFeeType(
        val transactionClass: KClass<*>,
        val listenerClass: KClass<*>,
    ) {
        BILL_PAYMENT(
            BillPaymentCustomerFee::class,
            BillPaymentCustomerFeeListener::class,
        ),
        DIGITAL_GOODS(
            DigitalGoodsCustomerFee::class,
            DigitalGoodsCustomerFeeListener::class,
        ),
    }

    enum class CustomerFeeAdjustmentType(
        val transactionClass: KClass<*>,
        val listenerClass: KClass<*>,
    ) {
        BILL_PAYMENT_ADJUSTMENT(
            BillPaymentCustomerFeeAdjustment::class,
            BillPaymentCustomerFeeAdjustmentListener::class,
        ),
        DIGITAL_GOODS_ADJUSTMENT(
            DigitalGoodsCustomerFeeAdjustment::class,
            DigitalGoodsCustomerFeeAdjustmentListener::class,
        ),
    }

    enum class CustomerFeeAdjustmentByReplacementType(
        val transactionClass: KClass<*>,
        val listenerClass: KClass<*>,
    ) {
        BILL_PAYMENT_ADJUSTMENT_BY_REPLACEMENT(
            BillPaymentCustomerFeeAdjustmentByReplacement::class,
            BillPaymentCustomerFeeAdjustmentByReplacementListener::class,
        ),
        DIGITAL_GOODS_ADJUSTMENT_BY_REPLACEMENT(
            DigitalGoodsCustomerFeeAdjustmentByReplacement::class,
            DigitalGoodsCustomerFeeAdjustmentByReplacementListener::class,
        ),
    }

    enum class CustomerFeeReleaseType(
        val transactionClass: KClass<*>,
        val listenerClass: KClass<*>,
    ) {
        BILL_PAYMENT_RELEASE(
            BillPaymentCustomerFeeRelease::class,
            BillPaymentCustomerFeeReleaseListener::class,
        ),
        DIGITAL_GOODS_RELEASE(
            DigitalGoodsCustomerFeeRelease::class,
            DigitalGoodsCustomerFeeReleaseListener::class,
        ),
    }

    enum class CustomerFeeSettlementType(
        val transactionClass: KClass<*>,
        val listenerClass: KClass<*>,
    ) {
        BILL_PAYMENT_SETTLEMENT(
            BillPaymentCustomerFeeSettlement::class,
            BillPaymentCustomerFeeSettlementListener::class,
        ),
        DIGITAL_GOODS_SETTLEMENT(
            DigitalGoodsCustomerFeeSettlement::class,
            DigitalGoodsCustomerFeeSettlementListener::class,
        ),
    }

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        coJustRun {
            handleCustomerFeeUpdateUseCaseMock.invoke(any())
        }
    }

    @ParameterizedTest
    // todo dataset
    @EnumSource(CustomerFeeType::class)
    fun `should update transaction when customer fee updated`(type: CustomerFeeType) = runTest {
        val message = type.transactionClass.primaryConstructor!!.call(
            OutboundAuthorisationRuntimeParams(
                amount = TestUtils.defaultTransaction.amount,
                accountId = TestUtils.defaultTransaction.senderDetails.accountId,
                denomination = TestUtils.defaultTransaction.currency,
                clientTransactionId = transactionId.toString(),
            )
        )

        val listener = type.listenerClass.primaryConstructor!!.call(listenerHandler)
        val process = type.listenerClass.memberFunctions.find { it.name == "process" }

        process!!.call(listener, TestUtils.defaultInstructionMetadata, message)
    }

    @ParameterizedTest
    // todo dataset
    @EnumSource(CustomerFeeAdjustmentType::class)
    fun `should update transaction when customer fee updated`(type: CustomerFeeAdjustmentType) = runTest {
        val message = type.transactionClass.primaryConstructor!!.call(
            AuthorisationAdjustmentRuntimeParams(
                amount = TestUtils.defaultTransaction.amount,
                clientTransactionId = transactionId.toString(),
            )
        )

        val listener = type.listenerClass.primaryConstructor!!.call(listenerHandler)
        val process = type.listenerClass.memberFunctions.find { it.name == "process" }

        process!!.call(listener, TestUtils.defaultInstructionMetadata, message)
    }

    @ParameterizedTest
    // todo dataset
    @EnumSource(CustomerFeeAdjustmentByReplacementType::class)
    fun `should update transaction when customer fee updated`(type: CustomerFeeAdjustmentByReplacementType) = runTest {
        val message = type.transactionClass.primaryConstructor!!.call(
            AuthorisationAdjustmentByReplacementRuntimeParams(
                replacementAmount = TestUtils.defaultTransaction.amount,
                clientTransactionId = transactionId.toString(),
            )
        )

        val listener = type.listenerClass.primaryConstructor!!.call(listenerHandler)
        val process = type.listenerClass.memberFunctions.find { it.name == "process" }

        process!!.call(listener, TestUtils.defaultInstructionMetadata, message)
    }

    @ParameterizedTest
    // todo dataset
    @EnumSource(CustomerFeeReleaseType::class)
    fun `should update transaction when customer fee updated`(type: CustomerFeeReleaseType) = runTest {
        val message = type.transactionClass.primaryConstructor!!.call(
            ReleaseRuntimeParams(
                clientTransactionId = transactionId.toString()
            )
        )

        val listener = type.listenerClass.primaryConstructor!!.call(listenerHandler)
        val process = type.listenerClass.memberFunctions.find { it.name == "process" }

        process!!.call(listener, TestUtils.defaultInstructionMetadata, message)
    }

    @ParameterizedTest
    // todo dataset
    @EnumSource(CustomerFeeSettlementType::class)
    fun `should update transaction when customer fee updated`(type: CustomerFeeSettlementType) = runTest {
        val message = type.transactionClass.primaryConstructor!!.call(
            SettlementRuntimeParams(
                clientTransactionId = transactionId.toString()
            )
        )

        val listener = type.listenerClass.primaryConstructor!!.call(listenerHandler)
        val process = type.listenerClass.memberFunctions.find { it.name == "process" }

        process!!.call(listener, TestUtils.defaultInstructionMetadata, message)
    }
}
