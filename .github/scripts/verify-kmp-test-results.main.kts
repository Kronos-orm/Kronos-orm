@file:Import("../../scripts/common.main.kts")

import java.nio.file.Path
import org.w3c.dom.Element

if (args.any { it == "--help" || it == "-h" }) {
    println("Usage: verify-kmp-test-results.main.kts [--require-suite NAME] DIRECTORY ...")
    kotlin.system.exitProcess(0)
}

// Require actual successful execution, including nested Android device reports.
fun verify(directory: Path, requiredSuites: List<String>): Int {
    val reports = tree(directory).filter { it.fileName.toString().let { name -> name.startsWith("TEST-") && name.endsWith(".xml") } }
    require(reports.isNotEmpty()) { "$directory: no JUnit reports; test execution is required" }
    var executed = 0
    val executedSuites = mutableSetOf<String>()
    for (report in reports) {
        val root = xml(report).documentElement
        val suites = if (root.tagName == "testsuite") listOf(root) else {
            val nodes = root.getElementsByTagName("testsuite")
            (0 until nodes.length).map { nodes.item(it) as Element }
        }
        for (suite in suites) {
            fun count(name: String) = suite.getAttribute(name).ifEmpty { "0" }.toInt()
            require(count("failures") == 0 && count("errors") == 0) { "$report: failing tests" }
            for (case in suite.children("testcase")) {
                require(case.child("failure") == null && case.child("error") == null) { "$report: failing tests" }
                if (case.child("skipped") == null && case.getAttribute("status") !in setOf("disabled", "notrun")) {
                    executedSuites += case.getAttribute("classname").ifEmpty { suite.getAttribute("name") }
                }
            }
            executed += count("tests") - count("skipped") - count("disabled")
        }
    }
    require(executed > 0) { "$directory: no tests executed" }
    for (suite in requiredSuites) require(suite in executedSuites) { "$directory: required suite did not execute: $suite" }
    return executed
}

cli {
    val options = Options(args)
    options.validate(setOf("--require-suite"))
    require(options.positional.isNotEmpty()) { "Usage: verify-kmp-test-results.main.kts [--require-suite NAME] DIRECTORY ..." }
    for (value in options.positional) {
        val directory = Path.of(value)
        println("$directory: ${verify(directory, options.all("--require-suite"))} tests executed")
    }
}
