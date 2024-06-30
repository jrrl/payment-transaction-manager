package ph.safibank.paymenttransactionmanager.core.posting

import ph.safibank.paymenttransactionmanager.core.transaction.Transaction

interface PostingService {
    suspend fun reserveTransactionAmount(transaction: Transaction): PostingResult
    suspend fun settleAmount(transaction: Transaction): PostingResult
    suspend fun releaseAmount(transaction: Transaction): PostingResult
}
