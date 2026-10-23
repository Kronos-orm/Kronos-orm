@file:Import("common.main.kts")

import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.readBytes

val releaseModules =
    listOf(
        "kronos-syntax",
        "kronos-core",
        "kronos-logging",
        "kronos-compiler-plugin",
        "kronos-codegen",
        "kronos-gradle-plugin",
        "kronos-jdbc-wrapper",
        "kronos-maven-plugin",
    )
val finalRepository =
    repositoryRoot.resolve(
        "build/tasks/_kronos-logging_finalizeNativePublication@kronos-native/repository"
    )

fun gradleMarker(repository: Path, version: String): Path {
    val plugin = "com.kotlinorm.kronos-gradle-plugin"
    val artifact = "$plugin.gradle.plugin"
    val implementation =
        repository.resolve(
            "com/kotlinorm/kronos-gradle-plugin/$version/kronos-gradle-plugin-$version.pom"
        )
    val model = xml(implementation)
    val project = model.documentElement
    for (name in listOf("description", "url", "licenses", "developers", "scm")) require(
        project.child(name)?.textContent?.isNotBlank() == true
    ) {
        "Missing Maven Central metadata $name in $implementation"
    }
    for ((name, value) in
        mapOf(
            "groupId" to plugin,
            "artifactId" to artifact,
            "version" to version,
            "packaging" to "pom",
            "name" to "Kronos Gradle plugin marker",
        )) (project.child(name) ?: project.append(name)).textContent = value
    project.children("dependencies").forEach { project.removeChild(it) }
    val dependency = project.append("dependencies").append("dependency")
    for ((name, value) in
        mapOf(
            "groupId" to "com.kotlinorm",
            "artifactId" to "kronos-gradle-plugin",
            "version" to version,
        )) dependency.append(name, value)
    return repository
        .resolve("${plugin.replace('.', '/')}/$artifact/$version/$artifact-$version.pom")
        .also { saveXml(model, it) }
}

fun checksums(repository: Path) {
    tree(repository)
        .filter { it.isRegularFile() && it.extension !in checksumAlgorithms }
        .forEach { path ->
            checksumAlgorithms.forEach { algorithm ->
                path.resolveSibling("${path.fileName}.$algorithm").write(digest(path, algorithm))
            }
        }
}

fun publicationFiles(repository: Path, version: String): List<Path> =
    tree(repository).filter { path ->
        if (!path.isRegularFile()) false
        else {
            var name = path.fileName.toString()
            while (name.substringAfterLast('.', "") in checksumAlgorithms + "asc") name =
                name.substringBeforeLast('.')
            val relative = repository.relativize(path)
            name.substringAfterLast('.', "") in setOf("pom", "jar", "klib", "module", "json") &&
                (path.parent.name == version || path.parent.name.endsWith("-$version")) &&
                relative.nameCount >= 3 &&
                relative.getName(0).toString() == "com" &&
                relative.getName(1).toString() == "kotlinorm"
        }
    }

fun addDocumentation(repository: Path, version: String) {
    for (pom in publicationFiles(repository, version).filter { it.extension == "pom" }) {
        if (xml(pom).documentElement.value("packaging", "jar") == "pom") continue
        val base = pom.name.removeSuffix(".pom")
        val source = pom.resolveSibling("$base-sources.jar")
        if (!source.isRegularFile()) {
            val origin =
                repository.resolve(
                    "com/kotlinorm/kronos-compiler-plugin/$version/kronos-compiler-plugin-$version-sources.jar"
                )
            require(pom.parent.parent.name == "kronos-compiler-plugin" && origin.isRegularFile()) {
                "Sources publication missing: $source"
            }
            Files.copy(origin, source)
        }
        val target = pom.resolveSibling("$base-javadoc.jar")
        if (target.isRegularFile()) {
            val stub =
                ZipFile(target.toFile()).use { archive ->
                    val files =
                        archive
                            .entries()
                            .asSequence()
                            .filterNot { it.isDirectory }
                            .map { it.name }
                            .toSet()
                    files.all { it in setOf("META-INF/MANIFEST.MF", "README.md") } &&
                        "README.md" in files &&
                        archive
                            .getInputStream(archive.getEntry("README.md"))
                            .reader()
                            .readText()
                            .trim() ==
                            "# Empty Javadoc JAR\n\nJavadocs are not available for this module at the moment."
                }
            if (!stub) continue
        }
        ZipOutputStream(Files.newOutputStream(target)).use { archive ->
            archive.putNextEntry(ZipEntry("index.html"))
            archive.write(
                "<!doctype html><meta charset=\"utf-8\"><title>Kronos API</title><p><a href=\"https://www.kotlinorm.com/api/\">Kronos API documentation</a></p>"
                    .toByteArray()
            )
            archive.closeEntry()
            archive.putNextEntry(ZipEntry("META-INF/LICENSE"))
            archive.write(repositoryRoot.resolve("LICENSE").readBytes())
            archive.closeEntry()
        }
    }
}

