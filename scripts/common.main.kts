@file:Import("../build/scripts/dependencies.main.kts")

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.toml.TomlMapper
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.readBytes
import kotlin.io.path.readLines
import kotlin.io.path.writeText
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.xml.sax.SAXException

val repositoryRoot =
    Path.of(System.getenv("KRONOS_ROOT") ?: System.getProperty("user.dir"))
        .toAbsolutePath()
        .normalize()
val windows = System.getProperty("os.name").startsWith("Windows")
val macos = System.getProperty("os.name").startsWith("Mac")
val jsonMapper = ObjectMapper()
val tomlMapper = TomlMapper()
val bootstrapRepository = repositoryRoot.resolve("build/repository/bootstrap")
val checksumAlgorithms = listOf("md5", "sha1", "sha256", "sha512")

fun catalog(root: Path = repositoryRoot) =
    tomlMapper.readTree(root.resolve("libs.versions.toml").toFile())

fun dependency(alias: String): String {
    val model = catalog()
    val entry = model["libraries"][alias] ?: error("Unknown dependency alias: $alias")
    val configured = entry["version"]
    val version =
        if (configured.isObject) model["versions"][configured["ref"].asText()].asText()
        else configured.asText()
    return entry["module"].asText() + ":" + version
}

fun Path.text(): String =
    Files.readString(this, Charsets.UTF_8).replace("\r\n", "\n").replace('\r', '\n')

fun Path.write(content: String) {
    parent?.createDirectories()
    writeText(content, Charsets.UTF_8)
}

fun Path.json(value: Any) =
    write(jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value) + "\n")

fun tree(path: Path): List<Path> =
    if (!path.exists()) emptyList() else Files.walk(path).use { it.sorted().toList() }

fun deleteTree(path: Path) {
    tree(path).asReversed().forEach { it.deleteIfExists() }
}

fun copyTree(source: Path, target: Path, skip: Set<String> = emptySet()) {
    Files.walkFileTree(
        source,
        object : SimpleFileVisitor<Path>() {
            override fun preVisitDirectory(
                dir: Path,
                attributes: java.nio.file.attribute.BasicFileAttributes,
            ): FileVisitResult {
                if (dir != source && dir.fileName.toString() in skip)
                    return FileVisitResult.SKIP_SUBTREE
                target.resolve(source.relativize(dir)).createDirectories()
                return FileVisitResult.CONTINUE
            }

            override fun visitFile(
                file: Path,
                attributes: java.nio.file.attribute.BasicFileAttributes,
            ): FileVisitResult {
                if (file.fileName.toString() !in skip)
                    Files.copy(
                        file,
                        target.resolve(source.relativize(file)),
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.COPY_ATTRIBUTES,
                    )
                return FileVisitResult.CONTINUE
            }
        },
    )
}

fun digest(path: Path, algorithm: String = "sha256"): String {
    val hash = MessageDigest.getInstance(algorithm.uppercase().replace("SHA", "SHA-"))
    java.security.DigestInputStream(Files.newInputStream(path), hash).use { it.transferTo(java.io.OutputStream.nullOutputStream()) }
    return hash.digest().joinToString("") { "%02x".format(it) }
}

fun <T> withArtifactCacheLock(target: Path, block: () -> T): T {
    target.parent.createDirectories()
    return synchronized(target.toAbsolutePath().normalize().toString().intern()) {
        java.nio.channels.FileChannel.open(target.resolveSibling("${target.fileName}.cache.lock"),
            java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.WRITE).use { channel -> channel.lock().use { block() } }
    }
}

fun extractedArchiveIsValid(directory: Path, archive: Path): Boolean {
    val manifest = directory.resolveSibling("${directory.fileName}.integrity.json")
    if (!manifest.isRegularFile() || !archive.isRegularFile()) return false
    return runCatching {
        val saved = jsonMapper.readTree(manifest.toFile())
        saved["archiveSha256"].asText() == digest(archive) && saved["files"] == jsonMapper.valueToTree<com.fasterxml.jackson.databind.JsonNode>(
            tree(directory).filter { it.isRegularFile() }.associate { directory.relativize(it).toString() to digest(it) })
    }.getOrDefault(false)
}

