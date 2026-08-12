import net.fabricmc.loom.api.LoomGradleExtensionAPI

plugins {
    id("net.fabricmc.fabric-loom") version "1.17.19" apply false
    id("net.fabricmc.fabric-loom-remap") version "1.17.19" apply false
    id("maven-publish")
}

if (sc.current.parsed >= "26.1") {
    apply(plugin = "net.fabricmc.fabric-loom")
} else {
    apply(plugin = "net.fabricmc.fabric-loom-remap")
}

val loomExtension = extensions.getByType<LoomGradleExtensionAPI>()

group = property("mod.group") as String

val releaseMinecraftVersions = when (stonecutter.current.version) {
    "1.21", "1.21.1" -> listOf("1.21", "1.21.1")
    "1.21.2", "1.21.3", "1.21.4" -> listOf("1.21.2", "1.21.3", "1.21.4")
    "1.21.5" -> listOf("1.21.5")
    "1.21.6", "1.21.7", "1.21.8" -> listOf("1.21.6", "1.21.7", "1.21.8")
    "1.21.9", "1.21.10" -> listOf("1.21.9", "1.21.10")
    "1.21.11" -> listOf("1.21.11")
    "26.1", "26.1.1", "26.1.2" -> listOf("26.1", "26.1.1", "26.1.2")
    "26.2" -> listOf("26.2")
    else -> error("Minecraft ${stonecutter.current.version} is not assigned to a release range")
}
val releaseMinecraftLabel = if (releaseMinecraftVersions.size == 1) {
    releaseMinecraftVersions.single()
} else {
    "${releaseMinecraftVersions.first()}-${releaseMinecraftVersions.last()}"
}
val releaseMinecraftPredicate = if (releaseMinecraftVersions.size == 1) {
    releaseMinecraftVersions.single()
} else {
    ">=${releaseMinecraftVersions.first()} <=${releaseMinecraftVersions.last()}"
}

version = "${property("mod.version")}+mc$releaseMinecraftLabel"

base {
    archivesName = property("mod.id") as String
}

repositories {
    mavenCentral()
}

dependencies {
    add("minecraft", "com.mojang:minecraft:${sc.current.version}")
    if (sc.current.parsed < "26.1") {
        add("mappings", loomExtension.officialMojangMappings())
    }

    val modDependencyConfiguration = if (sc.current.parsed >= "26.1") "implementation" else "modImplementation"
    add(modDependencyConfiguration, "net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    add(modDependencyConfiguration, "net.fabricmc.fabric-api:fabric-api:${property("deps.fabric_api")}")

    testImplementation(platform("org.junit:junit-bom:5.12.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

loomExtension.apply {
    splitEnvironmentSourceSets()

    mods {
        create("chestdiff") {
            sourceSet(sourceSets.main.get())
            sourceSet(sourceSets.getByName("client"))
        }
    }
}

sourceSets {
    main {
        java.srcDir("src/main/java")
        resources.srcDir("src/main/resources")
    }
    getByName("client") {
        java.srcDir("src/client/java")
        resources.srcDir("src/client/resources")
    }
    test {
        java.srcDir("src/test/java")
    }
}

val requiredJava = if (sc.current.parsed >= "26.1") 25 else 21

java {
    toolchain.languageVersion = JavaLanguageVersion.of(requiredJava)
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = requiredJava
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("minecraftVersion", releaseMinecraftPredicate)
    inputs.property("loaderVersion", project.property("deps.fabric_loader"))
    inputs.property("fabricApiVersion", project.property("deps.fabric_api"))
    filteringCharset = "UTF-8"
    filesMatching("fabric.mod.json") {
        expand(
            "version" to project.version,
            "minecraftVersion" to releaseMinecraftPredicate,
            "loaderVersion" to project.property("deps.fabric_loader").toString(),
            "fabricApiVersion" to project.property("deps.fabric_api").toString()
        )
    }
}

tasks.jar {
    from("LICENSE.txt") {
        rename { "LICENSE_${property("mod.id")}" }
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = property("mod.id") as String
            from(components["java"])
        }
    }
}