fun refreshSnapshotMetadata(repository: Path, version: String) {
    if (!version.endsWith("-SNAPSHOT")) return
    for (pom in publicationFiles(repository, version).filter { it.extension == "pom" }) {
        val directory = pom.parent
        val artifact = directory.parent.name
        val coordinateVersion = directory.name
        val prefix = "$artifact-$coordinateVersion"
        val payloads =
            directory
                .listDirectoryEntries()
                .filter {
                    it.isRegularFile() &&
                        it.extension in setOf("pom", "module", "jar", "klib", "json") &&
                        it.nameWithoutExtension.startsWith(prefix)
                }
                .sorted()
        val updated =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                .withZone(ZoneOffset.UTC)
                .format(
                    Instant.ofEpochMilli(
                        maxOf(
                            System.currentTimeMillis(),
                            payloads.maxOf { Files.getLastModifiedTime(it).toMillis() },
                        ) + 1000
                    )
                )
        val model = document("metadata")
        val metadata = model.documentElement
        metadata.setAttribute("modelVersion", "1.1.0")
        val group = repository.relativize(directory.parent.parent).joinToString(".")
        metadata.append("groupId", group)
        metadata.append("artifactId", artifact)
        metadata.append("version", coordinateVersion)
        val versioning = metadata.append("versioning")
        versioning.append("snapshot").append("localCopy", "true")
        versioning.append("lastUpdated", updated)
        val entries = versioning.append("snapshotVersions")
        for (payload in payloads) {
            val entry = entries.append("snapshotVersion")
            val classifier = payload.nameWithoutExtension.removePrefix(prefix).removePrefix("-")
            if (classifier.isNotEmpty()) entry.append("classifier", classifier)
            entry.append("extension", payload.extension)
            entry.append("value", coordinateVersion)
            entry.append("updated", updated)
        }
        saveXml(model, directory.resolve("maven-metadata-local.xml"))
    }
}

fun finishStage(repository: Path, version: String) {
    gradleMarker(repository, version)
    script(
        "ide",
        listOf(
            "bootstrap",
            "--repository",
            repository.toString(),
            "--library-version",
            version,
            "--kotlin-version",
            versions().getValue("kotlin"),
        ),
    )
    addDocumentation(repository, version)
    refreshSnapshotMetadata(repository, version)
    checksums(repository)
}

fun verify(repository: Path) {
    script(
        repositoryRoot.resolve(".github/scripts/kmp-publication-gate.main.kts").toString(),
        listOf(
            "verify",
            "--repository",
            repository.toString(),
            "--version",
            versions().getValue("version"),
            "--report",
            repository.parent.resolve("publications.json").toString(),
        ),
    )
    for (path in
        publicationFiles(repository, versions().getValue("version")).filter {
            it.extension in checksumAlgorithms
        }) {
        val original = path.resolveSibling(path.nameWithoutExtension)
        require(digest(original, path.extension) == path.text().trim()) {
            "Publication checksum mismatch: $path"
        }
    }
}

fun stage(repository: Path) {
    require(macos) { "The complete Apple and fifteen-target publication must be staged on macOS" }
    require(
        repository.toAbsolutePath().normalize() != finalRepository.toAbsolutePath().normalize()
    ) {
        "Choose a separate output repository for release staging"
    }
    require(!repository.exists()) { "Stage output must be a new directory: $repository" }
    bootstrap()
    runToolchain(
        listOf("publish", "kronosBootstrap", "--non-transitive") +
            releaseModules.flatMap { listOf("-m", it) }
    )
    runToolchain(listOf("do", "finalizeNativePublication", "-m", "kronos-logging"))
    copyTree(finalRepository, repository)
    finishStage(repository, versions().getValue("version"))
    verify(repository)
}

fun jvmStage(repository: Path) {
    require(!repository.exists()) { "Stage output must be a new directory: $repository" }
    val skip =
        setOf(
            ".git",
            ".idea",
            ".agents",
            ".github",
            ".gradle",
            ".kotlin",
            "build",
            "docs",
            "node_modules",
            "kronos-docs",
            "kmp-0.4.0-tasklist",
            ".angular",
            "__pycache__",
            ".intellijPlatform",
            ".codex",
            ".cache",
            ".tmp",
            "dist",
            "out",
        )
    val temporary = Files.createTempDirectory("kronos-jvm-publication-")
    try {
        val projected = temporary.resolve("project")
        copyTree(repositoryRoot, projected, skip)
        for (module in listOf("kronos-core", "kronos-syntax", "kronos-logging")) {
            val model = projected.resolve("$module/module.yaml")
            val pattern = Regex("(?m)(^  platforms:\n)(?:    - [A-Za-z0-9]+\n)+")
            val original = model.text()
            require(pattern.findAll(original).count() == 1) {
                "Expected one KMP platform list in $module"
            }
            model.write(
                pattern
                    .replace(original) { it.groupValues[1] + "    - jvm\n" }
                    .replace("jvmResourceFragment: '@jvm'", "jvmResourceFragment: ''")
            )
        }
        script(
            "build",
            listOf("publish", "kronosBootstrap", "--non-transitive") +
                releaseModules.flatMap { listOf("-m", it) },
            root = projected,
        )
        copyTree(projected.resolve("build/repository/bootstrap"), repository)
    } finally {
        deleteTree(temporary)
    }
    finishStage(repository, versions().getValue("version"))
}

val publicationGroups = linkedMapOf(
    "ios" to listOf("iosArm64", "iosSimulatorArm64"),
    "macos" to listOf("macosArm64", "macosX64", "iosX64"),
    "tvos" to listOf("tvosArm64", "tvosSimulatorArm64", "tvosX64"),
    "watchos" to listOf("watchosArm64", "watchosDeviceArm64", "watchosSimulatorArm64", "watchosX64"),
    "linux" to listOf("linuxX64", "linuxArm64"),
    "windows" to listOf("mingwX64"),
)
val nativeConsumerTargets = publicationGroups.map { (group, targets) ->
    "sqlite-$group" to if (group == "macos") targets.filter { it.startsWith("macos") } else targets
}.toMap() + ("sqlite-macos-intel" to listOf("macosX64"))
val requiredConsumers = nativeConsumerTargets.mapValues { "native" } + mapOf(
    "server" to "jvm", "ide" to "jvm", "android-build" to "jvm", "android-api-26" to "apk", "android-api-35" to "apk")
