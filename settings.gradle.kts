rootProject.name = "payment-transaction-manager"

pluginManagement.resolutionStrategy.eachPlugin {
    val kotlinVersion: String by settings
    if (requested.id.id.startsWith("org.jetbrains.kotlin.")) {
        useVersion(kotlinVersion)
    }
}

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven {
            url = uri("https://asia-southeast1-maven.pkg.dev/safi-repos/safi-maven")
        }
    }
}
