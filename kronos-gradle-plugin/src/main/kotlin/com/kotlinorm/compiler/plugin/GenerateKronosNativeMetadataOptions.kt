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

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.plugin.FilesSubpluginOption
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

/** Materializes Native dependency symbols during task execution, never while KGP configures options. */
@CacheableTask
abstract class GenerateKronosNativeMetadataOptions : DefaultTask() {
    @get:Input
    abstract val providerId: Property<String>

    @get:Input
    abstract val additionalEntries: ListProperty<String>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val dependencyMetadata: ConfigurableFileCollection

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    init { additionalEntries.convention(emptyList()) }

    @TaskAction
    fun generate() {
        val dependencies = readNativeMetadataManifests(dependencyMetadata.files)
            .filter { it.providerId != providerId.get() }.map { it.entryFqName }
        val entries = (dependencies + additionalEntries.get()).distinct().sorted()
        entries.forEach { entry ->
            require(entry.matches(EntryFqName)) { "Invalid Kronos Native metadata entry: $entry" }
        }
        outputFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText(entries.joinToString("\n", postfix = if (entries.isEmpty()) "" else "\n"), Charsets.UTF_8)
        }
    }
}

private val EntryFqName = Regex("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)+")

/** KGP may read this during configuration; it only observes a producer's output location. */
internal fun nativeMetadataEntriesFileOption(file: Provider<RegularFile>): Provider<SubpluginOption> = file.map {
    FilesSubpluginOption(NATIVE_PROVIDER_DEPENDENCY_ENTRIES_FILE_OPTION_NAME, listOf(it.asFile))
}
