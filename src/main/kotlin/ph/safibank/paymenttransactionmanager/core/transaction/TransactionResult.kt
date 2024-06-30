package ph.safibank.paymenttransactionmanager.core.transaction

import java.util.UUID

data class TransactionResult(
    val transactionId: UUID,
    val transactionStatus: TransactionStatus
)
