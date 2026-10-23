@file:Import("../../scripts/common.main.kts")

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.io.StringReader
import javax.swing.text.MutableAttributeSet
import javax.swing.text.html.HTML
import javax.swing.text.html.HTMLEditorKit
import javax.swing.text.html.parser.ParserDelegator
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlin.system.exitProcess

if (args.any { it == "--help" || it == "-h" }) {
    println("Usage: compare-coverage.main.kts --module NAME=XML_PATH [--module ...] [--baseline-dir DIRECTORY] " +
        "--output-json PATH --output-md PATH [--summary PATH]")
    exitProcess(0)
}

val marker = "<!-- kronos-coverage-comparison -->"
data class Metric(val percent: BigDecimal?, val covered: Long? = null, val missed: Long? = null) {
    val total: Long? get() = if (covered == null || missed == null) null else covered + missed
    fun json() = mapOf("percent" to percent, "covered" to covered, "missed" to missed, "total" to total)
}
data class Coverage(val module: String, val line: Metric? = null, val branch: Metric? = null, val error: String? = null) {
    fun complete() = error == null && line?.percent != null && branch?.percent != null
}
data class Comparison(val module: String, val baseline: Coverage, val current: Coverage,
    val lineDelta: BigDecimal?, val branchDelta: BigDecimal?, val status: String, val errors: List<String>, val failed: Boolean) {
    fun json() = mapOf("module" to module, "baseline" to mapOf("line" to baseline.line?.json(), "branch" to baseline.branch?.json()),
        "current" to mapOf("line" to current.line?.json(), "branch" to current.branch?.json()),
        "delta" to mapOf("line" to lineDelta, "branch" to branchDelta), "status" to status, "errors" to errors)
}

fun percentage(covered: Long, missed: Long): BigDecimal {
    require(covered >= 0 && missed >= 0) { "Coverage counters must be non-negative" }
    val total = covered + missed
    return if (total == 0L) BigDecimal(100) else BigDecimal(covered).multiply(BigDecimal(100)).divide(BigDecimal(total), MathContext.DECIMAL128)
}

fun currentCoverage(module: String, path: Path): Coverage {
    if (!path.isRegularFile()) return Coverage(module, error = "Current XML report not found: $path")
    return try {
        val metrics = xml(path).documentElement.children("counter").mapNotNull { counter ->
            val kind = counter.getAttribute("type").uppercase()
            if (kind !in setOf("LINE", "BRANCH")) null else {
                val covered = counter.getAttribute("covered").ifEmpty { "0" }.toLong()
                val missed = counter.getAttribute("missed").ifEmpty { "0" }.toLong()
                kind to Metric(percentage(covered, missed), covered, missed)
            }
        }.toMap()
        val missing = listOf("LINE", "BRANCH").filterNot { it in metrics }
        Coverage(module, metrics["LINE"], metrics["BRANCH"], if (missing.isEmpty()) null else
            "Current XML report has no report-level ${missing.joinToString(", ")} counter")
    } catch (failure: org.xml.sax.SAXException) {
        Coverage(module, error = "Cannot parse current XML report: ${failure.message}")
    } catch (failure: java.io.IOException) {
        Coverage(module, error = "Cannot parse current XML report: ${failure.message}")
    } catch (failure: javax.xml.parsers.ParserConfigurationException) {
        Coverage(module, error = "Cannot parse current XML report: ${failure.message}")
    } catch (failure: IllegalArgumentException) {
        Coverage(module, error = "Cannot parse current XML report: ${failure.message}")
    }
}

