package ph.safibank.paymenttransactionmanager.api.messaging

import com.github.database.rider.core.api.dataset.DataSet
import com.github.database.rider.core.api.dataset.ExpectedDataSet
import com.github.database.rider.junit5.api.DBRider
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import ph.safibank.common.testutils.kafka.UseKafka
import ph.safibank.paymenttransactionmanager.TestUtils.defaultInstructionMetadata
import ph.safibank.tm.model.transaction.SettlementRuntimeParams
import java.util.UUID
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.primaryConstructor

@ExperimentalCoroutinesApi
@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@UseKafka
@DBRider
class TMSettlementResponseHandlerTest {
    @Inject
    private lateinit var listenerHandler: TMSettlementResponseHandler

    private val transactionId = UUID.fromString("c4103d99-c601-4ccc-a1c4-10a7c5d6d817")

    @ParameterizedTest
    @DataSet(value = ["payment-pending-settlement.yaml"], cleanBefore = true)
    @ExpectedDataSet("expected-after-payment-is-settled.yaml")
    @EnumSource(PaymentType::class)
    fun `should set status to SUCCESS when transaction is settled`(type: PaymentType) = runTest {
        val message = type.settlementClass.primaryConstructor!!.call(
            SettlementRuntimeParams(
                clientTransactionId = transactionId.toString()
            )
        )

        val listener = type.settlementListenerClass.primaryConstructor!!.call(listenerHandler)
        val process = type.settlementListenerClass.memberFunctions.find { it.name == "process" }

        process!!.call(listener, defaultInstructionMetadata, message)
    }

    @ParameterizedTest
    @DataSet(value = ["payment-pending-settlement.yaml"], cleanBefore = true)
    @ExpectedDataSet("expected-after-payment-is-settled.yaml")
    @EnumSource(PaymentType::class)
    fun `should return the same result when same request id is sent`(type: PaymentType) = runTest {
        val message = type.settlementClass.primaryConstructor!!.call(
            SettlementRuntimeParams(
                clientTransactionId = transactionId.toString()
            )
        )

        val listener = type.settlementListenerClass.primaryConstructor!!.call(listenerHandler)
        val process = type.settlementListenerClass.memberFunctions.find { it.name == "process" }

        process!!.call(listener, defaultInstructionMetadata, message)
        process.call(listener, defaultInstructionMetadata, message)
    }
}
