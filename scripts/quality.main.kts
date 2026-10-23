@file:Import("common.main.kts")

import java.math.BigDecimal
import java.math.MathContext
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.time.Duration
import java.util.zip.ZipFile
import javax.swing.text.MutableAttributeSet
import javax.swing.text.html.HTML
import javax.swing.text.html.HTMLEditorKit
import javax.swing.text.html.parser.ParserDelegator
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.fileSize
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readLines
import kotlin.io.path.reader

val coverageModules =
    listOf("kronos-core", "kronos-syntax", "kronos-compiler-plugin", "kronos-codegen")
val documentationModules =
    coverageModules +
        listOf(
            "kronos-jdbc-wrapper",
            "kronos-logging",
            "kronos-gradle-plugin",
            "kronos-maven-plugin",
        )
val coverageExcludes = mapOf("kronos-core" to listOf("com.kotlinorm.orm.join.PatchKt"))
val coverageMinimums =
    coverageModules.take(3).associateWith {
        mapOf("LINE" to BigDecimal(90), "BRANCH" to BigDecimal(70))
    }

fun artifact(alias: String): Path {
    val model = catalog()
    val entry = model["libraries"][alias] ?: error("Unknown library alias: $alias")
    val (group, name) = entry["module"].asText().split(':')
    val configured = entry["version"]
    val version =
        if (configured.isObject) model["versions"][configured["ref"].asText()].asText()
        else configured.asText()
    val relative = "${group.replace('.', '/')}/$name/$version/$name-$version.jar"
    val target = repositoryRoot.resolve("build/tools/$relative")
    if (!target.isRegularFile()) {
        target.parent.createDirectories()
        val temporary = target.resolveSibling("${target.name}.download")
        try {
            val request =
                HttpRequest.newBuilder(URI.create("https://repo.maven.apache.org/maven2/$relative"))
                    .timeout(Duration.ofSeconds(120))
                    .build()
            val response =
                HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofFile(temporary))
            require(response.statusCode() == 200) {
                "Maven Central HTTP ${response.statusCode()}: $relative"
            }
            ZipFile(temporary.toFile()).use {
                require(it.entries().hasMoreElements()) { "Invalid Maven Central JAR: $relative" }
            }
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING)
        } finally {
            temporary.deleteIfExists()
        }
    }
    return target
}

fun sourceRoots(module: String): List<Path> {
    val directory = repositoryRoot.resolve(module)
    val maven = directory.resolve("src/main/kotlin")
    return if (maven.isDirectory()) listOf(maven)
    else listOf(directory.resolve("src"), directory.resolve("src@jvm")).filter { it.isDirectory() }
}

fun mainJar(module: String): Path =
    repositoryRoot.resolve("build/tasks/_${module}_jarJvm/$module-jvm.jar").also {
        require(it.isRegularFile()) {
            "Missing production JAR: $it; build module with Kotlin Toolchain"
        }
    }

fun testCounts(directory: Path): Map<String, Int> {
    val files = directory.listDirectoryEntries("TEST-*.xml").sorted()
    require(files.isNotEmpty()) { "No JUnit reports from current test run: $directory" }
    val counts = mutableMapOf("tests" to 0, "failures" to 0, "errors" to 0, "skipped" to 0)
    for (file in files) {
        val model = xml(file).documentElement
        val suites =
            if (model.tagName == "testsuite") listOf(model) else model.children("testsuite")
        for (suite in suites) for (key in counts.keys.toList()) counts[key] =
            counts.getValue(key) + suite.getAttribute(key).ifEmpty { "0" }.toInt()
    }
    require(counts.getValue("tests") > counts.getValue("skipped")) {
        "No executed tests in $directory: $counts"
    }
    return counts
}

fun counters(path: Path): Map<String, Map<String, Any>> {
    val result =
        xml(path)
            .documentElement
            .children("counter")
            .filter { it.getAttribute("type") in setOf("LINE", "BRANCH") }
            .associate { counter ->
                val covered = counter.getAttribute("covered").toLong()
                val missed = counter.getAttribute("missed").toLong()
                val total = covered + missed
                val percent =
                    if (total == 0L) BigDecimal(100)
                    else
                        BigDecimal(covered)
                            .multiply(BigDecimal(100))
                            .divide(BigDecimal(total), MathContext.DECIMAL128)
                counter.getAttribute("type") to
                    mapOf(
                        "covered" to covered,
                        "missed" to missed,
                        "percent" to percent.toPlainString(),
                    )
            }
    require(result.keys == setOf("LINE", "BRANCH")) {
        "Missing report-level line/branch counters: $path"
    }
    return result
}