fun extractVerifiedArchive(archive: Path, directory: Path, extract: () -> Unit) = withArtifactCacheLock(directory) {
    if (!extractedArchiveIsValid(directory, archive)) {
        deleteTree(directory)
        directory.createDirectories()
        extract()
        directory.resolveSibling("${directory.fileName}.integrity.json").json(mapOf("archiveSha256" to digest(archive),
            "files" to tree(directory).filter { it.isRegularFile() }.associate { directory.relativize(it).toString() to digest(it) }))
    }
}

fun downloadVerified(url: String, target: Path, checksumUrl: String? = "$url.sha256", algorithm: String = "sha256"): Path {
    require(algorithm in setOf("sha1", "sha256")) { "Unsupported upstream checksum: $algorithm" }
    target.parent.createDirectories()
    val metadata = target.resolveSibling("${target.fileName}.verified-download.json")
    return withArtifactCacheLock(target) {
        val pattern = Regex(if (algorithm == "sha1") "[0-9a-fA-F]{40}" else "[0-9a-fA-F]{64}")
        val cached = if (metadata.isRegularFile()) runCatching { jsonMapper.readTree(metadata.toFile()) }.getOrNull() else null
        val trusted = cached?.takeIf { it.path("url").asText() == url && it.path("checksumUrl").asText() == checksumUrl.orEmpty() &&
            it.path("algorithm").asText() == algorithm && it.path("expected").asText().matches(pattern) }?.get("expected")?.asText()
        if (trusted != null && target.isRegularFile() && digest(target, algorithm).equals(trusted, ignoreCase = true)) return@withArtifactCacheLock target
        val temporary = Files.createTempFile(target.parent, "${target.fileName}.", ".part")
        val record = Files.createTempFile(target.parent, "${target.fileName}.", ".metadata")
        fun download(source: String) = run(listOf("curl", "--fail", "--silent", "--show-error", "--location", "--retry", "3",
            "--connect-timeout", "30", "--max-time", "600", "--output", temporary.toString(), source))
        try {
            val expected = if (checksumUrl == null) null else trusted ?: checksumUrl.let { source ->
                download(source)
                temporary.text().trim().split(Regex("\\s+")).first().also { require(it.matches(pattern)) { "Invalid upstream checksum: $source" } }
            }
            download(url)
            val actual = digest(temporary, algorithm)
            require(expected == null || actual.equals(expected, ignoreCase = true)) { "Downloaded artifact checksum mismatch: $url" }
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING)
            record.json(mapOf("url" to url, "checksumUrl" to checksumUrl.orEmpty(), "algorithm" to algorithm, "expected" to (expected ?: actual)))
            Files.move(record, metadata, StandardCopyOption.REPLACE_EXISTING)
            target
        } finally { temporary.deleteIfExists(); record.deleteIfExists() }
    }
}

/** Maven Central publishes SHA-1 sidecars for all releases; SHA-256 is not universal. */
fun catalogMavenJar(alias: String): Path {
    val (group, artifact, version) = dependency(alias).split(':')
    val relative = "${group.replace('.', '/')}/$artifact/$version/$artifact-$version.jar"
    val url = "https://repo.maven.apache.org/maven2/$relative"
    val target = downloadVerified(url, repositoryRoot.resolve("build/tools/$relative"), "$url.sha1", "sha1")
    java.util.zip.ZipFile(target.toFile()).use { archive ->
        require(archive.entries().hasMoreElements()) { "Invalid Maven Central JAR: $relative" }
    }
    return target
}

/** An explicit source identity wins when a parent CI workflow belongs to another repository. */
fun sourceRevision(sourceEnvironment: Map<String, String> = System.getenv()): String =
    sourceEnvironment["KRONOS_SOURCE_SHA"]?.takeIf { it.isNotBlank() }
        ?: sourceEnvironment["GITHUB_SHA"]?.takeIf { it.isNotBlank() }
        ?: run(listOf("git", "rev-parse", "HEAD"), capture = true).output.trim()

