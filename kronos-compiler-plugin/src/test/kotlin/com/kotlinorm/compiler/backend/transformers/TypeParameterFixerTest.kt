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

package com.kotlinorm.compiler.backend.transformers

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import com.kotlinorm.compiler.utils.isSelectFromQueryFunctionFqName
import org.jetbrains.kotlin.name.FqName

class TypeParameterFixerTest {
    @Test
    fun `shouldFix matches direct typed query functions`() {
        assertTrue(FqName("com.kotlinorm.beans.task.KronosQueryTask.queryList").shouldFix())
        assertTrue(FqName("com.kotlinorm.beans.task.KronosQueryTask.queryOne").shouldFix())
        assertTrue(FqName("com.kotlinorm.beans.task.KronosQueryTask.queryOneOrNull").shouldFix())
    }

    @Test
    fun `shouldFix matches select from typed query functions by arity`() {
        assertTrue(FqName("com.kotlinorm.orm.join.SelectFrom2.queryList").shouldFix())
        assertTrue(FqName("com.kotlinorm.orm.join.SelectFrom10.queryOne").shouldFix())
        assertTrue(FqName("com.kotlinorm.orm.join.SelectFrom16.queryOneOrNull").shouldFix())
    }

    @Test
    fun `shouldFix ignores untyped or unrelated functions`() {
        assertFalse(FqName("com.kotlinorm.orm.select.SelectClause.build").shouldFix())
        assertFalse(FqName("com.kotlinorm.orm.join.SelectFromMany.queryList").shouldFix())
        assertFalse(FqName("com.example.Query.queryList").shouldFix())
    }

    @Test
    fun `select from query parser accepts typed query names only`() {
        assertTrue(isSelectFromQueryFunctionFqName("com.kotlinorm.orm.join.SelectFrom2.queryList"))
        assertTrue(isSelectFromQueryFunctionFqName("com.kotlinorm.orm.join.SelectFrom16.queryOne"))
        assertTrue(isSelectFromQueryFunctionFqName("com.kotlinorm.orm.join.SelectFrom10.queryOneOrNull"))
        assertFalse(isSelectFromQueryFunctionFqName("com.kotlinorm.orm.join.SelectFrom.queryList"))
        assertFalse(isSelectFromQueryFunctionFqName("com.kotlinorm.orm.join.SelectFromX.queryList"))
        assertFalse(isSelectFromQueryFunctionFqName("com.kotlinorm.orm.join.SelectFrom2"))
        assertFalse(isSelectFromQueryFunctionFqName("com.kotlinorm.orm.join.SelectFrom2."))
        assertFalse(isSelectFromQueryFunctionFqName("com.kotlinorm.orm.join.SelectFrom2.build"))
        assertFalse(isSelectFromQueryFunctionFqName("com.kotlinorm.orm.select.SelectFrom2.queryList"))
    }
}