fun coverageReport(module: String, binaries: List<Path>, classes: Path, output: Path) {
    require(binaries.isNotEmpty() && binaries.all { it.isRegularFile() && it.fileSize() > 0 }) {
        "Missing executed coverage data for $module"
    }
    val command =
        listOf(java(), "-jar", artifact("kover-cli").toString(), "report") +
            binaries.map { it.toString() } +
            listOf(
                "--classfiles",
                classes.toString(),
                "--xml",
                output.resolve("report.xml").toString(),
                "--html",
                output.resolve("html").toString(),
                "--title",
                module,
            ) +
            sourceRoots(module).flatMap { listOf("--src", it.toString()) } +
            coverageExcludes[module].orEmpty().flatMap { listOf("--exclude", it) }
    run(command)
}

fun coverage(modules: List<String>): Boolean {
    val agent = artifact("kover-agent")
    var failed = false
    for (module in modules) {
        val output = repositoryRoot.resolve("$module/build/reports/kover")
        deleteTree(output)
        output.createDirectories()
        val junit = repositoryRoot.resolve("build/reports/$module/jvm")
        deleteTree(junit)
        runToolchain(listOf("task", ":$module:jarJvm"))
        val binary = output.resolve("report.ic")
        val agentArgs = output.resolve("agent.args")
        agentArgs.write("report.file=$binary\nreport.append=false\ninclude=com.kotlinorm.*\n")
        val completed =
            runToolchain(
                listOf(
                    "test",
                    "-m",
                    module,
                    "-p",
                    "jvm",
                    "--jvm-args=" + quoteJvmArgument("-javaagent:$agent=file:$agentArgs"),
                ),
                check = false,
            )
        val counts = testCounts(junit)
        val runFailed =
            completed.exitCode != 0 ||
                listOf("failures", "errors", "skipped").any { counts.getValue(it) != 0 }
        failed = failed || runFailed
        val classes = output.resolve("classes")
        classes.createDirectories()
        ZipFile(mainJar(module).toFile()).use { archive ->
            for (entry in archive.entries().asSequence().filter { it.name.endsWith(".class") }) {
                val target = classes.resolve(entry.name).normalize()
                require(target.startsWith(classes.normalize())) {
                    "Invalid class path in production JAR: ${entry.name}"
                }
                target.parent.createDirectories()
                archive.getInputStream(entry).use {
                    Files.copy(it, target, StandardCopyOption.REPLACE_EXISTING)
                }
            }
        }
        coverageReport(module, listOf(binary), classes, output)
        val metrics = counters(output.resolve("report.xml"))
        val violations =
            coverageMinimums[module].orEmpty().mapNotNull { (kind, minimum) ->
                val percent = metrics.getValue(kind).getValue("percent").toString()
                if (percent.toBigDecimal() < minimum) "$kind $percent < $minimum" else null
            }
        val result =
            mapOf(
                "module" to module,
                "tests" to counts,
                "testExitCode" to completed.exitCode,
                "coverage" to metrics,
                "minimumViolations" to violations,
                "failed" to (runFailed || violations.isNotEmpty()),
            )
        output.resolve("summary.json").json(result)
        println(jsonMapper.writeValueAsString(result))
        System.getenv("GITHUB_OUTPUT")?.let { destination ->
            val rounded = metrics.mapValues { (_, values) ->
                values
                    .getValue("percent")
                    .toString()
                    .toBigDecimal()
                    .setScale(2, java.math.RoundingMode.HALF_EVEN)
            }
            Files.writeString(
                Path.of(destination),
                "lines=${rounded.getValue("LINE")}%\nbranch=${rounded.getValue("BRANCH")}\n$module-lines=${rounded.getValue("LINE")}%\n",
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND,
            )
        }
        failed = failed || violations.isNotEmpty()
    }
    return failed
}

