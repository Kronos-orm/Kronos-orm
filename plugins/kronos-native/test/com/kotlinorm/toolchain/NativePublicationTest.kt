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

import com.google.gson.JsonParser
import kotlin.test.Test
import kotlin.test.assertEquals

class NativePublicationTest {
    @Test
    fun nativePomRewritesOwnAndTransitiveLeafCoordinates() {
        val source = """
            <project><artifactId>kronos-core-macosx64</artifactId><dependencies>
            <dependency><artifactId>kronos-syntax-macosx64</artifactId></dependency>
            <dependency><artifactId>kotlinx-datetime-macosx64</artifactId></dependency>
            <dependency><artifactId>kotlin-stdlib</artifactId></dependency>
            </dependencies></project>
        """.trimIndent()
        val expected = source.replace("-macosx64</artifactId>", "-watchosx64</artifactId>")
        assertEquals(expected, rewriteNativePom(source, "macosX64"))
    }

    @Test
    fun bridgeToolingRetainsExistingTargetsAndCompilerAbi() {
        val source = JsonParser.parseString("""{
            "projectTargets": [
                {"platformType":"jvm","extras":{"jvm":{"jvmTarget":"1.8"}}},
                {"platformType":"native","extras":{"native":{"konanTarget":"macos_x64","konanVersion":"2.4.21","konanAbiVersion":"2.4.0"}}}
            ]
        }""").asJsonObject
        appendWatchToolingTarget(source, "macosX64")
        val expected = JsonParser.parseString("""{
            "projectTargets": [
                {"platformType":"jvm","extras":{"jvm":{"jvmTarget":"1.8"}}},
                {"platformType":"native","extras":{"native":{"konanTarget":"macos_x64","konanVersion":"2.4.21","konanAbiVersion":"2.4.0"}}},
                {"platformType":"native","extras":{"native":{"konanTarget":"watchos_x64","konanVersion":"2.4.21","konanAbiVersion":"2.4.0"}}}
            ]
        }""")
        assertEquals(expected, source)
    }

}
