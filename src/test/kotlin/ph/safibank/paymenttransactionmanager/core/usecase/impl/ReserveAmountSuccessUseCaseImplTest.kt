package ph.safibank.paymenttransactionmanager.core.usecase.impl

import io.mockk.coEvery
import io.mockk.coJustRun
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
import ph.safibank.paymenttransactionmanager.core.fee.VendorFee
import ph.safibank.paymenttransactionmanager.core.fraud.FraudStatus
import ph.safibank.paymenttransactionmanager.core.provider.ProviderResult
import ph.safibank.paymenttransactionmanager.core.provider.ProviderService
import ph.safibank.paymenttransactionmanager.core.provider.ProviderStatus
import ph.safibank.paymenttransactionmanager.core.transaction.PostingDetails
import ph.safibank.paymenttransactionmanager.core.transaction.PostingStatus
import ph.safibank.paymenttransactionmanager.core.transaction.ProviderDetails
import ph.safibank.paymenttransactionmanager.core.transaction.ReserveTransactionRequest
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
import strikt.assertions.isTrue
import strikt.assertions.withFirst
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@ExperimentalCoroutinesApi
class ReserveAmountSuccessUseCaseImplTest {

    private val transactionId = UUID.randomUUID()
    private val expectedProviderId = UUID.randomUUID()

    private val eventService = mockk<EventService>()
    private val providerService = mockk<ProviderService>()
    private val transactionRepo = TransactionRepoMockImpl()
    private val useCase = ReserveAmountSuccessUseCaseImpl(transactionRepo, listOf(providerService), eventService)

    private val expectedProviderResult = ProviderResult(
        transactionId = transactionId,
        providerId = expectedProviderId.toString(),
        status = ProviderStatus.PENDING
    )

    private val expectedTransaction = Transaction(
        id = transactionId,
        amount = BigDecimal("500"),
        currency = "PHP",
        type = TransactionType.BILL_PAYMENT,
        status = TransactionStatus.PENDING,
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
        coEvery { providerService.getName() } returns expectedTransaction.providerDetails.provider
        coEvery { providerService.initiatePayment(any()) } returns expectedProviderResult
        coJustRun { eventService.sendTransactionReservedEvent(any()) }
        coJustRun { eventService.sendTransactionSentToProviderEvent(any()) }
    }

    @Test
    fun `should throw error if transaction does not exist`() = runTest {
        val transactionId = UUID.randomUUID()
        expectCatching {
            useCase.invoke(
                ReserveTransactionRequest(
                    transactionId = transactionId
                )
            )
        }.isFailure().isA<TransactionNotFoundException>()
            .get { message } isEqualTo "Transaction: $transactionId not found"
    }

    @ParameterizedTest
    @EnumSource(value = TransactionStatus::class, mode = EnumSource.Mode.EXCLUDE, names = ["PENDING", "WAITING_FOR_APPROVAL"])
    fun `should throw error if transaction status is not PENDING or WAITING_FOR_APPROVAL`(transactionStatus: TransactionStatus) = runTest {
        val expectedTransaction = expectedTransaction.copy(
            status = transactionStatus,
        )

        transactionRepo.saveTransaction(expectedTransaction)

        expectCatching {
            useCase.invoke(
                ReserveTransactionRequest(
                    transactionId = transactionId
                )
            )
        }.isFailure()
            .isA<ValidationException>()
            .and {
                get { errors }
                    .hasSize(1)
                    .withFirst {
                        isEqualTo(ValidationError("Transaction (${expectedTransaction.id}) Status should be ${TransactionStatus.PENDING.name} or ${TransactionStatus.WAITING_FOR_APPROVAL.name}"))
                    }
            }
    }

    @Test
    fun `should throw error if transaction does not have posting details`() = runTest {
        val expectedTransaction = expectedTransaction.copy(
            status = TransactionStatus.PENDING,
            postingDetails = null
        )

        transactionRepo.saveTransaction(expectedTransaction)

        expectCatching {
            useCase.invoke(
                ReserveTransactionRequest(
                    transactionId = transactionId
                )
            )
        }.isFailure()
            .isA<ValidationException>()
            .and {
                get { errors }
                    .hasSize(1)
                    .withFirst {
                        isEqualTo(ValidationError("Transaction (${expectedTransaction.id}) should have posting details"))
                    }
            }
    }

