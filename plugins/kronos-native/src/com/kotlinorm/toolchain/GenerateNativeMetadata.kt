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

import java.nio.file.Path
import org.jetbrains.amper.plugins.Input
import org.jetbrains.amper.plugins.Output
import org.jetbrains.amper.plugins.TaskAction

@TaskAction
fun generateNativeMetadata(
    moduleName: String,
    group: String?,
    generateMetadata: Boolean,
    jvmSourceSetPrefix: String,
    @Input dependencyMetadata: List<Path>,
    @Output mainSources: Path,
    @Output testSources: Path,
    @Output resources: Path,
    @Output jvmMainResources: Path,
    @Output jvmTestResources: Path
) {
    for (directory in listOf(mainSources, testSources, resources, jvmMainResources, jvmTestResources)) {
        directory.toFile().apply { deleteRecursively(); mkdirs() }
    }
    if (!generateMetadata) return
    val main = nativeProviderIdentity(group, moduleName, "commonMain")
    val test = nativeProviderIdentity(group, moduleName, "commonTest")
    val entry = NativeMetadataManifestEntry(main.id, main.entryFqName + "Dependencies")
    validateEntry(entry)
    validateEntry(NativeMetadataManifestEntry(test.id, test.entryFqName))
    writeNativeMetadataEntry(main.entryFqName, main.id, emptyList(), dependencyMetadata, mainSources)
    writeNativeMetadataEntry(test.entryFqName, test.id,
        listOf(main.entryFqName + "Dependencies"), dependencyMetadata, testSources)
    resources.resolve(NATIVE_METADATA_MANIFEST_PATH).toFile().apply {
        parentFile.mkdirs()
        writeText("${entry.providerId}\t${entry.entryFqName}\n", Charsets.UTF_8)
    }
    require(jvmSourceSetPrefix in setOf("", "jvm")) { "Unsupported JVM source-set prefix: $jvmSourceSetPrefix" }
    for ((suffix, directory) in listOf("Main" to jvmMainResources, "Test" to jvmTestResources)) {
        val scope = if (jvmSourceSetPrefix.isEmpty()) suffix.lowercase() else jvmSourceSetPrefix + suffix
        val provider = gradleGeneratedTypeProviderIdentity("gradle:${requireNotNull(group)}:$moduleName::$moduleName:$scope").fqName
        directory.resolve("META-INF/services/com.kotlinorm.utils.GeneratedTypeProvider").toFile().apply {
            parentFile.mkdirs()
            writeText("$provider\n", Charsets.UTF_8)
        }
    }
}
