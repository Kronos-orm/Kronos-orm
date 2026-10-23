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

package com.kotlinorm.compiler.plugin

import java.io.File
import java.util.zip.ZipFile
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

internal const val NATIVE_METADATA_MANIFEST_PATH = "META-INF/kronos/native-metadata-v1.tsv"

internal data class NativeMetadataManifestEntry(val providerId: String, val entryFqName: String)

/** One common module contribution, embedded in the standard KMP metadata publication. */
@CacheableTask
abstract class GenerateKronosNativeMetadataManifest : DefaultTask() {
    @get:Input abstract val providerId: Property<String>
    @get:Input abstract val entryFqName: Property<String>
    @get:OutputFile abstract val outputFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val entry = NativeMetadataManifestEntry(providerId.get(), entryFqName.get())
        validateEntry(entry)
        outputFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText("${entry.providerId}\t${entry.entryFqName}\n", Charsets.UTF_8)
        }
    }
}

internal fun readNativeMetadataManifests(files: Set<File>): List<NativeMetadataManifestEntry> {
    val entries = files.sortedBy(File::getName).flatMap { file ->
        val text = when {
            file.isDirectory -> file.resolve(NATIVE_METADATA_MANIFEST_PATH)
                .takeIf(File::isFile)?.readText(Charsets.UTF_8)
            file.extension in setOf("jar", "zip", "klib") && file.isFile -> ZipFile(file).use { archive ->
                archive.getEntry(NATIVE_METADATA_MANIFEST_PATH)?.let { entry ->
                    archive.getInputStream(entry).bufferedReader(Charsets.UTF_8).use { it.readText() }
                }
            }
            else -> null
        }
        text.orEmpty().lineSequence().filter(String::isNotBlank).map { line ->
            val fields = line.split('\t')
            require(fields.size == 2) { "Malformed Kronos Native metadata manifest in ${file.name}" }
            NativeMetadataManifestEntry(fields[0], fields[1]).also(::validateEntry)
        }.toList()
    }
    return entries.groupBy { it.providerId }.map { (id, candidates) ->
        require(candidates.map { it.entryFqName }.distinct().size == 1) {
            "Conflicting Kronos Native metadata entries for provider '$id'"
        }
        candidates.first()
    }.sortedBy { it.providerId }.also { unique ->
        require(unique.map { it.entryFqName }.distinct().size == unique.size) {
            "Conflicting Kronos Native metadata provider identities share a registration entry"
        }
    }
}

private fun validateEntry(entry: NativeMetadataManifestEntry) {
    require(entry.providerId.isNotBlank() && entry.providerId.none { it == '\t' || it == '\n' || it == '\r' }) {
        "Invalid Kronos Native metadata provider id"
    }
    require(entry.entryFqName.matches(Regex("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)+"))) {
        "Invalid Kronos Native metadata entry: ${entry.entryFqName}"
    }
}
