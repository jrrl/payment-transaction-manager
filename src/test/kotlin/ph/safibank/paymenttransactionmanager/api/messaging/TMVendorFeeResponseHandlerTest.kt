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
import ph.safibank.paymenttransactionmanager.core.usecase.HandleVendorFeeUpdateUseCase
import ph.safibank.tm.model.transaction.BillPaymentVendorFeeECPay
import ph.safibank.tm.model.transaction.BillPaymentVendorFeeECPayReversal
import ph.safibank.tm.model.transaction.BillPaymentVendorFeePaynamics
import ph.safibank.tm.model.transaction.BillPaymentVendorFeePaynamicsReversal
import ph.safibank.tm.model.transaction.CustomInternalToInternalRuntimeParams
import java.math.BigDecimal
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
internal class TMVendorFeeResponseHandlerTest {
    @MockBean(HandleVendorFeeUpdateUseCase::class)
    fun handleVendorFeeUpdateUseCaseMock() = mockk<HandleVendorFeeUpdateUseCase>()

    @Inject
    private lateinit var handleVendorFeeUpdateUseCaseMock: HandleVendorFeeUpdateUseCase

    @Inject
    private lateinit var listenerHandler: TMVendorFeeResponseHandler

    private val transactionId = UUID.fromString("c4103d99-c601-4ccc-a1c4-10a7c5d6d817")

    enum class VendorFeeType(
        val transactionClass: KClass<*>,
        val listenerClass: KClass<*>,
    ) {
        EC_PAY(
            BillPaymentVendorFeeECPay::class,
            BillPaymentVendorFeeECPayListener::class,
        ),
        PAYNAMICS(
            BillPaymentVendorFeePaynamics::class,
            BillPaymentVendorFeePaynamicsListener::class,
        ),
    }

    enum class VendorFeeReversalType(
        val transactionClass: KClass<*>,
        val listenerClass: KClass<*>,
    ) {
        EC_PAY(
            BillPaymentVendorFeeECPayReversal::class,
            BillPaymentVendorFeeECPayReversalListener::class,
        ),
        PAYNAMICS(
            BillPaymentVendorFeePaynamicsReversal::class,
            BillPaymentVendorFeePaynamicsReversalListener::class,
        ),
    }

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        coJustRun {
            handleVendorFeeUpdateUseCaseMock.invoke(any())
        }
    }

    @ParameterizedTest
    // todo dataset
    @EnumSource(VendorFeeType::class)
    fun `should update transaction when customer fee updated`(type: VendorFeeType) = runTest {
        val message = type.transactionClass.primaryConstructor!!.call(
            CustomInternalToInternalRuntimeParams(
                clientTransactionId = transactionId.toString(),
                amount = BigDecimal.TEN,
                denomination = "PHP",
            )
        )

        val listener = type.listenerClass.primaryConstructor!!.call(listenerHandler)
        val process = type.listenerClass.memberFunctions.find { it.name == "process" }

        process!!.call(listener, TestUtils.defaultInstructionMetadata, message)
    }

    @ParameterizedTest
    // todo dataset
    @EnumSource(VendorFeeReversalType::class)
    fun `should update transaction when customer fee updated`(type: VendorFeeReversalType) = runTest {
        val message = type.transactionClass.primaryConstructor!!.call(
            UUID.randomUUID(),
            UUID.randomUUID(),
            CustomInternalToInternalRuntimeParams(
                clientTransactionId = transactionId.toString(),
                amount = BigDecimal.TEN,
                denomination = "PHP",
            )
        )

        val listener = type.listenerClass.primaryConstructor!!.call(listenerHandler)
        val process = type.listenerClass.memberFunctions.find { it.name == "process" }

        process!!.call(listener, TestUtils.defaultInstructionMetadata, message)
    }
}
