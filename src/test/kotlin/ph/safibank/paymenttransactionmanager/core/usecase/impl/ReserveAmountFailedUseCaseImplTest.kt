package ph.safibank.paymenttransactionmanager.core.usecase.impl

import io.mockk.clearAllMocks
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
import strikt.assertions.withFirst
import strikt.assertions.withLast
import java.math.BigDecimal
import java.util.UUID

@ExperimentalCoroutinesApi
internal class ReserveAmountFailedUseCaseImplTest {

    private val transactionRepo = TransactionRepoMockImpl()
    private val eventService = mockk<EventService>()
    private val feeService = mockk<FeeService>()

    private val useCase = ReserveAmountFailedUseCaseImpl(
        transactionRepo,
        eventService,
        feeService,
    )

    private val expectedTransaction = Transaction(
        id = UUID.randomUUID(),
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
    fun setUp() {
        transactionRepo.reset()
        clearAllMocks()
        coJustRun { eventService.sendTransactionFailedEvent(any()) }
        coJustRun { feeService.revertCustomerFee(any()) }
    }

    @Test
    fun `should throw TransactionNotFoundException if transaction is not existing`() {
        expectCatching {
            useCase.invoke(
                ReserveTransactionRequest(
                    transactionId = UUID.randomUUID()
                )
            )
        }.isFailure()
            .isA<TransactionNotFoundException>()
    }

    @ParameterizedTest
    @EnumSource(value = TransactionStatus::class, mode = EnumSource.Mode.EXCLUDE, names = ["PENDING", "WAITING_FOR_APPROVAL"])
    fun `should throw ValidationException if transaction status is not pending`(transactionStatus: TransactionStatus) = runTest {
        val expectedTransaction = expectedTransaction.copy(
            status = transactionStatus,
        )

        transactionRepo.saveTransaction(expectedTransaction)

        expectCatching {
            useCase.invoke(
                ReserveTransactionRequest(
                    transactionId = expectedTransaction.id
                )
            )
        }.isFailure()
            .isA<ValidationException>()
            .and {
                get { errors }
                    .hasSize(1)
                    .withFirst {
                        isEqualTo(ValidationError("Transaction (${expectedTransaction.id}) Status should be pending"))
                    }
            }
    }

    @Test
    fun `should throw ValidationException if posting details is empty`() = runTest {
        val expectedTransaction = expectedTransaction.copy(
            postingDetails = null,
        )

        transactionRepo.saveTransaction(expectedTransaction)

        expectCatching {
            useCase.invoke(
                ReserveTransactionRequest(
                    transactionId = expectedTransaction.id
                )
            )
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
    fun `should throw ValidationException with multiple errors`() = runTest {
        val expectedTransaction = expectedTransaction.copy(
            status = TransactionStatus.INITIATED,
            postingDetails = null,
        )

        transactionRepo.saveTransaction(expectedTransaction)

        expectCatching {
            useCase.invoke(
                ReserveTransactionRequest(
                    transactionId = expectedTransaction.id
                )
            )
        }.isFailure()
            .isA<ValidationException>()
            .and {
                get { errors }
                    .hasSize(2)
                    .withFirst {
                        isEqualTo(ValidationError("Transaction (${expectedTransaction.id}) Status should be pending"))
                    }
                    .withLast {
                        isEqualTo(ValidationError("Transaction (${expectedTransaction.id}) posting details should not be null"))
                    }
            }
    }

    @Test
    fun `should update transaction status to failed`() = runTest {
        transactionRepo.saveTransaction(expectedTransaction)

        useCase.invoke(
            ReserveTransactionRequest(
                transactionId = expectedTransaction.id
            )
        )

        expectThat(transactionRepo.getTransaction(expectedTransaction.id))
            .isNotNull().and {
                get { status } isEqualTo TransactionStatus.FAILED
            }
    }

    @Test
    fun `should send a failure event`() = runTest {
        transactionRepo.saveTransaction(expectedTransaction)

        val transactionSent = slot<Transaction>()
        coJustRun { eventService.sendTransactionFailedEvent(capture(transactionSent)) }

        useCase.invoke(
            ReserveTransactionRequest(
                transactionId = expectedTransaction.id
            )
        )

        expectThat(transactionSent.captured).and {
            get { id } isEqualTo expectedTransaction.id
            get { amount } isEqualTo expectedTransaction.amount
            get { currency } isEqualTo expectedTransaction.currency
            get { type } isEqualTo expectedTransaction.type
            get { status } isEqualTo TransactionStatus.FAILED
            get { senderDetails } isEqualTo expectedTransaction.senderDetails
            get { providerDetails } isEqualTo expectedTransaction.providerDetails
            get { fraudStatus } isEqualTo expectedTransaction.fraudStatus
            get { customerFee } isEqualTo CustomerFee.ZERO_PHP
            get { vendorFee } isEqualTo VendorFee.ZERO_PHP
            get { postingDetails }.isNotNull().and {
                get { batchId } isEqualTo expectedTransaction.postingDetails!!.batchId
                get { status } isEqualTo PostingStatus.FAILED
            }
        }
    }

    @Test
    fun `should reverse the customer fee`() = runTest {
        transactionRepo.saveTransaction(expectedTransaction)

        useCase.invoke(
            ReserveTransactionRequest(
                transactionId = expectedTransaction.id
            )
        )

        coVerify(exactly = 1) { feeService.revertCustomerFee(expectedTransaction.customerFee) }
    }

    @Test
    fun `should set the customer fee to ZERO`() = runTest {
        transactionRepo.saveTransaction(expectedTransaction)

        useCase.invoke(
            ReserveTransactionRequest(
                transactionId = expectedTransaction.id
            )
        )

        expectThat(transactionRepo.getTransaction(expectedTransaction.id))
            .isNotNull().and {
                get { customerFee } isEqualTo CustomerFee.ZERO_PHP
            }
    }

    @Test
    fun `should set the vendor fee to ZERO`() = runTest {
        transactionRepo.saveTransaction(expectedTransaction)

        useCase.invoke(
            ReserveTransactionRequest(
                transactionId = expectedTransaction.id
            )
        )

        expectThat(transactionRepo.getTransaction(expectedTransaction.id))
            .isNotNull().and {
                get { vendorFee } isEqualTo VendorFee.ZERO_PHP
            }
    }

    @Test
    fun `should set posting status to failed`() = runTest {
        transactionRepo.saveTransaction(expectedTransaction)

        useCase.invoke(
            ReserveTransactionRequest(
                transactionId = expectedTransaction.id
            )
        )

        expectThat(transactionRepo.getTransaction(expectedTransaction.id))
            .isNotNull().and {
                get { postingDetails }.isNotNull().and {
                    get { postingId } isEqualTo expectedTransaction.postingDetails!!.postingId
                    get { status } isEqualTo PostingStatus.FAILED
                }
            }
    }

    @Test
    fun `should not update fees when revert customer fee throws an error`() = runTest {
        transactionRepo.saveTransaction(expectedTransaction)

        coEvery { feeService.revertCustomerFee(any()) } throws Exception()

        expectCatching {
            useCase.invoke(
                ReserveTransactionRequest(
                    transactionId = expectedTransaction.id
                )
            )
        }

        expectThat(transactionRepo.getTransaction(expectedTransaction.id))
            .isNotNull().and {
                get { customerFee } isEqualTo expectedTransaction.customerFee
                get { vendorFee } isEqualTo expectedTransaction.vendorFee
            }
    }
}
