package ph.safibank.paymenttransactionmanager

import ph.safibank.common.utils.service.TimeService
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
import ph.safibank.tm.model.InstructionMetadata
import ph.safibank.tm.model.PostingBatchStatus
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import kotlin.random.Random

object TestUtils {
    fun randomAccountNumber(): AccountNumber {
        val value = Random
            .nextInt(999999999)
            .toString()
            .padStart(10, '0')
        return AccountNumber(value)
    }

    val defaultTransaction = Transaction(
        id = UUID.randomUUID(),
        amount = BigDecimal("100"),
        currency = "PHP",
        status = TransactionStatus.PENDING,
        type = TransactionType.BILL_PAYMENT,
        senderDetails = SenderDetails(
            accountNumber = AccountNumber("1234567890"),
            accountId = UUID.randomUUID(),
            customerId = UUID.randomUUID(),
        ),
        customerFee = CustomerFee.ZERO_PHP,
        vendorFee = VendorFee.ZERO_PHP,
        fraudStatus = FraudStatus.APPROVED,
        postingDetails = PostingDetails(
            batchId = UUID.randomUUID(),
            postingId = UUID.randomUUID(),
            postedAt = Instant.now(),
            status = PostingStatus.RESERVED
        ),
        providerDetails = ProviderDetails(
            providerStatus = ProviderStatus.NOT_SENT,
            provider = "paynamics",
            merchantCode = "Globe",
            merchantName = "Globe Telecom",
        )
    )

    val defaultInstructionMetadata = InstructionMetadata(
        instructionId = UUID.randomUUID().toString(),
        batchId = UUID.randomUUID().toString(),
        requestId = UUID.randomUUID().toString(),
        clientId = UUID.randomUUID().toString(),
        clientBatchId = UUID.randomUUID().toString(),
        batchStatus = PostingBatchStatus.ACCEPTED,
        valueTimestamp = TimeService.now(),
        insertionTimestamp = TimeService.now(),
        captureTimestamp = TimeService.now(),
        errorMessage = null,
    )
}
