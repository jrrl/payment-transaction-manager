package ph.safibank.paymenttransactionmanager.api.messaging

import com.github.database.rider.core.api.dataset.DataSet
import com.github.database.rider.core.api.dataset.ExpectedDataSet
import com.github.database.rider.junit5.api.DBRider
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockk
import jakarta.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import ph.safibank.common.testutils.kafka.UseKafka
import ph.safibank.common.testutils.wiremock.WireMockStubFor
import ph.safibank.paymenttransactionmanager.TestUtils.defaultInstructionMetadata
import ph.safibank.paymenttransactionmanager.TestUtils.defaultTransaction
import ph.safibank.paymenttransactionmanager.core.event.EventService
import ph.safibank.paymenttransactionmanager.core.provider.ProviderResult
import ph.safibank.paymenttransactionmanager.core.provider.ProviderService
import ph.safibank.paymenttransactionmanager.core.provider.ProviderStatus
import ph.safibank.tm.model.PostingBatchStatus
import ph.safibank.tm.model.transaction.OutboundAuthorisationRuntimeParams
import java.util.UUID
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.primaryConstructor

@ExperimentalCoroutinesApi
@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@WireMockStubFor(service = "paynamics-gateway", exposedUrl = "SAFI_PAYNAMICS_GATEWAY_URL")
@WireMockStubFor(service = "product-manager", exposedUrl = "SAFI_PRODUCT_MANAGER_URL")
@UseKafka
@DBRider
class TMReserveAmountResponseHandlerTest {
    @Inject
    private lateinit var listenerHandler: TMReserveAmountResponseHandler

    // todo remove this when providerService is implemented
    @MockBean(EventService::class)
    fun providerServiceA() = mockk<ProviderService>()

    @Inject
    private lateinit var providerServiceA: ProviderService

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        coEvery { providerServiceA.getName() } returns defaultTransaction.providerDetails.provider
        coEvery { providerServiceA.initiatePayment(any()) } returns ProviderResult(
            transactionId = transactionId,
            providerId = UUID.randomUUID().toString(),
            status = ProviderStatus.PENDING
        )
    }

    private val transactionId = UUID.fromString("c4103d99-c601-4ccc-a1c4-10a7c5d6d817")

    @ParameterizedTest
    @DataSet(value = ["payment-pending.yaml"], cleanBefore = true)
    @ExpectedDataSet("expected-after-payment-is-reserved.yaml")
    @EnumSource(PaymentType::class)
    fun `should set status to SUCCESS when transaction reservation is success`(type: PaymentType) = runTest {
        val message = type.reserveClass.primaryConstructor!!.call(
            "",
            "",
            "",
            "",
            OutboundAuthorisationRuntimeParams(
                amount = defaultTransaction.amount,
                accountId = defaultTransaction.senderDetails.accountId,
                denomination = defaultTransaction.currency,
                clientTransactionId = transactionId.toString(),
            ),
        )

        val listener = type.reserveListenerClass.primaryConstructor!!.call(listenerHandler)
        val process = type.reserveListenerClass.memberFunctions.find { it.name == "process" }

        process!!.call(listener, defaultInstructionMetadata, message)
    }

    @ParameterizedTest
    @DataSet(value = ["payment-pending.yaml"], cleanBefore = true)
    @ExpectedDataSet("expected-after-payment-is-reserved.yaml")
    @EnumSource(PaymentType::class)
    fun `should return the same result when same request id is sent`(type: PaymentType) = runTest {
        val message = type.reserveClass.primaryConstructor!!.call(
            "",
            "",
            "",
            "",
            OutboundAuthorisationRuntimeParams(
                amount = defaultTransaction.amount,
                accountId = defaultTransaction.senderDetails.accountId,
                denomination = defaultTransaction.currency,
                clientTransactionId = transactionId.toString(),
            ),
        )

        val listener = type.reserveListenerClass.primaryConstructor!!.call(listenerHandler)
        val process = type.reserveListenerClass.memberFunctions.find { it.name == "process" }

        process!!.call(listener, defaultInstructionMetadata, message)
        process.call(listener, defaultInstructionMetadata, message)
    }

    @ParameterizedTest
    @DataSet(value = ["payment-pending.yaml"], cleanBefore = true)
    @ExpectedDataSet("expected-after-payment-failed-reserved.yaml")
    @EnumSource(PaymentType::class)
    fun `should set status to FAILED when transaction reservation is failed`(type: PaymentType) = runTest {
        val message = type.reserveClass.primaryConstructor!!.call(
            "",
            "",
            "",
            "",
            OutboundAuthorisationRuntimeParams(
                amount = defaultTransaction.amount,
                accountId = defaultTransaction.senderDetails.accountId,
                denomination = defaultTransaction.currency,
                clientTransactionId = transactionId.toString(),
            ),
        )

        val args = defaultInstructionMetadata.copy(batchStatus = PostingBatchStatus.REJECTED)

        val listener = type.reserveListenerClass.primaryConstructor!!.call(listenerHandler)
        val process = type.reserveListenerClass.memberFunctions.find { it.name == "process" }

        process!!.call(listener, args, message)
    }
}
