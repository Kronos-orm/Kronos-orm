#!/usr/bin/env kotlin
@file:Import("common.main.kts", "coverage-support.main.kts")

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.jar.JarOutputStream
import java.util.zip.ZipFile
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import org.w3c.dom.Document
import org.w3c.dom.Element
import kotlin.io.path.createDirectories
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.readLines
import kotlin.io.path.readText
import kotlin.io.path.writeText

val ideBuild = repositoryRoot.resolve("build/ide")
val ideGroup = "com.kotlinorm"
val ideArtifact = "kronos-compiler-plugin"
val ideSuite = "com.kotlinorm.ide.KefsProjectionFixtureTest"
val ideChecksumAlgorithms = listOf("MD5", "SHA-1", "SHA-256", "SHA-512")
val ideRequiredTests = setOf(
    "testCollectionLiteralsAcrossProjectionGroupingAndSorting", "testProjectionTypesAndBracketSyntaxAreRecognized",
    "testJoinProjectionEmptySelectorCompletion", "testDerivedWindowAliasTypes", "testNullableAndListProjectionTypes",
    "testProjectionAliasCompletionAndCollectionSyntax", "testScalarSubqueryRequiresLimitDiagnostic",
    "testSharedProjectSettingsRoundTrip", "testJoinProjectionTypes", "testFirDiagnosticIsReportedWithoutKronosEditorExtensions",
    "testIndependentProjectionAliasesDoNotLeak", "testWindowAliasEmptySelectorCompletion",
    "testContextAliasShadowRequiresOptInDiagnostic", "testFunctionProjectionRequiresAliasDiagnostic",
    "testScalarSubqueryAliasTypes", "testIdentityProjectionKeepsSourceType", "testReselectedAliasTypes",
    "testFilterSelectedAliasCompletion", "testSameLayerWhereCannotReadWindowAlias", "testProjectionPropertyQuickDocumentation",
    "testCompiledEntityProjectionTypesAndNavigation", "testEditedProjectionIsReanalyzed",
    "testColdProjectionHighlightingDocumentationAndNavigation", "testNavigationSourcesFollowEditsDeletionAndRecreation",
    "testTenColdProjectionsShareOneScopeInitialization",
    "testNonphysicalPreviewsKeepPhysicalNavigationStable", "testExternalModuleUpdateRebuildsNavigationSources",
    "testClosingIndependentProjectReleasesNavigationSources",
    "testSourcePropertyQuickDocumentation", "testProjectionReceiverQuickDocumentation", "testProjectionPropertyGoToDeclaration",
    "testWindowAliasPropertyGoToDeclaration", "testWindowAliasPropertyQuickDocumentation", "testGenericProjectionPropertyType",
)

fun ideDigest(file: Path, algorithm: String = "SHA-256"): String = Files.newInputStream(file).use { input ->
    val digest = MessageDigest.getInstance(algorithm)
    val buffer = ByteArray(65536)
    while (true) {
        val length = input.read(buffer)
        if (length < 0) break
        digest.update(buffer, 0, length)
    }
    digest.digest().joinToString("") { "%02x".format(it) }
}
fun ideXmlFactory() = DocumentBuilderFactory.newInstance().apply {
    isNamespaceAware = false
    setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
    setFeature("http://xml.org/sax/features/external-general-entities", false)
    setFeature("http://xml.org/sax/features/external-parameter-entities", false)
    setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "")
    setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "")
}
fun ideElement(document: Document, parent: Element, name: String, value: String? = null): Element =
    document.createElement(name).also { parent.appendChild(it); if (value != null) it.textContent = value }
