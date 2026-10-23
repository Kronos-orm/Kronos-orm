@file:Import("common.main.kts")

import java.nio.file.Path
import java.security.MessageDigest
import java.util.zip.ZipFile
import kotlin.io.path.fileSize
import kotlin.io.path.isRegularFile

/** Shared Kover runtime and provenance for independent CLI and real IDE processes. */
fun coverageAgent(): Path = catalogMavenJar("kover-agent")

fun coverageAdapterSource(): Path = repositoryRoot.resolve(
    "kronos-testing/ide/src/coverage/java/com/kotlinorm/ide/coverage/KoverResourceAgent.java")

fun coverageSourceRevision(): String = sourceRevision()

/** Archive timestamps may differ across runners; every executable class must be byte-identical. */
fun coverageClassesFingerprint(jar: Path): String {
    val hash = MessageDigest.getInstance("SHA-256")
    ZipFile(jar.toFile()).use { archive ->
        val entries = archive.entries().asSequence().filter { it.name.endsWith(".class") }.sortedBy { it.name }.toList()
        require(entries.isNotEmpty()) { "Compiler coverage artifact has no classes: $jar" }
        for (entry in entries) {
            require(entry.name.startsWith("com/kotlinorm/compiler/")) { "Unexpected compiler coverage class: ${entry.name}" }
            val bytes = archive.getInputStream(entry).use { it.readBytes() }
            val classHash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
            hash.update("${entry.name} $classHash\n".toByteArray(Charsets.UTF_8))
        }
    }
    return hash.digest().joinToString("") { "%02x".format(it) }
}

fun coverageProvenance(kind: String, jar: Path, binary: Path, tests: Any): Map<String, Any> {
    require(binary.isRegularFile() && binary.fileSize() > 0) { "Missing actual $kind coverage data: $binary" }
    return linkedMapOf(
        "schema" to 1, "kind" to kind, "source" to coverageSourceRevision(),
        "runId" to (System.getenv("GITHUB_RUN_ID") ?: "local"),
        "runAttempt" to (System.getenv("GITHUB_RUN_ATTEMPT") ?: "local"),
        "toolchainSha256" to digest(repositoryRoot.resolve("kotlin")),
        "catalogSha256" to digest(repositoryRoot.resolve("libs.versions.toml")),
        "agentSha256" to digest(coverageAgent()), "binarySha256" to digest(binary),
        "compilerJarSha256" to digest(jar), "compilerClassesSha256" to coverageClassesFingerprint(jar),
        "tests" to tests,
    ) + if (kind == "ide") mapOf(
        "adapterSourceSha256" to digest(coverageAdapterSource()),
        "adapterJarSha256" to digest(binary.parent.resolve("instrumentation-adapter.jar")),
    ) else emptyMap()
}

fun verifyCoverageProvenance(directory: Path, kind: String): com.fasterxml.jackson.databind.JsonNode {
    val model = jsonMapper.readTree(directory.resolve("manifest.json").toFile())
    val jar = directory.resolve("compiler.jar")
    val binary = directory.resolve("compiler.ic")
    require(model["schema"]?.asInt() == 1 && model["kind"]?.asText() == kind) { "Invalid $kind coverage manifest" }
    val expected = coverageProvenance(kind, jar, binary, model.path("tests"))
    // A rerun of only the final job may reuse successful inputs from an earlier attempt
    // of the same run; source and executable bytes remain mandatory exact matches.
    for (key in expected.keys - setOf("tests", "runAttempt")) {
        require(model.path(key).asText() == expected.getValue(key).toString()) { "Stale or mismatched $kind coverage $key" }
    }
    val currentAttempt = System.getenv("GITHUB_RUN_ATTEMPT")?.toInt()
    if (currentAttempt != null) require(model.path("runAttempt").asInt() in 1..currentAttempt) {
        "Invalid $kind coverage run attempt"
    }
    return model
}
