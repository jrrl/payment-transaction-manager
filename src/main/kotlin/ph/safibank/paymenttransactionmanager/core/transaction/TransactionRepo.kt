package ph.safibank.paymenttransactionmanager.core.transaction

import java.util.UUID

interface TransactionRepo {
    suspend fun saveTransaction(transaction: Transaction): Transaction
    suspend fun updateTransaction(transaction: Transaction): Transaction
    suspend fun getTransaction(id: UUID): Transaction?
}
