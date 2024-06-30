package ph.safibank.paymenttransactionmanager.core.transaction

import java.util.UUID

data class ReserveTransactionRequest(
    val transactionId: UUID,
    val postingId: UUID? = null
)
