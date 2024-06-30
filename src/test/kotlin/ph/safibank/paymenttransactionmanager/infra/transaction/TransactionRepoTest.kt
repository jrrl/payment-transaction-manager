package ph.safibank.paymenttransactionmanager.infra.transaction

import com.github.database.rider.core.api.dataset.DataSet
import com.github.database.rider.core.api.dataset.ExpectedDataSet
import com.github.database.rider.junit5.api.DBRider
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import ph.safibank.paymenttransactionmanager.TestUtils
import ph.safibank.paymenttransactionmanager.core.provider.ProviderStatus
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import java.util.UUID

@ExperimentalCoroutinesApi
@MicronautTest
@DBRider
class TransactionRepoTest(
    private val repo: TransactionRepoImpl
) {

    private val defaultTransaction = TestUtils.defaultTransaction

    @ExpectedDataSet(value = ["expected-payment-transaction.yaml"])
    @Test
    fun `saveTransaction should successfully save the transaction and return it`(): Unit = runTest {
        val transaction = defaultTransaction.copy(id = UUID.randomUUID())
        repo.saveTransaction(transaction)
    }

    @DataSet(value = ["billpayment.yaml"], cleanBefore = true)
    @Test
    fun `findByBatchId should fetch the correct transaction`(): Unit = runTest {
        val defaultTransactionId = "c4103d99-c601-4ccc-a1c4-10a7c5d6d817"
        val result = repo.getTransaction(UUID.fromString(defaultTransactionId))
        expectThat(result).isNotNull().and {
            get { id.toString() } isEqualTo "c4103d99-c601-4ccc-a1c4-10a7c5d6d817"
        }
    }
    @DataSet(value = ["billpayment.yaml"], cleanBefore = true)
    @ExpectedDataSet(value = ["expected-updated-payment-transaction.yaml"])
    @Test
    fun `updateTransaction should update provider status`(): Unit = runTest {
        val defaultTransactionId = "c4103d99-c601-4ccc-a1c4-10a7c5d6d817"
        val result = repo.getTransaction(UUID.fromString(defaultTransactionId))
        repo.updateTransaction(result!!.copy(providerDetails = result.providerDetails.copy(providerStatus = ProviderStatus.PENDING)))
    }
}