fun ideSaveXml(document: Document, path: Path) {
    path.parent.createDirectories()
    TransformerFactory.newInstance().newTransformer().transform(DOMSource(document), StreamResult(path.toFile()))
}
fun ideSaveJson(path: Path, value: Any) { path.parent.createDirectories(); jsonMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), value) }
fun ideChecksums(path: Path) = ideChecksumAlgorithms.forEach {
    path.resolveSibling("${path.fileName}.${it.lowercase().replace("-", "")}").writeText(ideDigest(path, it) + "\n")
}
fun ideStage(jar: Path, repository: Path, libraryVersion: String, ideKotlinVersion: String): Map<String, Any> {
    listOf(libraryVersion, ideKotlinVersion).forEach { require(it.matches(Regex("[A-Za-z0-9_.+-]+"))) { "Invalid Maven version $it" } }
    var classCount = 0
    ZipFile(jar.toFile()).use { archive ->
        mapOf("CompilerPluginRegistrar" to "KronosCompilerPluginRegistrar", "CommandLineProcessor" to "KronosCommandLineProcessor").forEach { (service, implementation) ->
            val entry = requireNotNull(archive.getEntry("META-INF/services/org.jetbrains.kotlin.compiler.plugin.$service"))
            val actual = archive.getInputStream(entry).bufferedReader().use { it.readText().trim() }
            require(actual == "com.kotlinorm.compiler.plugin.$implementation") { "Incorrect compiler service $service: $actual" }
        }
        val classes = archive.entries().asSequence().filter { it.name.endsWith(".class") }.toList()
        require(classes.all { it.name.startsWith("com/kotlinorm/") }) { "Compiler, IDE or dependency classes must not be bundled" }
        classCount = classes.size
        val registrar = archive.getInputStream(archive.getEntry("com/kotlinorm/compiler/plugin/KronosCompilerPluginRegistrar.class")).use { it.readBytes() }
        require(String(registrar, Charsets.ISO_8859_1).contains("IrGenerationExtension")) { "A complete FIR and IR registrar is required" }
    }
    val version = "$ideKotlinVersion-$libraryVersion"
    val base = repository.resolve("${ideGroup.replace('.', '/')}/$ideArtifact")
    val output = base.resolve(version).createDirectories()
    val target = output.resolve("$ideArtifact-$version.jar")
    Files.copy(jar, target, StandardCopyOption.REPLACE_EXISTING)
    require(ideDigest(jar) == ideDigest(target)) { "Staging changed the compiler artifact" }
    val sourcePom = jar.resolveSibling(jar.fileName.toString().removeSuffix(".jar") + ".pom")
    val document = if (sourcePom.isRegularFile()) ideXmlFactory().newDocumentBuilder().parse(sourcePom.toFile())
        else ideXmlFactory().newDocumentBuilder().newDocument().apply {
            val root = createElement("project"); appendChild(root); root.setAttribute("xmlns", "http://maven.apache.org/POM/4.0.0")
            ideElement(this, root, "modelVersion", "4.0.0"); ideElement(this, root, "groupId", ideGroup); ideElement(this, root, "artifactId", ideArtifact)
        }
    val root = document.documentElement
    mapOf("version" to version, "name" to "Kronos compiler plugin IDE compatibility").forEach { (name, value) ->
        val child = root.childNodes.let { nodes -> (0 until nodes.length).map { nodes.item(it) }.filterIsInstance<Element>().firstOrNull { it.tagName == name } }
        (child ?: ideElement(document, root, name)).textContent = value
    }
    val pom = output.resolve("$ideArtifact-$version.pom"); ideSaveXml(document, pom)
    val metadata = ideXmlFactory().newDocumentBuilder().newDocument()
    val meta = metadata.createElement("metadata"); metadata.appendChild(meta)
    ideElement(metadata, meta, "groupId", ideGroup); ideElement(metadata, meta, "artifactId", ideArtifact)
    val versions = ideElement(metadata, ideElement(metadata, meta, "versioning"), "versions")
    Files.list(base).use { files -> files.filter { it.isDirectory() }.sorted().forEach { ideElement(metadata, versions, "version", it.fileName.toString()) } }
    ideSaveXml(metadata, base.resolve("maven-metadata.xml")); listOf(target, pom).forEach(::ideChecksums)
    return linkedMapOf("coordinate" to "$ideGroup:$ideArtifact:$version", "sourceJar" to jar.toAbsolutePath().toString(),
        "sha256" to ideDigest(target), "ideKotlinVersion" to ideKotlinVersion, "libraryVersion" to libraryVersion,
        "classes" to classCount, "byteIdenticalToInput" to true).also { ideSaveJson(repository.resolve("compatibility-candidate.json"), it) }
}
fun ideBootstrap(repository: Path, libraryVersion: String, kotlinVersion: String, importRepository: Path?): List<Map<String, Any>> {
    require(kotlinVersion == "2.4.21") { "No verified editor compatibility mapping for Kotlin $kotlinVersion" }
    val directory = repository.resolve("${ideGroup.replace('.', '/')}/$ideArtifact/$libraryVersion")
    val jar = directory.resolve("$ideArtifact-$libraryVersion.jar")
    val pom = directory.resolve("$ideArtifact-$libraryVersion.pom")
    ideXmlFactory().newDocumentBuilder().parse(pom.toFile())
    val report = ideStage(jar, repository, libraryVersion, "2.4.20-ij262-52").toMutableMap()
    if (importRepository != null) {
        val target = importRepository.resolve("${ideGroup.replace('.', '/')}/$ideArtifact/$libraryVersion").createDirectories()
        listOf(jar, pom).forEach { source ->
            val destination = target.resolve(source.fileName)
            if (source.toAbsolutePath().normalize() != destination.toAbsolutePath().normalize()) Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING)
            ideChecksums(destination)
        }
        if (libraryVersion.endsWith("-SNAPSHOT")) {
            val document = ideXmlFactory().newDocumentBuilder().newDocument()
            val root = document.createElement("metadata"); document.appendChild(root)
            mapOf("groupId" to ideGroup, "artifactId" to ideArtifact, "version" to libraryVersion).forEach { (key, value) -> ideElement(document, root, key, value) }
            val versioning = ideElement(document, root, "versioning")
            ideElement(document, ideElement(document, versioning, "snapshot"), "localCopy", "true")
            val updated = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneOffset.UTC).format(Instant.now())
            ideElement(document, versioning, "lastUpdated", updated)
            val entries = ideElement(document, versioning, "snapshotVersions")
            listOf("jar", "pom").forEach { extension ->
                val entry = ideElement(document, entries, "snapshotVersion")
                mapOf("extension" to extension, "value" to libraryVersion, "updated" to updated).forEach { (key, value) -> ideElement(document, entry, key, value) }
            }
            ideSaveXml(document, target.resolve("maven-metadata-local.xml"))
        }
        report["ideImportCoordinate"] = "$ideGroup:$ideArtifact:$libraryVersion"; report["ideImportRepository"] = importRepository.toAbsolutePath().toString()
    }
    return listOf(report)
}
fun ideVerify(results: Path, cliJar: Path): Map<String, Any> {
    val reports = Files.walk(results).use { paths -> paths.filter { it.name.startsWith("TEST-") && it.name.endsWith(".xml") }.sorted().toList() }
    require(reports.isNotEmpty()) { "No actual editor reports in $results" }
    val suites = reports.map { ideXmlFactory().newDocumentBuilder().parse(it.toFile()).documentElement }
    val cases = suites.flatMap { suite -> suite.getElementsByTagName("testcase").let { nodes -> (0 until nodes.length).map { nodes.item(it) as Element } } }
    val contracts = cases.filter { it.getAttribute("classname") == ideSuite }
    require(contracts.size == ideRequiredTests.size && contracts.map { it.getAttribute("name").removeSuffix("()") }.toSet() == ideRequiredTests) { "Not all ${ideRequiredTests.size} actual editor contracts executed" }
    cases.forEach { case ->
        require(listOf("skipped", "failure", "error").none { case.getElementsByTagName(it).length > 0 }) {
            "Editor contract did not pass: ${case.getAttribute("name")}: ${case.textContent.take(12000)}"
        }
    }
    suites.forEach { suite -> listOf("skipped", "disabled", "failures", "errors").forEach {
        require(suite.getAttribute(it).ifBlank { "0" }.toInt() == 0) { "Editor suite has $it" }
    } }
    val digest = ideDigest(cliJar)
    val checked = ZipFile(cliJar.toFile()).use { archive -> archive.entries().asSequence().count { entry ->
        entry.name.endsWith(".class") && (entry.name.startsWith("com/kotlinorm/compiler/fir/") || entry.name in listOf(
            "com/kotlinorm/compiler/plugin/KronosCompilerPluginRegistrar.class", "com/kotlinorm/compiler/plugin/KronosCommandLineProcessor.class"))
    } }
    require(checked > 0)
    require(suites.any { it.textContent.contains("KEFS loaded byte-identical compiler classes ($checked), artifact SHA-256=$digest") }) { "Editor did not verify current CLI class bytes" }
    return mapOf("suite" to ideSuite, "tests" to contracts.size, "checkedClasses" to checked, "cliSha256" to digest, "reports" to reports.map { it.toString() })
}

