@file:Import("../../scripts/common.main.kts")

import java.net.URI
import java.nio.file.Path
import java.util.zip.ZipFile
import kotlin.io.path.fileSize
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile

val nativeTargets = listOf("iosArm64", "iosSimulatorArm64", "iosX64", "macosArm64", "macosX64",
    "tvosArm64", "tvosSimulatorArm64", "tvosX64", "watchosArm64", "watchosDeviceArm64",
    "watchosSimulatorArm64", "watchosX64", "linuxX64", "linuxArm64", "mingwX64")
val runtimeModules = listOf("kronos-core", "kronos-syntax", "kronos-logging")
val publicationGroup = "com.kotlinorm"
val pluginMarker = "com.kotlinorm.kronos-gradle-plugin"
val mavenNamespace = "http://maven.apache.org/POM/4.0.0"

fun safeFile(repository: Path, parent: Path, url: String): Path {
    val uri = URI(url)
    require(uri.scheme == null && uri.rawAuthority == null && uri.rawQuery == null && uri.rawFragment == null) {
        "Nonlocal publication URL: $url"
    }
    val relative = uri.path.orEmpty()
    require(relative.isNotEmpty() && !relative.startsWith('/') && relative.none { it in "\\\u0000\r\n" }) {
        "Invalid publication URL: $url"
    }
    val result = parent.resolve(relative).toRealPath()
    require(result.startsWith(repository.toRealPath())) { "Publication URL escapes private M2: $url" }
    require(result.isRegularFile() && result.fileSize() > 0) { "Missing or empty publication file: $result" }
    return result
}

fun forbiddenDependency(group: String, artifact: String): Boolean =
    artifact == "kotlin-reflect" ||
        (group == "org.jetbrains.kotlinx" && Regex("kotlinx-serialization-(core|json)(-|$)").containsMatchIn(artifact)) ||
        group.startsWith("androidx.sqlite") || "sqlite" in artifact.lowercase() ||
        artifact in listOf("kronos-ios-wrapper", "kronos-kmp-wrapper") ||
        (group == "app.cash.sqldelight" && artifact.endsWith("driver"))

fun verifyPom(path: Path, runtime: Boolean = false): List<Triple<String, String, String>> {
    val model = xml(path).documentElement
    require(model.localName == "project" && model.namespaceURI == mavenNamespace) { "Invalid Maven POM namespace: $path" }
    val dependencies = model.getElementsByTagNameNS(mavenNamespace, "dependency")
    return (0 until dependencies.length).mapNotNull { index ->
        val node = dependencies.item(index) as org.w3c.dom.Element
        if (node.value("scope", "compile") == "test") null else {
            val group = node.value("groupId")
            val artifact = node.value("artifactId")
            require(!runtime || !forbiddenDependency(group, artifact)) { "Runtime dependency leaked into $path: $group:$artifact" }
            Triple(group, artifact, node.value("version"))
        }
    }
}

fun coordinate(repository: Path, artifact: String, version: String, group: String = publicationGroup): Path =
    repository.resolve(group.replace('.', '/')).resolve(artifact).resolve(version)

