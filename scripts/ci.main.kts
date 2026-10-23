@file:Import("common.main.kts")

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.util.concurrent.TimeUnit
import kotlin.io.path.createDirectories
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.name

val runtimeModules = listOf("kronos-syntax", "kronos-core", "kronos-logging")
val jvmModules =
    runtimeModules +
        listOf(
            "kronos-jdbc-wrapper",
            "kronos-compiler-plugin",
            "kronos-codegen",
            "kronos-gradle-plugin",
        )

fun selectedJdk(major: Int?, action: () -> Unit) {
    if (major == null) {
        action()
        return
    }
    require(major in listOf(17, 21, 25)) { "Unsupported matrix JDK: $major" }
    val home =
        System.getenv("JAVA_HOME")
            ?: error("--jdk requires JAVA_HOME from actions/setup-java or a local JDK")
    val executable = Path.of(home, "bin", if (windows) "java.exe" else "java").toString()
    val result = run(listOf(executable, "-XshowSettings:properties", "-version"), capture = true)
    val actual =
        Regex("java\\.specification\\.version\\s*=\\s*(\\d+)")
            .find(result.output)
            ?.groupValues
            ?.get(1)
            ?.toInt()
    require(actual == major) { "JAVA_HOME must identify JDK $major: $home" }
    val template = repositoryRoot.resolve("templates/common.module-template.yaml")
    val original = template.text()
    val regex = Regex("(?m)^(    jdk:\n      version: )\\d+$")
    require(regex.findAll(original).count() == 1) {
        "Expected one JDK selection in common template"
    }
    val updated =
        regex.replace(original) { it.groupValues[1] + "$major\n      selectionMode: javaHome" }
    println("Kronos CI: compiler and test JVM use $executable (JDK $major)")
    try {
        template.write(updated)
        action()
    } finally {
        template.write(original)
    }
}

fun exportEnvironment() {
    val destination =
        Path.of(System.getenv("GITHUB_ENV") ?: error("env requires GitHub Actions GITHUB_ENV"))
    val environment = testEnvironment()
    for (key in testDefaults.keys + "SQLSERVER_JDBC_URL") {
        val value = environment.getValue(key)
        require('\n' !in value && '\r' !in value) {
            "Integration setting must have a single line: $key"
        }
        Files.writeString(
            destination,
            "$key=$value\n",
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND,
        )
    }
}

fun integration() {
    val environment = toolchainEnvironment()
    val compose =
        listOf(
            "docker",
            "compose",
            "-f",
            repositoryRoot.resolve("kronos-testing/docker-compose.integration.yml").toString(),
        )
    try {
        run(
            compose + listOf("up", "-d", "--wait", "--wait-timeout", "600"),
            environment = environment,
        )
        val database = environment.getValue("SQLSERVER_DATABASE")
        require(database.matches(Regex("[A-Za-z_][A-Za-z0-9_]*"))) {
            "SQLSERVER_DATABASE must be a SQL identifier"
        }
        val query = "IF DB_ID('$database') IS NULL CREATE DATABASE [$database]"
        val prepared =
            listOf("/opt/mssql-tools18/bin/sqlcmd", "/opt/mssql-tools/bin/sqlcmd").any { executable
                ->
                run(
                        listOf(
                            "docker",
                            "exec",
                            "kronos-testing-sqlserver",
                            executable,
                            "-S",
                            "localhost",
                            "-U",
                            environment.getValue("SQLSERVER_USERNAME"),
                            "-P",
                            environment.getValue("SQLSERVER_PASSWORD"),
                            "-C",
                            "-Q",
                            query,
                        ),
                        environment = environment,
                        check = false,
                    )
                    .exitCode == 0
            }
        require(prepared) { "Could not prepare SQL Server integration database" }
        bootstrap()
        runToolchain(listOf("test", "-m", "kronos-testing", "-p", "jvm"))
    } finally {
        run(compose + "down", environment = environment, check = false)
    }
}

fun verifyResults(modules: List<String>) =
    script(
        repositoryRoot.resolve(".github/scripts/verify-kmp-test-results.main.kts").toString(),
        modules.map { repositoryRoot.resolve("build/reports/$it/jvm").toString() },
    )

