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
import org.gradle.api.provider.Property
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
 * Adds the service descriptor only when this compilation emitted its generated provider class.
 * The task is shared by JVM source sets and Android variants so their resource metadata always
 * uses the same identity passed to the compiler plugin.
 */
@CacheableTask
abstract class GenerateKronosGeneratedTypeService : DefaultTask() {
    @get:Input
    abstract val providerClassName: Property<String>

    @get:Input
    abstract val serviceContent: Property<String>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val providerClassDirectories: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val outputFile = outputDirectory.file(GENERATED_TYPE_PROVIDER_SERVICE_PATH).get().asFile
        val providerClassFile = providerClassName.get().replace('.', '/') + ".class"
        val providerExists = providerClassDirectories.files.any { directory ->
            directory.resolve(providerClassFile).isFile
        }
        if (!providerExists) {
            outputFile.delete()
            return
        }
        outputFile.parentFile.mkdirs()
        outputFile.writeText(serviceContent.get())
    }
}
