package ph.safibank.paymenttransactionmanager.core.transaction

data class CustomerFeeUpdateResult(
    val batchId: String,
    val postingId: String,
    val id: String,
)
