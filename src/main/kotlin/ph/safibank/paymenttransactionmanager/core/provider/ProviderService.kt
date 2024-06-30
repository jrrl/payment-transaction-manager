package ph.safibank.paymenttransactionmanager.core.provider

import ph.safibank.paymenttransactionmanager.core.transaction.Transaction
import ph.safibank.paymenttransactionmanager.core.transaction.TransactionType

interface ProviderService {
    fun getName(): String
    fun usableForTransaction(transactionType: TransactionType, merchantCode: String): Boolean
    suspend fun initiatePayment(transaction: Transaction): ProviderResult
}
