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

import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

private const val SDK_CONNECT_TIMEOUT_MILLIS = 30_000
private const val SDK_READ_TIMEOUT_MILLIS = 300_000
private const val CHECKSUM_BUFFER_BYTES = 65_536

/** Official v2.4.21 release asset digests: https://api.github.com/repos/JetBrains/kotlin/releases/tags/v2.4.21. */
private val compilerChecksums = mapOf("2.4.21" to mapOf(
    "x86_64" to "741799f163ad240f7fe54fae5334490c5a8a8fca9b9e9d39495db497b4f1763c",
    "aarch64" to "bf4edb526d91e17d0862102f69250525d788e79011f86c2c64362749adf260ae"
))

/** Keeps the fallback SDK under the compilation's declared output, without depending on private Toolchain cache paths. */
internal fun provisionKotlinNativeCompiler(directory: Path, version: String): Path {
    require(System.getProperty("os.name").startsWith("Mac")) { "watchos_x64 requires a macOS build host" }
    val architecture = when (System.getProperty("os.arch")) {
        "x86_64", "amd64" -> "x86_64"
        "aarch64", "arm64" -> "aarch64"
        else -> error("Unsupported macOS compiler host architecture: ${System.getProperty("os.arch")}")
    }
    val checksum = requireNotNull(compilerChecksums[version]) { "Add verified official SDK digests for Kotlin $version" }.getValue(architecture)
    val compiler = directory.resolve("bin/konanc")
    val marker = directory.resolve(".kronos-distribution-sha256").toFile()
    if (Files.isRegularFile(compiler) && marker.isFile && marker.readText().trim() == checksum) return compiler
    Files.createDirectories(directory.parent)
    val name = "kotlin-native-prebuilt-$version-macos-$architecture.tar.gz"
    val archive = directory.parent.resolve(name)
    if (!Files.isRegularFile(archive) || archiveChecksum(archive) != checksum) {
        val download = archive.resolveSibling("$name.part")
        Files.deleteIfExists(download)
        val connection = URI("https://github.com/JetBrains/kotlin/releases/download/v$version/$name").toURL().openConnection()
        connection.connectTimeout = SDK_CONNECT_TIMEOUT_MILLIS
        connection.readTimeout = SDK_READ_TIMEOUT_MILLIS
        connection.setRequestProperty("User-Agent", "Kronos-Kotlin-Toolchain")
        try {
            connection.getInputStream().use { input -> Files.newOutputStream(download).use { output -> input.copyTo(output) } }
            check(archiveChecksum(download) == checksum) { "Kotlin/Native $version SDK checksum mismatch" }
            Files.move(download, archive, StandardCopyOption.REPLACE_EXISTING)
        } finally {
            Files.deleteIfExists(download)
        }
    }
    directory.toFile().apply { deleteRecursively(); mkdirs() }
    check(ProcessBuilder("tar", "-xzf", archive.toString(), "--strip-components", "1", "-C", directory.toString())
        .inheritIO().start().waitFor() == 0) { "Cannot unpack Kotlin/Native $version SDK" }
    check(Files.isRegularFile(compiler)) { "Kotlin/Native distribution does not contain bin/konanc" }
    marker.writeText("$checksum\n")
    return compiler
}

private fun archiveChecksum(path: Path): String {
    val digest = MessageDigest.getInstance("SHA-256")
    Files.newInputStream(path).use { input ->
        val buffer = ByteArray(CHECKSUM_BUFFER_BYTES)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().hexDigestString()
}
