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

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NativeBuildCatalogTest {
    @Test
    fun nativeDependenciesUseRootCatalogVersionsAndRejectUnknownAliases() {
        val file = Files.createTempFile("native-catalog", ".toml")
        try {
            file.toFile().writeText("""
                [versions]
                kotlin = "2.4.21"
                datetime = "0.8.0"
                [libraries]
                ktx-datetime = { module = "org.jetbrains.kotlinx:kotlinx-datetime", version.ref = "datetime" }
                kronos-compiler-plugin = { module = "com.kotlinorm:kronos-compiler-plugin", version = "0.4.0-SNAPSHOT" }
            """.trimIndent())
            val catalog = NativeBuildCatalog(file)
            assertEquals("2.4.21", catalog.kotlinVersion)
            assertEquals("org.jetbrains.kotlinx:kotlinx-datetime:0.8.0", catalog.library("ktx-datetime").notation)
            assertEquals("com/kotlinorm/kronos-compiler-plugin/0.4.0-SNAPSHOT/kronos-compiler-plugin-0.4.0-SNAPSHOT",
                catalog.library("kronos-compiler-plugin").path)
            assertFailsWith<IllegalArgumentException> { catalog.library("missing") }
        } finally { Files.deleteIfExists(file) }
    }
}
