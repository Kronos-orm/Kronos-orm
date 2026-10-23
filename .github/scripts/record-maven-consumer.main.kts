@file:Import("../../scripts/common.main.kts")

import java.nio.file.Files
import kotlin.io.path.isRegularFile
import java.nio.file.Path
import java.util.UUID

cli {
    require(args.isEmpty()) { "record-maven-consumer.main.kts accepts no arguments" }
    val directory = Path.of(requireNotNull(System.getenv("KRONOS_CONSUMER_EVIDENCE_DIR")))
    val classpath = directory.resolve("maven-${UUID.randomUUID()}.classpath")
    Files.createDirectories(directory)
    // Resolve through the same Maven session configuration and repositories as the tested project.
    run(listOf("mvn", "-B", dependency("maven-dependency-plugin") + ":build-classpath",
        "-Dmdep.outputFile=$classpath"), cwd = Path.of(System.getProperty("user.dir")))
    val plugins = directory.resolve("maven-${UUID.randomUUID()}.plugins")
    run(listOf("mvn", "-B", dependency("maven-dependency-plugin") + ":resolve-plugins",
        "-DincludeArtifactIds=kotlin-maven-plugin", "-DoutputAbsoluteArtifactFilename=true", "-DoutputFile=$plugins"),
        cwd = Path.of(System.getProperty("user.dir")))
    val pluginRecords = plugins.text().lineSequence().mapNotNull { line ->
        val parts = line.trim().split(':', limit = 5)
        if (parts.size != 5 || parts[0] != "com.kotlinorm") null else {
            require(parts[2] == "jar") { "Unexpected Kronos compiler integration artifact: $line" }
            val path = Path.of(parts[4]).toAbsolutePath().normalize()
            require(path.isRegularFile()) { "Resolved Maven compiler artifact disappeared: $path" }
            mapOf("group" to parts[0], "artifact" to parts[1], "version" to parts[3], "filename" to path.fileName.toString(),
                "sha256" to digest(path), "path" to path.toString(), "configuration" to "maven-kotlin-plugin")
        }
    }.toList()
    require(pluginRecords.map { it["artifact"] }.containsAll(listOf("kronos-maven-plugin", "kronos-compiler-plugin"))) {
        "Maven did not resolve the Kronos compiler integrations"
    }
    val records = classpath.text().trim().split(java.io.File.pathSeparator).mapNotNull { entry ->
        val path = Path.of(entry).toAbsolutePath().normalize()
        val segments = path.map(Path::toString)
        val group = segments.windowed(2).indexOfLast { it == listOf("com", "kotlinorm") }
        if (group < 0) null else {
            require(path.isRegularFile()) { "Resolved Maven artifact disappeared: $path" }
            mapOf("group" to "com.kotlinorm", "artifact" to segments[group + 2],
                "version" to segments[group + 3], "filename" to path.fileName.toString(),
                "sha256" to digest(path), "path" to path.toString(), "configuration" to "maven-test-classpath")
        }
    }
    require(records.isNotEmpty()) { "No Kronos artifacts in the resolved Maven classpath" }
    directory.resolve("maven-${UUID.randomUUID()}.jsonl").write((records + pluginRecords).joinToString("\n") { jsonMapper.writeValueAsString(it) } + "\n")
}
