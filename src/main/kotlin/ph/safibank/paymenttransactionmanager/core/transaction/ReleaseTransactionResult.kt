package ph.safibank.paymenttransactionmanager.core.transaction

import java.util.UUID

data class ReleaseTransactionResult(
    val transactionId: UUID,
    val postingId: UUID,
)