fun recordConsumerArtifacts(files: List<Path>, context: String) {
    val directory = System.getenv("KRONOS_CONSUMER_EVIDENCE_DIR")?.let { Path.of(it) } ?: return
    val records = files.map { file ->
        require(file.isRegularFile()) { "Consumed artifact is missing: $file" }
        mapOf("path" to file.toAbsolutePath().toString(), "filename" to file.fileName.toString(),
            "sha256" to digest(file), "configuration" to context)
    }
    directory.resolve("runtime-${java.util.UUID.randomUUID()}.jsonl").write(records.joinToString("\n") { jsonMapper.writeValueAsString(it) } + "\n")
}

fun xml(path: Path): Document {
    val factory = DocumentBuilderFactory.newInstance()
    factory.isNamespaceAware = true
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
    factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
    factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "")
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "")
    return factory.newDocumentBuilder().parse(path.toFile())
}

fun document(root: String): Document =
    DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument().apply {
        appendChild(createElement(root))
    }

fun Element.children(name: String): List<Element> =
    (0 until childNodes.length)
        .map { childNodes.item(it) }
        .filterIsInstance<Element>()
        .filter { it.localName == name || it.tagName == name }

fun Element.child(name: String): Element? = children(name).firstOrNull()

fun Element.value(name: String, fallback: String = "") = child(name)?.textContent ?: fallback

fun Element.append(name: String, text: String? = null): Element =
    ownerDocument.createElementNS(namespaceURI, name).also {
        text?.let { content -> it.textContent = content }
        appendChild(it)
    }

fun saveXml(model: Document, path: Path) {
    path.parent.createDirectories()
    val transformer =
        TransformerFactory.newInstance()
            .apply {
                setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "")
                setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "")
            }
            .newTransformer()
    transformer.setOutputProperty(OutputKeys.INDENT, "yes")
    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8")
    transformer.transform(DOMSource(model), StreamResult(path.toFile()))
}

data class CommandResult(val exitCode: Int, val output: String)

fun run(
    command: List<String>,
    cwd: Path = repositoryRoot,
    environment: Map<String, String> = emptyMap(),
    check: Boolean = true,
    capture: Boolean = false,
    input: ByteArray? = null,
    timeoutSeconds: Long = 0,
    onLine: ((String) -> Unit)? = null,
): CommandResult {
    val builder = ProcessBuilder(command).directory(cwd.toFile()).redirectErrorStream(true)
    builder.environment().putAll(environment)
    if (!capture && timeoutSeconds == 0L && onLine == null) builder.inheritIO()
    if (input != null) builder.redirectInput(ProcessBuilder.Redirect.PIPE)
    val process = builder.start()
    if (input != null) process.outputStream.use { it.write(input) }
    else process.outputStream.close()
    val output = StringBuilder()
    val reader =
        if (capture || timeoutSeconds > 0 || onLine != null)
            Thread {
                process.inputStream.bufferedReader(Charsets.UTF_8).useLines { lines ->
                    lines.forEach {
                        output.append(it).append('\n')
                        if (onLine != null) onLine(it) else if (!capture) println(it)
                    }
                }
            }
                .apply { start() }
        else null
    if (timeoutSeconds > 0 && !process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
        process.destroyForcibly()
        reader?.join()
        error("Command timed out after ${timeoutSeconds}s: ${command.first()}")
    }
    val code = process.waitFor()
    reader?.join()
    if (check && code != 0)
        error(
            "Command failed ($code): ${command.joinToString(" ")}\n${output.toString().takeLast(16000)}"
        )
    return CommandResult(code, output.toString())
}

fun java(): String =
    System.getenv("JAVA_HOME")?.let {
        Path.of(it, "bin", if (windows) "java.exe" else "java").toString()
    } ?: "java"

fun quote(value: String): String =
    if (windows) "\"" + value.replace("\"", "\\\"") + "\""
    else "'" + value.replace("'", "'\\''") + "'"

fun quoteJvmArgument(value: String): String =
    "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

