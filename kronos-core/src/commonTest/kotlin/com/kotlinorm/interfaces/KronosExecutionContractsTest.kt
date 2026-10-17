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

package com.kotlinorm.interfaces

import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.KOperationType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class KronosExecutionContractsTest {
    @Test
    fun `adapter boundary keeps identity and transaction options platform neutral`() {
        val identity = KronosDatabaseIdentity(
            dbType = DBType.SQLite,
            databaseName = ":memory:",
            driverName = "bundled"
        )
        assertEquals(":memory:", identity.databaseName)
        assertEquals("bundled", identity.driverName)
        assertEquals(KronosTransactionMode.IMMEDIATE, KronosTransactionOptions(KronosTransactionMode.IMMEDIATE).mode)
        assertFailsWith<IllegalArgumentException> { KronosTransactionOptions(timeoutSeconds = -1) }
    }

    @Test
    fun `query mapper owns the physical row and action returns generated key atomically`() {
        val adapter = FakeExecutionDataSource()
        val query = KronosQueryExecutionPlan("select id from user", operationType = KOperationType.SELECT)
        val values = adapter.toList(query) { row ->
            (row.get(1) as KronosPhysicalValue.Integer).value
        }
        assertEquals(listOf(7L), values)

        val action = KronosActionExecutionPlan(
            sql = "insert into user(name) values (:name)",
            parameters = mapOf("name" to "Ada"),
            operationType = KOperationType.INSERT,
            generatedKey = KronosGeneratedKeyRequest("id")
        )
        val result = adapter.execute(action)
        assertEquals(1, result.affectedRows)
        assertEquals(listOf(42L), result.generatedKeys)
        assertEquals(42L, result.lastInsertId)
        assertEquals(1, adapter.generatedKeyReads)
    }

    @Test
    fun `batch entries and opaque savepoints stay inside adapter boundary`() {
        val adapter = FakeExecutionDataSource()
        val batch = KronosBatchExecutionPlan(
            sql = "delete from user where id = :id",
            entries = listOf(mapOf("id" to 1), mapOf("id" to 2)),
            operationType = KOperationType.DELETE
        )
        assertEquals(listOf(1, 1), adapter.executeBatch(batch).map { it.affectedRows })

        val observed = adapter.transact(KronosTransactionOptions(KronosTransactionMode.DEFERRED)) {
            val token = savepoint("nested")
            rollbackToSavepoint(token)
            releaseSavepoint(token)
            adapter.lastTransactionMode
        }
        assertEquals(KronosTransactionMode.DEFERRED, observed)
        assertEquals(1, adapter.savepointOperations)
    }

    private class FakeExecutionDataSource : KronosExecutionDataSource {
        override val identity = KronosDatabaseIdentity(DBType.SQLite, ":memory:")
        var generatedKeyReads = 0
        var savepointOperations = 0
        var lastTransactionMode = KronosTransactionMode.DEFAULT

        override fun <T> toList(
            plan: KronosQueryExecutionPlan,
            mapper: KronosPhysicalRowMapper<T>
        ): List<T> = listOf(mapper(Row(0, listOf("id"), listOf(KronosPhysicalValue.Integer(7)))))

        override fun <T> first(
            plan: KronosQueryExecutionPlan,
            mapper: KronosPhysicalRowMapper<T>
        ): KronosExecutionFirstResult<T> = KronosExecutionFirstResult.Present(
            mapper(Row(0, listOf("id"), listOf(KronosPhysicalValue.Integer(7))))
        )

        override fun execute(plan: KronosActionExecutionPlan): KronosExecutionResult {
            if (plan.generatedKey != null) generatedKeyReads++
            return KronosExecutionResult(1, listOf(42L), 42L)
        }

        override fun executeBatch(plan: KronosBatchExecutionPlan): List<KronosExecutionResult> =
            plan.entries.map { KronosExecutionResult(1) }

        override fun <T> transact(
            options: KronosTransactionOptions,
            block: KronosTransactionScope.() -> T
        ): T {
            lastTransactionMode = options.mode
            return Scope().block()
        }

        private inner class Scope : KronosTransactionScope {
            override fun savepoint(name: String?): KronosSavepointToken = Token

            override fun rollbackToSavepoint(savepoint: KronosSavepointToken) {
                assertEquals(Token, savepoint)
                savepointOperations++
            }

            override fun releaseSavepoint(savepoint: KronosSavepointToken) {
                assertEquals(Token, savepoint)
            }
        }

        private object Token : KronosSavepointToken

        private class Row(
            override val rowNumber: Int,
            private val labels: List<String>,
            private val values: List<KronosPhysicalValue>
        ) : KronosPhysicalRow {
            override val columnCount: Int = labels.size
            override val isActive: Boolean = true

            override fun columnLabel(position: Int): String {
                requireColumnPosition(position)
                return labels[position - 1]
            }

            override fun get(position: Int): KronosPhysicalValue {
                requireColumnPosition(position)
                return values[position - 1]
            }
        }
    }
}
