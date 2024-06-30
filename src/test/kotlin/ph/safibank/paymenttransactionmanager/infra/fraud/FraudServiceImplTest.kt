package ph.safibank.paymenttransactionmanager.infra.fraud

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.jsonResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.testcontainers.shaded.org.bouncycastle.util.test.SimpleTest.runTest
import ph.safibank.common.testutils.KafkaBaseTest
import ph.safibank.paymenttransactionmanager.TestUtils
import ph.safibank.paymenttransactionmanager.core.exception.ValidationException
import ph.safibank.paymenttransactionmanager.core.fee.CustomerFee
import ph.safibank.paymenttransactionmanager.core.fee.VendorFee
import ph.safibank.paymenttransactionmanager.core.fraud.FraudService
import ph.safibank.paymenttransactionmanager.core.fraud.FraudStatus
import ph.safibank.paymenttransactionmanager.core.provider.ProviderStatus
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
import strikt.assertions.withFirst
import java.math.BigDecimal
import java.util.UUID

@MicronautTest
class FraudServiceImplTest : KafkaBaseTest() {
    @Inject
    private lateinit var fraudService: FraudService

    private val wireMockServer = WireMockServer(WireMockConfiguration.options().dynamicPort())

    private val defaultTransaction = Transaction(
        id = UUID.randomUUID(),
        amount = BigDecimal("100"),
        currency = "PHP",
        status = TransactionStatus.PENDING,
        type = TransactionType.BILL_PAYMENT,
        senderDetails = SenderDetails(
            accountNumber = TestUtils.randomAccountNumber(),
            accountId = UUID.randomUUID(),
            customerId = UUID.randomUUID()
        ),
        customerFee = CustomerFee.ZERO_PHP,
        vendorFee = VendorFee.ZERO_PHP,
        fraudStatus = FraudStatus.UNKNOWN,
        providerDetails = ProviderDetails(
            providerStatus = ProviderStatus.NOT_SENT,
            provider = "provider",
            merchantCode = "BDO",
            merchantName = "Banco de Oro"
        )
    )

    private fun mapFraudStatus(source: String): FraudStatus = when (source) {
        "APPROVED" -> FraudStatus.APPROVED
        "REJECTED" -> FraudStatus.REJECTED
        "STEP_UP_NEEDED_LEVEL1" -> FraudStatus.STEP_UP_LEVEL1
        "STEP_UP_NEEDED_LEVEL2" -> FraudStatus.STEP_UP_LEVEL2
        "STEP_UP_NEEDED_LEVEL3" -> FraudStatus.STEP_UP_LEVEL3
        "STEP_UP_NEEDED_LEVEL4" -> FraudStatus.STEP_UP_LEVEL4
        "MANUAL_APPROVAL_NEEDED" -> FraudStatus.MANUAL_APPROVAL
        else -> FraudStatus.UNKNOWN
    }

    @ParameterizedTest
    @EnumSource(value = FraudStatus::class, mode = EnumSource.Mode.EXCLUDE, names = ["UNKNOWN"])
    fun `should throw validation exception when transaction fraud status is not unknown`(expectedFraudStatus: FraudStatus) {
        val transaction = defaultTransaction.copy(fraudStatus = expectedFraudStatus)

        expectCatching {
            fraudService.determineFraudStatus(transaction)
        }
            .isFailure()
            .isA<ValidationException>()
            .and {
                get { errors }
                    .hasSize(1)
                    .withFirst {
                        get { value } isEqualTo "Fraud status already set for transaction ${transaction.id}"
                    }
            }
    }

    @ParameterizedTest
    @CsvSource(
        "APPROVED",
        "REJECTED",
        "STEP_UP_NEEDED_LEVEL1",
        "STEP_UP_NEEDED_LEVEL2",
        "STEP_UP_NEEDED_LEVEL3",
        "STEP_UP_NEEDED_LEVEL4",
        "MANUAL_APPROVAL_NEEDED"
    )
    fun `should map fraud service response to correct fraud status when transaction is valid`(slackerFraudResponse: String) = runTest {
        val transaction = defaultTransaction

        wireMockServer.stubFor(
            post("/slacker/transaction/validate")
                .withRequestBody(
                    equalToJson(
                        """
                            {
                              "transactionId": "${transaction.id}",
                              "accountId": "${transaction.senderDetails.accountId}",
                              "customerId": "${transaction.senderDetails.customerId}",
                              "transactionType": "INTERBANK_TRANSACTION",
                              "transactionDirection": "OUTGOING",
                              "amount": ${transaction.amount},
                              "currency": "${transaction.currency}"
                              
                            }
                        """.trimIndent(),
                        true,
                        true
                    )
                )
                .willReturn(
                    jsonResponse(
                        """
                            {
                                "transactionId": "${transaction.id}", 
                                "status": "$slackerFraudResponse", 
                                "timestamp": "2023-01-31T03:50:58.290Z"
                            }
                        """.trimIndent(),
                        200
                    )
                )
        )

        val result = fraudService.determineFraudStatus(transaction)

        expectThat(result) isEqualTo mapFraudStatus(slackerFraudResponse)
    }

    override fun getProperties(): MutableMap<String, String> {
        wireMockServer.start()
        return mutableMapOf(
            "slacker-manager-api-client-base-path" to wireMockServer.baseUrl()
        )
    }
}
