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
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

internal const val GENERATED_TYPE_PROVIDER_SERVICE_PATH =
    "META-INF/services/com.kotlinorm.utils.GeneratedTypeProvider"

/**
 * Builds a deterministic service descriptor from all live source-owned provider fragments for one
 * JVM or Android compilation. Kotlin's incremental output mapping owns deletion of classes emitted
 * for deleted or renamed source files, so scanning the compilation output removes stale service
 * entries without maintaining a second mutable manifest.
 */
@CacheableTask
abstract class GenerateKronosGeneratedTypeService : DefaultTask() {
    @get:Input
    abstract val providerClassNamePrefix: org.gradle.api.provider.Property<String>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val providerClassDirectories: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val outputFile = outputDirectory.file(GENERATED_TYPE_PROVIDER_SERVICE_PATH).get().asFile
        val providerClassNames = providerClassDirectories.files
            .asSequence()
            .filter { it.isDirectory }
            .flatMap { directory -> directory.walkTopDown().asSequence().map { directory to it } }
            .mapNotNull { (directory, file) ->
                file.takeIf { it.isFile && it.extension == "class" }
                    ?.relativeTo(directory)
                    ?.invariantSeparatorsPath
                    ?.removeSuffix(".class")
                    ?.replace('/', '.')
            }
            .filter { it.isSourceProviderFragment(providerClassNamePrefix.get()) }
            .distinct()
            .sorted()
            .toList()
        if (providerClassNames.isEmpty()) {
            outputFile.delete()
            return
        }
        outputFile.parentFile.mkdirs()
        outputFile.writeText(providerClassNames.joinToString(separator = "\n", postfix = "\n"))
    }
}

private fun String.isSourceProviderFragment(providerClassNamePrefix: String): Boolean {
    val prefix = "${providerClassNamePrefix}_"
    if (!startsWith(prefix)) return false
    val sourceHash = removePrefix(prefix)
    return sourceHash.length == GENERATED_PROVIDER_SOURCE_HASH_LENGTH &&
        sourceHash.all { it in '0'..'9' || it in 'a'..'f' }
}

private const val GENERATED_PROVIDER_SOURCE_HASH_LENGTH = 16