fun jvm() {
    bootstrap()
    runToolchain(listOf("test", "-p", "jvm") + jvmModules.flatMap { listOf("-m", it) })
    verifyResults(jvmModules)
    runToolchain(
        listOf(
            "test",
            "-m",
            "kronos-testing",
            "-p",
            "jvm",
            "--include-classes=com.kotlinorm.integration.Sqlite*",
        )
    )
    verifyResults(listOf("kronos-testing"))
    runToolchain(listOf("task", ":kronos-maven-plugin:jarJvm"))
}

fun nativeBinary(module: String, target: String): Path =
    repositoryRoot.resolve(
        "build/tasks/_${module}_link${target.replaceFirstChar { it.uppercase() }}TestDebug/$module" +
            "_test" +
            if (target == "mingwX64") ".exe" else ".kexe"
    )

fun simulator(target: String): String? {
    val family =
        mapOf(
            "iosSimulatorArm64" to "iOS",
            "tvosSimulatorArm64" to "tvOS",
            "watchosSimulatorArm64" to "watchOS",
        )[target] ?: return null
    val devices =
        jsonMapper
            .readTree(
                run(
                        listOf("xcrun", "simctl", "list", "devices", "available", "--json"),
                        capture = true,
                    )
                    .output
            )["devices"]
    val candidates =
        devices
            .fields()
            .asSequence()
            .filter { "SimRuntime.$family-" in it.key }
            .flatMap { it.value.asSequence() }
            .filter { it.path("isAvailable").asBoolean(true) }
            .sortedBy { it["name"].asText() }
            .toList()
    val selected = candidates.firstOrNull() ?: error("No available $family simulator")
    val id = selected["udid"].asText()
    if (selected["state"].asText() != "Booted") run(listOf("xcrun", "simctl", "boot", id))
    run(listOf("xcrun", "simctl", "bootstatus", id, "-b"))
    return id
}

fun runNative(command: List<String>): Pair<CommandResult, String> {
    val process = ProcessBuilder(command).directory(repositoryRoot.toFile()).start()
    val stdout = StringBuilder()
    val stderr = StringBuilder()
    val out = Thread {
        process.inputStream.bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.forEach { stdout.append(it).append('\n') }
        }
    }.apply { start() }
    val err = Thread {
        process.errorStream.bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.forEach { stderr.append(it).append('\n') }
        }
    }.apply { start() }
    process.outputStream.close()
    if (!process.waitFor(600, TimeUnit.SECONDS)) {
        process.destroyForcibly()
        out.join()
        err.join()
        error("Native executable timed out after 600s")
    }
    out.join()
    err.join()
    return CommandResult(process.exitValue(), stdout.toString()) to stderr.toString()
}

fun native(target: String, linkOnly: Boolean) {
    bootstrap()
    runToolchain(listOf("task") + runtimeModules.map {
        ":$it:link${target.replaceFirstChar { it.uppercase() }}TestDebug"
    })
    for (module in runtimeModules) {
        require(nativeBinary(module, target).isRegularFile()) {
            "Native test executable missing: ${nativeBinary(module, target)}"
        }
    }
    val destination = repositoryRoot.resolve("build/reports/native/$target")
    destination.createDirectories()
    if (linkOnly) {
        runtimeModules.forEach { module ->
            Files.copy(
                nativeBinary(module, target),
                destination.resolve(nativeBinary(module, target).fileName),
                StandardCopyOption.REPLACE_EXISTING,
            )
        }
        return
    }
    val id = simulator(target)
    val results = mutableListOf<Map<String, Any>>()
    try {
        for (module in runtimeModules) {
            val command =
                (if (id != null) listOf("xcrun", "simctl", "spawn", id) else emptyList()) +
                    nativeBinary(module, target).toString() +
                    (if (id != null) listOf("--") else emptyList()) +
                    "--ktest_logger=GTEST"
            val (result, stderr) = runNative(command)
            print(result.output)
            System.err.print(stderr)
            destination.resolve("$module.log").write(result.output + stderr)
            val summaries =
                Regex("(?m)^\\[  PASSED  \\] (\\d+) tests?\\.$")
                    .findAll(result.output)
                    .map { it.groupValues[1].toInt() }
                    .toList()
            val passed =
                Regex("(?m)^\\[       OK \\] (.+) \\(\\d+ ms\\)$")
                    .findAll(result.output)
                    .map { it.groupValues[1] }
                    .toList()
            require(
                result.exitCode == 0 &&
                    summaries.size == 1 &&
                    summaries.single() > 0 &&
                    passed.size == summaries.single() &&
                    passed.toSet().size == passed.size &&
                    !Regex("(?m)^\\[  FAILED  \\]").containsMatchIn(result.output)
            ) {
                "Native test summary does not match successful completed test cases: $module"
            }
            results +=
                mapOf(
                    "module" to module,
                    "target" to target,
                    "executed" to passed.size,
                    "tests" to passed,
                )
        }
    } finally {
        destination.resolve("runtime-results.json").json(results)
        if (id != null) run(listOf("xcrun", "simctl", "shutdown", id), check = false)
    }
}