fun script(
    name: String,
    arguments: List<String> = emptyList(),
    root: Path = repositoryRoot,
    check: Boolean = true,
    environment: Map<String, String> = emptyMap(),
): CommandResult {
    val launcher = root.resolve("scripts/" + if (windows) "run.cmd" else "run").toString()
    val command =
        (if (windows) listOf(System.getenv("COMSPEC") ?: "cmd.exe", "/d", "/c", launcher)
        else listOf(launcher)) + name + arguments
    return run(command, cwd = root, check = check, environment = environment)
}

fun versions(root: Path = repositoryRoot): Map<String, String> {
    val model = catalog(root)
    val version = model["libraries"]["kronos-compiler-plugin"]["version"].asText()
    val text = root.resolve("templates/common.module-template.yaml").text()
    val configured =
        Regex("(?m)^    version: ([^\\s]+)$")
            .findAll(text.substringAfter("  publishing:\n").substringBefore("\ntest-settings:"))
            .map { it.groupValues[1] }
            .toList()
    require(configured == listOf(version)) {
        "Publication version differs from compiler catalog: $configured, $version"
    }
    val resource =
        root.resolve(
            "kronos-gradle-plugin/src/main/resources/META-INF/kronos/compiler-plugin.properties"
        )
    require(resource.text() == "version=$version\n") {
        "Gradle plugin version resource differs from compiler catalog"
    }
    return mapOf("version" to version, "kotlin" to model["versions"]["kotlin"].asText())
}

val testDefaults =
    mapOf(
        "MYSQL_USERNAME" to "kronos",
        "MYSQL_PASSWORD" to "kronos",
        "MYSQL_DATABASE" to "kronos_testing",
        "POSTGRES_USERNAME" to "kronos",
        "POSTGRES_PASSWORD" to "",
        "POSTGRES_DATABASE" to "kronos_testing",
        "SQLSERVER_USERNAME" to "SA",
        "SQLSERVER_PASSWORD" to "YourStrong!Passw0rd",
        "SQLSERVER_DATABASE" to "kronos_testing",
        "ORACLE_JDBC_URL" to "jdbc:oracle:thin:@localhost:1521/FREEPDB1",
        "ORACLE_USERNAME" to "kronos",
        "ORACLE_PASSWORD" to "KronosPassw0rd1",
        "ORACLE_SYS_PASSWORD" to "YourStrong!Passw0rd",
        "DM_JDBC_URL" to "jdbc:dm://localhost:5237",
        "DM_USERNAME" to "SYSDBA",
        "DM_PASSWORD" to "DMdba_123",
        "DM_SYSAUDITOR_PASSWORD" to "DMAuditor_123",
        "ORACLE_DOCKER_IMAGE" to "ghcr.io/gvenzl/oracle-free:23.9-slim",
    )

fun testEnvironment(): MutableMap<String, String> {
    val values = testDefaults.toMutableMap()
    repositoryRoot
        .resolve("kronos-testing/env.local.properties")
        .takeIf { it.isRegularFile() }
        ?.readLines()
        ?.forEach { line ->
            if (line.isNotBlank() && !line.trimStart().startsWith("#")) {
                val key = line.substringBefore('=')
                require('=' in line && key.matches(Regex("[A-Za-z_][A-Za-z0-9_]*"))) {
                    "Invalid environment key: $key"
                }
                values[key] = line.substringAfter('=')
            }
        }
    values.putAll(
        System.getenv().entries.associate { (key, value) ->
            (if (windows) key.uppercase(java.util.Locale.ROOT) else key) to value
        }
    )
    if (values["SQLSERVER_JDBC_URL"].isNullOrEmpty())
        values["SQLSERVER_JDBC_URL"] =
            "jdbc:sqlserver://localhost:1433;databaseName=${values["SQLSERVER_DATABASE"]};encrypt=true;trustServerCertificate=true"
    return values
}

