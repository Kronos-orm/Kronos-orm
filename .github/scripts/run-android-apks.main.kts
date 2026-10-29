@file:Import("../../scripts/common.main.kts")

import java.nio.file.Path
import kotlin.io.path.createDirectories

cli {
    val options = Options(args)
    options.validate(setOf("--bundle", "--report"))
    require(options.positional.isEmpty())
    val bundle = Path.of(requireNotNull(options.one("--bundle")))
    val report = Path.of(requireNotNull(options.one("--report")))
    run(listOf("adb", "install", "-r", bundle.resolve("app.apk").toString()))
    run(listOf("adb", "install", "-r", bundle.resolve("test.apk").toString()))
    val result = run(listOf("adb", "shell", "am", "instrument", "-w", "-r",
        "com.kotlinorm.example.android.test/androidx.test.runner.AndroidJUnitRunner"), capture = true, check = false)
    report.parent.createDirectories()
    report.resolveSibling("instrumentation.log").write(result.output)
    print(result.output)
    val model = document("testsuite")
    val suite = model.documentElement
    suite.setAttribute("name", "AndroidInstrumentation")
    val status = mutableMapOf<String, String>()
    val cases = mutableSetOf<Pair<String, String>>()
    var expected = 0
    var failures = 0
    var skipped = 0
    var completed = false
    var lastKey: String? = null
    for (line in result.output.lineSequence().map { it.trimEnd('\r') }) {
        when {
            line.startsWith("INSTRUMENTATION_STATUS: ") -> {
                val field = line.removePrefix("INSTRUMENTATION_STATUS: ")
                lastKey = field.substringBefore('=')
                status[lastKey!!] = field.substringAfter('=', "")
            }
            line.startsWith("INSTRUMENTATION_STATUS_CODE: ") -> {
                val code = line.substringAfter(": ").toInt()
                status["numtests"]?.toIntOrNull()?.let { expected = maxOf(expected, it) }
                if (code <= 0) {
                    val identity = requireNotNull(status["class"]) to requireNotNull(status["test"])
                    require(cases.add(identity)) { "Duplicate instrumentation result: $identity" }
                    val case = suite.append("testcase")
                    case.setAttribute("classname", identity.first)
                    case.setAttribute("name", identity.second)
                    when (code) {
                        0 -> Unit
                        -3, -4 -> { skipped++; case.append("skipped", status["stack"].orEmpty()) }
                        else -> { failures++; case.append("failure", status["stack"].orEmpty().ifBlank { "Instrumentation status $code" }) }
                    }
                }
                status.clear()
                lastKey = null
            }
            line == "INSTRUMENTATION_CODE: -1" -> completed = true
            lastKey != null && !line.startsWith("INSTRUMENTATION_") -> status[lastKey!!] = status[lastKey!!].orEmpty() + "\n" + line
        }
    }
    suite.setAttribute("tests", cases.size.toString())
    suite.setAttribute("failures", failures.toString())
    suite.setAttribute("errors", if (completed && result.exitCode == 0) "0" else "1")
    suite.setAttribute("skipped", skipped.toString())
    saveXml(model, report)
    require(completed && result.exitCode == 0 && failures == 0 && skipped == 0 && expected > 0 && cases.size == expected) {
        "Instrumentation incomplete or failed: executed=${cases.size}, expected=$expected, failures=$failures, skipped=$skipped"
    }
    require(cases.any { it.first == "com.kotlinorm.example.android.AndroidKronosIntegrationTest" }) { "SQLite instrumentation suite did not execute" }
    recordConsumerArtifacts(listOf(bundle.resolve("app.apk"), bundle.resolve("test.apk")), "android-instrumentation")
}
