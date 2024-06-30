package ph.safibank.paymenttransactionmanager.infra.util

import io.micronaut.configuration.kafka.annotation.KafkaListener
import ph.safibank.avro.transactions.AirtimeLoadTransactionCreatedEventV2
import ph.safibank.avro.transactions.AirtimeLoadTransactionOccurredEventV2
import ph.safibank.avro.transactions.AirtimeLoadTransactionUpdatedEventV1
import ph.safibank.avro.transactions.BillPaymentTransactionOccurredEventV2
import ph.safibank.kafka.transactions.TransactionsAirtimeloadTransactionCreatedEventV2Listener
import ph.safibank.kafka.transactions.TransactionsAirtimeloadTransactionOccurredEventV2Listener
import ph.safibank.kafka.transactions.TransactionsAirtimeloadTransactionUpdatedEventV1Listener
import ph.safibank.kafka.transactions.TransactionsBillPaymentTransactionEventV2Listener
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

sealed interface TestListener {
    fun getTransaction(id: String): Any?
}

@KafkaListener
class BillPaymentTransactionEventListener : TransactionsBillPaymentTransactionEventV2Listener(), TestListener {
    private val map = ConcurrentHashMap<String, Any>()
    override fun process(idempotencyKey: String?, messageKey: UUID, transaction: BillPaymentTransactionOccurredEventV2) {
        map[transaction.id.toString()] = transaction
    }
    override fun getTransaction(id: String) = map[id]
    fun reset() {
        map.clear()
    }
}

@KafkaListener
class AirtimeloadTransactionCreatedEventListener : TransactionsAirtimeloadTransactionCreatedEventV2Listener(), TestListener {
    private val map = ConcurrentHashMap<String, Any>()
    override fun process(idempotencyKey: String?, messageKey: UUID, transaction: AirtimeLoadTransactionCreatedEventV2) {
        map[transaction.id.toString()] = transaction
    }
    override fun getTransaction(id: String) = map[id]
    fun reset() {
        map.clear()
    }
}

@KafkaListener
class AirtimeloadTransactionOccurredEventListener : TransactionsAirtimeloadTransactionOccurredEventV2Listener(), TestListener {
    private val map = ConcurrentHashMap<String, Any>()
    override fun process(idempotencyKey: String?, messageKey: UUID, transaction: AirtimeLoadTransactionOccurredEventV2) {
        map[transaction.id.toString()] = transaction
    }
    override fun getTransaction(id: String) = map[id]
    fun reset() {
        map.clear()
    }
}

@KafkaListener
class AirtimeloadTransactionUpdateEventListener : TransactionsAirtimeloadTransactionUpdatedEventV1Listener(), TestListener {
    private val map = ConcurrentHashMap<String, Any>()
    override fun process(idempotencyKey: String?, messageKey: UUID, transaction: AirtimeLoadTransactionUpdatedEventV1) {
        map[transaction.id.toString()] = transaction
    }
    override fun getTransaction(id: String) = map[id]
    fun reset() {
        map.clear()
    }
}
