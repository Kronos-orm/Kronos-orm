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

import com.fasterxml.jackson.dataformat.toml.TomlMapper
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.Properties
import java.util.zip.ZipFile
import org.jetbrains.amper.plugins.Input
import org.jetbrains.amper.plugins.Output
import org.jetbrains.amper.plugins.TaskAction

private const val MAVEN_CONNECT_TIMEOUT_MILLIS = 30_000
private const val MAVEN_METADATA_READ_TIMEOUT_MILLIS = 60_000
private const val MAVEN_ARTIFACT_READ_TIMEOUT_MILLIS = 120_000

internal data class MavenCoordinate(val group: String, val artifact: String, val version: String) {
    val key: String get() = "$group:$artifact"
    val notation: String get() = "$key:$version"
    val path: String get() = "${group.replace('.', '/')}/$artifact/$version/$artifact-$version"

    init {
        require(group.matches(Regex("[A-Za-z0-9_.-]+")) && artifact.matches(Regex("[A-Za-z0-9_.-]+")) &&
            version.matches(Regex("[A-Za-z0-9_.+-]+"))) { "Expected exact Maven coordinates: $notation" }
    }
}

internal class NativeBuildCatalog(file: Path) {
    private val tree = TomlMapper().readTree(file.toFile())
    val kotlinVersion: String get() = tree.path("versions").path("kotlin").asText().also { require(it.isNotBlank()) }

    fun library(alias: String): MavenCoordinate {
        val library = tree.path("libraries").path(alias)
        require(!library.isMissingNode) { "Unknown library catalog alias: $alias" }
        val module = library.path("module").asText().split(':')
        require(module.size == 2) { "Catalog $alias must declare a Maven module" }
        val version = library.path("version")
        val value = if (version.isTextual) version.asText()
        else tree.path("versions").path(version.path("ref").asText()).asText()
        require(value.isNotBlank()) { "Catalog $alias must declare an exact version or version.ref" }
        return MavenCoordinate(module[0], module[1], value)
    }
}

@TaskAction
fun resolveWatchDependencies(
    @Input catalogFile: Path,
    @Input(inferTaskDependency = false) bootstrapRepository: Path,
    libraryAliases: List<String>,
    includeCompilerPlugin: Boolean,
    @Output outputDirectory: Path
) {
    val catalog = NativeBuildCatalog(catalogFile)
    outputDirectory.toFile().apply { deleteRecursively(); mkdirs() }
    outputDirectory.resolve("kotlin-version.txt").toFile().writeText(catalog.kotlinVersion)
    if (includeCompilerPlugin) {
        val compiler = catalog.library("kronos-compiler-plugin")
        val jar = bootstrapRepository.resolve(compiler.path + ".jar")
        require(Files.isRegularFile(jar)) { "Missing source-built $jar; run scripts/run build bootstrap" }
        Files.copy(jar, outputDirectory.resolve("compiler-plugin.jar"), StandardCopyOption.REPLACE_EXISTING)
    }
    val requested = libraryAliases.map(catalog::library)
    WatchDependencyResolver(outputDirectory, requested.associateBy(MavenCoordinate::key)).resolve(requested)
}

private class WatchDependencyResolver(private val output: Path, private val overrides: Map<String, MavenCoordinate>) {
    private val repository = URI("https://repo.maven.apache.org/maven2/")
    private val versions = mutableMapOf<String, String>()
    private val visited = mutableSetOf<String>()
    private val resolved = linkedMapOf<String, String>()

    fun resolve(coordinates: List<MavenCoordinate>) {
        coordinates.forEach(::visit)
        output.resolve("resolved-libraries.tsv").toFile().writeText(resolved.entries.joinToString("") { (id, file) -> "$id\t$file\n" })
    }

