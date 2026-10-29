/**
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kotlinorm.toolchain

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import org.jetbrains.amper.plugins.Input
import org.jetbrains.amper.plugins.Output
import org.jetbrains.amper.plugins.TaskAction

private const val WATCH_TARGET = "watchos_x64"
private const val WATCH_PLATFORM = "watchosX64"
private const val NATIVE_ATTRIBUTE = "org.jetbrains.kotlin.native.target"
private const val STRUCTURE_METADATA = "META-INF/kotlin-project-structure-metadata.json"
private val jsonWriter = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
private val digests = listOf("SHA-512" to "sha512", "SHA-256" to "sha256", "SHA-1" to "sha1", "MD5" to "md5")

/** Finalizes private staging artifacts; signatures are created from this output afterwards. */
@TaskAction
fun finalizeNativePublication(
    moduleName: String,
    group: String?,
    artifact: String?,
    version: String?,
    templateTarget: String,
    @Input stageRepository: Path?,
    @Input(inferTaskDependency = false) bootstrapStageRepository: Path?,
    @Input watchArtifacts: Path,
    @Input metadataResources: Path,
    @Output finalRepository: Path
) {
    require(stageRepository == null || bootstrapStageRepository == null) { "Configure only one publication stage" }
    val stage = requireNotNull(stageRepository ?: bootstrapStageRepository) { "Configure the private Maven stage" }
        .toAbsolutePath().normalize()
    val output = finalRepository.toAbsolutePath().normalize()
    require(!output.startsWith(stage) && !stage.startsWith(output)) { "Staging and finalized repositories must be separate" }
    val publicationGroup = requireNotNull(group)
    val publicationArtifact = artifact ?: moduleName
    val publicationVersion = requireNotNull(version)
    val watchKlib = watchArtifacts.resolve("$moduleName-watchosx64.klib")
    val watchSources = watchArtifacts.resolve("$moduleName-watchosx64-sources.jar")
    ZipFile(watchKlib.toFile()).use { zip ->
        val manifest = zip.getInputStream(requireNotNull(zip.getEntry("default/manifest"))).bufferedReader().use { it.readText() }
        val fields = manifest.lineSequence().mapNotNull { line ->
            val separator = line.indexOf('=')
            if (separator < 0) null else line.substring(0, separator) to line.substring(separator + 1)
        }.toMap()
        require(fields["compiler_version"] == watchArtifacts.resolve("compiler-version.txt").toFile().readText().trim() && fields["native_targets"] == WATCH_TARGET) {
            "Expected an actual catalog-version Kotlin/Native watchos_x64 KLIB"
        }
    }
    val groupDirectory = stage.resolve(publicationGroup.replace('.', '/'))
    require(Files.isDirectory(groupDirectory)) { "Missing staged publication group: $publicationGroup" }
    Files.walk(groupDirectory).use { paths ->
        require(paths.iterator().asSequence().none { it.fileName.toString().endsWith(".asc") }) {
            "Finalize unsigned artifacts before creating signatures"
        }
    }
    output.toFile().apply { deleteRecursively(); mkdirs() }
    Files.walk(stage).use { paths ->
        paths.iterator().forEachRemaining { source ->
            require(!Files.isSymbolicLink(source)) { "Maven stage must contain regular files: $source" }
            val target = output.resolve(stage.relativize(source))
            if (Files.isDirectory(source)) Files.createDirectories(target)
            else Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING)
        }
    }
    val base = output.resolve(publicationGroup.replace('.', '/'))
    val rootDirectory = base.resolve("$publicationArtifact/$publicationVersion")
    val rootModule = rootDirectory.resolve("$publicationArtifact-$publicationVersion.module")
    val root = readJson(rootModule)
    val templateArtifact = "$publicationArtifact-${templateTarget.lowercase(Locale.ROOT)}"
    val watchArtifact = "$publicationArtifact-watchosx64"
    val templateDirectory = base.resolve("$templateArtifact/$publicationVersion")
    val watchDirectory = base.resolve("$watchArtifact/$publicationVersion")
    Files.createDirectories(watchDirectory)
    val leaf = readJson(templateDirectory.resolve("$templateArtifact-$publicationVersion.module"))
    val templateVariants = root.getAsJsonArray("variants").map { it.asJsonObject }
        .filter { it.get("name").asString.startsWith(templateTarget) }
    require(templateVariants.isNotEmpty()) { "Staged publication has no $templateTarget variant" }
    require(templateVariants.none { it.getAsJsonObject("attributes").get(NATIVE_ATTRIBUTE) == null })
    require(root.getAsJsonArray("variants").none { it.asJsonObject.get("name").asString.startsWith(WATCH_PLATFORM) }) {
        "watchosX64 has already been added to this stage"
    }
    for (variantElement in leaf.getAsJsonArray("variants")) {
        val variant = variantElement.asJsonObject
        variant.addProperty("name", variant.get("name").asString.replace(templateTarget, WATCH_PLATFORM))
        variant.getAsJsonObject("attributes").addProperty(NATIVE_ATTRIBUTE, WATCH_TARGET)
        for (fileElement in variant.getAsJsonArray("files") ?: JsonArray()) {
            val file = fileElement.asJsonObject
            val oldUrl = file.get("url").asString
            val source = when {
                oldUrl.endsWith("-sources.jar") -> watchSources
                oldUrl.endsWith(".klib") && !oldUrl.contains("-cinterop-") -> watchKlib
                else -> error("Native bridge needs an explicit artifact for $oldUrl")
            }
            val extension = if (source == watchSources) "-sources.jar" else ".klib"
            val destination = watchDirectory.resolve("$watchArtifact-$publicationVersion$extension")
            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING)
            file.addProperty("name", file.get("name").asString.replace(templateTarget, WATCH_PLATFORM))
            file.addProperty("url", destination.fileName.toString())
        }
    }
    writeJson(watchDirectory.resolve("$watchArtifact-$publicationVersion.module"), leaf)
    val pom = templateDirectory.resolve("$templateArtifact-$publicationVersion.pom").toFile().readText()
    val artifactTag = "<artifactId>$templateArtifact</artifactId>"
    require(pom.contains(artifactTag)) { "Template POM has unexpected coordinates" }
    watchDirectory.resolve("$watchArtifact-$publicationVersion.pom").toFile()
        .writeText(rewriteNativePom(pom, templateTarget))
    for (original in templateVariants) {
        val variant = original.deepCopy()
        variant.addProperty("name", variant.get("name").asString.replace(templateTarget, WATCH_PLATFORM))
        variant.getAsJsonObject("attributes").addProperty(NATIVE_ATTRIBUTE, WATCH_TARGET)
        variant.getAsJsonObject("available-at").apply {
            addProperty("module", watchArtifact)
            addProperty("url", "../../$watchArtifact/$publicationVersion/$watchArtifact-$publicationVersion.module")
        }
        root.getAsJsonArray("variants").add(variant)
    }
    val metadataVariant = root.getAsJsonArray("variants").first {
        it.asJsonObject.get("name").asString == "metadataApiElements"
    }.asJsonObject
    val metadataJar = rootDirectory.resolve(metadataVariant.getAsJsonArray("files")[0].asJsonObject.get("url").asString)
    updateMetadataJar(metadataJar, metadataResources.resolve(NATIVE_METADATA_MANIFEST_PATH), templateTarget)
    writeJson(rootModule, root)
    val toolingFile = rootDirectory.resolve("$publicationArtifact-$publicationVersion-kotlin-tooling-metadata.json")
    if (Files.isRegularFile(toolingFile)) {
        val tooling = readJson(toolingFile)
        appendWatchToolingTarget(tooling, templateTarget)
        writeJson(toolingFile, tooling)
    }
    refreshChecksums(base, output)
    println("Finalized $publicationGroup:$publicationArtifact:$publicationVersion with a real $WATCH_TARGET KLIB at $output")
}

