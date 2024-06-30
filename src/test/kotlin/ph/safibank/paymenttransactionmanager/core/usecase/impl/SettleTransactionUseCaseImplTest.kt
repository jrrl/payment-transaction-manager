package ph.safibank.paymenttransactionmanager.core.usecase.impl

import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import ph.safibank.paymenttransactionmanager.TestUtils
import ph.safibank.paymenttransactionmanager.core.event.EventService
import ph.safibank.paymenttransactionmanager.core.exception.TransactionNotFoundException
import ph.safibank.paymenttransactionmanager.core.exception.ValidationError
import ph.safibank.paymenttransactionmanager.core.exception.ValidationException
import ph.safibank.paymenttransactionmanager.core.fee.CustomerFee
import ph.safibank.paymenttransactionmanager.core.fee.VendorFee
import ph.safibank.paymenttransactionmanager.core.fraud.FraudStatus
import ph.safibank.paymenttransactionmanager.core.transaction.PostingDetails
import ph.safibank.paymenttransactionmanager.core.transaction.PostingStatus
import ph.safibank.paymenttransactionmanager.core.transaction.ProviderDetails
import ph.safibank.paymenttransactionmanager.core.transaction.SenderDetails
import ph.safibank.paymenttransactionmanager.core.transaction.SettleTransactionResult
import ph.safibank.paymenttransactionmanager.core.transaction.Transaction
import ph.safibank.paymenttransactionmanager.core.transaction.TransactionStatus
import ph.safibank.paymenttransactionmanager.core.transaction.TransactionType
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isNotNull
import strikt.assertions.withFirst
import java.math.BigDecimal
import java.util.UUID

@ExperimentalCoroutinesApi
internal class SettleTransactionUseCaseImplTest {
    private val transactionRepo = TransactionRepoMockImpl()
    private val eventService = mockk<EventService>()

    private val useCase = SettleTransactionUseCaseImpl(
        transactionRepo,
        eventService,
    )

    private val transactionId = UUID.randomUUID()
    private val expectedTransaction = Transaction(
        id = transactionId,
        amount = BigDecimal("500"),
        currency = "PHP",
        type = TransactionType.BILL_PAYMENT,
        status = TransactionStatus.PENDING_SETTLEMENT,
        senderDetails = SenderDetails(
            accountId = UUID.randomUUID(),
            customerId = UUID.randomUUID(),
            accountNumber = TestUtils.randomAccountNumber(),
        ),
        fraudStatus = FraudStatus.APPROVED,
        customerFee = CustomerFee(BigDecimal("10"), "PHP"),
        vendorFee = VendorFee(BigDecimal("10"), "PHP"),
        providerDetails = ProviderDetails(
            provider = "PAYNAMICS",
            merchantCode = "APEC1",
            merchantName = "APEC1",
        ),
        postingDetails = PostingDetails(
            batchId = UUID.randomUUID(),
        )
    )

    @BeforeEach
    fun setup() {
        coJustRun { eventService.sendTransactionSuccessEvent(any()) }
    }

    @Test
    fun `should throw TransactionNotFoundException if transaction is not existing`() {
        expectCatching {
            useCase.invoke(SettleTransactionResult(UUID.randomUUID(), UUID.randomUUID()))
        }.isFailure()
            .isA<TransactionNotFoundException>()
    }

    @ParameterizedTest
    @EnumSource(value = TransactionStatus::class, mode = EnumSource.Mode.EXCLUDE, names = ["PENDING_SETTLEMENT"])
    fun `should throw ValidationException if transaction status is not pending settlement`(transactionStatus: TransactionStatus) = runTest {
        transactionRepo.saveTransaction(expectedTransaction.copy(status = transactionStatus))
        val result = SettleTransactionResult(
            transactionId = expectedTransaction.id,
            postingId = UUID.randomUUID()
        )
        expectCatching {
            useCase.invoke(result)
        }.isFailure()
            .isA<ValidationException>()
            .and {
                get { errors }
                    .hasSize(1)
                    .withFirst {
                        isEqualTo(ValidationError("Transaction (${expectedTransaction.id}) Status should be pending settlement"))
                    }
            }
    }

    @Test
    fun `should throw ValidationException if transaction posting details is null`() = runTest {
        transactionRepo.saveTransaction(expectedTransaction.copy(postingDetails = null))
        val result = SettleTransactionResult(
            transactionId = expectedTransaction.id,
            postingId = UUID.randomUUID()
        )
        expectCatching {
            useCase.invoke(result)
        }.isFailure()
            .isA<ValidationException>()
            .and {
                get { errors }
                    .hasSize(1)
                    .withFirst {
                        isEqualTo(ValidationError("Transaction (${expectedTransaction.id}) posting details should not be null"))
                    }
            }
    }

    @Test
    fun `should set transaction status to SUCCESS`() = runTest {
        val transaction = transactionRepo.saveTransaction(expectedTransaction)
        val result = SettleTransactionResult(
            transactionId = transaction.id,
            postingId = UUID.randomUUID()
        )
        useCase.invoke(result)

        expectThat(transactionRepo.getTransaction(transaction.id)).isNotNull()
            .and {
                get { status } isEqualTo TransactionStatus.SUCCESS
            }
    }

    @Test
    fun `should update transaction posting details`() = runTest {
        val transaction = transactionRepo.saveTransaction(expectedTransaction)
        val result = SettleTransactionResult(
            transactionId = transaction.id,
            postingId = UUID.randomUUID()
        )
        useCase.invoke(result)

        expectThat(transactionRepo.getTransaction(transaction.id)).isNotNull()
            .and {
                get { postingDetails }.isNotNull().and {
                    get { postingId } isEqualTo result.postingId
                    get { postedAt }.isNotNull()
                    get { status } isEqualTo PostingStatus.SETTLED
                }
            }
    }

    @Test
    fun `should send transaction settled event`() = runTest {
        transactionRepo.saveTransaction(expectedTransaction)

        useCase.invoke(SettleTransactionResult(expectedTransaction.id, UUID.randomUUID()))

        coVerify(exactly = 1) { eventService.sendTransactionSuccessEvent(any()) }
    }
}
