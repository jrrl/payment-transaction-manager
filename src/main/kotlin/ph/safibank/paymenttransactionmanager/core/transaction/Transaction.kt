package ph.safibank.paymenttransactionmanager.core.transaction

import ph.safibank.paymenttransactionmanager.core.account.AccountNumber
import ph.safibank.paymenttransactionmanager.core.fee.CustomerFee
import ph.safibank.paymenttransactionmanager.core.fee.VendorFee
import ph.safibank.paymenttransactionmanager.core.fraud.FraudStatus
import ph.safibank.paymenttransactionmanager.core.provider.ProviderStatus
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class Transaction(
    val id: UUID,
    val amount: BigDecimal,
    val currency: String,
    val type: TransactionType,
    val status: TransactionStatus,
    val senderDetails: SenderDetails,
    val fraudStatus: FraudStatus,
    val customerFee: CustomerFee,
    val vendorFee: VendorFee,
    val providerDetails: ProviderDetails,
    val postingDetails: PostingDetails? = null,
    val version: Long? = 0,
)

data class SenderDetails(
    val accountId: UUID,
    val customerId: UUID,
    val accountNumber: AccountNumber,
)

data class ProviderDetails(
    val provider: String,
    val merchantCode: String,
    val merchantName: String,
    val providerId: String? = null,
    val providerStatus: ProviderStatus = ProviderStatus.NOT_SENT,
)

data class PostingDetails(
    val batchId: UUID,
    val postingId: UUID? = null,
    val postedAt: Instant? = null,
    val status: PostingStatus? = null
)

enum class PostingStatus {
    SUCCESS, FAILED, RESERVED, PENDING_RELEASE, PENDING_SETTLEMENT, RELEASED, SETTLED
}

enum class TransactionType {
    BILL_PAYMENT,
    AIRTIME_LOAD
}

enum class TransactionStatus {
    INITIATED,
    PENDING,
    FAILED,
    SENT_TO_PROVIDER,
    WAITING_FOR_APPROVAL,
    PENDING_RELEASE,
    PENDING_SETTLEMENT,
    SUCCESS,
    CANCELLED,
    EXPIRED,
}