fun compare(modules: List<String>, baseline: Path, output: Path): Boolean {
    val command =
        modules.flatMap {
            listOf(
                "--module",
                "$it=${repositoryRoot.resolve("$it/build/reports/kover/report.xml")}",
            )
        } +
            listOf(
                "--baseline-dir",
                baseline.toString(),
                "--output-json",
                output.resolve("comparison.json").toString(),
                "--output-md",
                output.resolve("comparison.md").toString(),
            ) +
            (System.getenv("GITHUB_STEP_SUMMARY")?.let { listOf("--summary", it) } ?: emptyList())
    val result =
        script(
            repositoryRoot.resolve(".github/scripts/compare-coverage.main.kts").toString(),
            command,
            check = false,
        )
    val report = jsonMapper.readTree(output.resolve("comparison.json").toFile())
    return result.exitCode != 0 || !report.has("failed") || report["failed"].asBoolean(true)
}

fun joinTypes(arity: Int, start: Int = 1) =
    (start until start + arity).joinToString(", ") { "T$it" }

fun joinSource(arity: Int, start: Int = 1) = "JoinSource$arity<${joinTypes(arity, start)}>"

fun joinRows(arity: Int) = (1..arity).joinToString(", ") { "state.sourceAt<T$it>(${it - 1})" }

fun joinOverload(arity: Int, receiver: String, parameters: String, body: List<String>): String {
    val constraints = (1..arity).joinToString(", ") { "reified T$it : KPojo" }
    return "inline fun <$constraints, R : JoinResult> $receiver.join(\n$parameters,\n    block: ${joinSource(arity)}.(${joinTypes(arity)}) -> R\n): R {\n" +
        body.joinToString("\n") { "    $it" } +
        "\n}"
}

fun joinOverloads(): String {
    val overloads =
        mutableListOf(
            joinOverload(
                2,
                "T1",
                "    query: KSelectable<T2>",
                listOf(
                    "val (state, row) = selectableJoinState(this, query)",
                    "return JoinSource2<T1, T2>(state).block(this, row)",
                ),
            ),
            joinOverload(
                2,
                "KSelectable<T1>",
                "    table: T2",
                listOf(
                    "val (state, row, right) = selectableLeftJoinState(this, table)",
                    "return JoinSource2<T1, T2>(state).block(row, right)",
                ),
            ),
            joinOverload(
                2,
                "KSelectable<T1>",
                "    query: KSelectable<T2>",
                listOf(
                    "val state = selectableSelectableJoinState(this, query)",
                    "return JoinSource2<T1, T2>(state).block(state.sourceAt(0), state.sourceAt(1))",
                ),
            ),
        )
    for (arity in 2..16) {
        val tables = (2..arity).joinToString(", ") { "table$it" }
        overloads +=
            joinOverload(
                arity,
                "T1",
                (2..arity).joinToString(",\n") { "    table$it: T$it" },
                listOf(
                    "val source = ${joinSource(arity)}(tableJoinState(this, $tables))",
                    "return source.block(this, $tables)",
                ),
            )
    }
    for (raw in 2..15) {
        val arity = raw + 1
        for (selectable in listOf(false, true)) {
            val state =
                if (selectable) "selectableRawJoinState(this, source.joinState)"
                else "prependJoinState(this, source.joinState)"
            val arguments =
                if (selectable) joinRows(arity)
                else "this, " + (2..arity).joinToString(", ") { "state.sourceAt<T$it>(${it - 1})" }
            overloads +=
                joinOverload(
                    arity,
                    if (selectable) "KSelectable<T1>" else "T1",
                    "    source: ${joinSource(raw, 2)}",
                    listOf(
                        "val state = $state",
                        "val joined = ${joinSource(arity)}(state)",
                        "return joined.block($arguments)",
                    ),
                )
        }
        for (selectable in listOf(false, true)) {
            val parameter =
                if (selectable) "query: KSelectable<T$arity>" else "table$arity: T$arity"
            val state =
                if (selectable) "rawSelectableJoinState(joinState, query)"
                else "appendJoinState(joinState, table$arity)"
            val arguments =
                if (selectable) joinRows(arity)
                else
                    (1..arity).joinToString(", ") {
                        if (it == arity) "table$arity" else "state.sourceAt<T$it>(${it - 1})"
                    }
            overloads +=
                joinOverload(
                    arity,
                    joinSource(raw),
                    "    $parameter",
                    listOf(
                        "val state = $state",
                        "val joined = ${joinSource(arity)}(state)",
                        "return joined.block($arguments)",
                    ),
                )
        }
    }
    for (left in 2..15) for (right in 2 until 17 - left) {
        val arity = left + right
        overloads +=
            joinOverload(
                arity,
                joinSource(left),
                "    source: ${joinSource(right, left + 1)}",
                listOf(
                    "val state = rawRawJoinState(joinState, source.joinState)",
                    "val joined = ${joinSource(arity)}(state)",
                    "return joined.block(${joinRows(arity)})",
                ),
            )
    }
    return overloads.joinToString("\n\n")
}

