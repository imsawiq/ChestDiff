pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
        gradlePluginPortal()
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.7"
}

rootProject.name = "ChestDiff"

stonecutter {
    create(rootProject) {
        val supportedVersions = arrayOf(
            "1.21", "1.21.1", "1.21.2", "1.21.3", "1.21.4", "1.21.5",
            "1.21.6", "1.21.7", "1.21.8", "1.21.9", "1.21.10", "1.21.11",
            "26.1", "26.1.1", "26.1.2", "26.2"
        )
        val developmentTarget = providers.gradleProperty("chestdiffDevTarget").orNull
        val configuredVersions = when (developmentTarget) {
            null -> supportedVersions
            "latest" -> arrayOf("26.2")
            "legacy" -> arrayOf("1.21.11")
            "middle" -> arrayOf("1.21.9")
            "earliest" -> arrayOf("1.21")
            else -> arrayOf(developmentTarget)
        }
        versions(*configuredVersions)
        vcsVersion = when (developmentTarget) {
            "legacy" -> "1.21.11"
            "middle" -> "1.21.9"
            "earliest" -> "1.21"
            else -> "26.2"
        }
    }
}
