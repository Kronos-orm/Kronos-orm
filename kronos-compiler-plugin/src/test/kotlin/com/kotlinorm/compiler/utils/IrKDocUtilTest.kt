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

package com.kotlinorm.compiler.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class IrKDocUtilTest {

    // ========================================================================
    // realStartOffset tests
    // ========================================================================

    @Test
    fun `realStartOffset returns same offset when no annotations or comments`() {
        val lines = listOf(
            "class User {",
            "    val name: String = \"\"",
            "}"
        )
        assertEquals(0, realStartOffset(lines, 0))
    }

    @Test
    fun `realStartOffset skips single annotation`() {
        val lines = listOf(
            "@Entity",
            "class User {"
        )
        assertEquals(1, realStartOffset(lines, 0))
    }

    @Test
    fun `realStartOffset skips multiple annotations`() {
        val lines = listOf(
            "@Entity",
            "@Table(name = \"user\")",
            "class User {"
        )
        assertEquals(2, realStartOffset(lines, 0))
    }

    @Test
    fun `realStartOffset skips single-line comment`() {
        val lines = listOf(
            "// This is a comment",
            "class User {"
        )
        assertEquals(1, realStartOffset(lines, 0))
    }

    @Test
    fun `realStartOffset skips multi-line comment`() {
        val lines = listOf(
            "/* start of comment",
            " * middle line",
            " end of comment */",
            "class User {"
        )
        assertEquals(3, realStartOffset(lines, 0))
    }

    @Test
    fun `realStartOffset skips annotations and comments mixed`() {
        val lines = listOf(
            "// A comment",
            "@Entity",
            "@Table(name = \"user\")",
            "class User {"
        )
        assertEquals(3, realStartOffset(lines, 0))
    }

    @Test
    fun `realStartOffset respects startOffset parameter`() {
        val lines = listOf(
            "package com.example",
            "",
            "@Entity",
            "class User {"
        )
        // Starting at line 2 which is @Entity, should skip to line 3
        assertEquals(3, realStartOffset(lines, 2))
    }

    @Test
    fun `realStartOffset returns startOffset when line is plain code`() {
        val lines = listOf(
            "package com.example",
            "",
            "class User {"
        )
        // Starting at line 2 which is plain code, returns 2
        assertEquals(2, realStartOffset(lines, 2))
    }

    @Test
    fun `realStartOffset skips annotation after multi-line comment`() {
        val lines = listOf(
            "/* comment */",
            "@Entity",
            "class User {"
        )
        assertEquals(2, realStartOffset(lines, 0))
    }

    // ========================================================================
    // extractDeclarationComment tests
    // ========================================================================

    @Test
    fun `extractDeclarationComment returns single-line comment within range`() {
        val lines = listOf(
            "// This is a user class",
            "class User {"
        )
        val comment = extractDeclarationComment(lines, 0..1)
        assertEquals("This is a user class", comment)
    }

    @Test
    fun `extractDeclarationComment returns inline single-line KDoc within range`() {
        val lines = listOf(
            "/** User entity */",
            "class User {"
        )
        val comment = extractDeclarationComment(lines, 0..1)
        assertEquals("User entity", comment)
    }

    @Test
    fun `extractDeclarationComment returns null when no comment in range or above`() {
        val lines = listOf(
            "class User {"
        )
        val comment = extractDeclarationComment(lines, 0..0)
        assertNull(comment)
    }

    @Test
    fun `extractDeclarationComment searches upward when no comment in range`() {
        val lines = listOf(
            "// User entity",
            "class User {",
            "    val name: String"
        )
        // Range is just the property line; comment is above
        val comment = extractDeclarationComment(lines, 2..2)
        assertNull(comment) // line 1 is "class User {" which is not blank/comment, so search stops
    }

    @Test
    fun `extractDeclarationComment finds comment above skipping annotations`() {
        val lines = listOf(
            "// User entity",
            "@Entity",
            "class User {"
        )
        val comment = extractDeclarationComment(lines, 2..2)
        assertEquals("User entity", comment)
    }

    @Test
    fun `extractDeclarationComment finds multi-line comment above`() {
        val lines = listOf(
            "/** User entity */",
            "class User {"
        )
        val comment = extractDeclarationComment(lines, 1..1)
        assertEquals("User entity", comment)
    }

    @Test
    fun `extractDeclarationComment handles multi-line block comment spanning lines above`() {
        val lines = listOf(
            "/* User",
            " * entity */",
            "class User {"
        )
        val comment = extractDeclarationComment(lines, 2..2)
        // The function concatenates without separator: "User" + "entity" = "Userentity"
        assertEquals("Userentity", comment)
    }

    @Test
    fun `extractDeclarationComment prefers in-range comment over upward search`() {
        val lines = listOf(
            "// Above comment",
            "// In-range comment",
            "val name: String"
        )
        val comment = extractDeclarationComment(lines, 1..2)
        assertEquals("In-range comment", comment)
    }

    @Test
    fun `extractDeclarationComment handles empty lines list gracefully`() {
        val lines = emptyList<String>()
        val comment = extractDeclarationComment(lines, 0..0)
        assertNull(comment)
    }

    @Test
    fun `extractDeclarationComment with only annotations above returns null`() {
        val lines = listOf(
            "@Entity",
            "@Table",
            "class User {"
        )
        // Range is line 2, searching upward finds only annotations then nothing
        val comment = extractDeclarationComment(lines, 2..2)
        assertNull(comment)
    }
}
