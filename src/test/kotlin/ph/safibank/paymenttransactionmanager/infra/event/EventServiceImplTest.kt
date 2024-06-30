package ph.safibank.paymenttransactionmanager.infra.event

import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import ph.safibank.common.testutils.KafkaBaseTest
import ph.safibank.paymenttransactionmanager.TestUtils
import ph.safibank.paymenttransactionmanager.core.fee.CustomerFee
import ph.safibank.paymenttransactionmanager.core.fee.VendorFee
import ph.safibank.paymenttransactionmanager.core.fraud.FraudStatus
import ph.safibank.paymenttransactionmanager.core.transaction.PostingDetails
import ph.safibank.paymenttransactionmanager.core.transaction.ProviderDetails
import ph.safibank.paymenttransactionmanager.core.transaction.SenderDetails
import ph.safibank.paymenttransactionmanager.core.transaction.Transaction
import ph.safibank.paymenttransactionmanager.core.transaction.TransactionStatus
import ph.safibank.paymenttransactionmanager.core.transaction.TransactionType
import ph.safibank.paymenttransactionmanager.infra.util.AirtimeloadTransactionCreatedEventListener
import ph.safibank.paymenttransactionmanager.infra.util.AirtimeloadTransactionOccurredEventListener
import ph.safibank.paymenttransactionmanager.infra.util.AirtimeloadTransactionUpdateEventListener
import ph.safibank.paymenttransactionmanager.infra.util.BillPaymentTransactionEventListener
import strikt.api.expectThat
import strikt.assertions.isNotNull
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit

