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
import ph.safibank.paymenttransactionmanager.core.account.AccountDetails
import ph.safibank.paymenttransactionmanager.core.account.AccountService
import ph.safibank.paymenttransactionmanager.core.event.EventService
import ph.safibank.paymenttransactionmanager.core.exception.FraudException
import ph.safibank.paymenttransactionmanager.core.exception.InvalidProviderException
import ph.safibank.paymenttransactionmanager.core.exception.StepUpException
import ph.safibank.paymenttransactionmanager.core.exception.UnknownStatusException
import ph.safibank.paymenttransactionmanager.core.exception.ValidationError
import ph.safibank.paymenttransactionmanager.core.exception.ValidationException
import ph.safibank.paymenttransactionmanager.core.fee.CustomerFee
import ph.safibank.paymenttransactionmanager.core.fee.FeeService
import ph.safibank.paymenttransactionmanager.core.fee.VendorFee
import ph.safibank.paymenttransactionmanager.core.fraud.FraudService
import ph.safibank.paymenttransactionmanager.core.fraud.FraudStatus
import ph.safibank.paymenttransactionmanager.core.merchant.MerchantService
import ph.safibank.paymenttransactionmanager.core.merchant.ProductDetails
import ph.safibank.paymenttransactionmanager.core.posting.PostingResult
import ph.safibank.paymenttransactionmanager.core.posting.PostingService
import ph.safibank.paymenttransactionmanager.core.provider.ProviderService
import ph.safibank.paymenttransactionmanager.core.transaction.Transaction
import ph.safibank.paymenttransactionmanager.core.transaction.TransactionStatus
import ph.safibank.paymenttransactionmanager.core.transaction.TransactionType
import ph.safibank.paymenttransactionmanager.core.usecase.AirtimeLoadRequest
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import strikt.assertions.isSuccess
import strikt.assertions.withFirst
import strikt.assertions.withLast
import java.math.BigDecimal
import java.util.UUID

@ExperimentalCoroutinesApi
internal class CreateAirtimeLoadUseCaseImplTest {

    private val transactionRepo = TransactionRepoMockImpl()
    private val accountService = mockk<AccountService>()
    private val merchantService = mockk<MerchantService>()
    private val fraudService = mockk<FraudService>()
    private val feeService = mockk<FeeService>()
    private val postingService = mockk<PostingService>()
    private val providerServiceA = mockk<ProviderService>()
    private val providerServiceB = mockk<ProviderService>()
    private val eventService = mockk<EventService>()

    private val useCase = CreateAirtimeLoadUseCaseImpl(
        accountService,
        merchantService,
        providers = listOf(providerServiceA, providerServiceB),
        fraudService,
        transactionRepo,
        eventService,
        feeService,
        postingService
    )

    private val defaultAccountNumber = TestUtils.randomAccountNumber()

    private val defaultAccountDetails = AccountDetails(
        id = UUID.randomUUID(),
        customerId = UUID.randomUUID(),
        accountNumber = defaultAccountNumber,
        active = true,
        balance = BigDecimal("1000"),
        currency = "PHP"
    )

    private val defaultProductDetails = ProductDetails(
        name = "SMARTLOAD100",
        code = "SMARTLOAD100",
        sellingPrice = BigDecimal("100"),
        provideName = "SMART"
    )

    private val defaultRequest = AirtimeLoadRequest(
        id = UUID.randomUUID(),
        accountNumber = defaultAccountNumber,
        amount = BigDecimal("100"),
        currency = "PHP",
        product = "SMARTLOAD100",
        recipientMobile = "+639151234567",
        recipientName = "Test Mobile User"
    )

    @BeforeEach
    fun setUp() {
        transactionRepo.reset()
        clearAllMocks()
        coEvery { merchantService.getProduct(any()) } returns defaultProductDetails
        coEvery { accountService.getAccountDetails(defaultAccountNumber) } returns defaultAccountDetails
        coEvery { feeService.calculateVendorFee(any()) } returns VendorFee.ZERO_PHP
        coEvery { feeService.calculateCustomerFee(any()) } returns CustomerFee.ZERO_PHP
        coEvery { providerServiceA.usableForTransaction(any(), any()) } returns true
        coEvery { providerServiceB.usableForTransaction(any(), any()) } returns false
        coEvery { providerServiceA.getName() } returns "providerA"
        coEvery { providerServiceB.getName() } returns "providerB"
        coEvery { postingService.reserveTransactionAmount(any()) } returns PostingResult(UUID.randomUUID())
        coJustRun { eventService.sendTransactionCreatedEvent(any()) }
        coJustRun { eventService.sendTransactionApprovedEvent(any()) }
    }

