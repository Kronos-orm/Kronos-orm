@file:Import("../../scripts/common.main.kts")

import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

if (args.any { it == "--help" || it == "-h" }) {
    println("Usage: run-kmp-native-binaries.main.kts --report jsonMapper [--require-suite NAME] BINARY ...")
    exitProcess(0)
}

data class NativeResult(val exitCode: Int, val output: String, val errors: String)
fun runNative(binary: Path): NativeResult {
    val process = ProcessBuilder(binary.toAbsolutePath().toString(), "--ktest_logger=GTEST").start()
    process.outputStream.close()
    val output = StringBuilder()
    val errors = StringBuilder()
    fun drain(stream: java.io.InputStream, target: StringBuilder) = Thread {
        stream.bufferedReader(Charsets.UTF_8).useLines { lines -> lines.forEach { target.append(it).append('\n') } }
    }.apply { isDaemon = true; start() }
    val stdout = drain(process.inputStream, output)
    val stderr = drain(process.errorStream, errors)
    val completed = process.waitFor(300, TimeUnit.SECONDS)
    if (!completed) {
        process.descendants().use { children -> children.forEach { it.destroyForcibly() } }
        process.destroyForcibly()
    }
    stdout.join(5000); stderr.join(5000)
    require(!stdout.isAlive && !stderr.isAlive) { "Native executable output did not close: $binary" }
    if (!completed) {
        print(output); print(errors)
        error("Native test executable timed out after 300 seconds: $binary")
    }
    return NativeResult(process.exitValue(), output.toString(), errors.toString())
}

fun executedTests(output: String, returncode: Int): List<String> {
    require(returncode == 0) { "Native test executable exited with $returncode" }
    val summaries = Regex("^\\[  PASSED  \\] (\\d+) tests?\\.$", RegexOption.MULTILINE).findAll(output).map { it.groupValues[1].toInt() }.toList()
    val passed = Regex("^\\[       OK \\] (.+) \\(\\d+ ms\\)$", RegexOption.MULTILINE).findAll(output).map { it.groupValues[1] }.toList()
    require(summaries.size == 1 && summaries.single() > 0) { "Native executable must report a positive executed test count" }
    require(passed.size == summaries.single() && passed.distinct().size == passed.size) { "Native test summary does not match completed test cases" }
    require(!Regex("^\\[  FAILED  \\]", RegexOption.MULTILINE).containsMatchIn(output)) { "Native executable reported failing tests" }
    return passed
}

fun nativeMain() {
    val options = Options(args)
    options.validate(setOf("--report", "--require-suite"))
    val report = Path.of(requireNotNull(options.one("--report")) { "--report is required" })
    require(options.positional.isNotEmpty()) { "At least one Native binary is required" }
    val results = options.positional.map { binary ->
        val result = runNative(Path.of(binary))
        print(result.output)
        print(result.errors)
        val passed = executedTests(result.output, result.exitCode)
        mapOf("binary" to binary, "executed" to passed.size, "tests" to passed)
    }
    val tests = results.flatMap { @Suppress("UNCHECKED_CAST") (it.getValue("tests") as List<String>) }
    for (suite in options.all("--require-suite")) require(tests.any { it.startsWith("$suite.") }) { "Required Native suite did not execute: $suite" }
    report.json(results)
}

fun nativeFailure(failure: Throwable): Nothing {
    val message = failure.message.orEmpty().replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")
    println("::error title=Native runtime gate failed::$message")
    exitProcess(1)
}

try { nativeMain() }
catch (failure: java.io.IOException) { nativeFailure(failure) }
catch (failure: IllegalArgumentException) { nativeFailure(failure) }
catch (failure: IllegalStateException) { nativeFailure(failure) }
catch (failure: InterruptedException) { nativeFailure(failure) }