/** Preserve the actual compiler/ABI metadata when adding the bridge's real target. */
internal fun appendWatchToolingTarget(tooling: JsonObject, templateTarget: String) {
    val targets = tooling.getAsJsonArray("projectTargets")
    val templateName = templateTarget.replace(Regex("([a-z])([A-Z])"), "$1_$2").lowercase(Locale.ROOT)
    val template = targets.map { it.asJsonObject }.first {
        it.getAsJsonObject("extras")?.getAsJsonObject("native")?.get("konanTarget")?.asString == templateName
    }.deepCopy()
    template.getAsJsonObject("extras").getAsJsonObject("native").addProperty("konanTarget", WATCH_TARGET)
    targets.add(template)
}

/** Maven POM consumers also need target-specific transitive artifact coordinates. */
internal fun rewriteNativePom(pom: String, templateTarget: String): String {
    val suffix = Regex.escape(templateTarget.lowercase(Locale.ROOT))
    return Regex("<artifactId>([^<]+)-$suffix</artifactId>").replace(pom) { match ->
        "<artifactId>${match.groupValues[1]}-watchosx64</artifactId>"
    }
}

private fun updateMetadataJar(jar: Path, manifest: Path, templateTarget: String) {
    val entries = linkedMapOf<String, ByteArray>()
    ZipFile(jar.toFile()).use { zip ->
        zip.entries().asSequence().filterNot { it.isDirectory }.forEach { entry ->
            entries[entry.name] = zip.getInputStream(entry).use { it.readBytes() }
        }
    }
    val structure = JsonParser.parseString(requireNotNull(entries[STRUCTURE_METADATA]).toString(Charsets.UTF_8)).asJsonObject
    val project = structure.getAsJsonObject("projectStructure")
    val sourceNames = project.getAsJsonArray("sourceSets").map { it.asJsonObject.get("name").asString }.toSet()
    val watchSources = listOf("commonMain", "nativeMain", "appleMain", "watchosMain").filter { it in sourceNames }
    require("commonMain" in watchSources)
    val variants = project.getAsJsonArray("variants")
    val templates = variants.map { it.asJsonObject }.filter { it.get("name").asString.startsWith(templateTarget) }
    require(templates.isNotEmpty()) { "Project structure does not describe $templateTarget" }
    for (template in templates) {
        variants.add(JsonObject().apply {
            addProperty("name", template.get("name").asString.replace(templateTarget, WATCH_PLATFORM))
            add("sourceSet", JsonArray().apply { watchSources.forEach(::add) })
        })
    }
    entries[STRUCTURE_METADATA] = jsonWriter.toJson(structure).toByteArray(Charsets.UTF_8)
    if (Files.isRegularFile(manifest)) {
        readNativeMetadataManifests(setOf(manifest.parent.parent.parent.toFile()))
        entries[NATIVE_METADATA_MANIFEST_PATH] = Files.readAllBytes(manifest)
    }
    val temporary = jar.resolveSibling(jar.fileName.toString() + ".tmp")
    ZipOutputStream(Files.newOutputStream(temporary)).use { zip ->
        for ((name, bytes) in entries) {
            zip.putNextEntry(ZipEntry(name).apply { time = 0 })
            zip.write(bytes)
            zip.closeEntry()
        }
    }
    Files.move(temporary, jar, StandardCopyOption.REPLACE_EXISTING)
}