val kmpModules = listOf("kronos-syntax", "kronos-core", "kronos-logging")
fun candidateManifest(repository: Path, group: String) {
    repository.resolve("ci-candidate.json").json(linkedMapOf(
        "sha" to sourceRevision(), "versions" to versions(), "toolchain" to digest(repositoryRoot.resolve("kotlin")), "group" to group,
        "targets" to publicationGroups[group].orEmpty(),
        "files" to tree(repository).filter { it.isRegularFile() && it.name != "ci-candidate.json" }
            .associate { repository.relativize(it).toString().replace('\\', '/') to digest(it) },
    ))
}
fun readCandidate(repository: Path, group: String? = null, payloadsOnly: Boolean = false): com.fasterxml.jackson.databind.JsonNode {
    val model = jsonMapper.readTree(repository.resolve("ci-candidate.json").toFile())
    require(model["sha"].asText() == sourceRevision()) { "Candidate belongs to a different source revision" }
    require(model["versions"] == jsonMapper.valueToTree<com.fasterxml.jackson.databind.JsonNode>(versions())) { "Candidate tool versions differ" }
    require(model["toolchain"].asText() == digest(repositoryRoot.resolve("kotlin"))) { "Candidate Toolchain wrapper mismatch" }
    if (group != null) require(model["group"].asText() == group) { "Wrong candidate group: $group" }
    val actual = tree(repository).filter { it.isRegularFile() && it.name != "ci-candidate.json" }
        .map { repository.relativize(it).toString().replace('\\', '/') }.toSet()
    require(payloadsOnly || actual == model["files"].fieldNames().asSequence().toSet()) { "Candidate inventory changed" }
    for ((name, hash) in model["files"].properties().asSequence().filter { !payloadsOnly || it.key.endsWith(".jar") || it.key.endsWith(".klib") || it.key.endsWith(".apk") }) {
        val file = repository.resolve(name).normalize()
        require(file.startsWith(repository.normalize()) && digest(file) == hash.asText()) { "Candidate digest mismatch: $name" }
    }
    return model
}
fun validateConsumedPayloads(files: Set<String>, family: String, consumer: String) {
    require(family in setOf("jvm", "native", "apk")) { "Unexpected consumer artifact family" }
    if (family == "apk") {
        require(files.containsAll(setOf("app.apk", "test.apk"))) { "Both installed application and instrumentation APKs are required" }
        return
    }
    val targets = if (family == "jvm") setOf("jvm") else
        (nativeConsumerTargets[consumer] ?: publicationGroups.values.flatten()).map { it.lowercase() }.toSet()
    fun consumedTargets(module: String): Set<String> = files.mapNotNull { path ->
        val parts = path.split('/')
        val artifact = parts.getOrNull(2).orEmpty()
        val extension = if (family == "jvm") ".jar" else ".klib"
        if (parts.take(2) != listOf("com", "kotlinorm") || !path.endsWith(extension)) null
        else targets.firstOrNull { artifact == "$module-$it" }
    }.toSet()
    require((consumedTargets("kronos-core") intersect consumedTargets("kronos-syntax")).isNotEmpty()) {
        "Consumer lacks actual core and syntax payloads for the same $family target: $consumer"
    }
}

