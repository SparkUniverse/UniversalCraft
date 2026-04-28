import com.replaymod.gradle.preprocess.PreprocessTask
import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode

plugins {
    kotlin("jvm")
    id("gg.essential.defaults")
    id("gg.essential.defaults.maven-publish")
}

val parent = evaluationDependsOn(project.parent!!.path)

group = "gg.essential"
version = parent.version
base.archivesName = "universalcraft-standalone"
kotlin.compilerOptions.moduleName = "universalcraft-standalone"
publishing.publications.named<MavenPublication>("maven") { artifactId = "universalcraft-standalone" }
java.withSourcesJar()
kotlin.compilerOptions.jvmDefault.set(JvmDefaultMode.NO_COMPATIBILITY)
kotlin.jvmToolchain(8)

dependencies {
    api(kotlin("stdlib-jdk8", "2.0.20-RC"))
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0-RC")

    implementation("dev.folomeev.kotgl:kotgl-matrix:0.0.1-beta")

    val lwjglModules = listOf("lwjgl", "lwjgl-glfw", "lwjgl-opengl", "lwjgl-stb", "lwjgl-nanovg")
    val lwjglNatives = listOf("linux", "macos", "macos-arm64", "windows")
    api(platform("org.lwjgl:lwjgl-bom:3.3.3"))
    for (module in lwjglModules) {
        api("org.lwjgl", module)
        for (native in lwjglNatives) {
            api("org.lwjgl", module, classifier = "natives-$native")
        }
    }

    // MC provides these and (at least some of) our libs depend on them
    api("org.slf4j:slf4j-api:2.0.13")
    // Same as above but we do not want to expose them to our downstream so we can eventually remove them
    implementation("org.apache.logging.log4j:log4j-api:2.23.1")
    implementation("org.apache.logging.log4j:log4j-core:2.23.1")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:2.23.1")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("commons-codec:commons-codec:1.17.1")
    implementation("org.apache.httpcomponents:httpclient:4.5.14")
    // Pretty colors in Console logs
    implementation("net.minecrell:terminalconsoleappender:1.3.0")
}

tasks.processResources {
    exclude("pack.mcmeta", "mcmod.info", "META-INF/mods.toml", "META-INF/neoforge.mods.toml", "fabric.mod.json")
}

fun setupPreprocessor() {
    val inherited = parent.evaluationDependsOn("1.8.9-forge")

    fun Provider<Directory>.dir(path: String): Provider<Directory> =
        map { it.dir(path) }

    val generatedRoot = layout.buildDirectory.dir("preprocessed/main")
    val generatedKotlin = generatedRoot.dir("kotlin")
    val generatedJava = generatedRoot.dir("java")
    val generatedResources = generatedRoot.dir("resources")

    val overwritesKotlin = file("src/main/kotlin").also { it.mkdirs() }
    val overwritesJava = file("src/main/java").also { it.mkdirs() }
    val overwriteResources = file("src/main/resources").also { it.mkdirs() }

    val inheritedSourceSet = inherited.sourceSets.main.get()

    val preprocessCode = tasks.register<PreprocessTask>("preprocessCode") {
        entry(
            source = inherited.files(inheritedSourceSet.java.srcDirs),
            overwrites = overwritesJava,
            generated = generatedJava.get().asFile,
        )
        entry(
            source = inherited.files(inheritedSourceSet.kotlin.srcDirs.filter { it.endsWith("kotlin") }),
            overwrites = overwritesKotlin,
            generated = generatedKotlin.get().asFile,
        )
        keywords = mapOf(
            ".java" to PreprocessTask.DEFAULT_KEYWORDS,
            ".kt" to PreprocessTask.DEFAULT_KEYWORDS,
        )
        vars = mapOf(
            "MC" to 99_99_99,
            "FABRIC" to 1,
            "FORGE" to 0,
            "NEOFORGE" to 0,
            "FORGELIKE" to 0,
            "STANDALONE" to 1,
            "!STANDALONE" to 0,
        )
    }
    sourceSets.main {
        java.setSrcDirs(listOf(overwritesJava, preprocessCode.map { generatedJava }))
        kotlin.setSrcDirs(listOf(
            overwritesKotlin,
            preprocessCode.map { generatedKotlin },
            overwritesJava,
            preprocessCode.map { generatedJava },
        ))
    }

    val preprocessResources = tasks.register<PreprocessTask>("preprocessResources") {
        entry(
            source = inherited.files(inheritedSourceSet.resources.srcDirs),
            overwrites = overwriteResources,
            generated = generatedResources.get().asFile,
        )
    }
    tasks.processResources { dependsOn(preprocessResources) }
    sourceSets.main {
        resources.setSrcDirs(listOf(overwriteResources, preprocessResources.map { generatedResources }))
    }
}
setupPreprocessor()
