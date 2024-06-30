package ph.safibank.paymenttransactionmanager

import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.micronaut.transaction.SynchronousTransactionManager
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.sql.Connection

@MicronautTest
class IntegrationDbAvailable(
    private val connection: Connection,
    private val transactionManager: SynchronousTransactionManager<Connection>,
) {

    @Test
    fun testDbAvailable() {
        val actualDatabaseVersion = transactionManager.executeRead {
            connection.prepareStatement("SELECT version()").use { ps ->
                ps.executeQuery().use { rs ->
                    if (rs.next()) {
                        return@executeRead rs.getString("version")
                    }
                }
            }
        } as String

        val dbVersion = "PostgreSQL 14.2"
        assertTrue(actualDatabaseVersion.contains(dbVersion), "Expected '$dbVersion',  found '$actualDatabaseVersion'")
    }
}