fun consumerReceipt(repository: Path, output: Path, name: String, family: String, evidence: Path = Path.of(requireNotNull(System.getenv("KRONOS_CONSUMER_EVIDENCE_DIR")) { "Missing actual consumer resolution evidence" })) {
    require(family in listOf("jvm", "native", "apk"))
    val model = readCandidate(repository, payloadsOnly = true)
    val candidates = model["files"].properties().asSequence().filter { (path, _) ->
        (path.endsWith(".jar") || path.endsWith(".klib") || path.endsWith(".apk")) &&
            !path.endsWith("-sources.jar") && !path.endsWith("-javadoc.jar")
    }.associate { it.key to it.value.asText() }
    val logs = tree(evidence).filter { it.name.endsWith(".jsonl") }
    require(logs.isNotEmpty()) { "No actual resolution records in $evidence" }
    val observed = logs.flatMap { log -> log.text().lineSequence().filter { it.isNotBlank() }.map { line ->
        val entry = jsonMapper.readTree(line)
        mapOf("path" to entry["path"].asText(), "sha256" to entry["sha256"].asText(),
            "coordinate" to listOf("group", "artifact", "version").joinToString(":") { entry.path(it).asText() },
            "configuration" to entry["configuration"].asText())
    }.toList() }
    require(observed.isNotEmpty()) { "No consumed artifacts were observed: $name" }
    val files = linkedMapOf<String, String>()
    val resolved = observed.distinct().map { entry ->
        val path = Path.of(entry.getValue("path"))
        require(path.isRegularFile()) { "Consumed artifact is missing: $path" }
        val hash = digest(path)
        require(entry["sha256"] == null || entry["sha256"] == hash) { "Consumed artifact changed after resolution: $path" }
        val coordinate = entry.getValue("coordinate").split(':')
        val matches = candidates.filter { (name, expected) -> expected == hash &&
            (coordinate.all { it.isEmpty() } || name.startsWith("${coordinate[0].replace('.', '/')}/${coordinate[1]}/${coordinate[2]}/")) }
        require(matches.isNotEmpty()) { "Resolved Kronos artifact differs from this run's candidate: $path" }
        matches.forEach { (key, value) -> files[key] = value }
        entry + mapOf("sha256" to hash, "candidatePaths" to matches.keys.joinToString(","))
    }
    validateConsumedPayloads(files.keys, family, name)
    output.json(mapOf("sha" to sourceRevision(), "consumer" to name, "family" to family, "files" to files, "resolved" to resolved))
}
fun verifyConsumers(repository: Path, apkManifest: Path, receipts: Path) {
    val final = jsonMapper.readTree(repository.toFile())
    val apk = jsonMapper.readTree(apkManifest.toFile())
    require(final["sha"].asText() == sourceRevision() && apk["sha"] == final["sha"])
    val expected = requiredConsumers
    val seen = mutableSetOf<String>()
    for (file in tree(receipts).filter { it.extension == "json" }) {
        val receipt = jsonMapper.readTree(file.toFile())
        require(receipt["sha"] == final["sha"] && seen.add(receipt["consumer"].asText())) { "Mixed or duplicate consumer receipt" }
        require(expected[receipt["consumer"].asText()] == receipt["family"].asText()) { "Wrong artifact family for consumer" }
        val reference = if (receipt["family"].asText() == "apk") apk else final
        require(receipt["files"].size() > 0 && receipt.path("resolved").size() > 0) { "Missing actual consumed artifact evidence" }
        for ((name, hash) in receipt["files"].properties().asSequence()) require(reference["files"][name] == hash) { "Consumer bytes differ from final candidate: $name" }
        validateConsumedPayloads(receipt["files"].fieldNames().asSequence().toSet(), receipt["family"].asText(), receipt["consumer"].asText())
        val hashes = receipt["files"].properties().map { it.value.asText() }.toSet()
        require(receipt["resolved"].all { it.path("sha256").asText() in hashes }) { "Resolved artifacts are absent from the candidate receipt" }
    }
    require(seen == expected.keys) { "Missing consumer evidence: ${expected.keys - seen}; unexpected: ${seen - expected.keys}" }
}
fun refreshModuleFiles(repository: Path) {
    for (path in tree(repository).filter { it.extension == "module" }) {
        val model = jsonMapper.readTree(path.toFile())
        for (variant in model.path("variants")) for (entry in variant.path("files")) {
            val file = path.parent.resolve(entry["url"].asText()).normalize()
            require(file.startsWith(repository.normalize()) && file.isRegularFile()) { "Missing module payload: $file" }
            val node = entry as com.fasterxml.jackson.databind.node.ObjectNode
            node.put("size", Files.size(file))
            checksumAlgorithms.forEach { node.put(it, digest(file, it)) }
        }
        path.json(model)
    }
    checksums(repository)
}
fun projectPlatforms(projected: Path, platforms: List<String>) {
    for (module in kmpModules) {
        val path = projected.resolve("$module/module.yaml")
        val pattern = Regex("(?m)(^  platforms:\n)(?:    - [A-Za-z0-9]+\n)+")
        val original = path.text()
        require(pattern.findAll(original).count() == 1)
        path.write(pattern.replace(original) { it.groupValues[1] + platforms.joinToString("") { p -> "    - $p\n" } })
    }
}
fun metadataStage(repository: Path, seed: Path) {
    require(macos && !repository.exists()) { "Full metadata requires macOS and a new output directory" }
    readCandidate(seed, "jvm")
    bootstrap()
    runToolchain(listOf("task") + kmpModules.map { ":$it:assembleMetadata" })
    copyTree(seed, repository)
    val version = versions().getValue("version")
    for (module in kmpModules) {
        val base = repository.resolve("com/kotlinorm/$module/$version")
        val assembled = repositoryRoot.resolve("build/tasks/_${module}_assembleMetadata")
        val metadata = assembled.resolve("$module-metadata-$version.jar")
        val entries = linkedMapOf<String, ByteArray>()
        ZipFile(metadata.toFile()).use { zip -> zip.entries().asSequence().filterNot { it.isDirectory }.forEach {
            entries[it.name] = zip.getInputStream(it).use { input -> input.readBytes() }
        } }
        val key = "META-INF/kotlin-project-structure-metadata.json"
        val structure = jsonMapper.readTree(requireNotNull(entries[key]))
        val project = structure["projectStructure"]
        val variants = project["variants"] as com.fasterxml.jackson.databind.node.ArrayNode
        val sourceSets = project["sourceSets"].map { it["name"].asText() }.toSet()
        val templates = variants.filter { it["name"].asText().startsWith("watchosArm64") }
        require(templates.isNotEmpty()) { "Missing full watchOS metadata" }
        for (template in templates) variants.add(jsonMapper.createObjectNode().apply {
            put("name", template["name"].asText().replace("watchosArm64", "watchosX64"))
            set<com.fasterxml.jackson.databind.JsonNode>("sourceSet", jsonMapper.valueToTree(
                listOf("commonMain", "nativeMain", "appleMain", "watchosMain").filter { it in sourceSets }))
        })
        entries[key] = jsonMapper.writeValueAsBytes(structure)
        val manifest = repositoryRoot.resolve("build/tasks/_${module}_generateNativeMetadata@kronos-native/resources/META-INF/kronos/native-metadata-v1.tsv")
        if (manifest.isRegularFile()) entries["META-INF/kronos/native-metadata-v1.tsv"] = manifest.readBytes()
        ZipOutputStream(Files.newOutputStream(base.resolve("$module-$version.jar"))).use { zip ->
            entries.forEach { (name, bytes) -> zip.putNextEntry(ZipEntry(name).apply { time = 0 }); zip.write(bytes); zip.closeEntry() }
        }
        Files.copy(assembled.resolve("$module-kotlin-$version-sources.jar"), base.resolve("$module-$version-sources.jar"), java.nio.file.StandardCopyOption.REPLACE_EXISTING)
    }
    refreshModuleFiles(repository)
    candidateManifest(repository, "metadata")
}
fun appendGroup(base: Path, addition: Path, group: String) {
    val version = versions().getValue("version")
    val targets = publicationGroups.getValue(group)
    for (module in kmpModules) {
        val directory = "com/kotlinorm/$module/$version"
        val rootFile = base.resolve("$directory/$module-$version.module")
        val root = jsonMapper.readTree(rootFile.toFile())
        val variants = root["variants"] as com.fasterxml.jackson.databind.node.ArrayNode
        val leafRoot = jsonMapper.readTree(addition.resolve("$directory/$module-$version.module").toFile())
        for (target in targets) {
            val artifact = "$module-${target.lowercase()}"
            val leaf = "com/kotlinorm/$artifact/$version"
            require(addition.resolve(leaf).exists()) { "Missing target publication: $artifact" }
            require(!base.resolve(leaf).exists()) { "Duplicate target publication: $artifact" }
            copyTree(addition.resolve(leaf), base.resolve(leaf))
            val matching = leafRoot["variants"].filter { it.path("available-at").path("module").asText() == artifact }
            require(matching.isNotEmpty()) { "Missing actual target variants: $artifact" }
            for (variant in matching) {
                require(variants.none { it["name"] == variant["name"] }) { "Duplicate root variant" }
                variants.add(variant)
            }
        }
        rootFile.json(root)
        val toolingName = "$module-$version-kotlin-tooling-metadata.json"
        val toolingFile = base.resolve("$directory/$toolingName")
        val tooling = jsonMapper.readTree(toolingFile.toFile())
        val toolingTargets = tooling["projectTargets"] as com.fasterxml.jackson.databind.node.ArrayNode
        val stagedTooling = jsonMapper.readTree(addition.resolve("$directory/$toolingName").toFile())
        for (target in targets) {
            val nativeName = Regex("([a-z])([A-Z])").replace(target) { "${it.groupValues[1]}_${it.groupValues[2]}" }.lowercase()
            val from = stagedTooling["projectTargets"].firstOrNull { it.path("extras").path("native").path("konanTarget").asText() == nativeName }
                ?: stagedTooling["projectTargets"].first { target == "watchosX64" && it.path("extras").path("native").path("konanTarget").asText() == "watchos_arm64" }
            val node = from.deepCopy<com.fasterxml.jackson.databind.node.ObjectNode>()
            (node["extras"]["native"] as com.fasterxml.jackson.databind.node.ObjectNode).put("konanTarget", nativeName)
            require(toolingTargets.none { it.path("extras").path("native").path("konanTarget").asText() == nativeName }) { "Duplicate tooling target" }
            toolingTargets.add(node)
        }
        toolingFile.json(tooling)
    }
}
fun shardStage(repository: Path, seed: Path, group: String) {
    val targets = publicationGroups.getValue(group)
    require(!repository.exists()) { "Shard output must be a new directory" }
    readCandidate(seed, "metadata")
    val projected = repositoryRoot.resolve("build/ci-projects/$group")
    if (projected.exists()) projected.toFile().listFiles()!!.filter { it.name != "build" }.forEach { deleteTree(it.toPath()) }
    copyTree(repositoryRoot, projected, setOf(".git", ".idea", ".agents", ".github", "build", "node_modules", "kronos-docs", ".gradle", ".kotlin", ".cache", "dist", "out"))
    // Only leaf descriptors are taken from this projection. Full shared metadata stays authoritative.
    projected.resolve("project.yaml").write("modules:\n" + (kmpModules + "plugins/kronos-native").sorted().joinToString("") { "  - $it\n" } + "plugins:\n  - ./plugins/kronos-native\n")
    // Keep JVM in the compilation model: Kotlin optional JVM expectations in common
    // sources require the complete common/native distinction during metadata compilation.
    projectPlatforms(projected, listOf("jvm") + targets.filter { it != "watchosX64" })
    if (group == "watchos") for (module in kmpModules) {
        val model = projected.resolve("$module/module.yaml")
        model.write(model.text() + "\n")
        model.write(model.text().replace("    enabled: true\n", "    enabled: true\n    publicationTemplateTarget: watchosArm64\n"))
    }
    script("build", listOf("publish", "kronosBootstrap", "--non-transitive") + kmpModules.flatMap { listOf("-m", it) }, root = projected, environment = mapOf("KRONOS_SOURCE_SHA" to sourceRevision()))
    var staged = projected.resolve("build/repository/bootstrap")
    if (group == "watchos") {
        script("build", listOf("do", "finalizeNativePublication", "-m", "kronos-logging"), root = projected, environment = mapOf("KRONOS_SOURCE_SHA" to sourceRevision()))
        staged = projected.resolve("build/tasks/_kronos-logging_finalizeNativePublication@kronos-native/repository")
    }
    copyTree(seed, repository)
    appendGroup(repository, staged, group)
    addDocumentation(repository, versions().getValue("version"))
    refreshModuleFiles(repository)
    candidateManifest(repository, group)
    val macTarget = if (System.getProperty("os.arch") in listOf("x86_64", "amd64")) "macosX64" else "macosArm64"
    val runtimeTarget = mapOf(
        "ios" to "iosSimulatorArm64", "macos" to macTarget,
        "tvos" to "tvosSimulatorArm64", "watchos" to "watchosSimulatorArm64",
        "linux" to "linuxX64", "windows" to "mingwX64",
    ).getValue(group)
    script("ci", listOf("native", "--target", runtimeTarget), root = projected, environment = mapOf("KRONOS_SOURCE_SHA" to sourceRevision()))
    if (group == "linux") script("ci", listOf("native", "--target", "linuxArm64", "--link-only"), root = projected, environment = mapOf("KRONOS_SOURCE_SHA" to sourceRevision()))
    copyTree(projected.resolve("build/reports"), repositoryRoot.resolve("build/reports"))
}
fun assembleShards(repository: Path, metadata: Path, inputs: List<Path>) {
    require(!repository.exists())
    readCandidate(metadata, "metadata")
    require(inputs.size == publicationGroups.size) { "All six publication groups are required" }
    val groups = inputs.associateBy { readCandidate(it)["group"].asText() }
    require(groups.keys == publicationGroups.keys) { "Incomplete publication groups" }
    copyTree(metadata, repository)
    for ((group, input) in groups) {
        // Metadata/JVM bytes must match the authoritative candidate even if descriptors add variants.
        val model = readCandidate(metadata)
        for ((name, hash) in model["files"].properties().asSequence()) if (name.endsWith(".jar") || name.endsWith(".klib")) {
            require(digest(input.resolve(name)) == hash.asText()) { "Shard changed a base artifact: $name" }
        }
        appendGroup(repository, input, group)
    }
    refreshModuleFiles(repository)
    candidateManifest(repository, "complete")
    verify(repository)
}


