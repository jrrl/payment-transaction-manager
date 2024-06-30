package ph.safibank.paymenttransactionmanager.core.provider

import java.util.UUID

data class ProviderResult(
    val transactionId: UUID,
    val providerId: String,
    val status: ProviderStatus,
)

enum class ProviderStatus {
    SUCCESS,
    PENDING,
    FAILED,
    NOT_SENT,
    UNKNOWN,
}
