package ph.safibank.paymenttransactionmanager.infra.account

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import ph.safibank.common.testutils.KafkaBaseTest
import ph.safibank.paymenttransactionmanager.TestUtils
import ph.safibank.paymenttransactionmanager.core.account.AccountDetails
import ph.safibank.paymenttransactionmanager.core.account.AccountService
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNull
import java.math.BigDecimal
import java.util.UUID

@MicronautTest
@ExperimentalCoroutinesApi
class AccountServiceImplTest : KafkaBaseTest() {
    @Inject
    private lateinit var accountService: AccountService

    private val wireMockServer = WireMockServer(WireMockConfiguration.options().dynamicPort())

    @Test
    fun `getAccountDetails should return null when account does not exist`() = runTest {
        val accountNumber = TestUtils.randomAccountNumber()

        wireMockServer.stubFor(
            WireMock.get("/v2/main-account/by-account-number/${accountNumber.value}")
                .willReturn(WireMock.notFound())
        )

        val result = accountService.getAccountDetails(accountNumber)

        expectThat(result).isNull()
    }

    @Test
    fun `getAccountDetails should map and return account details when account exists`() = runTest {
        val accountNumber = TestUtils.randomAccountNumber()

        val expectedAccountDetails = AccountDetails(
            id = UUID.randomUUID(),
            customerId = UUID.randomUUID(),
            accountNumber = accountNumber,
            balance = BigDecimal("100"),
            active = true,
            currency = "PHP"
        )

        wireMockServer.stubFor(
            WireMock.get("/v2/main-account/by-account-number/${accountNumber.value}")
                .willReturn(
                    WireMock.jsonResponse(
                        """
                            {
                              "id": "${expectedAccountDetails.id}",
                              "ownerId": "${expectedAccountDetails.customerId}",
                              "accountState": "ACTIVE",
                              "name": "string",
                              "createdAt": "2023-01-31T05:11:46.936Z",
                              "updatedAt": "2023-01-31T05:11:46.936Z",
                              "mainAccount": {
                                "accountNumber": "${expectedAccountDetails.accountNumber.value}",
                                "interestRate": 0,
                                "balances": {
                                  "available": {
                                    "amount": ${expectedAccountDetails.balance.toPlainString()},
                                    "currency": "${expectedAccountDetails.currency}",
                                    "latestChange": "2023-01-31T05:11:46.936Z"
                                  },
                                  "total": {
                                    "amount": 0,
                                    "currency": "string",
                                    "latestChange": "2023-01-31T05:11:46.936Z"
                                  },
                                  "overdraft": {
                                    "amount": 0,
                                    "currency": "string",
                                    "latestChange": "2023-01-31T05:11:46.936Z"
                                  },
                                  "interest": {
                                    "amount": 0,
                                    "currency": "string",
                                    "latestChange": "2023-01-31T05:11:46.936Z"
                                  }
                                }
                              },
                              "savingAccount": {
                                "imageLink": "string",
                                "targetAmount": 0,
                                "interestRate": 0,
                                "additionalInterestRate": 0,
                                "lockedDetails": {
                                  "tenureMonths": 0,
                                  "autoRenewal": true,
                                  "lockDate": "2023-01-31T05:11:46.936Z"
                                },
                                "unlockedDetails": {
                                  "targetDate": "2023-01-31T05:11:46.936Z"
                                },
                                "balances": {
                                  "total": {
                                    "amount": 0,
                                    "currency": "string",
                                    "latestChange": "2023-01-31T05:11:46.936Z"
                                  },
                                  "lostIfUnlocked": {
                                    "amount": 0,
                                    "currency": "string",
                                    "latestChange": "2023-01-31T05:11:46.936Z"
                                  },
                                  "expectedProfit": {
                                    "amount": 0,
                                    "currency": "string",
                                    "latestChange": "2023-01-31T05:11:46.936Z"
                                  }
                                }
                              }
                            }
                        """.trimIndent(),
                        200
                    )
                )
        )

        val result = accountService.getAccountDetails(accountNumber)

        expectThat(result) isEqualTo expectedAccountDetails
    }

    override fun getProperties(): MutableMap<String, String> {
        wireMockServer.start()
        return mutableMapOf(
            "account-manager-api-client-base-path" to wireMockServer.baseUrl(),
        )
    }
}