fun sign(repository: Path, files: List<Path>) {
    val key =
        System.getenv("KOTLIN_TOOLCHAIN_SIGNING_KEY")
            ?: error("Release signing requires KOTLIN_TOOLCHAIN_SIGNING_KEY")
    val temporary = Files.createTempDirectory("kronos-release-gnupg-")
    try {
        if (!windows)
            Files.setPosixFilePermissions(temporary, PosixFilePermissions.fromString("rwx------"))
        val environment = mapOf("GNUPGHOME" to temporary.toString())
        run(
            listOf("gpg", "--batch", "--import"),
            environment = environment,
            input = key.toByteArray(),
            capture = true,
        )
        for (path in
            files.filter { it.extension !in checksumAlgorithms && it.extension != "asc" }) run(
            listOf(
                "gpg",
                "--batch",
                "--yes",
                "--pinentry-mode",
                "loopback",
                "--passphrase-fd",
                "0",
                "--armor",
                "--detach-sign",
                path.toString(),
            ),
            environment = environment,
            input =
                System.getenv("KOTLIN_TOOLCHAIN_SIGNING_KEY_PASSPHRASE").orEmpty().toByteArray(),
        )
    } finally {
        deleteTree(temporary)
    }
    checksums(repository)
}

fun bundle(repository: Path, output: Path, signed: Boolean) {
    val version = versions().getValue("version")
    verify(repository)
    if (signed) sign(repository, publicationFiles(repository, version))
    val files = publicationFiles(repository, version)
    output.parent.createDirectories()
    ZipOutputStream(Files.newOutputStream(output)).use { archive ->
        for (path in files) {
            archive.putNextEntry(
                ZipEntry(repository.relativize(path).toString().replace('\\', '/'))
            )
            archive.write(path.readBytes())
            archive.closeEntry()
        }
    }
    output.resolveSibling("${output.name}.sha256").write(digest(output) + "\n")
    println(
        "Kronos: created ${if (signed) "signed" else "unsigned"} Maven bundle $output (${files.size} files)"
    )
}

