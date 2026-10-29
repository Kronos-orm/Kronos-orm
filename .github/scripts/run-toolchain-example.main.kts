@file:Import("../../scripts/common.main.kts")

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.createDirectories
import com.fasterxml.jackson.databind.JsonNode

fun alignExample(workspace: Path, current: Map<String, String>) {
    val template = workspace.resolve("common.module-template.yaml")
    val kotlinVersion = Regex("(?m)^(    version: )[^\\s]+$")
    require(kotlinVersion.findAll(template.text()).count() == 1) { "Expected one Kotlin version in the example template" }
    require(!Regex("(?m)^repositories:").containsMatchIn(template.text())) { "The example repository configuration changed" }
    template.write("repositories:\n  - id: stagedKronos\n    url: mavenLocal\n\n" +
        kotlinVersion.replace(template.text()) { it.groupValues[1] + current.getValue("kotlin") })
    for ((module, artifacts) in mapOf(
        "models" to setOf("kronos-core", "kronos-compiler-plugin"),
        "jvm-app" to setOf("kronos-compiler-plugin"),
        "native-app" to setOf("kronos-compiler-plugin"),
    )) {
        val yaml = workspace.resolve("$module/module.yaml")
        var text = yaml.text()
        for (artifact in artifacts) {
            val coordinate = Regex("com\\.kotlinorm:${Regex.escape(artifact)}:[A-Za-z0-9_.+-]+")
            require(coordinate.findAll(text).count() == 1) { "Expected one $artifact dependency in $yaml" }
            text = coordinate.replace(text, "com.kotlinorm:$artifact:${current.getValue("version")}")
        }
        yaml.write(text)
    }
}

fun loggedCommand(command: List<String>, cwd: Path, environment: Map<String, String>, report: Path): CommandResult {
    report.parent.createDirectories()
    Files.newBufferedWriter(report).use { log ->
        log.appendLine("Command: ${command.joinToString(" ")}")
        val result = run(command, cwd = cwd, environment = environment, check = false,
            capture = true, timeoutSeconds = 1800, onLine = { line ->
                println(line)
                log.appendLine(line)
                log.flush()
            })
        require(result.exitCode == 0) { "Example command failed (${result.exitCode}); see $report" }
        return result
    }
}

fun checkExampleCandidate(repository: Path, reports: Path, name: String) {
    loggedCommand(listOf(repositoryRoot.resolve("scripts/run").toString(), "release", "check-candidate",
        "--repository", repository.toString()), repositoryRoot, emptyMap(), reports.resolve(name))
}

fun prepareExample(workspace: Path, repository: Path, reports: Path) {
    require(!workspace.startsWith(repositoryRoot)) { "The consumer must be outside the source checkout" }
    require(!Files.exists(workspace)) { "A fresh consumer workspace is required: $workspace" }
    val source = repositoryRoot.resolve("kronos-docs/src/assets/examples/kotlin-toolchain")
    require(digest(source.resolve("kotlin")) == digest(repositoryRoot.resolve("kotlin"))) {
        "The documented Toolchain wrapper differs from the candidate wrapper"
    }
    checkExampleCandidate(repository, reports, "candidate-before.log")
    copyTree(source, workspace, skip = setOf("build", ".kotlin"))
    // Maven resolution can update sidecars; consumers must never mutate the publication input.
    copyTree(repository, workspace.resolve("candidate-repository"))
    alignExample(workspace, versions())
    loggedCommand(listOf(repositoryRoot.resolve("scripts/run").toString(), workspace.resolve("prepare.main.kts").toString()),
        workspace, emptyMap(), reports.resolve("prepare.log"))
    Files.copy(repository.resolve("ci-candidate.json"), reports.resolve("candidate.json"), StandardCopyOption.REPLACE_EXISTING)
}

fun isKronosRuntimePayload(rawPath: String): Boolean {
    val value = rawPath.replace('\\', '/')
    return "/com/kotlinorm/" in value && (value.endsWith(".jar") || value.endsWith(".klib")) &&
        !value.endsWith("-sources.jar") && !value.endsWith("-javadoc.jar")
}

fun resolvedArtifactRecord(rawPath: String, root: Path, candidateFiles: JsonNode, state: Path, role: String): Map<String, String> {
    val path = Path.of(rawPath).toRealPath()
    require(path.startsWith(root)) { "Resolved Kronos artifact is outside this consumer's candidate copy: $path" }
    val relative = root.relativize(path).toString().replace('\\', '/')
    val hash = digest(path)
    require(candidateFiles.path(relative).asText() == hash) { "Resolved artifact differs from candidate: $path" }
    val parts = relative.split('/')
    require(parts.size == 5 && parts.take(2) == listOf("com", "kotlinorm")) { "Unexpected Maven payload path: $relative" }
    return mapOf("path" to path.toString(), "sha256" to hash, "candidatePath" to relative,
        "coordinate" to "com.kotlinorm:${parts[2]}:${parts[3]}", "state" to state.fileName.toString(), "role" to role)
}

fun resolvedStateArtifacts(state: Path, root: Path, candidateFiles: JsonNode): List<Map<String, String>> {
    // Toolchain 0.13 writes the actual dependency-resolution outputs and compiler inputs as JSON.
    val model = jsonMapper.readTree(state.toFile())
    return listOf("inputFiles", "outputFiles").flatMap { role ->
        model.path(role).map { it.asText() }.filter(::isKronosRuntimePayload).map { path ->
            resolvedArtifactRecord(path, root, candidateFiles, state, role)
        }
    }
}

