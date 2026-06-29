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

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.jetbrains.kotlin.cli.common.messages.MessageCollector

class KronosIrGenerationExtensionTest {
    @Test
    fun filePatternMatchingSupportsExactAndWildcardPatterns() {
        val extension = KronosIrGenerationExtension(
            messageCollector = MessageCollector.NONE,
            dumpIr = false,
            dumpIrPath = "build/tmp/kronosDebug",
            dumpIrMode = "common",
            dumpIrFiles = null,
        )

        assertTrue(extension.matches("User.kt", listOf("User.kt")))
        assertTrue(extension.matches("UserService.kt", listOf("*Service.kt")))
        assertTrue(extension.matches("OrderRepository.kt", listOf("User.kt", "*Repository.kt")))
        assertFalse(extension.matches("Order.kt", listOf("User.kt", "*Service.kt")))
    }

    @Test
    fun wildcardPatternEscapesDotsBeforeMatching() {
        val extension = KronosIrGenerationExtension(
            messageCollector = MessageCollector.NONE,
            dumpIr = false,
            dumpIrPath = "build/tmp/kronosDebug",
            dumpIrMode = "common",
            dumpIrFiles = null,
        )

        assertTrue(extension.matches("User.kt", listOf("*.kt")))
        assertFalse(extension.matches("UserXkt", listOf("*.kt")))
    }

    private fun KronosIrGenerationExtension.matches(fileName: String, patterns: List<String>): Boolean {
        val method = KronosIrGenerationExtension::class.java.getDeclaredMethod(
            "matchesAnyPattern",
            String::class.java,
            List::class.java,
        )
        method.isAccessible = true
        return method.invoke(this, fileName, patterns) as Boolean
    }
}