fun verifyModule(repository: Path, path: Path, runtime: Boolean = false,
                 seen: MutableSet<Path> = mutableSetOf()): Set<Triple<String, String, String>> {
    if (!seen.add(path.toRealPath())) return emptySet()
    val links = mutableSetOf<Triple<String, String, String>>()
    var files = 0
    val model = jsonMapper.readTree(path.toFile())
    for (variant in model.path("variants")) {
        if (runtime) for (dependency in variant.path("dependencies")) {
            require(!forbiddenDependency(dependency.path("group").asText(), dependency.path("module").asText())) {
                "Runtime dependency leaked into $path: $dependency"
            }
        }
        for (entry in variant.path("files")) {
            val artifact = safeFile(repository, path.parent, entry.path("url").asText())
            require(!entry.has("size") || entry["size"].asLong() == artifact.fileSize()) { "Wrong artifact size: $artifact" }
            for (algorithm in checksumAlgorithms) if (entry.has(algorithm)) {
                require(digest(artifact, algorithm) == entry[algorithm].asText()) { "Wrong $algorithm: $artifact" }
            }
            files++
        }
        if (variant.has("available-at")) {
            val redirect = variant["available-at"]
            val group = redirect.path("group").asText()
            val artifact = redirect.path("module").asText()
            val version = redirect.path("version").asText()
            val target = safeFile(repository, path.parent, redirect.path("url").asText())
            val expected = coordinate(repository, artifact, version, group).resolve("$artifact-$version.module").toRealPath()
            require(target == expected) { "Wrong available-at coordinate path in $path: $redirect" }
            val component = jsonMapper.readTree(target.toFile()).path("component")
            if (component.has("url")) {
                val owner = safeFile(repository, target.parent, component["url"].asText())
                val ownerComponent = jsonMapper.readTree(owner.toFile()).path("component")
                require(listOf("group", "module", "version").all { component[it] == ownerComponent[it] }) {
                    "Wrong KMP component owner: $target"
                }
                require(listOf("group", "version").all { component[it] == redirect[it] }) { "Wrong available-at owner: $target" }
            } else require(listOf("group", "module", "version").all { component[it] == redirect[it] }) {
                "Wrong available-at coordinate in $path: $redirect"
            }
            links += Triple(group, artifact, version)
            verifyModule(repository, target, runtime, seen)
        }
    }
    require(files > 0 || links.isNotEmpty()) { "No artifacts or redirects in $path" }
    return links
}

fun verifyComponent(path: Path, artifact: String, version: String) {
    val component = jsonMapper.readTree(path.toFile()).path("component")
    require(component.path("group").asText() == publicationGroup && component.path("module").asText() == artifact &&
        component.path("version").asText() == version) { "Wrong publication component: $path" }
}