// The JDK HTML parser handles entities and nested markup from published Kover pages.
fun coverageTables(html: String): List<List<List<String>>> {
    val tables = mutableListOf<List<List<String>>>()
    var table: MutableList<List<String>>? = null
    var row: MutableList<String>? = null
    var cell: StringBuilder? = null
    ParserDelegator().parse(StringReader(html), object : HTMLEditorKit.ParserCallback() {
        override fun handleStartTag(tag: HTML.Tag, attributes: MutableAttributeSet, position: Int) {
            when {
                tag == HTML.Tag.TABLE && attributes.getAttribute(HTML.Attribute.CLASS)?.toString()?.split(Regex("\\s+"))?.contains("coverageStats") == true -> table = mutableListOf()
                table != null && tag == HTML.Tag.TR -> row = mutableListOf()
                row != null && (tag == HTML.Tag.TD || tag == HTML.Tag.TH) -> cell = StringBuilder()
            }
        }
        override fun handleText(data: CharArray, position: Int) { cell?.append(data) }
        override fun handleEndTag(tag: HTML.Tag, position: Int) {
            when {
                cell != null && (tag == HTML.Tag.TD || tag == HTML.Tag.TH) -> {
                    row?.add(cell.toString().trim().replace(Regex("\\s+"), " ")); cell = null
                }
                row != null && tag == HTML.Tag.TR -> {
                    if (row!!.isNotEmpty()) {
                        table?.add(row!!.toList())
                    }
                    row = null
                }
                table != null && tag == HTML.Tag.TABLE -> { tables += table!!.toList(); table = null }
            }
        }
    }, true)
    return tables
}

fun htmlMetric(text: String): Metric {
    val absolute = Regex("\\((\\d+)\\s*/\\s*(\\d+)\\)").find(text)
    if (absolute != null) {
        val covered = absolute.groupValues[1].toLong()
        val missed = absolute.groupValues[2].toLong() - covered
        return Metric(percentage(covered, missed), covered, missed)
    }
    val normalized = text.trim()
    val percent = if (normalized.isEmpty() || normalized.uppercase() == "N/A") null else
        (Regex("([0-9]+(?:\\.[0-9]+)?)\\s*%").find(normalized)?.groupValues?.get(1) ?: normalized.removeSuffix("%")).toBigDecimal()
    return Metric(percent)
}

fun baselineCoverage(module: String, directory: Path): Coverage {
    val path = directory.resolve(module).resolve("index.html")
    if (!path.isRegularFile()) {
        val fetchError = directory.resolve(".baseline-error").takeIf { it.isRegularFile() }?.readText()?.trim().orEmpty()
        return Coverage(module, error = "Baseline HTML not found: $path" + if (fetchError.isEmpty()) "" else ". Baseline checkout error: $fetchError")
    }
    return try {
        for (table in coverageTables(path.readText())) {
            if (table.size < 2) continue
            val header = table.first()
            val name = header.indexOf("Package"); val line = header.indexOf("Line, %"); val branch = header.indexOf("Branch, %")
            if (minOf(name, line, branch) < 0) continue
            val row = table.drop(1).firstOrNull { it.size > maxOf(name, line, branch) && it[name] == "all classes" } ?: continue
            return Coverage(module, htmlMetric(row[line]), htmlMetric(row[branch]))
        }
        Coverage(module, error = "Baseline HTML does not contain an all classes row with Line and Branch coverage: $path")
    } catch (failure: java.io.IOException) {
        Coverage(module, error = "Cannot read baseline HTML $path: ${failure.message}")
    } catch (failure: IllegalArgumentException) {
        Coverage(module, error = "Cannot read baseline HTML $path: ${failure.message}")
    }
}

fun compare(module: String, baseline: Coverage, current: Coverage): Comparison {
    val errors = listOfNotNull(baseline.error, current.error).toMutableList()
    fun delta(name: String, before: Metric?, after: Metric?): BigDecimal? {
        if (before?.percent == null) { errors += "$name coverage missing baseline"; return null }
        if (after?.percent == null) { errors += "$name coverage missing current"; return null }
        return after.percent - before.percent
    }
    val line = delta("line", baseline.line, current.line)
    val branch = delta("branch", baseline.branch, current.branch)
    val down = (line?.signum() ?: 0) < 0 || (branch?.signum() ?: 0) < 0
    val status = when {
        !current.complete() -> "MISSING"
        !baseline.complete() -> if (baseline.error?.startsWith("Baseline HTML not found:") == true && !baseline.error.contains("Baseline checkout error:")) "NEW" else "MISSING"
        down -> "DOWN"
        errors.isNotEmpty() -> "FAIL"
        else -> "PASS"
    }
    return Comparison(module, baseline, current, line, branch, status, errors, errors.isNotEmpty() || down)
}