fun toolchainEnvironment(repository: Path = bootstrapRepository): Map<String, String> =
    testEnvironment().apply {
        if (windows) {
            val system = Path.of(getValue("SYSTEMROOT"), "System32")
            require(system.resolve("tar.exe").isRegularFile()) { "Windows system tar is required" }
            val unixTools =
                listOf("msys-2.0.dll", "shasum", "shasum.bat", "shasum.cmd", "shasum.exe")
            val paths =
                getValue("PATH").split(java.io.File.pathSeparator).filter { entry ->
                    unixTools.none { Path.of(entry.trim('"'), it).isRegularFile() }
                }
            put(
                "PATH",
                (listOf(system.toString()) + paths).joinToString(java.io.File.pathSeparator),
            )
        }
        put(
            "KOTLIN_CLI_JAVA_OPTIONS",
            (get("KOTLIN_CLI_JAVA_OPTIONS").orEmpty() +
                    " " +
                    quote("-Dmaven.repo.local=" + repository.toString().replace('\\', '/')))
                .trim(),
        )
        put("KOTLIN_CLI_NO_WELCOME_BANNER", "1")
    }

fun runToolchain(
    arguments: List<String>,
    check: Boolean = true,
    root: Path = repositoryRoot,
    repository: Path = bootstrapRepository,
): CommandResult {
    val wrapper = root.resolve(if (windows) "kotlin.bat" else "kotlin").toString()
    val command =
        (if (windows) listOf(System.getenv("COMSPEC") ?: "cmd.exe", "/d", "/c", wrapper)
        else listOf(wrapper)) + arguments
    println("Kronos: ${arguments.joinToString(" ")}")
    val result =
        run(
            command,
            root,
            toolchainEnvironment(repository),
            check = false,
            capture = true,
            onLine = { println(it) },
        )
    if (check && result.exitCode != 0) {
        if (System.getenv("GITHUB_ACTIONS") == "true")
            println(
                "::error title=Kotlin Toolchain failed::" +
                    result.output
                        .lines()
                        .takeLast(80)
                        .joinToString("\n")
                        .replace("%", "%25")
                        .replace("\r", "%0D")
                        .replace("\n", "%0A")
            )
        error("Kotlin Toolchain exited ${result.exitCode}")
    }
    return result
}

fun refreshLocalSnapshotDirectory(directory: Path, group: String, artifact: String, version: String) {
    if (!version.endsWith("-SNAPSHOT")) return
    val model = document("metadata")
    val metadata = model.documentElement
    metadata.setAttribute("modelVersion", "1.1.0")
    metadata.append("groupId", group)
    metadata.append("artifactId", artifact)
    metadata.append("version", version)
    val versioning = metadata.append("versioning")
    versioning.append("snapshot").append("localCopy", "true")
    val updated = java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
        .withZone(java.time.ZoneOffset.UTC).format(java.time.Instant.now().plusSeconds(2))
    versioning.append("lastUpdated", updated)
    val entries = versioning.append("snapshotVersions")
    val prefix = "$artifact-$version"
    for (file in tree(directory).filter { it.isRegularFile() && it.parent == directory }) {
        val name = file.fileName.toString()
        val extension = name.substringAfterLast('.')
        if (!name.startsWith(prefix) || extension !in listOf("pom", "module", "jar", "klib", "json")) continue
        val entry = entries.append("snapshotVersion")
        val classifier = name.substringBeforeLast('.').removePrefix(prefix).removePrefix("-")
        if (classifier.isNotEmpty()) entry.append("classifier", classifier)
        entry.append("extension", extension)
        entry.append("value", version)
        entry.append("updated", updated)
    }
    val descriptor = directory.resolve("maven-metadata-local.xml")
    saveXml(model, descriptor)
    checksumAlgorithms.forEach { descriptor.resolveSibling("${descriptor.fileName}.$it").write(digest(descriptor, it)) }
}

