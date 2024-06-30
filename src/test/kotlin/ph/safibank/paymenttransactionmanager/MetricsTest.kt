package ph.safibank.paymenttransactionmanager

import io.micronaut.core.type.Argument
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

@MicronautTest
class MetricsTest {

    @Inject
    @field:Client("/prometheus")
    lateinit var prometheusClient: HttpClient

    @Inject
    @field:Client("/metrics")
    lateinit var metricsClient: HttpClient

    @Test
    fun `test prometheus metrics`() {
        val request: HttpRequest<Any> = HttpRequest.GET("")
        val response = prometheusClient.toBlocking().exchange(request, Argument.of(String::class.java)).body()!!
        assertTrue(response.contains("jvm_memory_used_bytes"), response)
    }

    @Test
    fun `test metrics`() {
        val request: HttpRequest<Any> = HttpRequest.GET("")
        val exception = assertFailsWith(
            exceptionClass = HttpClientResponseException::class,
            message = "/metrics should be disabled and return 404",
            block = { metricsClient.toBlocking().exchange(request, Argument.of(String::class.java)) }
        )
        assertEquals(HttpStatus.NOT_FOUND, exception.status)
    }
}