fun rounded(value: BigDecimal?): String = value?.setScale(2, RoundingMode.HALF_UP)?.toPlainString()?.plus("%") ?: "N/A"
fun deltaLabel(value: BigDecimal?): String {
    if (value == null) return ":warning: ↔ N/A"
    val sign = value.signum()
    val indicator = when { sign > 0 -> ":green_circle: ↑"; sign < 0 -> ":red_circle: ↓"; else -> ":white_circle: →" }
    // Keep a tiny regression visible as -0.00 even after display rounding.
    val number = value.abs().setScale(2, RoundingMode.HALF_UP).toPlainString()
    return "$indicator ${if (sign < 0) "-" else "+"}${number}pp"
}
fun statusLabel(status: String): String = when (status) {
    "PASS" -> ":green_circle: **PASS**"
    "DOWN" -> ":red_circle: **DOWN**"
    "NEW" -> ":large_blue_circle: **NEW**"
    "MISSING" -> ":yellow_circle: **MISSING**"
    "FAIL" -> ":red_circle: **FAIL**"
    else -> status
}
fun markdownReport(rows: List<Comparison>, failed: Boolean): String = buildString {
    appendLine(marker)
    appendLine("## :bar_chart: Test Coverage Report")
    appendLine()
    appendLine(if (failed) "> :x: **Coverage gate failed.** At least one module regressed or a required report could not be compared." else
        "> :white_check_mark: **Coverage gate passed.** No module line or branch coverage decreased.")
    appendLine()
    appendLine("**Modules:** :green_circle: ${rows.count { it.status == "PASS" }} PASS · " +
        ":red_circle: ${rows.count { it.status == "DOWN" }} DOWN · " +
        ":large_blue_circle: ${rows.count { it.status == "NEW" }} NEW · " +
        ":yellow_circle: ${rows.count { it.status == "MISSING" }} MISSING")
    appendLine()
    appendLine("**Trend:** :green_circle: ↑ increase · :red_circle: ↓ decrease · :white_circle: → unchanged · :warning: ↔ unavailable")
    appendLine()
    appendLine("| Module | Line (base → head) | Δ | Branch (base → head) | Δ | Status |")
    appendLine("|--------|--------------------:|--:|----------------------:|--:|:------:|")
    for (row in rows) appendLine("| `${row.module}` | " +
        "${rounded(row.baseline.line?.percent)} → ${rounded(row.current.line?.percent)} | ${deltaLabel(row.lineDelta)} | " +
        "${rounded(row.baseline.branch?.percent)} → ${rounded(row.current.branch?.percent)} | ${deltaLabel(row.branchDelta)} | ${statusLabel(row.status)} |")
    if (rows.any { it.errors.isNotEmpty() }) {
        appendLine(); appendLine("### Coverage baseline/read errors"); appendLine()
        for (row in rows) for (error in row.errors) appendLine("- `${row.module}`: $error")
    }
}

cli {
    val options = Options(args)
    options.validate(setOf("--module", "--baseline-dir", "--output-json", "--output-md", "--summary"))
    require(options.positional.isEmpty()) { "Unexpected arguments: ${options.positional}" }
    val modules = options.all("--module")
    require(modules.isNotEmpty()) { "--module NAME=XML_PATH is required" }
    val baseline = Path.of(options.one("--baseline-dir", ".coverage-baseline")!!)
    val rows = modules.map { spec ->
        require('=' in spec && spec.substringBefore('=').isNotBlank()) { "Invalid --module value '$spec', expected NAME=XML_PATH" }
        val name = spec.substringBefore('=').trim()
        compare(name, baselineCoverage(name, baseline), currentCoverage(name, Path.of(spec.substringAfter('='))))
    }
    val failed = rows.any { it.failed }
    Path.of(requireNotNull(options.one("--output-json")) { "--output-json is required" }).json(
        mapOf("failed" to failed, "baselineDir" to baseline.toString(), "modules" to rows.map { it.json() }))
    val markdown = markdownReport(rows, failed)
    Path.of(requireNotNull(options.one("--output-md")) { "--output-md is required" }).write(markdown)
    options.one("--summary")?.let { Files.writeString(Path.of(it), markdown, Charsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND) }
    print(markdown)
    exitProcess(if (failed) 1 else 0)
}