    @Test
    fun `should throw a validation exception when accountNumber does exist`() = runTest {
        coEvery { accountService.getAccountDetails(any()) } returns null

        expectCatching {
            useCase.invoke(defaultRequest)
        }
            .isFailure()
            .isA<ValidationException>()
            .and {
                get { errors }
                    .hasSize(1)
                    .withFirst {
                        isEqualTo(ValidationError("Account ${defaultRequest.accountNumber} does not exist"))
                    }
            }
    }

    @Test
    fun `should throw a validation exception when account does not have enough balance`() = runTest {
        coEvery { accountService.getAccountDetails(any()) } returns
            defaultAccountDetails.copy(balance = BigDecimal.TEN)

        expectCatching {
            useCase.invoke(defaultRequest)
        }
            .isFailure()
            .isA<ValidationException>()
            .and {
                get { errors }
                    .hasSize(1)
                    .withFirst {
                        isEqualTo(ValidationError("Account $defaultAccountNumber has insufficient balance"))
                    }
            }
    }

    @Test
    fun `should throw a validation exception when request amount is different from the product amount`() = runTest {
        val requestAmount = BigDecimal.TEN

        expectCatching {
            useCase.invoke(defaultRequest.copy(amount = requestAmount))
        }
            .isFailure()
            .isA<ValidationException>()
            .and {
                get { errors }
                    .hasSize(1)
                    .withFirst {
                        isEqualTo(ValidationError("Request amount $requestAmount is not equal to product selling price ${defaultProductDetails.sellingPrice}"))
                    }
            }
    }

    @Test
    fun `should throw a validation exception when account is inactive`() = runTest {
        coEvery { accountService.getAccountDetails(defaultAccountNumber) } returns defaultAccountDetails.copy(active = false)

        expectCatching {
            useCase.invoke(defaultRequest)
        }
            .isFailure()
            .isA<ValidationException>()
            .and {
                get { errors }
                    .hasSize(1)
                    .withFirst {
                        isEqualTo(ValidationError("Account $defaultAccountNumber is not active"))
                    }
            }
    }

    @Test
    fun `should throw a validation exception if product does not exist`() = runTest {
        coEvery { merchantService.getProduct(defaultRequest.product) } returns null

        expectCatching {
            useCase.invoke(defaultRequest)
        }
            .isFailure()
            .isA<ValidationException>()
            .and {
                get { errors }
                    .hasSize(1)
                    .withFirst {
                        isEqualTo(ValidationError("Product ${defaultRequest.product} does not exist"))
                    }
            }
    }

    @Test
    fun `should throw multiple validation errors if there are multiple validations`() = runTest {
        coEvery { accountService.getAccountDetails(defaultAccountNumber) } returns
            defaultAccountDetails.copy(active = false, balance = BigDecimal.TEN)

        expectCatching {
            useCase.invoke(defaultRequest)
        }
            .isFailure()
            .isA<ValidationException>()
            .and {
                get { errors }
                    .hasSize(2)
                    .withFirst {
                        isEqualTo(ValidationError("Account ${defaultAccountDetails.accountNumber} has insufficient balance"))
                    }
                    .withLast {
                        isEqualTo(ValidationError("Account $defaultAccountNumber is not active"))
                    }
            }
    }

    @Test
    fun `should throw an invalid provider exception when no valid provider found`(): Unit = runTest {
        coEvery { providerServiceA.usableForTransaction(TransactionType.AIRTIME_LOAD, any()) } returns false
        coEvery { providerServiceB.usableForTransaction(TransactionType.AIRTIME_LOAD, any()) } returns false

        expectCatching {
            useCase.invoke(defaultRequest)
        }
            .isFailure()
            .isA<InvalidProviderException>()
            .and {
                get { message } isEqualTo "Invalid provider found for ${defaultRequest.product}"
            }
    }

    @Test
    fun `should throw an invalid provider exception when multiple valid providers found`(): Unit = runTest {
        coEvery { providerServiceA.usableForTransaction(any(), any()) } returns true
        coEvery { providerServiceB.usableForTransaction(any(), any()) } returns true

        expectCatching {
            useCase.invoke(defaultRequest)
        }
            .isFailure()
            .isA<InvalidProviderException>()
            .and {
                get { message } isEqualTo "Invalid provider found for ${defaultRequest.product}"
            }
    }

    @Test
    fun `should throw FraudException when fraud status is rejected`() = runTest {
        coEvery { fraudService.determineFraudStatus(any()) } returns FraudStatus.REJECTED

        expectCatching {
            useCase.invoke(defaultRequest)
        }
            .isFailure()
            .isA<FraudException>()
    }

