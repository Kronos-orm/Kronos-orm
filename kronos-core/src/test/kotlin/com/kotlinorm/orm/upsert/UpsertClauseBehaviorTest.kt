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

package com.kotlinorm.orm.upsert

import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.KronosFunctionExpr
import com.kotlinorm.testfixtures.cascade.onetoone.CarDetails
import com.kotlinorm.beans.task.TransactionScope
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.enums.TransactionIsolation
import com.kotlinorm.exceptions.EmptyFieldsException
import com.kotlinorm.interfaces.KAtomicActionTask
import com.kotlinorm.interfaces.KAtomicQueryTask
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.syntax.SqlIdentifier
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlParameter
import com.kotlinorm.syntax.statement.SqlAssignmentTarget
import com.kotlinorm.syntax.statement.SqlConflictTarget
import com.kotlinorm.syntax.statement.SqlDmlStatement
import com.kotlinorm.syntax.statement.SqlUpdateSetPair
import com.kotlinorm.syntax.statement.SqlUpsertAction
import com.kotlinorm.syntax.table.SqlTable
import com.kotlinorm.testutils.MysqlTestBase
import com.kotlinorm.wrappers.SampleMysqlJdbcWrapper
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class UpsertClauseBehaviorTest : MysqlTestBase() {

    @Test
    fun `on conflict build exposes complete syntax upsert statement`() {
        val task = UpsertBehaviorUser(id = 7, name = "seed", count = 2)
            .upsert { it.name }
            .patch(
                "count" to SqlExpr.NumberLiteral("10"),
                "name" to KronosFunctionExpr(SqlExpr.StringLiteral("patched"), "literal")
            )
            .on { it.id }
            .onConflict()
            .build()

        assertEquals(
            SqlDmlStatement.Upsert(
                table = SqlTable.Ident("upsert_behavior_user"),
                columns = listOf(SqlIdentifier.of("id"), SqlIdentifier.of("name"), SqlIdentifier.of("count")),
                values = listOf(
                    SqlExpr.Parameter(SqlParameter.Named("id")),
                    SqlExpr.Parameter(SqlParameter.Named("name")),
                    SqlExpr.Parameter(SqlParameter.Named("count"))
                ),
                primaryKeys = listOf(SqlIdentifier.of("id")),
                conflictTarget = SqlConflictTarget(columns = listOf(SqlIdentifier.of("id"))),
                action = SqlUpsertAction.Update(
                    listOf(
                        SqlUpdateSetPair(
                            SqlAssignmentTarget.Column(SqlIdentifier.of("name")),
                            SqlExpr.StringLiteral("patched")
                        ),
                        SqlUpdateSetPair(
                            SqlAssignmentTarget.Column(SqlIdentifier.of("count")),
                            SqlExpr.NumberLiteral("10")
                        )
                    )
                )
            ),
            task.atomicTasks.single().statement
        )
        assertEquals(
            mapOf("id" to 7, "name" to "seed", "count" to 2),
            task.atomicTasks.single().paramMap
        )
    }

    @Test
    fun `on conflict assignment can reuse another field expression`() {
        val countField = UpsertBehaviorUser().kronosColumns().single { it.name == "count" }
        val task = UpsertBehaviorUser(id = 8, name = "seed", count = 5)
            .upsert { it.name }
            .patch("name" to countField)
            .on { it.id }
            .onConflict()
            .build()

        assertEquals(
            SqlUpsertAction.Update(
                listOf(
                    SqlUpdateSetPair(
                        SqlAssignmentTarget.Column(SqlIdentifier.of("name")),
                        SqlExpr.Column(tableName = "upsert_behavior_user", columnName = "count")
                    )
                )
            ),
            (task.atomicTasks.single().statement as SqlDmlStatement.Upsert).action
        )
        assertEquals(mapOf("id" to 8, "name" to "seed", "count" to 5), task.atomicTasks.single().paramMap)
    }

    @Test
    fun `fallback upsert executes insert when conflict row is absent`() {
        val wrapper = UpsertFallbackWrapper(countResult = 0)

        val result = UpsertBehaviorUser(id = 1, name = "inserted", count = 3)
            .upsert { it.name }
            .on { it.id }
            .cascade(enabled = false)
            .execute(wrapper)

        assertEquals(1, result.affectedRows)
        assertEquals(
            listOf(
                QueryShape("SELECT COUNT(1) FROM `upsert_behavior_user` WHERE `id` = :id LIMIT 1 FOR UPDATE", mapOf("id" to 1)),
                ActionShape(
                    "INSERT INTO `upsert_behavior_user` (`id`, `name`, `count`) VALUES (:id, :name, :count)",
                    mapOf("id" to 1, "name" to "inserted", "count" to 3),
                    KOperationType.UPSERT,
                    "Upsert"
                )
            ),
            wrapper.shapes()
        )
    }

    @Test
    fun `fallback upsert executes update when conflict row exists`() {
        val wrapper = UpsertFallbackWrapper(countResult = 1)

        val result = UpsertBehaviorUser(id = 2, name = "updated", count = 4)
            .upsert { it.name }
            .on { it.id }
            .lock()
            .execute(wrapper)

        assertEquals(1, result.affectedRows)
        assertEquals(
            listOf(
                QueryShape("SELECT COUNT(1) FROM `upsert_behavior_user` WHERE `id` = :id LIMIT 1 FOR UPDATE", mapOf("id" to 2)),
                ActionShape(
                    "UPDATE `upsert_behavior_user` SET `name` = :nameNew WHERE `id` = :id",
                    mapOf("id" to 2, "nameNew" to "updated"),
                    KOperationType.UPSERT,
                    "Upsert"
                )
            ),
            wrapper.shapes()
        )
    }

    @Test
    fun `fallback upsert build exposes selected action as upsert metadata after execute`() {
        val wrapper = UpsertFallbackWrapper(countResult = 1)
        val task = UpsertBehaviorUser(id = 3, name = "metadata", count = 5)
            .upsert { it.name }
            .on { it.id }
            .cascade(enabled = false)
            .build(wrapper)

        assertEquals(0, task.atomicTasks.size)

        task.execute(wrapper)

        val atomicTask = task.atomicTasks.single()
        assertEquals(KOperationType.UPSERT, atomicTask.operationType)
        assertIs<SqlDmlStatement.Upsert>(atomicTask.statement)
    }

    @Test
    fun `field callbacks fail fast when upsert receivers return no fields`() {
        assertEquals(
            listOf(
                EmptyFieldsException::class,
                EmptyFieldsException::class,
                EmptyFieldsException::class,
                EmptyFieldsException::class
            ),
            listOf(
                assertFailsWith<EmptyFieldsException> { UpsertBehaviorUser().upsert { emptyList<Any?>() } }::class,
                assertFailsWith<EmptyFieldsException> { UpsertBehaviorUser().upsert().on { emptyList<Any?>() } }::class,
                assertFailsWith<EmptyFieldsException> { UpsertBehaviorUser().upsert().set(null) }::class,
                assertFailsWith<EmptyFieldsException> { UpsertBehaviorUser().upsert().cascade(null) }::class
            )
        )
    }

    @Test
    fun `cascade reference callback accepts class qualified references`() {
        val task = CarDetails(id = 1, carId = 2)
            .upsert { it.vin }
            .on { it.id }
            .cascade { [CarDetails::car] }
            .onConflict()
            .build()

        assertEquals(KOperationType.UPSERT, task.atomicTasks.single().operationType)
    }
}

