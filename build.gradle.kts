import gg.essential.gradle.util.*
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    kotlin("jvm")
    id("gg.essential.multi-version")
    id("gg.essential.defaults")
    id("gg.essential.defaults.maven-publish")
}

group = "gg.essential"

java.withSourcesJar()
tasks.compileKotlin.setJvmDefault("all")
loom.noServerRunConfigs()

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.25")
}

kotlin {
    compilerOptions {
        languageVersion = KotlinVersion.KOTLIN_1_9
        apiVersion = KotlinVersion.KOTLIN_1_9
    }
}

preprocess {
    vars.put("STANDALONE", 0)
    vars.put("!STANDALONE", 1)
}

tasks.jar {
    if (platform.isModLauncher) {
        manifest {
            // `GAMELIBRARY` is required to access Minecraft classes from ModLauncher 9 and higher.
            val modType = if (platform.mcVersion >= 11700) "GAMELIBRARY" else "LIBRARY"
            attributes(mapOf("FMLModType" to modType))
        }
    }
}