    @Test
    fun `should save transaction with transaction status as FAILED when fraud status is rejected`() = runTest {
        coEvery { fraudService.determineFraudStatus(any()) } returns FraudStatus.REJECTED

        expectCatching {
            useCase.invoke(defaultRequest)
        }

        expectThat(transactionRepo.getTransaction(defaultRequest.id))
            .isNotNull()
            .and { get { status } isEqualTo TransactionStatus.FAILED }
    }

    @ParameterizedTest
    @EnumSource(FraudStatus::class, names = ["STEP_UP_LEVEL1", "STEP_UP_LEVEL2", "STEP_UP_LEVEL3", "STEP_UP_LEVEL4"])
    fun `should throw StepUpException when fraud status is step up`(fraudStatus: FraudStatus) = runTest {
        coEvery { fraudService.determineFraudStatus(any()) } returns fraudStatus

        expectCatching {
            useCase.invoke(defaultRequest)
        }
            .isFailure()
            .isA<StepUpException>()
            .get { message } isEqualTo "Step-up error: $fraudStatus"
    }

    @ParameterizedTest
    @EnumSource(FraudStatus::class, names = ["STEP_UP_LEVEL1", "STEP_UP_LEVEL2", "STEP_UP_LEVEL3", "STEP_UP_LEVEL4"])
    fun `should save transaction with transaction status as FAILED when fraud status is step up`(fraudStatus: FraudStatus) = runTest {
        coEvery { fraudService.determineFraudStatus(any()) } returns fraudStatus

        expectCatching {
            useCase.invoke(defaultRequest)
        }

        expectThat(transactionRepo.getTransaction(defaultRequest.id))
            .isNotNull()
            .and { get { status } isEqualTo TransactionStatus.FAILED }
    }

    @Test
    fun `should throw a unknown status exception when fraud status is unknown`(): Unit = runTest {
        coEvery { fraudService.determineFraudStatus(any()) } returns FraudStatus.UNKNOWN

        expectCatching {
            useCase.invoke(defaultRequest)
        }
            .isFailure()
            .isA<UnknownStatusException>()
            .and {
                get { message } isEqualTo "Unknown fraud status for transaction ${defaultRequest.id}"
            }
    }

    @ParameterizedTest
    @EnumSource(FraudStatus::class, names = ["APPROVED", "MANUAL_APPROVAL"])
    fun `should save transaction with transaction status as PENDING when fraud status is not a failure`(fraudStatus: FraudStatus) = runTest {
        coEvery { fraudService.determineFraudStatus(any()) } returns fraudStatus

        expectCatching {
            useCase.invoke(defaultRequest)
        }

        expectThat(transactionRepo.getTransaction(defaultRequest.id))
            .isNotNull()
            .and {
                get { status } isEqualTo TransactionStatus.PENDING
            }
    }

    @Test
    fun `should get customer fee and vendor fee when transaction with fraud status as APPROVED`() = runTest {
        coEvery { fraudService.determineFraudStatus(any()) } returns FraudStatus.APPROVED

        val expectedCustomerFee = CustomerFee(
            amount = BigDecimal("10"),
            currency = "PHP",
            subscriptionId = UUID.randomUUID()
        )
        val expectedVendorFee = VendorFee(
            amount = BigDecimal("15"),
            currency = "USD"
        )

        coEvery { feeService.calculateCustomerFee(any()) } returns expectedCustomerFee
        coEvery { feeService.calculateVendorFee(any()) } returns expectedVendorFee

        expectCatching { useCase.invoke(defaultRequest) }.isSuccess()

        expectThat(transactionRepo.getTransaction(defaultRequest.id))
            .isNotNull()
            .and {
                get { customerFee }.and {
                    get { amount } isEqualTo expectedCustomerFee.amount
                    get { currency } isEqualTo expectedCustomerFee.currency
                    get { subscriptionId } isEqualTo expectedCustomerFee.subscriptionId
                }
                get { vendorFee }.and {
                    get { amount } isEqualTo expectedVendorFee.amount
                    get { currency } isEqualTo expectedVendorFee.currency
                }
            }
    }

    @Test
    fun `should reserve amount when fraud status APPROVED`(): Unit = runTest {
        coEvery { fraudService.determineFraudStatus(any()) } returns FraudStatus.APPROVED

        val expectedPostingResult = PostingResult(UUID.randomUUID())
        coEvery { postingService.reserveTransactionAmount(any()) } returns expectedPostingResult

        expectCatching { useCase.invoke(defaultRequest) }.isSuccess()

        coVerify(exactly = 1) { postingService.reserveTransactionAmount(any()) }

        expectThat(transactionRepo.getTransaction(defaultRequest.id))
            .isNotNull()
            .and {
                get { postingDetails }.isNotNull().and {
                    get { batchId } isEqualTo expectedPostingResult.batchId
                }
            }
    }

