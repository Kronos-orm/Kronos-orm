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

import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.gradle.api.artifacts.Configuration
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GenerateKronosNativeMetadataOptionsTest {
    @Test
    fun configuringCrossProjectCompileInputsDoesNotResolveDependenciesOrRequireAnExistingOutput() {
        val directory = Files.createTempDirectory("kronos-native-options-lazy")
        try {
            val root = ProjectBuilder.builder().withProjectDir(directory.toFile()).build()
            val project = ProjectBuilder.builder().withParent(root).withName("shared")
                .withProjectDir(Files.createDirectories(directory.resolve("shared")).toFile()).build()
            var resolutions = 0
            val configuration = project.configurations.create("nativeManifests") {
                it.isCanBeResolved = true
                it.isCanBeConsumed = false
                it.incoming.beforeResolve { resolutions++ }
            }
            val location = project.layout.buildDirectory.file("options/entries.txt")
            val generator = project.tasks.register("generateOptions", GenerateKronosNativeMetadataOptions::class.java) {
                it.providerId.set("fixture:main")
                it.dependencyMetadata.from(configuration.incoming.artifactView { }.files)
                it.outputFile.set(location)
            }
            val output = generator.flatMap { it.outputFile }
            val compile = root.tasks.register("compileFixture") { it.inputs.file(output) }
            val option = nativeMetadataEntriesFileOption(location).get()
            assertEquals(NATIVE_PROVIDER_DEPENDENCY_ENTRIES_FILE_OPTION_NAME, option.key)
            assertEquals(location.get().asFile.absolutePath, option.value)
            assertEquals(Configuration.State.UNRESOLVED, configuration.state)
            assertEquals(0, resolutions)
            assertFalse(location.get().asFile.exists())
            assertTrue(generator.get() in compile.get().inputs.files.buildDependencies.getDependencies(compile.get()))
            assertEquals(0, resolutions)
        } finally { directory.toFile().deleteRecursively() }
    }

    @Test
    fun materializesTargetOnlyAndAssociatedEntriesInStableOrderWithoutItsOwnProvider() {
        val directory = Files.createTempDirectory("kronos-native-options")
        try {
            val project = ProjectBuilder.builder().withProjectDir(directory.toFile()).build()
            val first = archive(directory.resolve("target-only.jar"), "platform\tfixture.installPlatformDependencies\n")
            val second = archive(directory.resolve("common.jar"), "common\tfixture.installCommonDependencies\nfixture:main\tfixture.installOwnDependencies\n")
            val duplicate = archive(directory.resolve("diamond.jar"), "common\tfixture.installCommonDependencies\n")
            val task = project.tasks.create("generateOptions", GenerateKronosNativeMetadataOptions::class.java).apply {
                providerId.set("fixture:main")
                dependencyMetadata.from(first, second, duplicate)
                additionalEntries.set(listOf("fixture.installMainDependencies", "fixture.installCommonDependencies"))
                outputFile.set(directory.resolve("out/dependency-entries.txt").toFile())
            }
            task.generate()
            assertEquals("fixture.installCommonDependencies\nfixture.installMainDependencies\nfixture.installPlatformDependencies\n",
                task.outputFile.get().asFile.readText(Charsets.UTF_8))
            task.additionalEntries.set(listOf("fixture.installDifferentMainDependencies"))
            task.generate()
            assertEquals("fixture.installCommonDependencies\nfixture.installDifferentMainDependencies\nfixture.installPlatformDependencies\n",
                task.outputFile.get().asFile.readText(Charsets.UTF_8))
        } finally { directory.toFile().deleteRecursively() }
    }

    @Test
    fun noDependencyEntriesProduceAnExistingEmptyUtf8Input() {
        val directory = Files.createTempDirectory("kronos-native-options-empty")
        try {
            val project = ProjectBuilder.builder().withProjectDir(directory.toFile()).build()
            val task = project.tasks.create("generateOptions", GenerateKronosNativeMetadataOptions::class.java).apply {
                providerId.set("fixture:main")
                outputFile.set(directory.resolve("out/entries.txt").toFile())
            }
            task.generate()
            assertTrue(task.outputFile.get().asFile.isFile)
            assertEquals("", task.outputFile.get().asFile.readText(Charsets.UTF_8))
        } finally { directory.toFile().deleteRecursively() }
    }

    @Test
    fun malformedManifestOrAssociatedEntryFailsBeforeWritingCompilerInput() {
        val directory = Files.createTempDirectory("kronos-native-options-invalid")
        try {
            val project = ProjectBuilder.builder().withProjectDir(directory.toFile()).build()
            val task = project.tasks.create("generateOptions", GenerateKronosNativeMetadataOptions::class.java).apply {
                providerId.set("fixture:main")
                outputFile.set(directory.resolve("out/entries.txt").toFile())
            }
            task.dependencyMetadata.from(archive(directory.resolve("invalid.jar"), "missing-entry\n"))
            assertFailsWith<IllegalArgumentException> { task.generate() }
            assertFalse(task.outputFile.get().asFile.exists())
            task.dependencyMetadata.setFrom(emptyList<java.io.File>())
            task.additionalEntries.set(listOf("fixture.install();error(1)"))
            assertFailsWith<IllegalArgumentException> { task.generate() }
            assertFalse(task.outputFile.get().asFile.exists())
        } finally { directory.toFile().deleteRecursively() }
    }

    private fun archive(path: Path, text: String): java.io.File = path.toFile().also { file ->
        ZipOutputStream(file.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry(NATIVE_METADATA_MANIFEST_PATH))
            zip.write(text.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
    }
}