fun generateJoin(output: Path, check: Boolean) {
    val header =
        "/*\n * Copyright 2022-2026 kronos-orm\n *\n * Licensed under the Apache License, Version 2.0 (the \"License\");\n * you may not use this file except in compliance with the License.\n */\n\n"
    val provenance = "// Generated by scripts/quality.main.kts.\n\n"
    val patch =
        header +
            "@file:Suppress(\"MagicNumber\", \"TooManyFunctions\")\n\npackage com.kotlinorm.orm.join\n\nimport com.kotlinorm.beans.dsl.KSelectable\nimport com.kotlinorm.interfaces.KPojo\n\n" +
            provenance +
            joinOverloads() +
            "\n"
    val classes =
        (2..16).map { arity ->
            val constraints = (1..arity).joinToString(", ") { "T$it : KPojo" }
            "class JoinSource$arity<$constraints> @PublishedApi internal constructor(\n" +
                "    state: JoinSourceState<T1>\n) : JoinSource<T1, ${joinSource(arity)}>(state) {\n" +
                "    @PublishedApi\n    internal override fun recreate(state: JoinSourceState<T1>): ${joinSource(arity)} =\n" +
                "        JoinSource$arity(state)\n}"
        }
    val sources =
        header +
            "package com.kotlinorm.orm.join\n\nimport com.kotlinorm.interfaces.KPojo\n\n" +
            provenance +
            classes.joinToString("\n\n") +
            "\n"
    output.createDirectories()
    for ((name, content) in mapOf("JoinSources.kt" to sources, "Patch.kt" to patch)) {
        val target = output.resolve(name)
        if (check)
            require(
                Regex("// Generated by .*\\.").replace(target.text(), provenance.trim()) == content
            ) {
                "Generated JOIN source differs: $target"
            }
        else target.write(content)
    }
    println(
        if (check) "JOIN sources match" else "Generated JOIN sources for arities 2..16 in $output"
    )
}

data class DocSource(
    val name: String,
    val path: Path,
    val platform: String,
    val dependencies: List<String>,
)

fun dokkaConfiguration(
    module: String,
    plugins: List<String>,
    output: Path,
    classpath: List<String>,
): Map<String, Any> {
    val directory = repositoryRoot.resolve(module)
    val maven = directory.resolve("src/main/kotlin")
    val roots =
        (if (maven.isDirectory()) listOf(DocSource("main", maven, "jvm", emptyList()))
            else
                listOf(
                    DocSource("common", directory.resolve("src"), "common", emptyList()),
                    DocSource("jvm", directory.resolve("src@jvm"), "jvm", listOf("common")),
                    DocSource(
                        "native",
                        directory.resolve("src@native"),
                        "native",
                        listOf("common"),
                    ),
                    DocSource("apple", directory.resolve("src@apple"), "native", listOf("native")),
                    DocSource("linux", directory.resolve("src@linux"), "native", listOf("native")),
                    DocSource(
                        "mingwX64",
                        directory.resolve("src@mingwX64"),
                        "native",
                        listOf("native"),
                    ),
                ))
            .filter { it.path.isDirectory() }
    val available = roots.map { it.name }.toSet()
    val readme = directory.resolve("README.md")
    val sets = roots.map { source ->
        mapOf(
            "sourceSetID" to mapOf("scopeId" to module, "sourceSetName" to source.name),
            "displayName" to source.name,
            "sourceRoots" to listOf(source.path.toString()),
            "classpath" to classpath,
            "includes" to
                if (source.name in setOf("main", "common") && readme.isRegularFile())
                    listOf(readme.toString())
                else emptyList<String>(),
            "analysisPlatform" to source.platform,
            "jdkVersion" to 17,
            "dependentSourceSets" to
                source.dependencies
                    .filter { it in available }
                    .map { mapOf("scopeId" to module, "sourceSetName" to it) },
            "documentedVisibilities" to listOf("PUBLIC"),
            "reportUndocumented" to true,
            "skipEmptyPackages" to true,
            "sourceLinks" to
                listOf(
                    mapOf(
                        "localDirectory" to source.path.toString(),
                        "remoteUrl" to
                            "https://github.com/Kronos-orm/Kronos-orm/tree/main/" +
                                repositoryRoot
                                    .relativize(source.path)
                                    .toString()
                                    .replace('\\', '/'),
                        "remoteLineSuffix" to "#L",
                    )
                ),
        )
    }
    return mapOf(
        "moduleName" to module,
        "outputDir" to output.toString(),
        "sourceSets" to sets,
        "pluginsClasspath" to plugins,
        "failOnWarning" to false,
    )
}

