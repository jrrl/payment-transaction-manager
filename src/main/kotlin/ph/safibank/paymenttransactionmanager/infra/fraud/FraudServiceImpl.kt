package ph.safibank.paymenttransactionmanager.infra.fraud

import jakarta.inject.Singleton
import ph.safibank.paymenttransactionmanager.core.exception.ValidationException
import ph.safibank.paymenttransactionmanager.core.fraud.FraudService
import ph.safibank.paymenttransactionmanager.core.fraud.FraudStatus
import ph.safibank.paymenttransactionmanager.core.transaction.Transaction
import ph.safibank.slackermanager.client.api.TransactionApi
import ph.safibank.slackermanager.client.model.TransactionDetailsDto
import ph.safibank.slackermanager.client.model.TransactionDirection
import ph.safibank.slackermanager.client.model.TransactionStatus
import ph.safibank.slackermanager.client.model.TransactionType
import java.time.Instant
import java.time.ZoneOffset

@Singleton
class FraudServiceImpl(
    private val fraudApi: TransactionApi
) : FraudService {
    override suspend fun determineFraudStatus(transaction: Transaction): FraudStatus {
        if (transaction.fraudStatus != FraudStatus.UNKNOWN) {
            throw ValidationException.singleError("Fraud status already set for transaction ${transaction.id}")
        }
        val request = TransactionDetailsDto(
            transaction.id,
            transaction.senderDetails.accountId,
            transaction.senderDetails.customerId,
            TransactionType.INTERBANK_TRANSACTION,
            TransactionDirection.OUTGOING,
            transaction.amount,
            transaction.currency,
            Instant.now().atOffset(ZoneOffset.UTC),
            Instant.now().atOffset(ZoneOffset.UTC)
        )

        val result = fraudApi.validateTransaction(request)

        return when (result.status) {
            TransactionStatus.APPROVED -> FraudStatus.APPROVED
            TransactionStatus.REJECTED -> FraudStatus.REJECTED
            TransactionStatus.STEP_UP_NEEDED_LEVEL1 -> FraudStatus.STEP_UP_LEVEL1
            TransactionStatus.STEP_UP_NEEDED_LEVEL2 -> FraudStatus.STEP_UP_LEVEL2
            TransactionStatus.STEP_UP_NEEDED_LEVEL3 -> FraudStatus.STEP_UP_LEVEL3
            TransactionStatus.STEP_UP_NEEDED_LEVEL4 -> FraudStatus.STEP_UP_LEVEL4
            TransactionStatus.MANUAL_APPROVAL_NEEDED -> FraudStatus.MANUAL_APPROVAL
        }
    }
}