fun reflectCheck() {
    val roots =
        repositoryRoot.resolve("kronos-core").toFile().listFiles()?.filter {
            it.isDirectory && it.name.startsWith("src")
        } ?: error("Could not read production core source roots")
    val sources =
        roots.flatMap { tree(it.toPath()) }.filter { it.isRegularFile() && it.extension == "kt" }
    require(sources.isNotEmpty()) { "No production core Kotlin sources found" }
    val violations = sources.filter { source ->
        val content = source.text()
        listOf("kotlin.reflect.full", "kotlin.reflect.jvm").any { it in content }
    }
    require(violations.isEmpty()) {
        "Production reflect.full regression:\n" + violations.joinToString("\n")
    }
    println("Production core: no kotlin.reflect.full or kotlin.reflect.jvm references")
}

fun collectRelease(repository: Path, destination: Path) {
    require(repository.isDirectory()) { "Release repository missing: $repository" }
    destination.createDirectories()
    val binaries =
        tree(repository).filter { it.isRegularFile() && it.extension in setOf("jar", "klib") }
    require(binaries.isNotEmpty()) { "Release repository has no binaries: $repository" }
    binaries.forEach {
        Files.copy(
            it,
            destination.resolve(it.fileName),
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.COPY_ATTRIBUTES,
        )
    }
    val artifacts =
        tree(destination).filter {
            it.parent == destination &&
                it.isRegularFile() &&
                it.name !in setOf("SHA256SUMS.txt", "ARTIFACTS.txt")
        }
    destination
        .resolve("SHA256SUMS.txt")
        .write(artifacts.joinToString("") { digest(it) + "  " + it.name + "\n" })
    destination.resolve("ARTIFACTS.txt").write(artifacts.joinToString("\n") { it.name } + "\n")
    println(
        "Collected ${binaries.size} published binaries and ${artifacts.size} release artifacts in $destination"
    )
}

cli {
    if (args.isEmpty() || args[0] in setOf("--help", "-h"))
        println(
            "Usage: scripts/run ci jvm|native|integration|env|reflect-check|collect-release [--jdk 17|21|25] [--target TARGET] [--link-only]"
        )
    else {
        val options = Options(args, setOf("--link-only"))
        options.validate(setOf("--jdk", "--target", "--repository", "--output"))
        require(options.positional.size == 1) { "Expected one CI command" }
        selectedJdk(options.one("--jdk")?.toInt()) {
            when (options.positional.single()) {
                "reflect-check" -> reflectCheck()
                "collect-release" ->
                    collectRelease(
                        Path.of(
                                options.one(
                                    "--repository",
                                    repositoryRoot.resolve("build/release/repository").toString(),
                                )!!
                            )
                            .toAbsolutePath()
                            .normalize(),
                        Path.of(
                                options.one(
                                    "--output",
                                    repositoryRoot.resolve("dist/release").toString(),
                                )!!
                            )
                            .toAbsolutePath()
                            .normalize(),
                    )
                "env" -> exportEnvironment()
                "integration" -> integration()
                "jvm" -> jvm()
                "native" ->
                    native(
                        options.one("--target") ?: error("native requires --target"),
                        options.has("--link-only"),
                    )
                else -> error("Unknown CI command")
            }
        }
    }
}