fun verifySite(directory: Path, readme: Path? = null) {
    val index = directory.resolve("index.html")
    require(
        index.isRegularFile() &&
            directory.resolve("scripts").isDirectory() &&
            directory.resolve("styles").isDirectory()
    ) {
        "Dokka failed to produce a complete HTML site: $directory"
    }
    for (page in tree(directory).filter { it.extension == "html" }) {
        val text = StringBuilder()
        val callback =
            object : HTMLEditorKit.ParserCallback() {
                override fun handleText(data: CharArray, position: Int) {
                    text.append(data)
                }

                fun target(tag: HTML.Tag, attrs: MutableAttributeSet) {
                    if (tag !in listOf(HTML.Tag.SCRIPT, HTML.Tag.IMG, HTML.Tag.LINK, HTML.Tag.A))
                        return
                    val reference =
                        (attrs.getAttribute(HTML.Attribute.SRC)
                                ?: attrs.getAttribute(HTML.Attribute.HREF))
                            ?.toString() ?: return
                    val uri = URI(reference)
                    if (uri.scheme == null && uri.authority == null && !uri.path.isNullOrEmpty()) {
                        val target =
                            if (uri.path.startsWith('/'))
                                directory.resolve(uri.path.removePrefix("/"))
                            else page.parent.resolve(uri.path)
                        require(target.isRegularFile()) {
                            "Missing Dokka site target '$reference' in $page"
                        }
                    }
                }

                override fun handleStartTag(
                    tag: HTML.Tag,
                    attrs: MutableAttributeSet,
                    position: Int,
                ) = target(tag, attrs)

                override fun handleSimpleTag(
                    tag: HTML.Tag,
                    attrs: MutableAttributeSet,
                    position: Int,
                ) = target(tag, attrs)
            }
        ParserDelegator().parse(page.reader(), callback, true)
        if (page == index && readme != null && readme.isRegularFile()) {
            val markdown = readme.text()
            val paragraphs = markdown.trim().split(Regex("\n\\s*\n"))
            val introduction = paragraphs.drop(1).firstOrNull { !it.startsWith('#') }.orEmpty()
            val expected =
                listOf(introduction) +
                    Regex("(?m)^#{2,6}\\s+(.+)$")
                        .findAll(markdown)
                        .map { it.groupValues[1] }
                        .toList()
            val rendered = text.toString().replace(Regex("\\s+"), " ").trim()
            for (block in expected) {
                val content = block.replace(Regex("[`*]"), "").replace(Regex("\\s+"), " ").trim()
                require(content.isEmpty() || content in rendered) {
                    "Missing README content '$content' in $index"
                }
            }
            for (code in
                Regex("(?ms)^```[^\n]*\n(.*?)^```").findAll(markdown).map {
                    it.groupValues[1]
                }) require(code.replace(Regex("\\s+"), "") in rendered.replace(Regex("\\s+"), "")) {
                "Missing README code from $readme in $index"
            }
        }
    }
}

