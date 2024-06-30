package ph.safibank.paymenttransactionmanager.infra.event

import io.micronaut.configuration.kafka.annotation.KafkaClient
import io.micronaut.context.annotation.Factory
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ph.safibank.avro.transactions.AirtimeLoadTransactionCreatedEventV2
import ph.safibank.avro.transactions.AirtimeLoadTransactionOccurredEventV2
import ph.safibank.avro.transactions.AirtimeLoadTransactionUpdatedEventV1
import ph.safibank.avro.transactions.BillPaymentDetails
import ph.safibank.avro.transactions.BillPaymentTransactionOccurredEventV2
import ph.safibank.avro.transactions.BillerProvider
import ph.safibank.avro.transactions.ServiceType
import ph.safibank.avro.transactions.StatusV2
import ph.safibank.avro.transactions.VendorType
import ph.safibank.kafka.transactions.TransactionsAirtimeloadTransactionCreatedEventV2Producer
import ph.safibank.kafka.transactions.TransactionsAirtimeloadTransactionOccurredEventV2Producer
import ph.safibank.kafka.transactions.TransactionsAirtimeloadTransactionUpdatedEventV1Producer
import ph.safibank.kafka.transactions.TransactionsBillPaymentTransactionEventV2Producer
import ph.safibank.paymenttransactionmanager.core.event.EventService
import ph.safibank.paymenttransactionmanager.core.fraud.FraudStatus
import ph.safibank.paymenttransactionmanager.core.transaction.Transaction
import ph.safibank.paymenttransactionmanager.core.transaction.TransactionStatus
import ph.safibank.paymenttransactionmanager.core.transaction.TransactionType
import ph.safibank.paymenttransactionmanager.util.getLogger
import java.util.UUID

