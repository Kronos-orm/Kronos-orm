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

package com.kotlinorm.compiler.transformers

import com.kotlinorm.compiler.transformers.shouldFix
import com.kotlinorm.compiler.utils.KotlinSourceDynamicCompiler.compile
import com.kotlinorm.compiler.utils.testBaseName
import com.tschuchort.compiletesting.KotlinCompilation
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.name.FqName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCompilerApi::class)
class TypeParameterFixerTest {

    @Test
    fun `should return true for KronosQueryTask queryList`() {
        val fqName = FqName("com.kotlinorm.beans.task.KronosQueryTask.queryList")
        assertTrue(fqName.shouldFix())
    }

    @Test
    fun `should return true for KronosQueryTask queryOne`() {
        val fqName = FqName("com.kotlinorm.beans.task.KronosQueryTask.queryOne")
        assertTrue(fqName.shouldFix())
    }

    @Test
    fun `should return true for KronosQueryTask queryOneOrNull`() {
        val fqName = FqName("com.kotlinorm.beans.task.KronosQueryTask.queryOneOrNull")
        assertTrue(fqName.shouldFix())
    }

    @Test
    fun `should return true for SelectClause queryList`() {
        val fqName = FqName("com.kotlinorm.orm.select.SelectClause.queryList")
        assertTrue(fqName.shouldFix())
    }

    @Test
    fun `should return true for SelectClause queryOne`() {
        val fqName = FqName("com.kotlinorm.orm.select.SelectClause.queryOne")
        assertTrue(fqName.shouldFix())
    }

    @Test
    fun `should return true for SelectClause queryOneOrNull`() {
        val fqName = FqName("com.kotlinorm.orm.select.SelectClause.queryOneOrNull")
        assertTrue(fqName.shouldFix())
    }

    @Test
    fun `should return true for SqlHandler queryList`() {
        val fqName = FqName("com.kotlinorm.database.SqlHandler.queryList")
        assertTrue(fqName.shouldFix())
    }

    @Test
    fun `should return true for SqlHandler queryOne`() {
        val fqName = FqName("com.kotlinorm.database.SqlHandler.queryOne")
        assertTrue(fqName.shouldFix())
    }

    @Test
    fun `should return true for SqlHandler queryOneOrNull`() {
        val fqName = FqName("com.kotlinorm.database.SqlHandler.queryOneOrNull")
        assertTrue(fqName.shouldFix())
    }

    @Test
    fun `should return true for SelectFrom0 queryList`() {
        val fqName = FqName("com.kotlinorm.orm.join.SelectFrom0.queryList")
        assertTrue(fqName.shouldFix())
    }

    @Test
    fun `should return true for SelectFrom1 queryOne`() {
        val fqName = FqName("com.kotlinorm.orm.join.SelectFrom1.queryOne")
        assertTrue(fqName.shouldFix())
    }

    @Test
    fun `should return true for SelectFrom9 queryOneOrNull`() {
        val fqName = FqName("com.kotlinorm.orm.join.SelectFrom9.queryOneOrNull")
        assertTrue(fqName.shouldFix())
    }

    @Test
    fun `should return false for unrelated function`() {
        val fqName = FqName("com.kotlinorm.someOtherFunction")
        assertFalse(fqName.shouldFix())
    }

    @Test
    fun `should return false for partial match`() {
        val fqName = FqName("com.kotlinorm.orm.select.SelectClause.otherMethod")
        assertFalse(fqName.shouldFix())
    }
}