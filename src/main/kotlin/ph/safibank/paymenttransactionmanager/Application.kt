package ph.safibank.paymenttransactionmanager

import io.micronaut.runtime.Micronaut.build
import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Info
import org.slf4j.LoggerFactory

@OpenAPIDefinition(
    info = Info(
        title = "payment-transaction-manager",
        version = "\${api.version}"
    )
)
object Api

private val INIT_CONTAINER = "INITCONTAINER"

fun main(args: Array<String>) {
    val log = LoggerFactory.getLogger("ph.safibank.paymenttransactionmanager.Application.kt")
    val init = System.getenv(INIT_CONTAINER).toBoolean()
    log.info("'$INIT_CONTAINER' property value is $init")

    val micronaut = build()
        .eagerInitSingletons(true)
        .args(*args)
        .packages("ph.safibank")
    if (init) {
        log.info("Running container with DB environment only")
        micronaut
            .defaultEnvironments("init")
    } else {
        log.info("Running container with DB and Main environment")
        micronaut
            .defaultEnvironments("main")
    }.start()
}