@Singleton
class EventServiceImpl(
    private val eventProducerScope: CoroutineScope,
    private val billPaymentTransactionEventProducer: BillPaymentTransactionEventProducer,
    private val airtimeloadTransactionCreatedEventProducer: AirtimeloadTransactionCreatedEventProducer,
    private val airtimeloadTransactionOccurredEventProducer: AirtimeloadTransactionOccurredEventProducer,
    private val airtimeloadTransactionUpdatedEventProducer: AirtimeloadTransactionUpdatedEventProducer,
) : EventService {

    private val log = getLogger()

    override fun sendTransactionCreatedEvent(transaction: Transaction) {
        eventProducerScope.launch {
            when (transaction.type) {
                TransactionType.BILL_PAYMENT -> sendBillPaymentEvent(transaction)
                TransactionType.AIRTIME_LOAD -> sendAirtimeLoadCreatedEvent(transaction)
            }
        }
    }

    override fun sendTransactionApprovedEvent(transaction: Transaction) {
        eventProducerScope.launch {
            when (transaction.type) {
                TransactionType.BILL_PAYMENT -> sendBillPaymentEvent(transaction)
                TransactionType.AIRTIME_LOAD -> sendAirtimeLoadOccurredEvent(transaction)
            }
        }
    }

    override fun sendTransactionFailedEvent(transaction: Transaction) {
        eventProducerScope.launch {
            when (transaction.type) {
                TransactionType.BILL_PAYMENT -> sendBillPaymentEvent(transaction)
                TransactionType.AIRTIME_LOAD -> sendAirtimeLoadUpdatedEvent(transaction)
            }
        }
    }

    override fun sendTransactionReservedEvent(transaction: Transaction) {
        eventProducerScope.launch {
            when (transaction.type) {
                TransactionType.BILL_PAYMENT -> sendBillPaymentEvent(transaction)
                TransactionType.AIRTIME_LOAD -> sendAirtimeLoadOccurredEvent(transaction)
            }
        }
    }

    override fun sendTransactionSentToProviderEvent(transaction: Transaction) {
        eventProducerScope.launch {
            when (transaction.type) {
                TransactionType.BILL_PAYMENT -> sendBillPaymentEvent(transaction)
                TransactionType.AIRTIME_LOAD -> sendAirtimeLoadOccurredEvent(transaction)
            }
        }
    }

    override fun sendTransactionPendingSettlementEvent(transaction: Transaction) {
        eventProducerScope.launch {
            when (transaction.type) {
                TransactionType.BILL_PAYMENT -> sendBillPaymentEvent(transaction)
                TransactionType.AIRTIME_LOAD -> sendAirtimeLoadUpdatedEvent(transaction)
            }
        }
    }

    override fun sendTransactionReleasedEvent(transaction: Transaction) {
        eventProducerScope.launch {
            when (transaction.type) {
                TransactionType.BILL_PAYMENT -> sendBillPaymentEvent(transaction)
                TransactionType.AIRTIME_LOAD -> sendAirtimeLoadOccurredEvent(transaction)
            }
        }
    }

    override fun sendTransactionSuccessEvent(transaction: Transaction) {
        eventProducerScope.launch {
            when (transaction.type) {
                TransactionType.BILL_PAYMENT -> sendBillPaymentEvent(transaction)
                TransactionType.AIRTIME_LOAD -> sendAirtimeLoadOccurredEvent(transaction)
            }
        }
    }

    private fun sendBillPaymentEvent(transaction: Transaction) {
        log.info("Sending bills payment transaction event ${transaction.id}: $transaction")

        billPaymentTransactionEventProducer.send(
            UUID.randomUUID().toString(),
            UUID.randomUUID(),
            transaction.toBillPaymentTransactionOccurredEvent(),
        )
    }

    private fun sendAirtimeLoadCreatedEvent(transaction: Transaction) {
        log.info("Sending create airtime load transaction event ${transaction.id}: $transaction")

        airtimeloadTransactionCreatedEventProducer.send(
            UUID.randomUUID().toString(),
            UUID.randomUUID(),
            transaction.toAirtimeLoadTransactionCreatedEvent(),
        )
    }

    private fun sendAirtimeLoadOccurredEvent(transaction: Transaction) {
        log.info("Sending airtime load transaction event ${transaction.id}: $transaction")

        airtimeloadTransactionOccurredEventProducer.send(
            UUID.randomUUID().toString(),
            UUID.randomUUID(),
            transaction.toAirtimeLoadTransactionOccurredEvent(),
        )
    }

    private fun sendAirtimeLoadUpdatedEvent(transaction: Transaction) {
        log.info("Sending updated airtime load transaction event ${transaction.id}: $transaction")

        airtimeloadTransactionUpdatedEventProducer.send(
            UUID.randomUUID().toString(),
            UUID.randomUUID(),
            transaction.toAirtimeLoadTransactionUpdatedEvent(),
        )
    }

    private fun getVendorType(provider: String): VendorType {
        return VendorType.values().find { it.name == provider } ?: VendorType.UNKNOWN
    }

    private fun getBillerProvider(provider: String): BillerProvider? {
        return BillerProvider.values().find { it.name == provider }
    }

    private fun getStatus(transaction: Transaction): StatusV2 {
        return when {
            isEventPendingStatus(transaction.status) -> StatusV2.PENDING
            transaction.status == TransactionStatus.SUCCESS -> StatusV2.SUCCESS
            transaction.status == TransactionStatus.FAILED && isStepUpFraudStatus(transaction.fraudStatus) -> StatusV2.REJECTED_BY_STEP_UP
            transaction.status == TransactionStatus.FAILED -> StatusV2.FAILED
            transaction.status == TransactionStatus.WAITING_FOR_APPROVAL -> StatusV2.MANUAL_APPROVAL_REQUIRED
            else -> StatusV2.UNKNOWN
        }
    }

    private fun isEventPendingStatus(transactionStatus: TransactionStatus): Boolean {
        return when (transactionStatus) {
            TransactionStatus.PENDING,
            TransactionStatus.PENDING_RELEASE,
            TransactionStatus.PENDING_SETTLEMENT -> true
            else -> false
        }
    }

    private fun isStepUpFraudStatus(fraudStatus: FraudStatus): Boolean {
        return when (fraudStatus) {
            FraudStatus.STEP_UP_LEVEL1,
            FraudStatus.STEP_UP_LEVEL2,
            FraudStatus.STEP_UP_LEVEL3,
            FraudStatus.STEP_UP_LEVEL4 -> true
            else -> false
        }
    }

    private fun Transaction.toBillPaymentTransactionOccurredEvent() = BillPaymentTransactionOccurredEventV2(
        id,
        postingDetails?.postingId,
        postingDetails?.batchId,
        amount,
        customerFee.amount,
        vendorFee.amount,
        getVendorType(providerDetails.provider),
        ServiceType.BILL_PAYMENT,
        providerDetails.merchantCode,
        currency,
        senderDetails.customerId,
        senderDetails.accountId,
        BillPaymentDetails(
            providerDetails.merchantCode,
            getBillerProvider(providerDetails.provider),
            senderDetails.accountNumber.value,
            null,
        ),
        getStatus(this),
        "",
        null, // TODO need logic to set authorizedAt
        postingDetails?.postedAt, // TODO need logic to set settledAt
        providerDetails.providerId
    )

    private fun Transaction.toAirtimeLoadTransactionCreatedEvent() = AirtimeLoadTransactionCreatedEventV2(
        id,
        postingDetails?.postingId,
        amount,
        customerFee.amount,
        vendorFee.amount,
        getVendorType(providerDetails.provider),
        ServiceType.DIGITAL_GOODS,
        providerDetails.merchantCode,
        currency,
        senderDetails.customerId,
        senderDetails.accountId,
        getStatus(this),
        "", // TODO need to add sku in transaction model
        "", // TODO recepient name details? beneficiary?
        "", // TODO recepient mobile details? beneficiary?
        "",
        null,
        postingDetails?.postedAt, // TODO is settleAt = postedAt?
    )

    private fun Transaction.toAirtimeLoadTransactionOccurredEvent() = AirtimeLoadTransactionOccurredEventV2(
        id,
        postingDetails?.postingId,
        amount,
        customerFee.amount,
        vendorFee.amount,
        getVendorType(providerDetails.provider),
        ServiceType.DIGITAL_GOODS,
        providerDetails.merchantCode,
        currency,
        senderDetails.customerId,
        senderDetails.accountId,
        getStatus(this),
        "", // TODO need to add sku in transaction model
        "", // TODO recepient name details? beneficiary?
        "", // TODO recepient mobile details? beneficiary?
        "",
        null,
        postingDetails?.postedAt,
    )

    private fun Transaction.toAirtimeLoadTransactionUpdatedEvent() = AirtimeLoadTransactionUpdatedEventV1(
        id,
        senderDetails.customerId,
        postingDetails?.postingId,
        postingDetails?.batchId,
        getStatus(this),
        postingDetails?.postedAt,
    )
}

@KafkaClient
abstract class BillPaymentTransactionEventProducer : TransactionsBillPaymentTransactionEventV2Producer()

@KafkaClient
abstract class AirtimeloadTransactionCreatedEventProducer : TransactionsAirtimeloadTransactionCreatedEventV2Producer()

@KafkaClient
abstract class AirtimeloadTransactionOccurredEventProducer : TransactionsAirtimeloadTransactionOccurredEventV2Producer()

@KafkaClient
abstract class AirtimeloadTransactionUpdatedEventProducer : TransactionsAirtimeloadTransactionUpdatedEventV1Producer()

@Factory
class EventConfiguration {
    @Singleton
    fun eventProducerContext(): CoroutineScope = CoroutineScope(
        CoroutineName("event-producer") + Dispatchers.Default + SupervisorJob()
    )
}