fun ideDownload(url: String, target: Path) {
    // Marketplace and SDK metadata resources have no published checksum sidecar.
    // Bind cached bytes to their HTTPS URL and recheck their recorded SHA-256 offline.
    downloadVerified(url, target, checksumUrl = null)
}

fun ideUnzip(archive: Path, target: Path) {
    target.createDirectories()
    ZipFile(archive.toFile()).use { zip -> zip.entries().asSequence().forEach { entry ->
        val destination = target.resolve(entry.name).normalize(); require(destination.startsWith(target.normalize())) { "Unsafe ZIP entry ${entry.name}" }
        if (entry.isDirectory) destination.createDirectories() else {
            destination.parent.createDirectories(); zip.getInputStream(entry).use { input -> Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING) }
        }
    } }
}
fun ideHome(requested: String?, version: String): Path {
    val candidates = listOfNotNull(requested, System.getenv("IDEA_HOME"), if (System.getProperty("os.name").startsWith("Mac")) "${System.getProperty("user.home")}/Applications/IntelliJ IDEA.app/Contents" else null)
    for (candidate in candidates) {
        val path = Path.of(candidate).toAbsolutePath().normalize()
        if (path.resolve("Resources/product-info.json").isRegularFile() || path.resolve("product-info.json").isRegularFile()) return path
        if (requested != null) error("Not an IDEA SDK: $path")
    }
    val cache = Path.of(System.getenv("XDG_CACHE_HOME") ?: "${System.getProperty("user.home")}/.cache").resolve("kronos/idea/$version")
    val marker = cache.resolve("home.txt")
    if (marker.isRegularFile() && extractedArchiveIsValid(cache.resolve("distribution"), cache.resolve("idea.tar.gz")))
        return Path.of(marker.readText().trim())
    require(System.getProperty("os.name").startsWith("Linux")) { "Set IDEA_HOME to an installed IDEA $version SDK" }
    val releases = cache.resolve("releases.json"); ideDownload("https://data.services.jetbrains.com/products/releases?code=IIU&latest=false&type=release", releases)
    val release = jsonMapper.readTree(releases.toFile()).path("IIU").firstOrNull { it.path("version").asText() == version } ?: error("IDEA $version release was not found")
    val platform = if (System.getProperty("os.arch") in listOf("aarch64", "arm64")) "linuxARM64" else "linux"
    val download = release.path("downloads").path(platform)
    val archive = downloadVerified(download.path("link").asText(), cache.resolve("idea.tar.gz"), download.path("checksumLink").asText())
    val extracted = cache.resolve("distribution")
    extractVerifiedArchive(archive, extracted) { run(listOf("tar", "-xzf", archive.toString(), "-C", extracted.toString())) }
    val home = Files.walk(extracted, 3).use { paths -> paths.filter { it.fileName.toString() == "product-info.json" }.findFirst().orElseThrow().parent }
    marker.writeText(home.toString()); return home
}
val idePluginIds = listOf("com.intellij.java", "org.jetbrains.kotlin", "Coverage", "intellij.java.aetherDependencyResolver.plugin",
    "intellij.todo.plugin", "intellij.testRunner.plugin", "intellij.libraries.misc.plugin", "intellij.structureView.plugin",
    "intellij.structuralSearch.plugin", "intellij.execution.serviceView.plugin", "intellij.navbar.plugin")