fun credentials(): String =
    Base64.getEncoder()
        .encodeToString(
            (requireNotNull(System.getenv("MAVEN_CENTRAL_USERNAME")) +
                    ":" +
                    requireNotNull(System.getenv("MAVEN_CENTRAL_PASSWORD")))
                .toByteArray()
        )

fun post(url: String, body: ByteArray, headers: Map<String, String>, timeout: Long = 60): String {
    val request =
        HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(timeout))
            .POST(HttpRequest.BodyPublishers.ofByteArray(body))
    headers.forEach { (key, value) -> request.header(key, value) }
    val response =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(60))
            .build()
            .send(request.build(), HttpResponse.BodyHandlers.ofString())
    require(response.statusCode() in 200..299) {
        "Maven Central HTTP ${response.statusCode()}: ${response.body()}"
    }
    return response.body()
}

fun encoded(value: String) = URLEncoder.encode(value, Charsets.UTF_8)

fun releasePayloadFingerprint(output: Path): String = ZipFile(output.toFile()).use { archive ->
    val payloads = archive.entries().asSequence().filter { !it.isDirectory &&
        it.name.substringAfterLast('.') !in checksumAlgorithms + "asc" }.sortedBy { it.name }.map { entry ->
        entry.name + ":" + java.security.MessageDigest.getInstance("SHA-256").digest(archive.getInputStream(entry).use { it.readBytes() })
            .joinToString("") { "%02x".format(it) }
    }.toList()
    java.security.MessageDigest.getInstance("SHA-256").digest(payloads.joinToString("\n").toByteArray()).joinToString("") { "%02x".format(it) }
}