fun verifyRepository(repository: Path, version: String): Map<String, Any> {
    require(repository.isDirectory()) { "Private M2 missing: $repository" }
    for (path in tree(repository)) require(!Regex("kronos-(sqlite|ios|kmp)-wrapper").containsMatchIn(path.toString())) {
        "Unexpected wrapper publication: $path"
    }
    for (module in runtimeModules) {
        val root = coordinate(repository, module, version)
        val metadata = safeFile(repository, root, "$module-$version.module")
        verifyComponent(metadata, module, version)
        verifyPom(safeFile(repository, root, "$module-$version.pom"), runtime = true)
        val metadataJar = safeFile(repository, root, "$module-$version.jar")
        ZipFile(metadataJar.toFile()).use { zip ->
            val entry = requireNotNull(zip.getEntry("META-INF/kotlin-project-structure-metadata.json"))
            val structure = jsonMapper.readTree(zip.getInputStream(entry))["projectStructure"]
            val sourceSets = structure["sourceSets"].map { it["name"].asText() }.toSet()
            require(sourceSets == setOf("commonMain", "nativeMain", "appleMain", "iosMain", "tvosMain", "watchosMain", "linuxMain", "macosMain")) { "Incomplete shared source-set hierarchy: $module" }
            val variants = structure["variants"].map { it["name"].asText() }.toSet()
            require((nativeTargets + "jvm").all { "${it}ApiElements" in variants }) { "Incomplete project-structure targets: $module" }
            if (module == "kronos-core") require(zip.getEntry("META-INF/kronos/native-metadata-v1.tsv") != null) { "Native provider manifest missing" }
        }
        val tooling = jsonMapper.readTree(safeFile(repository, root, "$module-$version-kotlin-tooling-metadata.json").toFile())
        val toolingNative = tooling["projectTargets"].mapNotNull { it.path("extras").path("native").path("konanTarget").takeIf { node -> node.isTextual }?.asText() }.toSet()
        require(toolingNative == nativeTargets.map { target -> Regex("([a-z])([A-Z])").replace(target) { "${it.groupValues[1]}_${it.groupValues[2]}" }.lowercase() }.toSet()) { "Tooling metadata targets differ: $module" }
        val redirects = verifyModule(repository, metadata, runtime = true)
        for (suffix in listOf("jvm") + nativeTargets.map { it.lowercase() }) {
            val artifact = "$module-$suffix"
            val location = coordinate(repository, artifact, version)
            require(Triple(publicationGroup, artifact, version) in redirects) { "Missing root available-at for $artifact" }
            verifyPom(safeFile(repository, location, "$artifact-$version.pom"), runtime = true)
            val targetModule = safeFile(repository, location, "$artifact-$version.module")
            verifyModule(repository, targetModule, runtime = true)
            val variants = jsonMapper.readTree(targetModule.toFile())["variants"].toList()
            val platform = if (suffix == "jvm") "jvm" else "native"
            val targetName = Regex("([a-z])([A-Z])").replace(nativeTargets.firstOrNull { it.lowercase() == suffix } ?: "jvm") {
                "${it.groupValues[1]}_${it.groupValues[2]}"
            }.lowercase()
            val platformVariants = variants.filter { it.path("attributes").path("org.jetbrains.kotlin.platform.type").asText() == platform }
            require(platformVariants.isNotEmpty() && (platform == "jvm" || platformVariants.all {
                it.path("attributes").path("org.jetbrains.kotlin.native.target").asText() == targetName
            })) { "Wrong target platform attributes: $targetModule" }
            val extension = if (suffix == "jvm") "jar" else "klib"
            val mainFile = safeFile(repository, location, "$artifact-$version.$extension")
            if (platform == "native") ZipFile(mainFile.toFile()).use { archive ->
                val manifest = java.util.Properties()
                archive.getInputStream(requireNotNull(archive.getEntry("default/manifest")) {
                    "Native KLIB has no manifest: $mainFile"
                }).use { manifest.load(it) }
                require(manifest.getProperty("native_targets", "").split(' ').toSet() == setOf(targetName)) {
                    "Native payload architecture differs from its publication target: $mainFile"
                }
            }
            require(platformVariants.any { variant -> variant.path("files").any {
                safeFile(repository, targetModule.parent, it["url"].asText()) == mainFile
            } }) { "Main target artifact is not exposed by its module metadata: $mainFile" }
        }
    }
    val core = coordinate(repository, "kronos-core-jvm", version).resolve("kronos-core-jvm-$version.jar")
    ZipFile(core.toFile()).use { archive ->
        for (entry in listOf("com/kotlinorm/orm/select/SelectClause.class", "com/kotlinorm/wrappers/KronosExecutionWrapper.class")) {
            require(archive.getEntry(entry) != null) { "Full core API missing: $entry" }
        }
    }
    for (artifact in listOf("kronos-compiler-plugin", "kronos-gradle-plugin")) {
        val location = coordinate(repository, artifact, version)
        verifyPom(safeFile(repository, location, "$artifact-$version.pom"))
        val metadata = safeFile(repository, location, "$artifact-$version.module")
        verifyComponent(metadata, artifact, version)
        verifyModule(repository, metadata)
        ZipFile(safeFile(repository, location, "$artifact-$version.jar").toFile()).use { archive ->
            if (artifact == "kronos-gradle-plugin") {
                val properties = archive.getInputStream(requireNotNull(archive.getEntry("META-INF/kronos/compiler-plugin.properties")))
                    .bufferedReader().use { it.readLines() }
                require("version=$version" in properties) { "Published Gradle plugin references another compiler version" }
            } else require(archive.getEntry("com/kotlinorm/compiler/plugin/KronosCompilerPluginRegistrar.class") != null) {
                "Compiler registrar missing"
            }
        }
    }
    val artifact = "$pluginMarker.gradle.plugin"
    val marker = coordinate(repository, artifact, version, pluginMarker)
    require(Triple(publicationGroup, "kronos-gradle-plugin", version) in verifyPom(safeFile(repository, marker, "$artifact-$version.pom"))) {
        "Plugin marker does not resolve the published implementation"
    }
    return mapOf("version" to version, "nativeTargets" to nativeTargets, "modules" to runtimeModules)
}

cli {
    if (args.any { it in setOf("--help", "-h") }) {
        println("Usage: kmp-publication-gate.main.kts verify --repository PATH --version VERSION [--report PATH]")
    } else {
        val options = Options(args)
        options.validate(setOf("--repository", "--version", "--report"))
        require(options.positional == listOf("verify")) { "Expected verify command" }
        val repository = Path.of(requireNotNull(options.one("--repository"))).toAbsolutePath()
        val report = verifyRepository(repository, requireNotNull(options.one("--version")))
        options.one("--report")?.let { Path.of(it).json(report) }
        println("KMP publications verified: ${runtimeModules.size} modules, ${nativeTargets.size} Native targets")
    }
}
