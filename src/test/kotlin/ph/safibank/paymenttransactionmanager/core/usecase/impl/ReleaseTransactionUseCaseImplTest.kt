package ph.safibank.paymenttransactionmanager.core.usecase.impl

import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
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
import ph.safibank.paymenttransactionmanager.core.fee.FeeService
import ph.safibank.paymenttransactionmanager.core.fee.VendorFee
import ph.safibank.paymenttransactionmanager.core.fraud.FraudStatus
import ph.safibank.paymenttransactionmanager.core.transaction.PostingDetails
import ph.safibank.paymenttransactionmanager.core.transaction.PostingStatus
import ph.safibank.paymenttransactionmanager.core.transaction.ProviderDetails
import ph.safibank.paymenttransactionmanager.core.transaction.ReleaseTransactionResult
import ph.safibank.paymenttransactionmanager.core.transaction.SenderDetails
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
class ReleaseTransactionUseCaseImplTest {
    private val transactionRepo = TransactionRepoMockImpl()
    private val eventService: EventService = mockk()
    private val feeService: FeeService = mockk()
    private val useCase = RelaseTransactionUseCaseImpl(transactionRepo, eventService, feeService)

    private val transactionId = UUID.randomUUID()

    private val transaction = Transaction(
        id = transactionId,
        amount = BigDecimal("500"),
        currency = "PHP",
        type = TransactionType.BILL_PAYMENT,
        status = TransactionStatus.PENDING_RELEASE,
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
        coJustRun { eventService.sendTransactionReleasedEvent(any()) }
        coJustRun { feeService.revertCustomerFee(any()) }
    }

    @Test
    fun `should throw TransactionNotFoundException if transaction is not existing`() {
        expectCatching {
            useCase.invoke(
                ReleaseTransactionResult(
                    transactionId = UUID.randomUUID(),
                    postingId = UUID.randomUUID()
                )
            )
        }.isFailure().isA<TransactionNotFoundException>()
    }

    @ParameterizedTest
    @EnumSource(value = TransactionStatus::class, mode = EnumSource.Mode.EXCLUDE, names = ["PENDING_RELEASE"])
    fun `should throw ValidationException if transaction status is not sent to provider`(expectedStatus: TransactionStatus) = runTest {
        transactionRepo.saveTransaction(transaction.copy(status = expectedStatus))
        expectCatching {
            useCase.invoke(
                ReleaseTransactionResult(
                    transactionId = transaction.id,
                    postingId = UUID.randomUUID()
                )
            )
        }.isFailure()
            .isA<ValidationException>()
            .and {
                get { errors }
                    .hasSize(1)
                    .withFirst {
                        isEqualTo(ValidationError("Transaction (${transaction.id}) Status should be ${TransactionStatus.PENDING_RELEASE}"))
                    }
            }
    }

    @Test
    fun `should throw ValidationException if posting details is null`() = runTest {
        transactionRepo.saveTransaction(
            transaction.copy(
                status = TransactionStatus.PENDING_RELEASE,
                postingDetails = null
            )
        )
        expectCatching {
            useCase.invoke(
                ReleaseTransactionResult(
                    transactionId = transaction.id,
                    postingId = UUID.randomUUID()
                )
            )
        }.isFailure()
            .isA<ValidationException>()
            .and {
                get { errors }
                    .hasSize(1)
                    .withFirst {
                        isEqualTo(ValidationError("Transaction (${transaction.id}) posting details should not be null"))
                    }
            }
    }

    @Test
    fun `should update transaction status to FAILED`() = runTest {
        transactionRepo.saveTransaction(transaction)

        useCase.invoke(
            ReleaseTransactionResult(
                transactionId = transaction.id,
                postingId = UUID.randomUUID()
            )
        )

        expectThat(transactionRepo.getTransaction(transaction.id)).isNotNull().and {
            get { status } isEqualTo TransactionStatus.FAILED
        }
    }

    @Test
    fun `should update transaction vendorFee to ZERO`() = runTest {
        transactionRepo.saveTransaction(transaction.copy(vendorFee = VendorFee(BigDecimal("50"), "PHP")))

        useCase.invoke(
            ReleaseTransactionResult(
                transactionId = transaction.id,
                postingId = UUID.randomUUID()
            )
        )

        expectThat(transactionRepo.getTransaction(transaction.id)).isNotNull().and {
            get { vendorFee } isEqualTo VendorFee.ZERO_PHP
        }
    }

    @Test
    fun `should update transaction customerFee to ZERO`() = runTest {
        transactionRepo.saveTransaction(transaction.copy(customerFee = CustomerFee(BigDecimal("50"), "PHP")))

        useCase.invoke(
            ReleaseTransactionResult(
                transactionId = transaction.id,
                postingId = UUID.randomUUID()
            )
        )

        expectThat(transactionRepo.getTransaction(transaction.id)).isNotNull().and {
            get { customerFee } isEqualTo CustomerFee.ZERO_PHP
        }
    }

    @Test
    fun `should update transaction posting details`() = runTest {
        transactionRepo.saveTransaction(transaction)
        val result = ReleaseTransactionResult(
            transactionId = transaction.id,
            postingId = UUID.randomUUID()
        )
        useCase.invoke(result)

        expectThat(transactionRepo.getTransaction(transaction.id)).isNotNull().and {
            get { postingDetails }.isNotNull().and {
                get { postingId } isEqualTo result.postingId
                get { postedAt }.isNotNull()
                get { status } isEqualTo PostingStatus.RELEASED
            }
        }
    }

    @Test
    fun `should send a sendTransactionReleasedEvent`() = runTest {
        transactionRepo.saveTransaction(transaction)

        useCase.invoke(
            ReleaseTransactionResult(
                transactionId = transaction.id,
                postingId = UUID.randomUUID()
            )
        )

        coVerify(exactly = 1) { eventService.sendTransactionReleasedEvent(any()) }
    }

    @Test
    fun `should revert fees`() = runTest {
        transactionRepo.saveTransaction(transaction)

        useCase.invoke(
            ReleaseTransactionResult(
                transactionId = transaction.id,
                postingId = UUID.randomUUID()
            )
        )

        val captureCustomerFee = slot<CustomerFee>()

        coVerify(exactly = 1) {
            feeService.revertCustomerFee(capture(captureCustomerFee))
        }

        expectThat(captureCustomerFee.captured) {
            get { id } isEqualTo transaction.customerFee.id
        }
    }
}
