/*
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

package com.kotlinorm.interfaces

import com.kotlinorm.beans.task.KronosAtomicActionTask
import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.enums.DBType
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertEquals

class KronosExecutionPlanJvmBridgeTest {
    @Test
    fun `query bridge keeps static metadata and column storage`() {
        val task = KronosAtomicQueryTask(
            sql = "select id from user where id = :id",
            paramMap = mapOf("id" to 1),
            targetType = typeOf<Int>()
        )
        val plan = task.toKronosQueryExecutionPlan()
        assertEquals(task.sql, plan.sql)
        assertEquals(task.paramMap, plan.parameters)
        assertEquals(task.targetTypeInfo, plan.targetType)
    }

    @Test
    fun `action and batch bridges carry generated key request`() {
        val action = KronosAtomicActionTask(
            sql = "insert into user(name) values (:name)",
            paramMap = mapOf("name" to "Ada"),
            generatedKeyField = com.kotlinorm.beans.dsl.Field("id", "id")
        )
        assertEquals("id", action.toKronosActionExecutionPlan().generatedKey?.fieldName)

        val batch = KronosAtomicBatchTask(
            sql = "insert into user(name) values (:name)",
            paramMapArr = arrayOf(mapOf("name" to "Ada"), mapOf("name" to "Grace")),
            generatedKeyField = com.kotlinorm.beans.dsl.Field("id", "id")
        )
        assertEquals(2, batch.toKronosBatchExecutionPlan().entries.size)
        assertEquals("id", batch.toKronosBatchExecutionPlan().generatedKey?.fieldName)
    }

    @Test
    fun `legacy wrapper identity has common dialect and diagnostics`() {
        val wrapper = object : KronosDataSourceWrapper {
            override val url = "jdbc:sqlite:file:test"
            override val userName = ""
            override val dbType = DBType.SQLite
            override fun toList(task: KAtomicQueryTask): List<Any?> = emptyList()
            override fun first(task: KAtomicQueryTask): Any? = null
            override fun update(task: KAtomicActionTask): Int = 0
            override fun batchUpdate(task: KronosAtomicBatchTask): IntArray = IntArray(0)
            override fun transact(
                isolation: com.kotlinorm.enums.TransactionIsolation?,
                timeout: Int?,
                block: com.kotlinorm.beans.task.TransactionScope.() -> Any?
            ): Any? = null
        }
        assertEquals(DBType.SQLite, wrapper.toKronosDatabaseIdentity().dbType)
        assertEquals(wrapper.url, wrapper.toKronosDatabaseIdentity().databaseName)
    }
}