    private fun visit(requested: MavenCoordinate) {
        if (requested.group == "org.jetbrains.kotlin" && requested.artifact in setOf("kotlin-stdlib", "kotlin-stdlib-common")) return
        val coordinate = overrides[requested.key] ?: requested
        val previous = versions.put(coordinate.key, coordinate.version)
        require(previous == null || previous == coordinate.version) {
            "Conflicting Native versions for ${coordinate.key}; add a direct catalog alias to choose the version"
        }
        if (!visited.add(coordinate.notation)) return
        val moduleUri = repository.resolve(coordinate.path + ".module")
        val text = moduleUri.toURL().openConnection().apply {
            connectTimeout = MAVEN_CONNECT_TIMEOUT_MILLIS
            readTimeout = MAVEN_METADATA_READ_TIMEOUT_MILLIS
        }.getInputStream().bufferedReader(Charsets.UTF_8).use { it.readText() }
        val module = JsonParser.parseString(text).asJsonObject
        require(module.get("formatVersion").asString == "1.1") { "Unsupported Maven module metadata for ${coordinate.notation}" }
        val variant = module.getAsJsonArray("variants").map { it.asJsonObject }.singleOrNull {
            val attributes = it.getAsJsonObject("attributes")
            attributes.get("org.jetbrains.kotlin.native.target")?.asString == "watchos_x64" &&
                attributes.get("org.gradle.usage")?.asString == "kotlin-api" &&
                attributes.get("org.gradle.category")?.asString == "library"
        } ?: error("No watchos_x64 API variant in ${coordinate.notation}")
        variant.getAsJsonObject("available-at")?.let { location ->
            visit(MavenCoordinate(location.get("group").asString, location.get("module").asString, location.get("version").asString))
        }
        variant.getAsJsonArray("files")?.forEach { element ->
            val file = element.asJsonObject
            val url = file.get("url").asString
            require(url.endsWith(".klib")) { "Unexpected Native API artifact: $url" }
            val source = moduleUri.resolve(url).normalize()
            require(source.toString().startsWith(repository.toString())) { "Native artifact URL leaves its Maven repository" }
            val name = coordinate.group + "-" + url.substringAfterLast('/')
            val destination = output.resolve(name)
            source.toURL().openConnection().apply { connectTimeout = MAVEN_CONNECT_TIMEOUT_MILLIS; readTimeout = MAVEN_ARTIFACT_READ_TIMEOUT_MILLIS }
                .getInputStream().use { input -> Files.newOutputStream(destination).use { target -> input.copyTo(target) } }
            verifyArtifact(destination, file)
            ZipFile(destination.toFile()).use { archive ->
                val manifest = Properties()
                archive.getInputStream(requireNotNull(archive.getEntry("default/manifest"))).use { manifest.load(it) }
                require("watchos_x64" in manifest.getProperty("native_targets", "").split(' ')) {
                    "Resolved artifact targets a different Native platform: ${coordinate.notation}"
                }
            }
            resolved[coordinate.notation] = name
        }
        variant.getAsJsonArray("dependencies")?.forEach { element ->
            val dependency = element.asJsonObject
            val version = dependency.getAsJsonObject("version")
            val value = version.get("strictly") ?: version.get("requires") ?: version.get("prefers")
            visit(MavenCoordinate(dependency.get("group").asString, dependency.get("module").asString,
                requireNotNull(value) { "Native dependency must have an exact version" }.asString))
        }
    }

    private fun verifyArtifact(path: Path, record: JsonObject) {
        val (name, algorithm) = when {
            record.has("sha512") -> "sha512" to "SHA-512"
            record.has("sha256") -> "sha256" to "SHA-256"
            else -> error("Native artifact has no SHA256/SHA512 checksum: $path")
        }
        val digest = MessageDigest.getInstance(algorithm).digest(Files.readAllBytes(path)).hexDigestString()
        require(digest.equals(record.get(name).asString, ignoreCase = true)) { "Native artifact checksum mismatch: $path" }
        require(Files.size(path) == record.get("size").asLong) { "Native artifact size mismatch: $path" }
    }
}
