pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.fabricmc.net")
        maven("https://maven.architectury.dev/")
        maven("https://maven.minecraftforge.net")
        maven("https://repo.essential.gg/repository/maven-public")
    }
    plugins {
        val egtVersion = "0.7.0"
        id("gg.essential.multi-version.root") version egtVersion
        id("gg.essential.multi-version.api-validation") version egtVersion
    }
}

rootProject.name = "UniversalCraft"
rootProject.buildFileName = "root.gradle.kts"

include(":standalone")
include(":standalone:example")

listOf(
    "1.8.9-forge",
    "1.12.2-forge",
    "1.16.2-forge",
    "1.16.2-fabric",
    "1.17.1-fabric",
    "1.17.1-forge",
    "1.18.1-fabric",
    "1.18.1-forge",
    "1.19-fabric",
    "1.19.1-fabric",
    "1.19.2-fabric",
    "1.19.2-forge",
    "1.19.3-fabric",
    "1.19.3-forge",
    "1.19.4-fabric",
    "1.19.4-forge",
    "1.20-fabric",
    "1.20.1-fabric",
    "1.20.1-forge",
    "1.20.2-fabric",
    "1.20.2-forge",
    "1.20.4-fabric",
    "1.20.4-forge",
    "1.20.4-neoforge",
    "1.20.6-fabric",
    "1.20.6-forge",
    "1.20.6-neoforge",
    "1.21-fabric",
    "1.21-forge",
    "1.21-neoforge",
    "1.21.3-fabric",
    "1.21.3-forge",
    "1.21.3-neoforge",
    "1.21.4-fabric",
    "1.21.4-forge",
    "1.21.4-neoforge",
    "1.21.5-fabric",
    "1.21.5-forge",
    "1.21.5-neoforge",
    "1.21.6-fabric",
    "1.21.7-fabric",
    "1.21.7-forge",
    "1.21.7-neoforge",
    "1.21.9-fabric",
    "1.21.11-fabric",
    "26.1-fabric",
    "26.2-fabric",
).forEach { version ->
    include(":$version")
    project(":$version").apply {
        projectDir = file("versions/$version")
        buildFileName = "../../build.gradle.kts"
    }
}