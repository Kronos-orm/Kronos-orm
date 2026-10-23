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

@file:OptIn(com.kotlinorm.annotations.InternalKronosApi::class)

package com.kotlinorm.utils

import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.enums.DBType
import com.kotlinorm.interfaces.KronosActionExecutionPlan
import com.kotlinorm.interfaces.KronosBatchExecutionPlan
import com.kotlinorm.interfaces.KronosDatabaseIdentity
import com.kotlinorm.interfaces.KronosExecutionDataSource
import com.kotlinorm.interfaces.KronosExecutionFirstResult
import com.kotlinorm.interfaces.KronosPhysicalRow
import com.kotlinorm.interfaces.KronosPhysicalRowMapper
import com.kotlinorm.interfaces.KronosPhysicalValue
import com.kotlinorm.interfaces.KronosQueryExecutionPlan
import com.kotlinorm.interfaces.KronosTransactionOptions
import com.kotlinorm.interfaces.KronosTransactionScope
import com.kotlinorm.wrappers.KronosExecutionWrapper
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NativeRawRowEnumTest {
    @Test
    fun rawRowEnumUsesExplicitlyInitializedFactoriesWithoutColumnMetadata() {
        initializeGeneratedTestMetadata()
        val untypedResult = typeOf<Any?>()
        val task = KronosAtomicQueryTask("SELECT status FROM example", targetType = untypedResult)
        assertNull(task.targetTypeInfo)
        assertTrue(task.resultColumns.isEmpty())
        val wrapper = KronosExecutionWrapper(EnumRowSource())
        assertEquals(listOf(RawRowStatus.READY), wrapper.toList(task) { row -> row.get<RawRowStatus>(1) })
    }
}

internal enum class RawRowStatus { READY }

private class EnumRowSource : KronosExecutionDataSource {
    override val identity = KronosDatabaseIdentity(DBType.SQLite, ":memory:")

    override fun <T> toList(plan: KronosQueryExecutionPlan, mapper: KronosPhysicalRowMapper<T>): List<T> {
        var active = true
        val row = object : KronosPhysicalRow {
            override val rowNumber = 0
            override val columnCount = 1
            override val isActive get() = active
            override fun columnLabel(position: Int): String { requireColumnPosition(position); return "status" }
            override fun get(position: Int): KronosPhysicalValue {
                requireColumnPosition(position)
                return KronosPhysicalValue.Text("READY")
            }
        }
        return try { listOf(mapper(row)) } finally { active = false }
    }

    override fun <T> first(plan: KronosQueryExecutionPlan, mapper: KronosPhysicalRowMapper<T>) =
        KronosExecutionFirstResult.Present(toList(plan, mapper).single())

    override fun execute(plan: KronosActionExecutionPlan): Nothing = error("Query-only fixture")
    override fun executeBatch(plan: KronosBatchExecutionPlan): Nothing = error("Query-only fixture")
    override fun <T> transact(options: KronosTransactionOptions, block: KronosTransactionScope.() -> T): T =
        error("Query-only fixture")
}