    @ParameterizedTest
    @EnumSource(value = TransactionStatus::class, names = ["PENDING", "WAITING_FOR_APPROVAL"])
    fun `should save transaction posting details if request is valid`(transactionStatus: TransactionStatus) = runTest {
        val expectedPostingId = UUID.randomUUID()
        transactionRepo.saveTransaction(
            expectedTransaction.copy(
                status = transactionStatus
            )
        )

        val before = Instant.now()
        useCase.invoke(
            ReserveTransactionRequest(
                transactionId = transactionId,
                postingId = expectedPostingId
            )
        )

        val transaction = transactionRepo.getTransaction(transactionId)

        expectThat(transaction).isNotNull().and {
            get { postingDetails }.isNotNull().and {
                get { postingId } isEqualTo expectedPostingId
                get { postedAt }.isNotNull().and {
                    get { isAfter(before) }.isTrue()
                    get { isBefore(Instant.now()) }.isTrue()
                }
                get { status } isEqualTo PostingStatus.RESERVED
            }
        }
    }

    @Test
    fun `should save transaction posting details with transaction status SENT_TO_PROVIDER if request is a valid transaction with status PENDING`() = runTest {
        val expectedPostingId = UUID.randomUUID()
        transactionRepo.saveTransaction(
            expectedTransaction.copy(
                status = TransactionStatus.PENDING
            )
        )

        useCase.invoke(
            ReserveTransactionRequest(
                transactionId = transactionId,
                postingId = expectedPostingId
            )
        )

        val transaction = transactionRepo.getTransaction(transactionId)

        expectThat(transaction)
            .isNotNull().and {
                get { status } isEqualTo TransactionStatus.SENT_TO_PROVIDER
            }
    }

    @Test
    fun `should save transaction provider details if request is a valid transaction with status PENDING`() = runTest {
        val expectedPostingId = UUID.randomUUID()
        transactionRepo.saveTransaction(
            expectedTransaction.copy(
                status = TransactionStatus.PENDING
            )
        )

        val expectedProviderResult = ProviderResult(
            transactionId = transactionId,
            providerId = UUID.randomUUID().toString(),
            status = ProviderStatus.PENDING
        )

        coEvery { providerService.initiatePayment(any()) } returns expectedProviderResult

        useCase.invoke(
            ReserveTransactionRequest(
                transactionId = transactionId,
                postingId = expectedPostingId
            )
        )

        val transaction = transactionRepo.getTransaction(transactionId)

        expectThat(transaction)
            .isNotNull().and {
                get { providerDetails }.and {
                    get { providerId } isEqualTo expectedProviderResult.providerId
                    get { providerStatus } isEqualTo expectedProviderResult.status
                }
            }
    }

    @ParameterizedTest
    @EnumSource(value = TransactionStatus::class, names = ["PENDING", "WAITING_FOR_APPROVAL"])
    fun `should send transaction reserved event if request is valid transaction with status PENDING and WAITING FOR APPROVAL`(transactionStatus: TransactionStatus) = runTest {
        val expectedPostingId = UUID.randomUUID()
        transactionRepo.saveTransaction(
            expectedTransaction.copy(
                status = transactionStatus
            )
        )

        val transactionSent = slot<Transaction>()
        coJustRun { eventService.sendTransactionReservedEvent(capture(transactionSent)) }

        val before = Instant.now()
        useCase.invoke(
            ReserveTransactionRequest(
                transactionId = transactionId,
                postingId = expectedPostingId
            )
        )

        expectThat(transactionSent.captured).and {
            get { postingDetails }.isNotNull().and {
                get { postingId } isEqualTo expectedPostingId
                get { postedAt }.isNotNull().and {
                    get { isAfter(before) }.isTrue()
                    get { isBefore(Instant.now()) }.isTrue()
                }
                get { status } isEqualTo PostingStatus.RESERVED
            }
        }
    }

    @Test
    fun `should send transaction sent to provider event if request is a valid transaction with status PENDING`() = runTest {
        val expectedPostingId = UUID.randomUUID()
        transactionRepo.saveTransaction(
            expectedTransaction.copy(
                status = TransactionStatus.PENDING
            )
        )

        val expectedProviderResult = ProviderResult(
            transactionId = transactionId,
            providerId = UUID.randomUUID().toString(),
            status = ProviderStatus.PENDING
        )

        coEvery { providerService.initiatePayment(any()) } returns expectedProviderResult

        val transactionSent = slot<Transaction>()
        coJustRun { eventService.sendTransactionSentToProviderEvent(capture(transactionSent)) }

        useCase.invoke(
            ReserveTransactionRequest(
                transactionId = transactionId,
                postingId = expectedPostingId
            )
        )

        expectThat(transactionSent.captured).and {
            get { providerDetails }.and {
                get { providerId } isEqualTo expectedProviderResult.providerId
                get { providerStatus } isEqualTo expectedProviderResult.status
            }
        }
    }
}