fun bootstrap() {
    val version = versions()
    val candidate = System.getenv("KRONOS_COMPILER_REPOSITORY")?.let { Path.of(it).toAbsolutePath().normalize() }
    if (candidate == null) runToolchain(listOf("publish", "mavenLocal", "-m", "kronos-compiler-plugin"))
    else {
        val manifest = jsonMapper.readTree(candidate.resolve("ci-candidate.json").toFile())
        val revision = System.getenv("GITHUB_SHA") ?: System.getenv("KRONOS_SOURCE_SHA")
            ?: run(listOf("git", "rev-parse", "HEAD"), capture = true).output.trim()
        require(manifest["sha"].asText() == revision) { "Compiler candidate source revision mismatch" }
        require(manifest["versions"] == jsonMapper.valueToTree<com.fasterxml.jackson.databind.JsonNode>(version)) { "Compiler candidate versions mismatch" }
        val prefix = "com/kotlinorm/kronos-compiler-plugin/"
        for ((name, expected) in manifest["files"].properties().asSequence().filter { it.key.startsWith(prefix) }) {
            val source = candidate.resolve(name).normalize()
            require(source.startsWith(candidate) && digest(source) == expected.asText()) { "Compiler candidate digest mismatch: $name" }
            val target = bootstrapRepository.resolve(name)
            if (!target.isRegularFile() || digest(target) != expected.asText()) {
                target.parent.createDirectories()
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES)
            }
        }
        val directory = bootstrapRepository.resolve(prefix + version.getValue("version"))
        require(directory.resolve("kronos-compiler-plugin-${version.getValue("version")}.jar").isRegularFile()) { "Compiler candidate missing" }
        // Artifact restore assigns new mtimes; refresh the private local snapshot descriptor.
        refreshLocalSnapshotDirectory(directory, "com.kotlinorm", "kronos-compiler-plugin", version.getValue("version"))
        return
    }
    script(
        "ide",
        listOf(
            "bootstrap",
            "--repository",
            bootstrapRepository.toString(),
            "--library-version",
            version.getValue("version"),
            "--kotlin-version",
            version.getValue("kotlin"),
            "--ide-import-repository",
            System.getenv("KRONOS_IDE_IMPORT_REPOSITORY")
                ?: Path.of(System.getProperty("user.home"), ".m2/repository").toString(),
        ),
    )
}

class Options(arguments: Array<String>, val flags: Set<String> = emptySet()) {
    val positional = mutableListOf<String>()
    val values = linkedMapOf<String, MutableList<String>>()

    init {
        var i = 0
        while (i < arguments.size) {
            val item = arguments[i++]
            if (!item.startsWith("--")) positional += item
            else {
                val key = item.substringBefore('=')
                val value =
                    if (key in flags) "true"
                    else if ('=' in item) item.substringAfter('=')
                    else {
                        require(i < arguments.size) { "Missing value for $key" }
                        arguments[i++]
                    }
                values.getOrPut(key) { mutableListOf() } += value
            }
        }
    }

    fun one(name: String, fallback: String? = null): String? {
        val entries = values[name] ?: return fallback
        require(entries.size == 1) { "Repeated option: $name" }
        return entries.single()
    }

    fun all(name: String): List<String> = values[name].orEmpty()

    fun has(name: String) = name in values

    fun validate(names: Set<String>) {
        require(values.keys.all { it in names + flags }) {
            "Unknown option: ${values.keys - names - flags}"
        }
    }
}

fun reportFailure(failure: Throwable): Nothing {
    if (System.getenv("GITHUB_ACTIONS") == "true") {
        val message =
            failure
                .stackTraceToString()
                .replace("%", "%25")
                .replace("\r", "%0D")
                .replace("\n", "%0A")
        println("::error title=Kronos script failed::$message")
    }
    System.err.println(failure.message)
    kotlin.system.exitProcess(1)
}

fun cli(action: () -> Unit) {
    try {
        action()
    } catch (failure: IOException) {
        reportFailure(failure)
    } catch (failure: IllegalArgumentException) {
        reportFailure(failure)
    } catch (failure: IllegalStateException) {
        reportFailure(failure)
    } catch (failure: NoSuchElementException) {
        reportFailure(failure)
    } catch (failure: UnsupportedOperationException) {
        reportFailure(failure)
    } catch (failure: SecurityException) {
        reportFailure(failure)
    } catch (failure: java.net.URISyntaxException) {
        reportFailure(failure)
    } catch (failure: SAXException) {
        reportFailure(failure)
    } catch (failure: ParserConfigurationException) {
        reportFailure(failure)
    } catch (failure: TransformerException) {
        reportFailure(failure)
    } catch (failure: InterruptedException) {
        Thread.currentThread().interrupt()
        reportFailure(failure)
    }
}