private fun readJson(path: Path): JsonObject = path.toFile().reader(Charsets.UTF_8).use {
    JsonParser.parseReader(it).asJsonObject
}

private fun writeJson(path: Path, value: JsonObject) = path.toFile().writeText(jsonWriter.toJson(value) + "\n", Charsets.UTF_8)

private fun refreshChecksums(base: Path, repository: Path) {
    val files = Files.walk(base).use { stream -> stream.iterator().asSequence().filter(Files::isRegularFile).toList() }
    for (module in files.filter { it.fileName.toString().endsWith(".module") }) {
        val content = readJson(module)
        for (element in content.getAsJsonArray("variants")) {
            val variant = element.asJsonObject
            for (fileElement in variant.getAsJsonArray("files") ?: JsonArray()) {
                val file = fileElement.asJsonObject
                val path = module.parent.resolve(file.get("url").asString).normalize()
                require(path.startsWith(repository) && Files.isRegularFile(path)) { "Invalid artifact reference: $path" }
                val bytes = Files.readAllBytes(path)
                file.addProperty("size", bytes.size)
                digests.forEach { (algorithm, name) -> file.addProperty(name, digest(algorithm, bytes)) }
            }
            variant.getAsJsonObject("available-at")?.let { available ->
                val path = module.parent.resolve(available.get("url").asString).normalize()
                require(path.startsWith(repository) && Files.isRegularFile(path)) { "Invalid variant reference: $path" }
            }
        }
        writeJson(module, content)
    }
    for (path in files.filter { it.fileName.toString().substringAfterLast('.') in setOf("jar", "klib", "pom", "module", "xml") }) {
        val bytes = Files.readAllBytes(path)
        digests.forEach { (algorithm, name) -> path.resolveSibling(path.fileName.toString() + ".$name").toFile().writeText(digest(algorithm, bytes)) }
    }
}

private fun digest(algorithm: String, bytes: ByteArray): String = MessageDigest.getInstance(algorithm).digest(bytes).hexDigestString()