fun ideMavenJar(repository: Path, artifact: String, version: String, source: Path, dependencies: String = "") {
    val target = repository.resolve("com/kotlinorm/ide/$artifact/$version").createDirectories()
    val jar = target.resolve("$artifact-$version.jar")
    val pom = target.resolve("$artifact-$version.pom")
    val pomText = "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"><modelVersion>4.0.0</modelVersion><groupId>com.kotlinorm.ide</groupId><artifactId>$artifact</artifactId><version>$version</version>$dependencies</project>\n"
    if (jar.isRegularFile() && pom.isRegularFile() && Files.isSameFile(jar, source) && pom.readText() == pomText &&
        ideChecksumAlgorithms.all { jar.resolveSibling("${jar.fileName}.${it.lowercase().replace("-", "")}").isRegularFile() } &&
        ideDigest(source) == jar.resolveSibling("${jar.fileName}.sha256").readText().trim()) return
    if (!jar.isRegularFile() || ideDigest(jar) != ideDigest(source)) {
        Files.deleteIfExists(jar)
        try { Files.createLink(jar, source) } catch (_: Exception) { Files.copy(source, jar) }
    }
    pom.writeText(pomText)
    listOf(jar, pom).forEach(::ideChecksums)
}
fun ideFrameworkDependencies(build: String, repository: Path, version: String): List<String> {
    val coordinates = linkedSetOf<String>()
    val testLibraries = linkedMapOf<String, String>()
    val pending = java.util.ArrayDeque(listOf("com.jetbrains.intellij.platform:test-framework", "com.jetbrains.intellij.kotlin:kotlin-base-test-framework"))
    while (pending.isNotEmpty()) {
        val coordinate = pending.removeFirst()
        if (!coordinates.add(coordinate)) continue
        val (group, artifact) = coordinate.split(':')
        val pom = ideBuild.resolve("framework-metadata/$group/$artifact-$build.pom")
        ideDownload("https://www.jetbrains.com/intellij-repository/releases/${group.replace('.', '/')}/$artifact/$build/$artifact-$build.pom", pom)
        val model = xml(pom).documentElement
        model.child("dependencies")?.children("dependency").orEmpty().forEach { dependency ->
            val dependencyGroup = dependency.value("groupId")
            val dependencyArtifact = dependency.value("artifactId")
            val dependencyVersion = dependency.value("version").replace("\${project.version}", build)
            if (dependencyGroup.startsWith("com.jetbrains.intellij.") && dependencyArtifact.contains("test")) {
                require(dependencyVersion == build) { "IDE test-framework build mismatch: $dependencyGroup:$dependencyArtifact:$dependencyVersion" }
                pending.add("$dependencyGroup:$dependencyArtifact")
            } else if (dependencyGroup.startsWith("org.junit") || dependencyGroup in setOf("junit", "org.assertj", "org.hamcrest", "org.opentest4j", "org.apiguardian") || dependencyGroup == "org.jetbrains" && dependencyArtifact == "jetCheck") {
                require(dependencyVersion.isNotBlank() && '$' !in dependencyVersion) { "Unresolved IDE test dependency version" }
                testLibraries["$dependencyGroup:$dependencyArtifact"] = dependencyVersion
            }
        }
    }
    return coordinates.map { coordinate ->
        val (group, artifact) = coordinate.split(':')
        val path = "${group.replace('.', '/')}/$artifact/$build/$artifact-$build.jar"
        val jar = ideBuild.resolve("framework-metadata/$group/$artifact-$build.jar")
        downloadVerified("https://www.jetbrains.com/intellij-repository/releases/$path", jar,
            "https://www.jetbrains.com/intellij-repository/releases/$path.sha1", "sha1")
        val localArtifact = "framework-${group.replace('.', '-') }-$artifact"
        ideMavenJar(repository, localArtifact, version, jar)
        "<dependency><groupId>com.kotlinorm.ide</groupId><artifactId>$localArtifact</artifactId><version>$version</version></dependency>"
    } + testLibraries.map { (coordinate, version) ->
        val (group, artifact) = coordinate.split(':')
        "<dependency><groupId>$group</groupId><artifactId>$artifact</artifactId><version>$version</version></dependency>"
    }
}
fun idePrepare(requested: String?, cliJar: Path, coreJar: Path, syntaxJar: Path): Pair<Path, List<String>> {
    val catalog = tomlMapper.readTree(repositoryRoot.resolve("libs.versions.toml").toFile())
    val ideaVersion = catalog.path("versions").path("idea").asText(); val kefsVersion = catalog.path("versions").path("kefs").asText()
    val libraryVersion = catalog.path("versions").path("kronos").asText().ifBlank {
        catalog.path("libraries").path("kronos-compiler-plugin").path("version").asText()
    }
    val home = ideHome(requested, ideaVersion)
    val productFile = listOf(home.resolve("Resources/product-info.json"), home.resolve("product-info.json")).first { it.isRegularFile() }
    val info = jsonMapper.readTree(productFile.toFile())
    require(info.path("version").asText() == ideaVersion) { "Expected IDEA $ideaVersion, got ${info.path("version")}" }
    val build = info.path("buildNumber").asText()
    val cache = Path.of(System.getenv("XDG_CACHE_HOME") ?: "${System.getProperty("user.home")}/.cache").resolve("kronos/kefs/$kefsVersion")
    val kefsZip = cache.resolve("kefs.zip")
    ideDownload("https://plugins.jetbrains.com/maven/com/jetbrains/plugins/com.github.mr3zee.kotlinPlugins/$kefsVersion/com.github.mr3zee.kotlinPlugins-$kefsVersion.zip", kefsZip)
    val plugins = ideBuild.resolve("sandbox/plugins")
    extractVerifiedArchive(kefsZip, plugins) { ideUnzip(kefsZip, plugins) }
    val kefsDirectory = Files.list(plugins).use { dirs -> dirs.filter { it.isDirectory() && it.resolve("lib").isDirectory() }.findFirst().orElseThrow() }
    val sdkJars = linkedSetOf<Path>()
    Files.list(home.resolve("lib")).use { files -> files.filter { it.name.endsWith(".jar") }.sorted().forEach { sdkJars.add(it) } }
    for (item in info.path("layout")) {
        if (item.path("name").asText() in idePluginIds) item.path("classPath").forEach { path ->
            val jar = home.resolve(path.asText())
            if (jar.isRegularFile()) sdkJars.add(jar)
            val pluginLib = home.resolve(path.asText().substringBefore("/lib/") + "/lib")
            if (pluginLib.isDirectory()) Files.list(pluginLib).use { files -> files.filter { it.name.endsWith(".jar") }.sorted().forEach { sdkJars.add(it) } }
            val modules = pluginLib.resolve("modules")
            if (modules.isDirectory()) Files.walk(modules).use { files -> files.filter { it.name.endsWith(".jar") }.sorted().forEach { sdkJars.add(it) } }
        }
    }
    Files.walk(kefsDirectory.resolve("lib")).use { files -> files.filter { it.name.endsWith(".jar") }.sorted().forEach { sdkJars.add(it) } }
    val repository = ideBuild.resolve("repository").createDirectories()
    val dependencies = sdkJars.mapIndexed { index, jar ->
        val artifact = "sdk-${index.toString().padStart(4, '0')}-${jar.name.removeSuffix(".jar").replace(Regex("[^A-Za-z0-9_.-]"), "-")}"
        ideMavenJar(repository, artifact, ideaVersion, jar)
        "<dependency><groupId>com.kotlinorm.ide</groupId><artifactId>$artifact</artifactId><version>$ideaVersion</version></dependency>"
    }.toMutableList()
    dependencies += ideFrameworkDependencies(build, repository, ideaVersion)
    val emptyJar = ideBuild.resolve("sdk-aggregate.jar")
    JarOutputStream(Files.newOutputStream(emptyJar)).use { }
    ideMavenJar(repository, "idea-sdk", ideaVersion, emptyJar, "<dependencies>${dependencies.joinToString("")}</dependencies>")
    val compat = ideBuild.resolve("compatibility-repository")
    val compilerVersion = ZipFile(home.resolve("plugins/Kotlin/lib/kotlinc.kotlin-compiler-common.jar").toFile()).use { archive ->
        archive.getInputStream(archive.getEntry("META-INF/compiler.version")).bufferedReader().use { it.readText().trim() }
    }
    val originalDirectory = ideBuild.resolve("original-repository/com/kotlinorm/kronos-compiler-plugin/$libraryVersion").createDirectories()
    val original = originalDirectory.resolve("kronos-compiler-plugin-$libraryVersion.jar"); Files.copy(cliJar, original, StandardCopyOption.REPLACE_EXISTING)
    ideStage(original, compat, libraryVersion, compilerVersion)
    val runtime = ideBuild.resolve("runtime").createDirectories()
    val core = runtime.resolve("kronos-core.jar"); Files.copy(coreJar, core, StandardCopyOption.REPLACE_EXISTING)
    val syntax = runtime.resolve("kronos-syntax.jar"); Files.copy(syntaxJar, syntax, StandardCopyOption.REPLACE_EXISTING)
    val sandbox = ideBuild.resolve("sandbox")
    listOf("config", "system", "log").forEach { sandbox.resolve(it).createDirectories() }
    val hostOs = if (System.getProperty("os.name").startsWith("Mac")) "macOS" else if (System.getProperty("os.name").startsWith("Windows")) "Windows" else "Linux"
    val arch = if (System.getProperty("os.arch") in listOf("arm64", "aarch64")) "aarch64" else "amd64"
    val launch = info.path("launch").first { it.path("os").asText() == hostOs && it.path("arch").asText() == arch }
    fun resolveHome(value: String): String = value.replace("\$APP_PACKAGE/Contents", home.toString()).replace("\$IDE_HOME", home.toString())
    val vmPath = home.resolve(launch.path("vmOptionsFilePath").asText().removePrefix("../")).normalize()
    val vmOptions = vmPath.readLines().filter { it.isNotBlank() && !it.startsWith("#") && !it.startsWith("-Xmx") }
    val arguments = (vmOptions + launch.path("additionalJvmArguments").map { resolveHome(it.asText()) } + listOf("-Xmx2g")).distinct().toMutableList()
    val properties = linkedMapOf(
        "idea.classpath.index.enabled" to "false", "idea.force.use.core.classloader" to "true", "idea.use.core.classloader.for.plugin.path" to "true",
        "intellij.console.log.level" to "warning", "intellij.testFramework.rethrow.logged.errors" to "true", "kotlin.plugin.use.k2" to "true",
        "idea.home.path" to home.toString(), "idea.config.path" to sandbox.resolve("config").toString(), "idea.system.path" to sandbox.resolve("system").toString(),
        "idea.log.path" to sandbox.resolve("log").toString(), "idea.plugins.path" to plugins.toString(), "plugin.path" to kefsDirectory.toString(),
        "kronos.test.kotlin.home" to requireNotNull(System.getProperty("kotlin.home")) { "The official Kotlin SDK home is missing" },
        "probe.library.version" to libraryVersion, "probe.settings.output" to ideBuild.resolve("shared-kotlin-plugins.xml").toString(),
        "probe.cli.jar" to original.toString(), "probe.core.jar" to core.toString(), "probe.syntax.jar" to syntax.toString(), "probe.repository" to compat.toString(),
    )
    arguments += properties.map { (key, value) -> "-D$key=$value" }
    ideSaveJson(ideBuild.resolve("sdk.json"), mapOf("home" to home.toString(), "idea" to ideaVersion, "build" to build, "kefs" to kefsVersion, "compiler" to compilerVersion,
        "sdkJars" to sdkJars.map { mapOf("path" to it.toString(), "sha256" to ideDigest(it)) }, "jvmArguments" to arguments))
    return home to arguments
}
fun ideCoverageAdapter(jdk: Path): Path {
    val kover = coverageAgent()
    val inputs = mapOf("sourceSha256" to digest(coverageAdapterSource()), "agentSha256" to digest(kover),
        "agentPath" to kover.toUri().toASCIIString(), "jdkSha256" to digest(jdk.resolve("release")),
        "builderSha256" to digest(repositoryRoot.resolve("scripts/ide.main.kts")))
    val fingerprint = MessageDigest.getInstance("SHA-256").digest(jsonMapper.writeValueAsBytes(inputs))
        .joinToString("") { "%02x".format(it) }
    val directory = ideBuild.resolve("coverage-agent/${fingerprint.take(32)}")
    val jar = directory.resolve("instrumentation-adapter.jar")
    return withArtifactCacheLock(jar) {
        val receipt = directory.resolve("manifest.json")
        val saved = if (receipt.isRegularFile()) runCatching { jsonMapper.readTree(receipt.toFile()) }.getOrNull() else null
        if (jar.isRegularFile() && saved != null && saved.path("inputs") == jsonMapper.valueToTree<com.fasterxml.jackson.databind.JsonNode>(inputs) &&
            saved.path("jarSha256").asText() == digest(jar)) return@withArtifactCacheLock jar
        val classes = directory.resolve("classes")
        deleteTree(classes)
        classes.createDirectories()
        fun executable(name: String) = jdk.resolve("bin/${if (windows) "$name.exe" else name}").toString()
        run(listOf(executable("javac"), "--release", "17", "-classpath", kover.toString(), "-d", classes.toString(), coverageAdapterSource().toString()))
        val manifest = directory.resolve("agent.mf")
        val model = java.util.jar.Manifest().apply {
            mainAttributes[java.util.jar.Attributes.Name.MANIFEST_VERSION] = "1.0"
            mainAttributes.putValue("Premain-Class", "com.kotlinorm.ide.coverage.KoverResourceAgent")
            mainAttributes.putValue("Can-Retransform-Classes", "true")
            mainAttributes.putValue("Class-Path", kover.toUri().toASCIIString())
        }
        Files.newOutputStream(manifest).use { model.write(it) }
        run(listOf(executable("jar"), "--create", "--file", jar.toString(), "--manifest", manifest.toString(), "-C", classes.toString(), "."))
        receipt.json(mapOf("inputs" to inputs, "jarSha256" to digest(jar)))
        jar
    }
}

