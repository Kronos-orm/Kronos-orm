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
    fun writesServiceOnlyWhenTheGeneratedProviderClassExists() {
        val projectDir = Files.createTempDirectory("kronos-generated-service-test").toFile()
        val project = ProjectBuilder.builder().withProjectDir(projectDir).build()
        val classDir = projectDir.resolve("classes")
        val outputDir = projectDir.resolve("generated")
        val providerClassName = "com.kotlinorm.generated.factory.KronosGeneratedTypeProvider_test"
        val serviceFile = outputDir.resolve(GENERATED_TYPE_PROVIDER_SERVICE_PATH)
        val task = project.tasks.create(
            "generateKronosTestGeneratedTypeService",
            GenerateKronosGeneratedTypeService::class.java
        ).apply {
            this.providerClassName.set(providerClassName)
            serviceContent.set("$providerClassName\n")
            providerClassDirectories.from(classDir)
            outputDirectory.set(outputDir)
        }

        task.generate()
        assertFalse(serviceFile.exists())

        val providerClassFile = classDir.resolve(providerClassName.replace('.', '/') + ".class")
        providerClassFile.parentFile.mkdirs()
        providerClassFile.writeBytes(byteArrayOf())

        task.generate()
        assertEquals("$providerClassName\n", serviceFile.readText())
    }
}
