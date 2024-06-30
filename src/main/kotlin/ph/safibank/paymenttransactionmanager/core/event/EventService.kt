package ph.safibank.paymenttransactionmanager.core.event

import ph.safibank.paymenttransactionmanager.core.transaction.Transaction

interface EventService {
    fun sendTransactionCreatedEvent(transaction: Transaction)
    fun sendTransactionApprovedEvent(transaction: Transaction)
    fun sendTransactionFailedEvent(transaction: Transaction)
    fun sendTransactionReservedEvent(transaction: Transaction)
    fun sendTransactionSentToProviderEvent(transaction: Transaction)
    fun sendTransactionPendingSettlementEvent(transaction: Transaction)
    fun sendTransactionReleasedEvent(transaction: Transaction)
    fun sendTransactionSuccessEvent(transaction: Transaction)
//    fun sendUpdateParentTransactionEvent(transaction: Transaction)
}
