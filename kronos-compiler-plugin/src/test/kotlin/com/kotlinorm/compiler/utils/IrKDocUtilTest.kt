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
        val lines = [
            "class User {",
            "    val name: String = \"\"",
            "}"
        ]
        assertEquals(0, realStartOffset(lines, 0))
    }

    @Test
    fun `realStartOffset skips single annotation`() {
        val lines = [
            "@Entity",
            "class User {"
        ]
        assertEquals(1, realStartOffset(lines, 0))
    }

    @Test
    fun `realStartOffset skips multiple annotations`() {
        val lines = [
            "@Entity",
            "@Table(name = \"user\")",
            "class User {"
        ]
        assertEquals(2, realStartOffset(lines, 0))
    }

    @Test
    fun `realStartOffset skips single-line comment`() {
        val lines = [
            "// This is a comment",
            "class User {"
        ]
        assertEquals(1, realStartOffset(lines, 0))
    }

    @Test
    fun `realStartOffset skips multi-line comment`() {
        val lines = [
            "/* start of comment",
            " * middle line",
            " end of comment */",
            "class User {"
        ]
        assertEquals(3, realStartOffset(lines, 0))
    }

    @Test
    fun `realStartOffset skips annotations and comments mixed`() {
        val lines = [
            "// A comment",
            "@Entity",
            "@Table(name = \"user\")",
            "class User {"
        ]
        assertEquals(3, realStartOffset(lines, 0))
    }

    @Test
    fun `realStartOffset respects startOffset parameter`() {
        val lines = [
            "package com.example",
            "",
            "@Entity",
            "class User {"
        ]
        // Starting at line 2 which is @Entity, should skip to line 3
        assertEquals(3, realStartOffset(lines, 2))
    }

    @Test
    fun `realStartOffset returns startOffset when line is plain code`() {
        val lines = [
            "package com.example",
            "",
            "class User {"
        ]
        // Starting at line 2 which is plain code, returns 2
        assertEquals(2, realStartOffset(lines, 2))
    }

    @Test
    fun `realStartOffset skips annotation after multi-line comment`() {
        val lines = [
            "/* comment */",
            "@Entity",
            "class User {"
        ]
        assertEquals(2, realStartOffset(lines, 0))
    }

    @Test
    fun `realStartOffset skips multi-line comment followed by line comment and annotation`() {
        val lines = [
            "/* comment starts",
            " * comment middle",
            " */",
            "// generated marker",
            "@Entity",
            "class User {"
        ]
        assertEquals(5, realStartOffset(lines, 0))
    }

    // ========================================================================
    // extractDeclarationComment tests
    // ========================================================================

    @Test
    fun `extractDeclarationComment returns single-line comment within range`() {
        val lines = [
            "// This is a user class",
            "class User {"
        ]
        val comment = extractDeclarationComment(lines, 0..1)
        assertEquals("This is a user class", comment)
    }

    @Test
    fun `extractDeclarationComment returns inline single-line KDoc within range`() {
        val lines = [
            "/** User entity */",
            "class User {"
        ]
        val comment = extractDeclarationComment(lines, 0..1)
        assertEquals("User entity", comment)
    }

    @Test
    fun `extractDeclarationComment returns null when no comment in range or above`() {
        val lines = [
            "class User {"
        ]
        val comment = extractDeclarationComment(lines, 0..0)
        assertNull(comment)
    }

    @Test
    fun `extractDeclarationComment searches upward when no comment in range`() {
        val lines = [
            "// User entity",
            "class User {",
            "    val name: String"
        ]
        // Range is just the property line; comment is above
        val comment = extractDeclarationComment(lines, 2..2)
        assertNull(comment) // line 1 is "class User {" which is not blank/comment, so search stops
    }

    @Test
    fun `extractDeclarationComment finds comment above skipping annotations`() {
        val lines = [
            "// User entity",
            "@Entity",
            "class User {"
        ]
        val comment = extractDeclarationComment(lines, 2..2)
        assertEquals("User entity", comment)
    }

    @Test
    fun `extractDeclarationComment finds multi-line comment above`() {
        val lines = [
            "/** User entity */",
            "class User {"
        ]
        val comment = extractDeclarationComment(lines, 1..1)
        assertEquals("User entity", comment)
    }

    @Test
    fun `extractDeclarationComment handles multi-line block comment spanning lines above`() {
        val lines = [
            "/* User",
            " * entity */",
            "class User {"
        ]
        val comment = extractDeclarationComment(lines, 2..2)
        // The function concatenates without separator: "User" + "entity" = "Userentity"
        assertEquals("Userentity", comment)
    }

    @Test
    fun `extractDeclarationComment prefers in-range comment over upward search`() {
        val lines = [
            "// Above comment",
            "// In-range comment",
            "val name: String"
        ]
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
        val lines = [
            "@Entity",
            "@Table",
            "class User {"
        ]
        // Range is line 2, searching upward finds only annotations then nothing
        val comment = extractDeclarationComment(lines, 2..2)
        assertNull(comment)
    }

    @Test
    fun `extractDeclarationComment joins contiguous single-line comments above`() {
        val lines = [
            "// First ",
            "// Second",
            "@Column",
            "val name: String"
        ]
        val comment = extractDeclarationComment(lines, 3..3)
        assertEquals("FirstSecond", comment)
    }

    @Test
    fun `extractDeclarationComment reads block comment body above declaration`() {
        val lines = [
            "/* First",
            " * Second",
            " */",
            "@Column",
            "val name: String"
        ]
        val comment = extractDeclarationComment(lines, 4..4)
        assertEquals("FirstSecond", comment)
    }

    @Test
    fun `extractDeclarationComment skips missing range lines safely`() {
        val lines = [
            "// Existing"
        ]
        val comment = extractDeclarationComment(lines, 3..5)
        assertEquals("Existing", comment)
    }

    @Test
    fun `extractDeclarationComment searches above blank lines`() {
        val lines = [
            "// Existing",
            "",
            "@Column",
            "val name: String"
        ]
        val comment = extractDeclarationComment(lines, 3..3)
        assertEquals("Existing", comment)
    }

    @Test
    fun `extractDeclarationComment returns inline block comment before trailing text`() {
        val lines = [
            "/* Existing */ val name: String"
        ]
        val comment = extractDeclarationComment(lines, 0..0)
        assertEquals("Existing", comment)
    }

    @Test
    fun `extractDeclarationComment stops upward search at code`() {
        val lines = [
            "// Existing",
            "val other: String",
            "val name: String"
        ]
        val comment = extractDeclarationComment(lines, 2..2)
        assertNull(comment)
    }

    @Test
    fun `extractDeclarationComment ignores unterminated block marker inside range`() {
        val lines = [
            "/* Existing",
            "val name: String"
        ]
        val comment = extractDeclarationComment(lines, 0..1)
        assertNull(comment)
    }

    @Test
    fun `extractDeclarationComment skips blank lines while searching above`() {
        val lines = [
            "// Existing",
            "",
            "",
            "val name: String"
        ]
        val comment = extractDeclarationComment(lines, 3..3)
        assertEquals("Existing", comment)
    }

    @Test
    fun `extractDeclarationComment reads multi-line body with blank middle line`() {
        val lines = [
            "/* First",
            "",
            " * Second",
            " */",
            "val name: String"
        ]
        val comment = extractDeclarationComment(lines, 4..4)
        assertEquals("FirstSecond", comment)
    }

    @Test
    fun `extractDeclarationComment continues single-line scan to block comment above`() {
        val lines = [
            "/* Block */",
            "// Nearby",
            "val name: String"
        ]
        val comment = extractDeclarationComment(lines, 2..2)
        assertEquals("Block", comment)
    }
}