fun publishRelease(
    output: Path,
    checkpoint: Path,
    endpoint: String = "https://central.sonatype.com/api/v1/publisher",
    pause: () -> Unit = { Thread.sleep(15000) },
    publicRepository: String = "https://repo.maven.apache.org/maven2",
    attempts: Int = 120,
    version: String = versions().getValue("version")
) {
    require(!version.endsWith("-SNAPSHOT")) {
        "Use publish-snapshot for a SNAPSHOT version"
    }
    ZipFile(output.toFile()).use { archive ->
        val names = archive.entries().asSequence().map { it.name }.toSet()
        require(
            names.isNotEmpty() &&
                names.none { name ->
                    name.substringAfterLast('.') in setOf("jar", "klib", "pom", "module") &&
                        "$name.asc" !in names
                }
        ) {
            "Maven Central requires a complete signed release bundle"
        }
    }
    val fingerprint = releasePayloadFingerprint(output)
    val token = credentials()
    val headers = mapOf("Authorization" to "Bearer $token")
    var saved = if (checkpoint.isRegularFile()) jsonMapper.readTree(checkpoint.toFile()) else null
    if (saved != null) require(saved["version"].asText() == version && saved["source"].asText() == sourceRevision() &&
        saved["payloadSha256"].asText() == fingerprint) { "Central checkpoint belongs to different source bytes; refusing to publish or skip" }
    fun record(deployment: String?, state: String) {
        checkpoint.parent.createDirectories()
        val temporary = checkpoint.resolveSibling(checkpoint.fileName.toString() + ".tmp")
        temporary.json(mapOf("version" to version, "source" to sourceRevision(), "payloadSha256" to fingerprint,
            "deploymentId" to deployment, "state" to state))
        Files.move(temporary, checkpoint, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
    }
    val deployment = if (saved != null) {
        require(saved.path("deploymentId").isTextual && saved["deploymentId"].asText().isNotBlank()) {
            "Previous upload outcome is unknown; recover its deployment ID from Central before retrying, instead of uploading duplicate coordinates"
        }
        saved["deploymentId"].asText()
    } else {
        val boundary = "kronos-${UUID.randomUUID()}"
        val body = "--$boundary\r\nContent-Disposition: form-data; name=\"bundle\"; filename=\"${output.name}\"\r\nContent-Type: application/octet-stream\r\n\r\n".toByteArray() +
            output.readBytes() + "\r\n--$boundary--\r\n".toByteArray()
        record(null, "UPLOAD_OUTCOME_UNKNOWN")
        post("$endpoint/upload?name=${encoded("Kronos " + version)}&publishingType=AUTOMATIC", body,
            headers + ("Content-Type" to "multipart/form-data; boundary=$boundary"), 600).trim()
    }
    require(deployment.matches(Regex("[A-Za-z0-9-]+"))) { "Unexpected Central deployment identifier" }
    // Persist the real deployment ID before polling; the workflow retains it even on failure.
    record(deployment, saved?.get("state")?.asText() ?: "UPLOADED")
    println("Kronos Central deployment: $deployment")
    repeat(attempts) {
        val status = jsonMapper.readTree(post("$endpoint/status?id=${encoded(deployment)}", ByteArray(0), headers))
        val state = status["deploymentState"].asText()
        record(deployment, state)
        println("Kronos Central: $state")
        if (state == "PUBLISHED") {
            val visible = ZipFile(output.toFile()).use { archive ->
                val purls = archive.entries().asSequence().filter { it.name.endsWith(".pom") }.map { entry ->
                    val parts = entry.name.split('/')
                    "pkg:maven/" + parts.dropLast(3).joinToString(".") + "/" + parts[parts.size - 3] + "@" + parts[parts.size - 2]
                }.toSet()
                require(purls.isNotEmpty() && status.path("purls").map { it.asText() }.toSet().containsAll(purls)) {
                    "Published deployment does not contain every expected release coordinate"
                }
                val probes = listOf("com/kotlinorm/kronos-core/$version/kronos-core-$version.pom",
                    "com/kotlinorm/kronos-compiler-plugin/$version/kronos-compiler-plugin-$version.jar")
                    .mapNotNull { archive.getEntry(it) }
                require(probes.isNotEmpty()) { "No release visibility probes in bundle" }
                probes.all { entry ->
                    val response = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("$publicRepository/${entry.name}"))
                        .timeout(Duration.ofSeconds(60)).GET().build(), HttpResponse.BodyHandlers.ofByteArray())
                    response.statusCode() == 200 && archive.getInputStream(entry).use { it.readBytes() }.contentEquals(response.body())
                }
            }
            if (visible) return
            println("Kronos Central: waiting for the published version to become visible with matching bytes")
        }
        require(state != "FAILED") { "Central validation failed: ${status["errors"]}" }
        pause()
    }
    error("Central deployment did not reach PUBLISHED: $deployment; resume using $checkpoint")
}

data class Attachment(val path: Path, val type: String, val classifier: String)

data class SnapshotComponent(val pom: Path, val primary: Path, val attachments: List<Attachment>)

