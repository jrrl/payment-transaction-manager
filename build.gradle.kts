plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.kapt")
    id("org.jetbrains.kotlin.plugin.allopen")
    id("org.jetbrains.kotlin.plugin.jpa")
    id("com.github.johnrengelman.shadow") version "7.1.1"
    id("io.micronaut.application") version "3.3.2"
    id("com.softeq.gradle.itest") version "1.0.4"
    id("org.jlleitschuh.gradle.ktlint") version "10.2.1"
    id("com.google.cloud.tools.jib") version "3.3.0"
    id("com.github.davidmc24.gradle.plugin.avro") version "1.3.0"
    id("ph.safibank.common.generated.client.publisher") version "1.+"
    id("ph.safibank.common.generated.client.publisher.java") version "1.+"
    id("ph.safibank.common.coverage") version "2+"
}

val kotlinVersion = project.properties["kotlinVersion"]

version = "0.1-SNAPSHOT"
group = project.properties["group"]!!

repositories {
    mavenCentral()
    maven {
        url = uri("https://packages.confluent.io/maven")
    }
    maven {
        url = uri("https://asia-southeast1-maven.pkg.dev/safi-repos/safi-maven")
    }
}

dependencies {

    // common
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.0")
    implementation("ph.safibank.common:utils:9.20230221-102516")
    implementation("ph.safibank.common:idempotency-lib:1.20221202-133548")
    implementation("ph.safibank.common:avro-model:4.20230221-010717")
    implementation("ph.safibank.common:tm-client:1.20230207-055212")

    // common ktlint rules
    ktlintRuleset("ph.safibank.common:ktlint-rules:1.+")

    // client
    kapt("io.micronaut.openapi:micronaut-openapi")
    implementation("io.micronaut:micronaut-http-client")
    implementation("io.swagger.core.v3:swagger-annotations")
    implementation("ph.safibank.accountmanager:account-manager-api-client:0.1-SNAPSHOT")
    implementation("ph.safibank.feemanager.client:fee-manager-api-client:8")
    implementation("ph.safibank.productmanager.client:product-manager-api-client:30")
    implementation("ph.safibank.slackermanager.client:slacker-manager-api-client:6")

    // db
    implementation("io.micronaut.flyway:micronaut-flyway")
    implementation("io.micronaut.data:micronaut-data-jdbc:3.8.1")
    implementation("io.micronaut.sql:micronaut-jdbc-hikari")
    runtimeOnly("org.postgresql:postgresql")

    // kafka
    implementation("io.micronaut.kafka:micronaut-kafka")
    implementation("io.confluent:kafka-avro-serializer:7.0.1")
    implementation("org.apache.avro:avro:1.11.0")
    implementation("org.xerial.snappy:snappy-java:1.1.8.4") // Needed for Kafka on Apple Silicon

    // testing
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:kafka")
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")
    testImplementation("io.strikt:strikt-core:0.34.1")
    testImplementation("io.mockk:mockk")
    testImplementation("com.github.tomakehurst:wiremock-jre8:2.35.0")
    testImplementation("org.awaitility:awaitility-kotlin:4.2.0")
    testImplementation("ph.safibank.common:test-utils:2.20230206-015413")

    // integration testing
    kaptItest("io.micronaut:micronaut-inject-java")
    itestImplementation("io.micronaut.test:micronaut-test-junit5")
    itestImplementation("org.junit.jupiter:junit-jupiter")

    // metrics with Prometheus
    implementation("io.micronaut.micrometer:micronaut-micrometer-core")
    implementation("io.micronaut.micrometer:micronaut-micrometer-registry-prometheus")
    implementation("io.micronaut:micronaut-management")

    // tracing
    runtimeOnly("io.opentelemetry.javaagent:opentelemetry-javaagent:1.19.0")

    // iam authentication
    implementation("ph.safibank.common:iam-auth-lib:4.+")

    // TODO remove if issue ClassNotFoundException gets resolved
    implementation("io.micronaut:micronaut-messaging:3.8.5")
}

configurations.all {
    resolutionStrategy.cacheDynamicVersionsFor(0, "seconds")
    resolutionStrategy.cacheChangingModulesFor(0, "seconds")
}

application {
    mainClass.set("ph.safibank.paymenttransactionmanager.ApplicationKt")
}
java {
    sourceCompatibility = JavaVersion.toVersion("17")
}

tasks.matching { it.name == "kaptKotlin" }.configureEach { dependsOn("generateAvroJava") }
tasks.matching { it.name == "kaptGenerateStubsKotlin" }.configureEach { dependsOn("generateAvroJava") }
tasks.matching { it.name == "kaptGenerateStubsKotlin" }.configureEach { dependsOn("generateTestAvroJava") }
tasks.matching { it.name == "runKtlintCheckOverTestSourceSet" }.configureEach { dependsOn("generateAvroJava") }
tasks.matching { it.name == "runKtlintCheckOverTestSourceSet" }.configureEach { dependsOn("generateTestAvroJava") }
tasks.matching { it.name == "runKtlintCheckOverMainSourceSet" }.configureEach { dependsOn("generateAvroJava") }

tasks {
    compileKotlin {
        kotlinOptions {
            jvmTarget = "17"
        }
    }
    compileTestKotlin {
        kotlinOptions {
            jvmTarget = "17"
        }
    }
    compileItestKotlin {
        kotlinOptions {
            jvmTarget = "17"
        }
    }
}

micronaut {
    runtime("netty")
    testRuntime("junit5")
    processing {
        incremental(true)
        annotations("ph.safibank.*")
    }
}

allOpen {
    annotations("javax.transaction.Transactional", "io.micronaut.http.annotation.Controller")
}

jib {
    from {
        image = "gcr.io/distroless/java17-debian11@sha256:39d8afe84b04016546dd65a77bb86b0f7d7bf65f84c0773927e991e3431b8189"
    }
    container {
        creationTime = "USE_CURRENT_TIMESTAMP"
        environment = mapOf(
            "MICRONAUT_ENV_DEDUCTION" to "false",
        )
        user = "10001:10001"
        jvmFlags = listOf(
            "-javaagent:/app/libs/opentelemetry-javaagent-1.19.0.jar"
        )
    }
}
