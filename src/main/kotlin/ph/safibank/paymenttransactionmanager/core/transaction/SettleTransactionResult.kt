package ph.safibank.paymenttransactionmanager.core.transaction

import java.util.UUID

data class SettleTransactionResult(
    val transactionId: UUID,
    val postingId: UUID,
)
