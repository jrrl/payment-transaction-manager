package ph.safibank.paymenttransactionmanager.core.usecase.impl

import io.mockk.coEvery
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
import ph.safibank.paymenttransactionmanager.core.exception.InfraException
import ph.safibank.paymenttransactionmanager.core.exception.TransactionNotFoundException
import ph.safibank.paymenttransactionmanager.core.exception.ValidationError
import ph.safibank.paymenttransactionmanager.core.exception.ValidationException
import ph.safibank.paymenttransactionmanager.core.fee.CustomerFee
import ph.safibank.paymenttransactionmanager.core.fee.VendorFee
import ph.safibank.paymenttransactionmanager.core.fraud.FraudStatus
import ph.safibank.paymenttransactionmanager.core.posting.PostingResult
import ph.safibank.paymenttransactionmanager.core.posting.PostingService
import ph.safibank.paymenttransactionmanager.core.provider.ProviderStatus
import ph.safibank.paymenttransactionmanager.core.transaction.PostingDetails
import ph.safibank.paymenttransactionmanager.core.transaction.PostingStatus
import ph.safibank.paymenttransactionmanager.core.transaction.ProviderDetails
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
import strikt.assertions.isNotEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import strikt.assertions.withFirst
import java.math.BigDecimal
import java.util.UUID

@ExperimentalCoroutinesApi
class HandleFailedPaymentUseCaseImplTest {
    private val transactionRepo = TransactionRepoMockImpl()
    private val eventService: EventService = mockk()
    private val postingService: PostingService = mockk()

    private val useCase = HandleFailedPaymentUseCaseImpl(transactionRepo, eventService, postingService)

    private val transactionId = UUID.randomUUID()

    private val transaction = Transaction(
        id = transactionId,
        amount = BigDecimal("500"),
        currency = "PHP",
        type = TransactionType.BILL_PAYMENT,
        status = TransactionStatus.SENT_TO_PROVIDER,
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
        coJustRun { eventService.sendTransactionFailedEvent(any()) }
        coEvery { postingService.releaseAmount(any()) } returns PostingResult(UUID.randomUUID())
    }

    @Test
    fun `should throw TransactionNotFoundException if transaction is not existing`() {
        expectCatching {
            useCase.invoke(UUID.randomUUID())
        }.isFailure().isA<TransactionNotFoundException>()
    }

    @ParameterizedTest
    @EnumSource(value = TransactionStatus::class, mode = EnumSource.Mode.EXCLUDE, names = ["SENT_TO_PROVIDER"])
    fun `should throw ValidationException if transaction status is not sent to provider`(expectedStatus: TransactionStatus) = runTest {
        transactionRepo.saveTransaction(transaction.copy(status = expectedStatus))
        expectCatching {
            useCase.invoke(transaction.id)
        }.isFailure()
            .isA<ValidationException>()
            .and {
                get { errors }
                    .hasSize(1)
                    .withFirst {
                        isEqualTo(ValidationError("Transaction (${transaction.id}) Status should be ${TransactionStatus.SENT_TO_PROVIDER}"))
                    }
            }
    }

    @Test
    fun `should throw ValidationException if posting details is null`() = runTest {
        transactionRepo.saveTransaction(
            transaction.copy(
                status = TransactionStatus.SENT_TO_PROVIDER,
                postingDetails = null
            )
        )
        expectCatching {
            useCase.invoke(transaction.id)
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
    fun `should update transaction status to PENDING_RELEASE`() = runTest {
        transactionRepo.saveTransaction(transaction)

        useCase.invoke(transaction.id)

        expectThat(transactionRepo.getTransaction(transaction.id)).isNotNull().and {
            get { status } isEqualTo TransactionStatus.PENDING_RELEASE
        }
    }

    @Test
    fun `should update transaction vendorFee to ZERO`() = runTest {
        transactionRepo.saveTransaction(transaction.copy(vendorFee = VendorFee(BigDecimal("50"), "PHP")))

        useCase.invoke(transaction.id)

        expectThat(transactionRepo.getTransaction(transaction.id)).isNotNull().and {
            get { vendorFee } isEqualTo VendorFee.ZERO_PHP
        }
    }

    @Test
    fun `should update transaction customerFee to ZERO`() = runTest {
        transactionRepo.saveTransaction(transaction.copy(customerFee = CustomerFee(BigDecimal("50"), "PHP")))

        useCase.invoke(transaction.id)

        expectThat(transactionRepo.getTransaction(transaction.id)).isNotNull().and {
            get { customerFee } isEqualTo CustomerFee.ZERO_PHP
        }
    }

    @Test
    fun `should update transaction provider status to FAILED`() = runTest {
        transactionRepo.saveTransaction(transaction)

        useCase.invoke(transaction.id)

        expectThat(transactionRepo.getTransaction(transaction.id)).isNotNull().and {
            get { providerDetails.providerStatus } isEqualTo ProviderStatus.FAILED
        }
    }

    @Test
    fun `should update transaction posting details`() = runTest {
        transactionRepo.saveTransaction(transaction)

        useCase.invoke(transaction.id)

        expectThat(transactionRepo.getTransaction(transaction.id)).isNotNull().and {
            get { postingDetails }.isNotNull().and {
                get { status } isEqualTo PostingStatus.PENDING_RELEASE
                get { batchId } isNotEqualTo transaction.postingDetails?.batchId
                get { postedAt }.isNull()
                get { postingId }.isNull()
            }
        }
    }

    @Test
    fun `should send a failure event`() = runTest {
        transactionRepo.saveTransaction(transaction)

        useCase.invoke(transaction.id)

        coVerify(exactly = 1) { eventService.sendTransactionFailedEvent(any()) }
    }

    @Test
    fun `should release posting details`() = runTest {
        transactionRepo.saveTransaction(transaction)

        useCase.invoke(transaction.id)

        val capturedTransactionEvent = slot<Transaction>()

        coVerify(exactly = 1) {
            postingService.releaseAmount(capture(capturedTransactionEvent))
        }

        expectThat(capturedTransactionEvent.captured) {
            get { id } isEqualTo transaction.id
        }
    }

    @Test
    fun `should not update transaction when releaseAmount event fails`() = runTest {
        transactionRepo.saveTransaction(transaction)
        coEvery { postingService.releaseAmount(any()) } throws InfraException("", RuntimeException())

        expectCatching {
            useCase.invoke(transaction.id)
        }.isFailure().isA<InfraException>()
        expectThat(transactionRepo.getTransaction(transaction.id)).isNotNull().and {
            get { status } isEqualTo TransactionStatus.SENT_TO_PROVIDER
        }
    }

    @Test
    fun `should update transaction even when sendTransactionFailedEvent fails`() = runTest {
        transactionRepo.saveTransaction(transaction)
        coEvery { eventService.sendTransactionFailedEvent(any()) } throws InfraException("", RuntimeException())

        expectCatching {
            useCase.invoke(transaction.id)
        }.isFailure().isA<InfraException>()
        expectThat(transactionRepo.getTransaction(transaction.id)).isNotNull().and {
            get { status } isEqualTo TransactionStatus.PENDING_RELEASE
        }
    }
}