@ExperimentalCoroutinesApi
@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EventServiceImplTest : KafkaBaseTest() {
    @Inject
    private lateinit var billPaymentTransactionEventListener: BillPaymentTransactionEventListener

    @Inject
    private lateinit var airtimeloadTransactionCreatedEventListener: AirtimeloadTransactionCreatedEventListener

    @Inject
    private lateinit var airtimeloadTransactionOccurredEventListener: AirtimeloadTransactionOccurredEventListener

    @Inject
    private lateinit var airtimeloadTransactionUpdateEventListener: AirtimeloadTransactionUpdateEventListener

    @Inject
    private lateinit var eventService: EventServiceImpl

    private val defaultTransaction = Transaction(
        id = UUID.randomUUID(),
        amount = BigDecimal("100"),
        currency = "PHP",
        senderDetails = SenderDetails(
            accountNumber = TestUtils.randomAccountNumber(),
            accountId = UUID.randomUUID(),
            customerId = UUID.randomUUID(),
        ),
        status = TransactionStatus.PENDING,
        type = TransactionType.BILL_PAYMENT,
        fraudStatus = FraudStatus.APPROVED,
        vendorFee = VendorFee.ZERO_PHP,
        customerFee = CustomerFee.ZERO_PHP,
        providerDetails = ProviderDetails(
            provider = "PAYNAMICS",
            merchantCode = "BDO",
            merchantName = "Banco de Oro"
        ),
        postingDetails = PostingDetails(
            batchId = UUID.randomUUID(),
            postingId = UUID.randomUUID(),
            postedAt = Instant.now()
        )
    )

    @Test
    fun `sendTransactionCreatedEvent should emit an event to kafka with transactionType BILL_PAYMENT`() = runTest {
        billPaymentTransactionEventListener.reset()

        val transaction = defaultTransaction.copy(
            type = TransactionType.BILL_PAYMENT
        )
        eventService.sendTransactionCreatedEvent(transaction)

        var result: Any? = null
        await.atMost(5, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until {
            result = billPaymentTransactionEventListener.getTransaction(transaction.id.toString())
            result != null
        }
        expectThat(result).isNotNull()
    }

    @Test
    fun `sendTransactionApprovedEvent should emit an event to kafka with transactionType BILL_PAYMENT`() = runTest {
        billPaymentTransactionEventListener.reset()

        val transaction = defaultTransaction.copy(
            type = TransactionType.BILL_PAYMENT
        )
        eventService.sendTransactionApprovedEvent(transaction)

        var result: Any? = null
        await.atMost(5, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until {
            result = billPaymentTransactionEventListener.getTransaction(transaction.id.toString())
            result != null
        }
        expectThat(result).isNotNull()
    }

    @Test
    fun `sendTransactionFailedEvent should emit an event to kafka with transactionType BILL_PAYMENT`() = runTest {
        billPaymentTransactionEventListener.reset()

        val transaction = defaultTransaction.copy(
            type = TransactionType.BILL_PAYMENT
        )
        eventService.sendTransactionFailedEvent(transaction)

        var result: Any? = null
        await.atMost(5, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until {
            result = billPaymentTransactionEventListener.getTransaction(transaction.id.toString())
            result != null
        }
        expectThat(result).isNotNull()
    }

    @Test
    fun `sendTransactionReservedEvent should emit an event to kafka with transactionType BILL_PAYMENT`() = runTest {
        billPaymentTransactionEventListener.reset()

        val transaction = defaultTransaction.copy(
            type = TransactionType.BILL_PAYMENT
        )
        eventService.sendTransactionReservedEvent(transaction)

        var result: Any? = null
        await.atMost(5, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until {
            result = billPaymentTransactionEventListener.getTransaction(transaction.id.toString())
            result != null
        }
        expectThat(result).isNotNull()
    }

    @Test
    fun `sendTransactionSentToProviderEvent should emit an event to kafka with transactionType BILL_PAYMENT`() = runTest {
        billPaymentTransactionEventListener.reset()

        val transaction = defaultTransaction.copy(
            type = TransactionType.BILL_PAYMENT
        )
        eventService.sendTransactionSentToProviderEvent(transaction)

        var result: Any? = null
        await.atMost(5, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until {
            result = billPaymentTransactionEventListener.getTransaction(transaction.id.toString())
            result != null
        }
        expectThat(result).isNotNull()
    }

    @Test
    fun `sendTransactionPendingSettlementEvent should emit an event to kafka with transactionType BILL_PAYMENT`() = runTest {
        billPaymentTransactionEventListener.reset()

        val transaction = defaultTransaction.copy(
            type = TransactionType.BILL_PAYMENT
        )
        eventService.sendTransactionPendingSettlementEvent(transaction)

        var result: Any? = null
        await.atMost(5, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until {
            result = billPaymentTransactionEventListener.getTransaction(transaction.id.toString())
            result != null
        }
        expectThat(result).isNotNull()
    }

    @Test
    fun `sendTransactionReleasedEvent should emit an event to kafka with transactionType BILL_PAYMENT`() = runTest {
        billPaymentTransactionEventListener.reset()

        val transaction = defaultTransaction.copy(
            type = TransactionType.BILL_PAYMENT
        )
        eventService.sendTransactionReleasedEvent(transaction)

        var result: Any? = null
        await.atMost(5, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until {
            result = billPaymentTransactionEventListener.getTransaction(transaction.id.toString())
            result != null
        }
        expectThat(result).isNotNull()
    }

    @Test
    fun `sendTransactionSuccessEvent should emit an event to kafka with transactionType BILL_PAYMENT`() = runTest {
        billPaymentTransactionEventListener.reset()

        val transaction = defaultTransaction.copy(
            type = TransactionType.BILL_PAYMENT
        )
        eventService.sendTransactionSuccessEvent(transaction)

        var result: Any? = null
        await.atMost(5, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until {
            result = billPaymentTransactionEventListener.getTransaction(transaction.id.toString())
            result != null
        }
        expectThat(result).isNotNull()
    }

    @Test
    fun `sendTransactionCreatedEvent should emit an event to kafka with transactionType AIRTIME_LOAD`() = runTest {
        airtimeloadTransactionCreatedEventListener.reset()

        val transaction = defaultTransaction.copy(
            type = TransactionType.AIRTIME_LOAD
        )
        eventService.sendTransactionCreatedEvent(transaction)

        var result: Any? = null
        await.atMost(5, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until {
            result = airtimeloadTransactionCreatedEventListener.getTransaction(transaction.id.toString())
            result != null
        }
        expectThat(result).isNotNull()
    }

    @Test
    fun `sendTransactionApprovedEvent should emit an event to kafka with transactionType AIRTIME_LOAD`() = runTest {
        airtimeloadTransactionOccurredEventListener.reset()

        val transaction = defaultTransaction.copy(
            type = TransactionType.AIRTIME_LOAD
        )
        eventService.sendTransactionApprovedEvent(transaction)

        var result: Any? = null
        await.atMost(5, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until {
            result = airtimeloadTransactionOccurredEventListener.getTransaction(transaction.id.toString())
            result != null
        }
        expectThat(result).isNotNull()
    }

    @Test
    fun `sendTransactionFailedEvent should emit an event to kafka with transactionType AIRTIME_LOAD`() = runTest {
        airtimeloadTransactionUpdateEventListener.reset()

        val transaction = defaultTransaction.copy(
            type = TransactionType.AIRTIME_LOAD
        )
        eventService.sendTransactionFailedEvent(transaction)

        var result: Any? = null
        await.atMost(5, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until {
            result = airtimeloadTransactionUpdateEventListener.getTransaction(transaction.id.toString())
            result != null
        }
        expectThat(result).isNotNull()
    }

    @Test
    fun `sendTransactionReservedEvent should emit an event to kafka with transactionType AIRTIME_LOAD`() = runTest {
        airtimeloadTransactionOccurredEventListener.reset()

        val transaction = defaultTransaction.copy(
            type = TransactionType.AIRTIME_LOAD
        )
        eventService.sendTransactionReservedEvent(transaction)

        var result: Any? = null
        await.atMost(5, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until {
            result = airtimeloadTransactionOccurredEventListener.getTransaction(transaction.id.toString())
            result != null
        }
        expectThat(result).isNotNull()
    }

    @Test
    fun `sendTransactionSentToProviderEvent should emit an event to kafka with transactionType AIRTIME_LOAD`() = runTest {
        airtimeloadTransactionOccurredEventListener.reset()

        val transaction = defaultTransaction.copy(
            type = TransactionType.AIRTIME_LOAD
        )
        eventService.sendTransactionSentToProviderEvent(transaction)

        var result: Any? = null
        await.atMost(5, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until {
            result = airtimeloadTransactionOccurredEventListener.getTransaction(transaction.id.toString())
            result != null
        }
        expectThat(result).isNotNull()
    }

    @Test
    fun `sendTransactionPendingSettlementEvent should emit an event to kafka with transactionType AIRTIME_LOAD`() = runTest {
        airtimeloadTransactionUpdateEventListener.reset()

        val transaction = defaultTransaction.copy(
            type = TransactionType.AIRTIME_LOAD
        )
        eventService.sendTransactionPendingSettlementEvent(transaction)

        var result: Any? = null
        await.atMost(5, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until {
            result = airtimeloadTransactionUpdateEventListener.getTransaction(transaction.id.toString())
            result != null
        }
        expectThat(result).isNotNull()
    }

    @Test
    fun `sendTransactionReleasedEvent should emit an event to kafka with transactionType AIRTIME_LOAD`() = runTest {
        airtimeloadTransactionOccurredEventListener.reset()

        val transaction = defaultTransaction.copy(
            type = TransactionType.AIRTIME_LOAD
        )
        eventService.sendTransactionReleasedEvent(transaction)

        var result: Any? = null
        await.atMost(5, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until {
            result = airtimeloadTransactionOccurredEventListener.getTransaction(transaction.id.toString())
            result != null
        }
        expectThat(result).isNotNull()
    }

    @Test
    fun `sendTransactionSuccessEvent should emit an event to kafka with transactionType AIRTIME_LOAD`() = runTest {
        airtimeloadTransactionOccurredEventListener.reset()

        val transaction = defaultTransaction.copy(
            type = TransactionType.AIRTIME_LOAD
        )
        eventService.sendTransactionSuccessEvent(transaction)

        var result: Any? = null
        await.atMost(5, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until {
            result = airtimeloadTransactionOccurredEventListener.getTransaction(transaction.id.toString())
            result != null
        }
        expectThat(result).isNotNull()
    }
}
