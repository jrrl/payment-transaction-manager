package ph.safibank.paymenttransactionmanager

import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

@MicronautTest
class ApplicationRunningTest(private val application: EmbeddedApplication<*>) {

    @Test
    fun testItWorks() {
        Assertions.assertTrue(application.isRunning)
    }
}