    @Test
    fun `should return transaction request with status as pending when fraud status is approved`(): Unit = runTest {
        coEvery { fraudService.determineFraudStatus(any()) } returns FraudStatus.APPROVED

        expectCatching { useCase.invoke(defaultRequest) }
            .isSuccess()
            .and {
                get { transactionId } isEqualTo defaultRequest.id
                get { transactionStatus } isEqualTo TransactionStatus.PENDING
            }
    }

    @ParameterizedTest
    @EnumSource(FraudStatus::class, mode = EnumSource.Mode.EXCLUDE, names = ["UNKNOWN"])
    fun `should save transaction with expected fraud status`(expectedFraudStatus: FraudStatus) = runTest {
        coEvery { fraudService.determineFraudStatus(any()) } returns expectedFraudStatus

        expectCatching { useCase.invoke(defaultRequest) }
        expectThat(transactionRepo.getTransaction(defaultRequest.id))
            .isNotNull()
            .and {
                get { fraudStatus } isEqualTo expectedFraudStatus
            }
    }

    @ParameterizedTest
    @EnumSource(value = FraudStatus::class, mode = EnumSource.Mode.EXCLUDE, names = ["UNKNOWN"])
    fun `should save account details when a transaction is saved`(expectedFraudResult: FraudStatus): Unit = runTest {
        val request = defaultRequest.copy(accountNumber = TestUtils.randomAccountNumber())

        coEvery { fraudService.determineFraudStatus(any()) } returns expectedFraudResult

        val expectedAccountDetails = defaultAccountDetails.copy(accountNumber = request.accountNumber)
        coEvery { accountService.getAccountDetails(request.accountNumber) } returns expectedAccountDetails

        expectCatching { useCase.invoke(request) }
        expectThat(transactionRepo.getTransaction(request.id))
            .isNotNull()
            .and {
                get { senderDetails }.and {
                    get { accountNumber } isEqualTo expectedAccountDetails.accountNumber
                    get { accountId } isEqualTo expectedAccountDetails.id
                    get { customerId } isEqualTo expectedAccountDetails.customerId
                }
            }
    }

    @ParameterizedTest
    @EnumSource(value = FraudStatus::class, mode = EnumSource.Mode.EXCLUDE, names = ["UNKNOWN"])
    fun `should save provider details when a transaction is saved`(expectedFraudResult: FraudStatus): Unit = runTest {
        val request = defaultRequest.copy(product = "test")

        coEvery { fraudService.determineFraudStatus(any()) } returns expectedFraudResult

        val expectedProvider = providerServiceA
        val expectedMerchantDetails = defaultProductDetails.copy(code = request.product)
        coEvery { expectedProvider.usableForTransaction(any(), any()) } returns true
        coEvery { expectedProvider.getName() } returns "expectedProvider"
        coEvery { merchantService.getProduct(request.product) } returns expectedMerchantDetails

        expectCatching { useCase.invoke(request) }

        expectThat(transactionRepo.getTransaction(request.id))
            .isNotNull()
            .and {
                get { providerDetails }.and {
                    get { provider } isEqualTo expectedProvider.getName()
                    get { providerId }.isNull()
                    get { merchantCode } isEqualTo expectedMerchantDetails.code
                    get { merchantName } isEqualTo expectedMerchantDetails.name
                }
            }
    }

    @ParameterizedTest
    @EnumSource(value = FraudStatus::class, mode = EnumSource.Mode.EXCLUDE, names = ["UNKNOWN"])
    fun `should send a created transaction event when a transaction is saved`(expectedFraudResult: FraudStatus): Unit = runTest {
        val request = defaultRequest
        coEvery { fraudService.determineFraudStatus(any()) } returns expectedFraudResult

        val transactionSent = slot<Transaction>()
        coJustRun { eventService.sendTransactionCreatedEvent(capture(transactionSent)) }

        expectCatching { useCase.invoke(request) }
        val expectedTransaction = transactionRepo.getTransaction(request.id)!!
        expectThat(transactionSent.captured).and {
            get { id } isEqualTo expectedTransaction.id
            get { amount } isEqualTo expectedTransaction.amount
            get { currency } isEqualTo expectedTransaction.currency
            get { type } isEqualTo expectedTransaction.type
            get { status } isEqualTo expectedTransaction.status
            get { senderDetails } isEqualTo expectedTransaction.senderDetails
            get { providerDetails } isEqualTo expectedTransaction.providerDetails
            get { fraudStatus } isEqualTo expectedTransaction.fraudStatus
        }
    }
}
