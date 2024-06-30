package ph.safibank.paymenttransactionmanager.infra.transaction

import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Version
import ph.safibank.paymenttransactionmanager.core.account.AccountNumber
import ph.safibank.paymenttransactionmanager.core.fee.CustomerFee
import ph.safibank.paymenttransactionmanager.core.fee.VendorFee
import ph.safibank.paymenttransactionmanager.core.fraud.FraudStatus
import ph.safibank.paymenttransactionmanager.core.provider.ProviderStatus
import ph.safibank.paymenttransactionmanager.core.transaction.PostingDetails
import ph.safibank.paymenttransactionmanager.core.transaction.PostingStatus
import ph.safibank.paymenttransactionmanager.core.transaction.ProviderDetails
import ph.safibank.paymenttransactionmanager.core.transaction.SenderDetails
import ph.safibank.paymenttransactionmanager.core.transaction.Transaction
import ph.safibank.paymenttransactionmanager.core.transaction.TransactionStatus
import ph.safibank.paymenttransactionmanager.core.transaction.TransactionType
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@MappedEntity("payment_transaction")
data class TransactionEntity(
    @field:Id
    val id: UUID,
    val amount: BigDecimal,
    val currency: String,
    val type: TransactionType,
    val status: TransactionStatus,
    val senderAccountId: UUID,
    val senderCustomerId: UUID,
    val senderAccountNumber: String,
    val fraudStatus: FraudStatus,
    val customerFeeId: String? = null,
    val customerFeeAmount: BigDecimal,
    val customerFeeCurrency: String,
    val customerFeeSubscriptionId: UUID? = null,
    val customerFeePostingId: String? = null,
    val vendorFeeId: String? = null,
    val vendorFeeAmount: BigDecimal,
    val vendorFeeCurrency: String,
    val vendorFeePostingId: String? = null,
    val providerName: String,
    val providerMerchantCode: String,
    val providerMerchantName: String,
    val providerId: String? = null,
    val providerStatus: ProviderStatus = ProviderStatus.NOT_SENT,
    val postingBatchId: String? = null,
    val postingId: UUID? = null,
    val postedAt: Instant? = null,
    val postingStatus: PostingStatus? = null,
    @DateCreated(truncatedTo = ChronoUnit.MILLIS)
    val createdAt: Instant? = null,
    @DateCreated(truncatedTo = ChronoUnit.MILLIS)
    val updatedAt: Instant? = null,
    @field:Version
    val version: Long? = 0,
)

fun TransactionEntity.toModel() = Transaction(
    id = id,
    amount = amount,
    currency = currency,
    type = type,
    status = status,
    senderDetails = SenderDetails(
        accountId = senderAccountId,
        customerId = senderCustomerId,
        accountNumber = AccountNumber(senderAccountNumber)
    ),
    fraudStatus = fraudStatus,
    customerFee = CustomerFee(
        id = customerFeeId,
        amount = customerFeeAmount,
        currency = customerFeeCurrency,
        subscriptionId = customerFeeSubscriptionId,
        postingId = customerFeePostingId,
    ),
    vendorFee = VendorFee(
        id = vendorFeeId,
        amount = vendorFeeAmount,
        currency = vendorFeeCurrency,
        postingId = vendorFeePostingId,
    ),
    providerDetails = ProviderDetails(
        provider = providerName,
        merchantCode = providerMerchantCode,
        merchantName = providerMerchantName,
        providerId = providerId,
        providerStatus = providerStatus
    ),
    postingDetails = postingBatchId?.let {
        PostingDetails(
            batchId = UUID.fromString(it),
            postingId = postingId,
            postedAt = postedAt,
            status = postingStatus
        )
    },
    version = version,
)

fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id,
    amount = amount,
    currency = currency,
    type = type,
    status = status,
    senderAccountId = senderDetails.accountId,
    senderCustomerId = senderDetails.customerId,
    senderAccountNumber = senderDetails.accountNumber.value,
    fraudStatus = fraudStatus,
    customerFeeId = customerFee.id,
    customerFeeAmount = customerFee.amount,
    customerFeeSubscriptionId = customerFee.subscriptionId,
    customerFeeCurrency = customerFee.currency,
    customerFeePostingId = customerFee.postingId,
    vendorFeeId = vendorFee.id,
    vendorFeeAmount = vendorFee.amount,
    vendorFeeCurrency = vendorFee.currency,
    vendorFeePostingId = vendorFee.postingId,
    providerName = providerDetails.provider,
    providerId = providerDetails.providerId,
    providerStatus = providerDetails.providerStatus,
    providerMerchantCode = providerDetails.merchantCode,
    providerMerchantName = providerDetails.merchantName,
    postingBatchId = postingDetails?.batchId.toString(),
    postingId = postingDetails?.postingId,
    postedAt = postingDetails?.postedAt,
    postingStatus = postingDetails?.status,
    version = version,
)