fun snapshotComponents(repository: Path, version: String): List<SnapshotComponent> {
    val components =
        publicationFiles(repository, version)
            .filter { it.extension == "pom" }
            .map { pom ->
                val packaging = xml(pom).documentElement.value("packaging", "jar")
                val prefix = pom.nameWithoutExtension
                var primary =
                    if (packaging == "pom") pom
                    else pom.resolveSibling(prefix + if (packaging == "klib") ".klib" else ".jar")
                if (!primary.isRegularFile()) primary = pom.resolveSibling("$prefix.klib")
                require(primary.isRegularFile()) { "No primary snapshot artifact for $pom" }
                val attachments =
                    pom.parent
                        .listDirectoryEntries()
                        .filter {
                            it != pom &&
                                it != primary &&
                                it.extension in setOf("jar", "module", "json", "klib") &&
                                it.name.startsWith(prefix)
                        }
                        .sorted()
                        .map {
                            Attachment(
                                it,
                                it.extension,
                                it.nameWithoutExtension.removePrefix(prefix).removePrefix("-"),
                            )
                        }
                SnapshotComponent(pom, primary, attachments)
            }
    require(components.isNotEmpty()) { "No snapshot components found" }
    return components
}

fun publishSnapshots(
    repository: Path,
    url: String = "https://central.sonatype.com/repository/maven-snapshots/",
) {
    val version = versions().getValue("version")
    require(version.endsWith("-SNAPSHOT")) { "Snapshot publication requires a SNAPSHOT version" }
    verify(repository)
    val components = snapshotComponents(repository, version)
    val temporary = Files.createTempDirectory("kronos-snapshot-client-")
    try {
        val settings = document("settings")
        val server = settings.documentElement.append("servers").append("server")
        server.append("id", "kronos-snapshots")
        server.append("username", requireNotNull(System.getenv("MAVEN_CENTRAL_USERNAME")))
        server.append("password", requireNotNull(System.getenv("MAVEN_CENTRAL_PASSWORD")))
        val settingsFile = temporary.resolve("settings.xml")
        saveXml(settings, settingsFile)
        if (!windows)
            Files.setPosixFilePermissions(
                settingsFile,
                PosixFilePermissions.fromString("rw-------"),
            )
        val project = temporary.resolve("pom.xml")
        project.write(
            "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"><modelVersion>4.0.0</modelVersion><groupId>com.kotlinorm.build</groupId><artifactId>snapshot-client</artifactId><version>1</version></project>"
        )
        for ((pom, primary, attachments) in components) {
            val command =
                mutableListOf(
                    "mvn",
                    "-B",
                    "--no-transfer-progress",
                    "-s",
                    settingsFile.toString(),
                    "-f",
                    project.toString(),
                    dependency("maven-deploy-plugin") + ":deploy-file",
                    "-DrepositoryId=kronos-snapshots",
                    "-Durl=$url",
                    "-Dfile=$primary",
                    "-Dextension=${primary.extension}",
                    "-DpomFile=$pom",
                    "-DgeneratePom=false",
                    "-DretryFailedDeploymentCount=3",
                )
            if (attachments.isNotEmpty())
                command +=
                    listOf(
                        "-Dfiles=" + attachments.joinToString(",") { it.path.toString() },
                        "-Dtypes=" + attachments.joinToString(",") { it.type },
                        "-Dclassifiers=" + attachments.joinToString(",") { it.classifier },
                    )
            run(command, cwd = temporary)
        }
    } finally {
        deleteTree(temporary)
    }
}

cli {
    if (args.isEmpty() || args[0] in setOf("-h", "--help"))
        println(
            "Usage: scripts/run release stage|jvm-stage|metadata-stage|shard-stage|assemble|check-candidate|stamp|receipt|verify-consumers|verify|bundle|publish|publish-snapshot [--repository PATH] [--output ZIP] [--checkpoint JSON] [--sign]"
        )
    else {
        val options = Options(args, setOf("--sign"))
        options.validate(setOf("--repository", "--output", "--seed", "--group", "--input", "--family", "--checkpoint"))
        require(options.positional.size == 1) { "Expected one release command" }
        val repository =
            Path.of(
                    options.one(
                        "--repository",
                        repositoryRoot.resolve("build/release/repository").toString(),
                    )!!
                )
                .toAbsolutePath()
                .normalize()
        val output =
            Path.of(
                    options.one(
                        "--output",
                        repositoryRoot.resolve("build/release/central-bundle.zip").toString(),
                    )!!
                )
                .toAbsolutePath()
                .normalize()
        when (options.positional.single()) {
            "stage" -> stage(repository)
            "jvm-stage" -> { jvmStage(repository); candidateManifest(repository, "jvm") }
            "metadata-stage" -> metadataStage(repository, Path.of(requireNotNull(options.one("--seed"))).toAbsolutePath().normalize())
            "shard-stage" -> shardStage(repository, Path.of(requireNotNull(options.one("--seed"))).toAbsolutePath().normalize(), requireNotNull(options.one("--group")))
            "assemble" -> assembleShards(repository, Path.of(requireNotNull(options.one("--seed"))).toAbsolutePath().normalize(), options.values.getValue("--input").map { Path.of(it).toAbsolutePath().normalize() })
            "check-candidate" -> readCandidate(repository)
            "stamp" -> candidateManifest(repository, requireNotNull(options.one("--group")))
            "receipt" -> consumerReceipt(repository, output, requireNotNull(options.one("--group")), requireNotNull(options.one("--family")))
            "verify-consumers" -> verifyConsumers(repository, Path.of(requireNotNull(options.one("--seed"))), Path.of(requireNotNull(options.one("--input"))))
            "verify" -> verify(repository)
            "bundle" -> bundle(repository, output, options.has("--sign"))
            "publish" -> publishRelease(output, Path.of(options.one("--checkpoint", repositoryRoot.resolve("build/release/checkpoint/deployment.json").toString())!!))
            "publish-snapshot" -> publishSnapshots(repository)
            else -> error("Unknown release command")
        }
    }
}
