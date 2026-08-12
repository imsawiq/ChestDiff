plugins {
    id("dev.kikugie.stonecutter")
}

import org.gradle.api.tasks.Sync

stonecutter parameters {
    constants["new_minecraft"] = current.version >= "26.1"
}

tasks.register("buildAll") {
    group = "build"
    description = "Builds and tests every supported Minecraft target."
    dependsOn(stonecutter.tasks.named("build"))
}

val releaseTargets = mapOf(
    "1.21" to "1.21-1.21.1",
    "1.21.2" to "1.21.2-1.21.4",
    "1.21.5" to "1.21.5",
    "1.21.6" to "1.21.6-1.21.8",
    "1.21.9" to "1.21.9-1.21.10",
    "1.21.11" to "1.21.11",
    "26.1" to "26.1-26.1.2",
    "26.2" to "26.2"
)

tasks.register<Sync>("releaseAll") {
    group = "distribution"
    description = "Builds, tests and collects one ChestDiff JAR per compatible Minecraft range into release/."
    dependsOn(tasks.named("buildAll"))

    releaseTargets.forEach { (minecraftVersion, releaseLabel) ->
        from(layout.projectDirectory.dir("versions/$minecraftVersion/build/libs")) {
            include("chestdiff-*+mc$releaseLabel.jar")
        }
    }
    from(layout.projectDirectory.files("README.md", "CHANGELOG.md", "LICENSE.txt"))
    into(layout.projectDirectory.dir("release"))

    doLast {
        val jars = destinationDir.listFiles { file -> file.extension == "jar" }
            ?.sortedBy { it.name }
            .orEmpty()
        require(jars.size == releaseTargets.size) {
            "Expected ${releaseTargets.size} release JARs, found ${jars.size}"
        }
    }
}

val developmentTarget = providers.gradleProperty("chestdiffDevTarget").orNull
stonecutter active if (developmentTarget == null || developmentTarget == "latest") "26.2" else null
