package ph.safibank.paymenttransactionmanager.infra.fee

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.micronaut.test.support.TestPropertyProvider
import jakarta.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import ph.safibank.feemanager.client.model.TransactionTypeDto
import ph.safibank.paymenttransactionmanager.TestUtils
import ph.safibank.paymenttransactionmanager.core.fee.CustomerFee
import ph.safibank.paymenttransactionmanager.core.fee.VendorFee
import ph.safibank.paymenttransactionmanager.core.transaction.TransactionType
import ph.safibank.productmanager.client.model.SubscriptionOperationType
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.math.BigDecimal
import java.util.UUID

@ExperimentalCoroutinesApi
@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FeeServiceImplTest : TestPropertyProvider {
    @Inject
    lateinit var feeService: FeeServiceImpl

    private val wireMockServer = WireMockServer(WireMockConfiguration.options().dynamicPort())

    private val defaultTransaction = TestUtils.defaultTransaction

    @BeforeEach
    fun setUp() {
        wireMockServer.resetAll()
    }

    @Test
    fun `getCustomerFees should map the customer fees correctly`(): Unit = runTest {
        val expectedCustomerFee = CustomerFee(
            amount = BigDecimal("10"),
            currency = "PHP",
            subscriptionId = UUID.randomUUID(),
        )

        wireMockServer.stubFor(
            WireMock.post("/subscription-operation-usage/commit/v4")
                .withRequestBody(
                    WireMock.equalToJson(
                        """
                            {
                              "customerId": "${defaultTransaction.senderDetails.customerId}",
                              "type": "${SubscriptionOperationType.TRANSFER}",
                              "timesUsed": 1,
                              "amount": ${defaultTransaction.amount},
                              "currency": "${defaultTransaction.currency}"
                            }
                        """.trimIndent(),
                        true,
                        true
                    )
                )
                .willReturn(
                    WireMock.jsonResponse(
                        """
                            {
                              "remainingFreeUses": 0,
                              "fee": ${expectedCustomerFee.amount},
                              "percentageFee": 0,
                              "id": "${expectedCustomerFee.subscriptionId}",
                              "currency": "${expectedCustomerFee.currency}"
                            }
                        """.trimIndent(),
                        200
                    )
                )
        )

        val result = feeService.calculateCustomerFee(defaultTransaction)

        expectThat(result) isEqualTo expectedCustomerFee
    }

    @ParameterizedTest
    @EnumSource(TransactionType::class)
    fun `getVendorFees should map the vendor fees correctly`(type: TransactionType): Unit = runTest {
        val transaction = defaultTransaction.copy(type = type)
        val expectedVendorFee = VendorFee(
            amount = BigDecimal("10"),
            currency = "PHP",
        )
        val feeServiceTransactionType = convertToTransactionTypeDto(transaction.type)

        wireMockServer.stubFor(
            WireMock.post("/fees/calculate/vendor")
                .withRequestBody(
                    WireMock.equalToJson(
                        """
                            {
                              "amount": ${transaction.amount},
                              "currency": "${transaction.currency}",
                              "customerId": "${transaction.senderDetails.customerId}",
                              "transactionType": "$feeServiceTransactionType",
                              "merchantCode": "${transaction.providerDetails.merchantCode}"
                            }
                        """.trimIndent(),
                        true,
                        true,
                    )
                )
                .willReturn(
                    WireMock.jsonResponse(
                        """
                            {
                              "fee": ${expectedVendorFee.amount},
                              "feeCurrency": "${expectedVendorFee.currency}",
                              "transactionCount": 1
                            }
                        """.trimIndent(),
                        200
                    )
                )
        )

        val result = feeService.calculateVendorFee(transaction)

        expectThat(result) isEqualTo expectedVendorFee
    }

    @Test
    fun `revertCustomerFee should call product-manager revert`(): Unit = runTest {
        val transaction = defaultTransaction.copy(
            customerFee = CustomerFee(
                amount = BigDecimal("10"),
                currency = "PHP",
                subscriptionId = UUID.randomUUID(),
            )
        )

        wireMockServer.stubFor(
            WireMock.put("/subscription-operation-usage/revert/v3")
                .withRequestBody(
                    WireMock.equalToJson(
                        """
                            {
                              "subscriptionOperationUsageId": "${transaction.customerFee.subscriptionId}"
                            }
                        """.trimIndent(),
                        true,
                        true
                    )
                )
                .willReturn(
                    WireMock.aResponse().withStatus(200)
                )
        )

        feeService.revertCustomerFee(transaction.customerFee)

        wireMockServer.verify(1, WireMock.putRequestedFor(WireMock.urlEqualTo("/subscription-operation-usage/revert/v3")))
    }

    @Test
    fun `revertCustomerFee should not call product-manager revert when subscriptionId is null`(): Unit = runTest {
        val transaction = defaultTransaction.copy(
            customerFee = CustomerFee(
                amount = BigDecimal("10"),
                currency = "PHP",
                subscriptionId = null,
            )
        )

        wireMockServer.stubFor(
            WireMock.put("/subscription-operation-usage/revert/v2")
                .withRequestBody(
                    WireMock.equalToJson(
                        """
                            {
                              "subscriptionOperationUsageId": "${transaction.customerFee.subscriptionId}"
                            }
                        """.trimIndent(),
                        true,
                        true
                    )
                )
                .willReturn(
                    WireMock.aResponse().withStatus(200)
                )
        )

        feeService.revertCustomerFee(transaction.customerFee)

        wireMockServer.verify(0, WireMock.putRequestedFor(WireMock.urlEqualTo("/subscription-operation-usage/revert/v2")))
    }

    private fun convertToTransactionTypeDto(type: TransactionType): TransactionTypeDto {
        return when (type) {
            TransactionType.BILL_PAYMENT -> TransactionTypeDto.BILL_PAYMENT
            TransactionType.AIRTIME_LOAD -> TransactionTypeDto.AIRTIME_LOAD
        }
    }

    override fun getProperties(): MutableMap<String, String> {
        wireMockServer.start()
        return mutableMapOf(
            "product-manager-api-client-base-path" to wireMockServer.baseUrl(),
            "fee-manager-api-client-base-path" to wireMockServer.baseUrl(),
        )
    }
}
