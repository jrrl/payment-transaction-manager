package ph.safibank.paymenttransactionmanager.core.usecase.impl

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ph.safibank.paymenttransactionmanager.core.transaction.Transaction
import ph.safibank.paymenttransactionmanager.core.transaction.TransactionRepo
import java.util.UUID

class TransactionRepoMockImpl : TransactionRepo {
    private val map = mutableMapOf<UUID, Transaction>()
    private val mutex = Mutex()

    override suspend fun saveTransaction(transaction: Transaction): Transaction {
        if (map.containsKey(transaction.id)) throw Exception("duplicate")
        mutex.withLock {
            map.put(transaction.id, transaction)
        }
        return transaction
    }

    override suspend fun updateTransaction(transaction: Transaction): Transaction {
        if (!map.containsKey(transaction.id)) throw Exception("dne")
        mutex.withLock {
            map.put(transaction.id, transaction)
        }
        return transaction
    }

    override suspend fun getTransaction(id: UUID): Transaction? {
        return map[id]
    }

    fun reset() {
        map.clear()
    }
}