@Table("upsert_behavior_user")
data class UpsertBehaviorUser(
    @PrimaryKey
    var id: Int? = null,
    var name: String? = null,
    var count: Int? = null
) : KPojo

private data class QueryShape(
    val sql: String,
    val params: Map<String, Any?>
)

private data class ActionShape(
    val sql: String,
    val params: Map<String, Any?>,
    val operationType: KOperationType,
    val statementType: String?
)

private class UpsertFallbackWrapper(
    private val countResult: Int
) : SampleMysqlJdbcWrapper() {
    val queries = mutableListOf<QueryShape>()
    val actions = mutableListOf<ActionShape>()

    override fun forObject(
        task: KAtomicQueryTask,
        kClass: KClass<*>,
        isKPojo: Boolean,
        superTypes: List<String>
    ): Any? {
        queries += QueryShape(task.sql, task.paramMap)
        return countResult
    }

    override fun update(task: KAtomicActionTask): Int {
        actions += ActionShape(task.sql, task.paramMap, task.operationType, task.statement?.let { it::class.simpleName })
        return 1
    }

    override fun transact(isolation: TransactionIsolation?, timeout: Int?, block: TransactionScope.() -> Any?): Any? {
        return TransactionScope().block()
    }

    fun shapes(): List<Any> = queries + actions
}
