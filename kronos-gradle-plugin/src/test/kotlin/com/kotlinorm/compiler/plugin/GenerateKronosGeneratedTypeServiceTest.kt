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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import org.gradle.testfixtures.ProjectBuilder

class GenerateKronosGeneratedTypeServiceTest {
    @Test
    fun writesSortedLiveSourceProviderFragmentsAndRemovesDeletedEntries() {
        val projectDir = Files.createTempDirectory("kronos-generated-service-test").toFile()
        val project = ProjectBuilder.builder().withProjectDir(projectDir).build()
        val classDir = projectDir.resolve("classes")
        val outputDir = projectDir.resolve("generated")
        val providerClassNamePrefix = "com.kotlinorm.generated.factory.KronosGeneratedTypeProvider_test"
        val serviceFile = outputDir.resolve(GENERATED_TYPE_PROVIDER_SERVICE_PATH)
        val task = project.tasks.create(
            "generateKronosTestGeneratedTypeService",
            GenerateKronosGeneratedTypeService::class.java
        ).apply {
            this.providerClassNamePrefix.set(providerClassNamePrefix)
            providerClassDirectories.from(classDir)
            outputDirectory.set(outputDir)
        }

        task.generate()
        assertFalse(serviceFile.exists())

        val secondFragment = classDir.resolve("${providerClassNamePrefix.replace('.', '/')}_bbbbbbbbbbbbbbbb.class")
        val firstFragment = classDir.resolve("${providerClassNamePrefix.replace('.', '/')}_aaaaaaaaaaaaaaaa.class")
        listOf(secondFragment, firstFragment).forEach { providerClassFile ->
            providerClassFile.parentFile.mkdirs()
            providerClassFile.writeBytes(byteArrayOf())
        }
        // Compiler-generated lambda classes must not become service providers.
        classDir.resolve("${providerClassNamePrefix.replace('.', '/')}_aaaaaaaaaaaaaaaa\$contributeTo\$1.class")
            .writeBytes(byteArrayOf())

        task.generate()
        assertEquals(
            "${providerClassNamePrefix}_aaaaaaaaaaaaaaaa\n${providerClassNamePrefix}_bbbbbbbbbbbbbbbb\n",
            serviceFile.readText()
        )

        assertEquals(true, secondFragment.delete())
        task.generate()
        assertEquals("${providerClassNamePrefix}_aaaaaaaaaaaaaaaa\n", serviceFile.readText())

        assertEquals(true, firstFragment.delete())
        task.generate()
        assertFalse(serviceFile.exists())
    }
}