fun compileClasspath(module: String, buildDir: Path): List<String>? {
    val output =
        buildDir
            .resolve("artifacts/CompiledJvmArtifact/${module}jvm/kotlin-output")
            .toAbsolutePath()
            .normalize()
    val logs =
        tree(buildDir.resolve("logs"))
            .filter { it.name == "debug.log" }
            .sortedByDescending { Files.getLastModifiedTime(it).toMillis() }
    for (log in logs) for (line in log.readLines().asReversed()) {
        if (":$module:compileJvm " !in line || "Kotlin compiler args: " !in line) continue
        val arguments = line.substringAfter("Kotlin compiler args: ")
        if (!Regex(" -module-name " + Regex.escape(module) + "(?: |$)").containsMatchIn(arguments))
            continue
        val match = Regex("(?:^| )-classpath (.+?) -d (.+?) -jdk-home ").find(arguments) ?: continue
        if (
            Path.of(match.groupValues[2]).toAbsolutePath().normalize() == output &&
                output.isDirectory()
        ) {
            val paths = match.groupValues[1].split(java.io.File.pathSeparator)
            if (paths.isNotEmpty() && paths.all { Path.of(it).exists() }) return paths
        }
    }
    return null
}

fun dokka(modules: List<String>, output: Path) {
    val compiler = artifact("dokka-cli")
    val plugins =
        listOf("dokka-base", "dokka-analysis", "dokka-html", "dokka-freemarker").map {
            artifact(it).toString()
        }
    runToolchain(listOf("build", "-p", "jvm") + modules.flatMap { listOf("-m", it) })
    val configDir = repositoryRoot.resolve("build/quality/dokka")
    configDir.createDirectories()
    for (module in modules) {
        var classpath = compileClasspath(module, repositoryRoot.resolve("build"))
        if (classpath == null) {
            val fresh = repositoryRoot.resolve("build/quality/dokka-inputs/$module")
            deleteTree(fresh)
            runToolchain(
                listOf("build", "-m", module, "-p", "jvm", "--build-dir", fresh.toString())
            )
            classpath = compileClasspath(module, fresh)
        }
        require(classpath != null) {
            "Toolchain did not record exact main JVM classpath for $module"
        }
        val target = output.resolve(module)
        deleteTree(target)
        val config = configDir.resolve("$module.json")
        config.json(dokkaConfiguration(module, plugins, target, classpath))
        run(listOf(java(), "-Xmx2g", "-jar", compiler.toString(), config.toString()))
        verifySite(target, repositoryRoot.resolve("$module/README.md"))
    }
}

cli {
    if (args.isEmpty() || args[0] in setOf("-h", "--help"))
        println(
            "Usage: scripts/run quality coverage|compare|dokka|generate-join [--module NAME] [--output PATH] [--baseline-dir PATH] [--check]"
        )
    else {
        val options = Options(args, setOf("--check"))
        options.validate(setOf("--module", "--output", "--baseline-dir"))
        require(options.positional.size == 1) { "Expected one quality command" }
        val command = options.positional.single()
        val modules =
            options.all("--module").ifEmpty {
                if (command == "dokka") documentationModules else coverageModules
            }
        require(modules.all { it in documentationModules }) { "Unknown module" }
        val output = options.one("--output")?.let { Path.of(it).toAbsolutePath().normalize() }
        val baseline =
            options.one("--baseline-dir")?.let { Path.of(it).toAbsolutePath().normalize() }
        when (command) {
            "compare" ->
                if (
                    compare(
                        modules,
                        baseline ?: repositoryRoot.resolve(".coverage-baseline"),
                        output ?: repositoryRoot.resolve("coverage"),
                    )
                )
                    kotlin.system.exitProcess(1)
            "coverage" -> {
                require(modules.all { it in coverageModules }) {
                    "Coverage is defined for core, syntax, compiler-plugin and codegen"
                }
                bootstrap()
                val failed = coverage(modules)
                val regression =
                    baseline?.let {
                        compare(modules, it, output ?: repositoryRoot.resolve("coverage"))
                    } ?: false
                if (failed || regression) kotlin.system.exitProcess(1)
            }
            "generate-join" ->
                generateJoin(
                    output ?: repositoryRoot.resolve("kronos-core/src/com/kotlinorm/orm/join"),
                    options.has("--check"),
                )
            "dokka" -> {
                bootstrap()
                dokka(modules, output ?: repositoryRoot.resolve("docs"))
            }
            else -> error("Unknown quality command: $command")
        }
    }
}
