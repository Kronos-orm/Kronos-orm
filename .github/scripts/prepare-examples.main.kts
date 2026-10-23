@file:Import("../../scripts/common.main.kts")

import java.nio.file.Path
import kotlin.io.path.readText

fun replaceOne(path: Path, pattern: String, replacement: String) {
    val source = path.readText()
    val regex = Regex(pattern, RegexOption.MULTILINE)
    require(regex.findAll(source).count() == 1) { "Expected one match in $path: $pattern" }
    path.write(regex.replace(source) { replacement })
}

fun alignDependency(path: Path, artifact: String, version: String) = replaceOne(
    path, "implementation\\(\"com\\.kotlinorm:$artifact:[^\"]+\"\\)",
    "implementation(\"com.kotlinorm:$artifact:$version\")"
)

fun alignPlugin(path: Path, plugin: String, version: String) = replaceOne(
    path, "id\\(\"${Regex.escape(plugin)}\"\\) version \"[^\"]+\"", "id(\"$plugin\") version \"$version\""
)

fun verifySqliteSource(workspace: Path) {
    val source = workspace.resolve("kronos-example-ios/shared/src/nativeInterop/sqlite")
    val lock = jsonMapper.readTree(source.resolve("source-lock.json").toFile())
    val hashes = lock.path("sha256")
    require(hashes.isObject && hashes.fieldNames().asSequence().toSet() == setOf("sqlite3.c", "sqlite3.h")) {
        "SQLite source lock must identify sqlite3.c and sqlite3.h"
    }
    for ((name, expected) in hashes.fields().asSequence()) {
        require(expected.isTextual && expected.asText().matches(Regex("[0-9a-f]{64}"))) {
            "Invalid locked SHA-256: $name"
        }
        require(digest(source.resolve(name)) == expected.asText()) { "SQLite source SHA-256 mismatch: $name" }
    }
    println("SQLite ${lock.path("version").asText()}: source and header match source-lock.json")
}

fun alignNative(workspace: Path, version: String, kotlin: String) {
    val catalog = workspace.resolve("kronos-example-ios/gradle/libs.versions.toml")
    for ((key, value) in mapOf("kotlin" to kotlin, "kronos" to version)) {
        replaceOne(catalog, "^$key\\s*=\\s*\"[^\"]+\"", "$key = \"$value\"")
    }
}

fun alignAndroid(workspace: Path, version: String, kotlin: String) {
    val example = workspace.resolve("kronos-example-android")
    val build = example.resolve("build.gradle.kts")
    alignPlugin(build, "org.jetbrains.kotlin.android", kotlin)
    alignPlugin(build, "com.kotlinorm.kronos-gradle-plugin", version)
    for (artifact in listOf("kronos-core", "kronos-logging")) {
        alignDependency(example.resolve("app/build.gradle.kts"), artifact, version)
    }
}

fun alignJvm(workspace: Path, version: String, kotlin: String) {
    for (name in listOf("ktor", "vertx")) {
        val build = workspace.resolve("kronos-example-$name/build.gradle.kts")
        alignPlugin(build, "com.kotlinorm.kronos-gradle-plugin", version)
        for (artifact in listOf("kronos-core", "kronos-jdbc-wrapper")) alignDependency(build, artifact, version)
    }
    val spring = workspace.resolve("kronos-example-spring-boot")
    replaceOne(spring.resolve("gradle-project/libs.versions.toml"), "kronos = \"[^\"]+\"", "kronos = \"$version\"")
    for (key in listOf("version", "kronosVersion")) {
        replaceOne(spring.resolve("gradle-project/gradle.properties"), "^$key=.*$", "$key=$version")
    }
    for (pom in listOf(spring.resolve("maven-project/pom.xml"), workspace.resolve("kronos-example-solon/pom.xml"))) {
        replaceOne(pom, "<kronos.version>[^<]+</kronos.version>", "<kronos.version>$version</kronos.version>")
    }
    replaceOne(workspace.resolve("kronos-example-solon/pom.xml"),
        "<kotlin.version>[^<]+</kotlin.version>", "<kotlin.version>$kotlin</kotlin.version>")
}

cli {
    if (args.isEmpty() || args.any { it in setOf("--help", "-h") }) {
        println("Usage: prepare-examples.main.kts android|native|jvm --workspace PATH")
    } else {
        val options = Options(args)
        options.validate(setOf("--workspace"))
        require(options.positional.size == 1) { "Expected one example family" }
        val workspace = Path.of(requireNotNull(options.one("--workspace"))).toAbsolutePath()
        val current = versions()
        val version = current.getValue("version")
        val kotlin = current.getValue("kotlin")
        when (options.positional.single()) {
            "android" -> alignAndroid(workspace, version, kotlin)
            "native" -> {
                verifySqliteSource(workspace)
                alignNative(workspace, version, kotlin)
            }
            "jvm" -> alignJvm(workspace, version, kotlin)
            else -> error("Unknown example family: ${options.positional.single()}")
        }
        println("Example versions aligned: Kronos $version, Kotlin $kotlin")
    }
}