fun ideRunEditorTests(arguments: List<String>, coverage: Path? = null) {
    val toolchain = ideBuild.resolve("toolchain")
    runToolchain(listOf("task", ":ide:compileJvmTest", "--build-dir", toolchain.toString()), repository = ideBuild.resolve("repository"))
    val stateFile = Files.list(toolchain.resolve("incremental.state")).use { files ->
        files.filter { it.name.startsWith("_ide_compileJvmTest-") }.toList().single()
    }
    val state = jsonMapper.readTree(stateFile.toFile())
    require(state.path("codeVersion").asText() == "0.13.0" && state.path("inputFiles").isArray && state.path("outputFiles").isArray &&
        state.path("inputValues").path("jdk.home").asText().isNotBlank()) { "Unexpected Kotlin Toolchain IDE compilation record: $stateFile" }
    val jdk = Path.of(state.path("inputValues").path("jdk.home").asText())
    val compiled = state.path("outputFiles").map { Path.of(it.asText()) }
    require(compiled.any { it.resolve("com/kotlinorm/ide/KefsProjectionFixtureTest.class").isRegularFile() }) { "IDE test compilation output is missing" }
    val sdkState = jsonMapper.readTree(ideBuild.resolve("sdk.json").toFile())
    val sdk = sdkState.path("sdkJars").map { Path.of(it.path("path").asText()) }
    val frameworks = Files.walk(ideBuild.resolve("framework-metadata")).use { files ->
        files.filter { it.name.endsWith("-${sdkState.path("build").asText()}.jar") }.sorted().toList()
    }
    val dependencies = state.path("inputFiles").map { Path.of(it.asText()) }.filter { path ->
        path.name.endsWith(".jar") && !path.startsWith(ideBuild.resolve("repository")) &&
            listOf("/org/junit/", "/org/opentest4j/", "/org/apiguardian/").none { path.toString().replace('\\', '/').contains(it) }
    }
    val library = tomlMapper.readTree(repositoryRoot.resolve("libs.versions.toml").toFile()).path("libraries").path("junit-platform-console-standalone")
    val (group, artifact) = library.path("module").asText().split(':')
    val version = library.path("version").asText()
    require(version.isNotBlank()) { "JUnit console version is missing from catalog" }
    val console = ideBuild.resolve("runner/$artifact-$version.jar")
    val url = "https://repo.maven.apache.org/maven2/${group.replace('.', '/')}/$artifact/$version/${console.name}"
    downloadVerified(url, console)
    // IDEA's PathClassLoader does not expand agent manifest Class-Path entries.
    // Keep the exact catalogued Kover runtime ahead of bundled SDK dependencies.
    val coverageRuntime = if (coverage != null) listOf(coverageAgent()) else emptyList()
    val classpath = (coverageRuntime + listOf(console) + compiled + dependencies + sdk + frameworks).distinct()
    require(classpath.all { Files.exists(it) }) { "IDE runtime classpath contains a missing artifact" }
    val reports = toolchain.resolve("reports/ide/jvm").createDirectories()
    Files.list(reports).use { files -> files.filter { it.name.startsWith("TEST-") && it.name.endsWith(".xml") }.forEach(Files::delete) }
    val coverageArguments = coverage?.let { output ->
        output.createDirectories()
        Files.deleteIfExists(output.resolve("compiler.ic"))
        Files.deleteIfExists(output.resolve("manifest.json"))
        Files.deleteIfExists(output.resolve("verification.json"))
        Files.deleteIfExists(output.resolve("coverage-error.log"))
        val adapter = output.resolve("instrumentation-adapter.jar")
        Files.copy(ideCoverageAdapter(jdk), adapter, StandardCopyOption.REPLACE_EXISTING)
        val config = output.resolve("agent.args")
        config.writeText("report.file=${output.resolve("compiler.ic")}\nreport.append=false\ninclude=com.kotlinorm.compiler.*\n")
        listOf("-javaagent:$adapter=file:$config")
    }.orEmpty()
    val command = arguments + coverageArguments + listOf("-Djunit.platform.output.capture.stdout=true", "-Djunit.platform.output.capture.stderr=true",
        "-classpath", classpath.joinToString(java.io.File.pathSeparator), "org.junit.platform.console.ConsoleLauncher", "execute",
        "--select-class=com.kotlinorm.ide.KefsProjectionFixtureTest", "--include-engine=junit-vintage", "--fail-if-no-tests", "--reports-dir=$reports")
    val argfile = ideBuild.resolve("editor-test.args"); argfile.writeText(command.joinToString("\n", transform = ::quoteJvmArgument))
    val result = run(listOf(jdk.resolve("bin/${if (windows) "java.exe" else "java"}").toString(), "@$argfile"), check = false)
    if (result.exitCode != 0) {
        val failures = tree(reports).filter { it.name.startsWith("TEST-") && it.name.endsWith(".xml") }
            .flatMap { report ->
                val cases = ideXmlFactory().newDocumentBuilder().parse(report.toFile()).getElementsByTagName("testcase")
                (0 until cases.length).map { cases.item(it) as Element }.mapNotNull { case ->
                    val failure = listOf("failure", "error").firstNotNullOfOrNull {
                        case.getElementsByTagName(it).item(0)
                    }
                    failure?.let { "${case.getAttribute("classname")}.${case.getAttribute("name")}: ${it.textContent.take(12000)}" }
                }
            }
        if (System.getenv("GITHUB_ACTIONS") == "true") failures.forEach { failure ->
            val escaped = failure.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")
            println("::error title=IDE editor contract failed::$escaped")
        }
        error("IDE JUnit exited ${result.exitCode}:\n${failures.joinToString("\n")}")
    }
    coverage?.resolve("coverage-error.log")?.takeIf { it.isRegularFile() }?.let { log ->
        val failures = log.readLines().filter { Regex("Error during class (frame computation|instrumentation): com[/\\.]kotlinorm[/\\.]compiler[/\\.]").containsMatchIn(it) }
        require(failures.isEmpty()) { "Compiler coverage instrumentation failed: ${failures.joinToString("; ")}; see $log" }
    }
}
fun ideMain(arguments: List<String>): Any {
    val options = Options(arguments.toTypedArray(), flags = setOf("--skip-build", "--help"))
    val usage = "Usage: scripts/run ide stage|bootstrap|verify|prepare|test [options]"
    options.validate(setOf("--jar", "--repository", "--library-version", "--ide-kotlin-version", "--kotlin-version", "--ide-import-repository", "--results", "--cli-jar", "--core-jar", "--syntax-jar", "--ide-path", "--coverage-dir"))
    if (options.has("--help")) return usage
    val command = options.positional.singleOrNull() ?: error(usage)
    fun required(key: String) = requireNotNull(options.one(key)) { "$key is required" }
    fun path(key: String) = Path.of(required(key)).toAbsolutePath().normalize()
    return when (command) {
        "stage" -> ideStage(path("--jar"), path("--repository"), required("--library-version"), required("--ide-kotlin-version"))
        "bootstrap" -> ideBootstrap(path("--repository"), required("--library-version"), required("--kotlin-version"), options.one("--ide-import-repository")?.let { Path.of(it).toAbsolutePath() })
        "verify" -> ideVerify(path("--results"), path("--cli-jar"))
        "prepare", "test" -> {
            val artifacts = listOf("cli" to "compiler-plugin", "core" to "core", "syntax" to "syntax").associate { (name, module) ->
                name to (options.one("--$name-jar")?.let { Path.of(it) } ?: repositoryRoot.resolve("build/tasks/_kronos-${module}_jarJvm/kronos-$module-jvm.jar")).toAbsolutePath().normalize()
            }
            if (!options.has("--skip-build")) script("build", listOf("task", ":kronos-core:jarJvm", ":kronos-syntax:jarJvm"))
            artifacts.values.forEach { require(it.isRegularFile()) { "Missing artifact $it" } }
            val (_, jvmArguments) = idePrepare(options.one("--ide-path"), artifacts.getValue("cli"), artifacts.getValue("core"), artifacts.getValue("syntax"))
            if (command == "prepare") return jsonMapper.readTree(ideBuild.resolve("sdk.json").toFile())
            val coverage = options.one("--coverage-dir")?.let { Path.of(it).toAbsolutePath().normalize() }
            ideRunEditorTests(jvmArguments, coverage)
            val result = ideVerify(ideBuild.resolve("toolchain/reports/ide/jvm"), artifacts.getValue("cli"))
            ideSaveJson(ideBuild.resolve("verification.json"), result)
            coverage?.let { output ->
                Files.copy(artifacts.getValue("cli"), output.resolve("compiler.jar"), StandardCopyOption.REPLACE_EXISTING)
                ideSaveJson(output.resolve("verification.json"), result)
                ideSaveJson(output.resolve("manifest.json"), coverageProvenance("ide", output.resolve("compiler.jar"),
                    output.resolve("compiler.ic"), result))
            }
            recordConsumerArtifacts(listOf(ideBuild.resolve("runtime/kronos-core.jar"), ideBuild.resolve("runtime/kronos-syntax.jar"),
                Path.of(jsonMapper.readTree(ideBuild.resolve("compatibility-repository/compatibility-candidate.json").toFile())["sourceJar"].asText())), "kefs-editor")
            result
        }
        else -> error("Unknown IDE command: $command")
    }
}
cli { println(jsonMapper.writeValueAsString(ideMain(args.toList()))) }