fun verifyResolvedArtifacts(
    workspace: Path,
    consumedRepository: Path,
    reports: Path,
    required: Set<String> = setOf("kronos-compiler-plugin", "kronos-core-jvm", "kronos-syntax-jvm",
        "kronos-core-macosarm64", "kronos-syntax-macosarm64"),
) {
    val candidateFiles = jsonMapper.readTree(reports.resolve("candidate.json").toFile()).path("files")
    val root = consumedRepository.toRealPath()
    val stateDirectory = reports.resolve("resolution-state")
    stateDirectory.createDirectories()
    val observed = tree(workspace.resolve("build/incremental.state")).filter { Files.isRegularFile(it) }.flatMap { state ->
        val records = resolvedStateArtifacts(state, root, candidateFiles)
        if (records.isNotEmpty()) Files.copy(state, stateDirectory.resolve(state.fileName), StandardCopyOption.REPLACE_EXISTING)
        records
    }
    val artifacts = observed.map { it.getValue("coordinate").split(':')[1] }.toSet()
    reports.resolve("resolved-artifacts.json").json(observed)
    require(observed.isNotEmpty() && artifacts.containsAll(required)) { "Missing actual candidate resolution evidence: ${required - artifacts}" }
    println("Verified actual candidate resolution for ${artifacts.sorted().joinToString()}")
}

fun runExample(workspace: Path, repository: Path, reports: Path) {
    require(macos && System.getProperty("os.arch") in setOf("aarch64", "arm64")) {
        "The documented Native example gate requires an Arm64 macOS runner"
    }
    val candidate = jsonMapper.readTree(repository.resolve("ci-candidate.json").toFile())
    require(candidate.path("group").asText() == "complete") { "The example requires the complete publication candidate" }
    val consumedRepository = workspace.resolve("candidate-repository")
    val environment = toolchainEnvironment(consumedRepository) + mapOf(
        // Keep verified SDK downloads shared, but never restore resolved Kronos versions from another run.
        "KOTLIN_SHARED_CACHE_DIR" to workspace.resolve("build/consumer-cache").toString(),
    )
    val wrapper = listOf("sh", workspace.resolve("kotlin").toString())
    val expected = "OK factory=fresh models=shared projection=7/Ada"
    for ((module, platform) in listOf("jvm-app" to "jvm", "native-app" to "macosArm64")) {
        val result = loggedCommand(wrapper + listOf("run", "-m", module, "-p", platform), workspace,
            environment, reports.resolve("$module-$platform.log"))
        require(result.output.lineSequence().count { it == expected } == 1) { "$module did not report its verified projection values" }
    }
    loggedCommand(wrapper + listOf("test", "-m", "models", "-p", "jvm"), workspace,
        environment, reports.resolve("models-jvm.log"))
    script(repositoryRoot.resolve(".github/scripts/verify-kmp-test-results.main.kts").toString(),
        listOf("--require-suite", "example.ProjectionTest", workspace.resolve("build/reports/models/jvm").toString()))
    copyTree(workspace.resolve("build/reports/models/jvm"), reports.resolve("models-jvm"))

    loggedCommand(wrapper + listOf("test", "-m", "models", "-p", "macosArm64"), workspace,
        environment, reports.resolve("models-macosArm64.log"))
    val nativeReports = workspace.resolve("build/reports/models/macosArm64")
    if (Files.isDirectory(nativeReports)) copyTree(nativeReports, reports.resolve("models-macosArm64"))
    // Check completed Native cases independently of the CLI exit status and test-discovery messages.
    val binary = workspace.resolve("build/tasks/_models_linkMacosArm64TestDebug/models_test.kexe")
    loggedCommand(listOf(repositoryRoot.resolve("scripts/run").toString(),
        repositoryRoot.resolve(".github/scripts/run-kmp-native-binaries.main.kts").toString(),
        "--report", reports.resolve("models-macosArm64.json").toString(),
        "--require-suite", "example.ProjectionTest", binary.toString()),
        workspace, environment, reports.resolve("models-macosArm64-completed-cases.log"))
    verifyResolvedArtifacts(workspace, consumedRepository, reports)
    checkExampleCandidate(repository, reports, "candidate-after.log")
}

cli {
    if (args.isEmpty() || args.any { it in setOf("--help", "-h") }) {
        println("Usage: run-toolchain-example.main.kts --repository PATH --workspace PATH --reports PATH [--prepare-only]")
    } else {
        val options = Options(args, setOf("--prepare-only"))
        options.validate(setOf("--repository", "--workspace", "--reports"))
        require(options.positional.isEmpty()) { "Unexpected positional arguments" }
        fun path(name: String) = Path.of(requireNotNull(options.one(name)) { "$name is required" }).toAbsolutePath().normalize()
        val workspace = path("--workspace")
        val repository = path("--repository")
        val reports = path("--reports")
        try {
            prepareExample(workspace, repository, reports)
            if (!options.has("--prepare-only")) runExample(workspace, repository, reports)
        } finally {
            for ((source, destination) in mapOf("logs" to "toolchain-logs", "incremental.state" to "toolchain-state")) {
                val directory = workspace.resolve("build/$source")
                if (Files.isDirectory(directory)) copyTree(directory, reports.resolve(destination))
            }
        }
    }
}
