package com.kotlinorm.utils

import com.kotlinorm.beans.task.ActionEvent
import com.kotlinorm.beans.task.KronosAtomicActionTask
import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.beans.task.KronosOperationResult
import com.kotlinorm.beans.task.TransactionScope
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.enums.QueryType
import com.kotlinorm.enums.TransactionIsolation
import com.kotlinorm.interfaces.KAtomicActionTask
import com.kotlinorm.interfaces.KAtomicQueryTask
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.reflect.typeOf

class TaskUtilTest {

    @Test
    fun `atomic action execute invokes hooks wrapper update logging and stash`() {
        val wrapper = RecordingActionWrapper(updateResult = 3)
        val events = mutableListOf<String>()
        val logged = mutableListOf<LoggedResult>()
        val oldBefore = ActionEvent.beforeActionEvents.toList()
        val oldAfter = ActionEvent.afterActionEvents.toList()
        val oldLogResult = handleLogResult
        ActionEvent.beforeActionEvents.clear()
        ActionEvent.afterActionEvents.clear()
        ActionEvent.beforeActionEvents += { task, _ -> events += "before:${task.sql}" }
        ActionEvent.afterActionEvents += { task, _ -> events += "after:${task.sql}" }
        handleLogResult = { task, result, queryType ->
            logged += LoggedResult(task.sql, (result as KronosOperationResult).affectedRows, queryType)
        }
        val task = KronosAtomicActionTask(
            sql = "UPDATE user SET name = :name WHERE id = :id",
            paramMap = mapOf("name" to "alpha", "id" to 1),
            operationType = KOperationType.UPDATE,
            stash = mutableMapOf("trace" to "u1")
        )

        try {
            val result = task.execute(wrapper)

            assertEquals(3, result.affectedRows)
            assertEquals(mapOf<String, Any?>("trace" to "u1"), result.stash)
            assertEquals(mapOf<String, Any?>("trace" to "u1"), task.stash)
            assertEquals(
                listOf("before:UPDATE user SET name = :name WHERE id = :id", "after:UPDATE user SET name = :name WHERE id = :id"),
                events
            )
            assertEquals(listOf<KAtomicActionTask>(task), wrapper.actions)
            assertEquals(
                listOf(LoggedResult("UPDATE user SET name = :name WHERE id = :id", 3, null)),
                logged
            )
        } finally {
            ActionEvent.beforeActionEvents.clear()
            ActionEvent.beforeActionEvents.addAll(oldBefore)
            ActionEvent.afterActionEvents.clear()
            ActionEvent.afterActionEvents.addAll(oldAfter)
            handleLogResult = oldLogResult
        }
    }

    @Test
    fun `atomic batch execute sums batch updates and logs exact result`() {
        val wrapper = RecordingActionWrapper(batchResult = intArrayOf(1, 2, 3))
        val logged = mutableListOf<LoggedResult>()
        val oldLogResult = handleLogResult
        handleLogResult = { task, result, queryType ->
            logged += LoggedResult(task.sql, (result as KronosOperationResult).affectedRows, queryType)
        }
        val task = KronosAtomicBatchTask(
            sql = "UPDATE user SET status = :status WHERE id = :id",
            paramMapArr = arrayOf(
                mapOf("status" to "A", "id" to 1),
                mapOf("status" to "B", "id" to 2),
                mapOf("status" to "C", "id" to 3)
            ),
            operationType = KOperationType.UPDATE
        )

        try {
            val result = task.execute(wrapper)

            assertEquals(6, result.affectedRows)
            assertEquals(listOf(task), wrapper.batchActions)
            assertContentEquals(intArrayOf(1, 2, 3), wrapper.batchResult)
            assertEquals(
                listOf(LoggedResult("UPDATE user SET status = :status WHERE id = :id", 6, null)),
                logged
            )
        } finally {
            handleLogResult = oldLogResult
        }
    }

    @Test
    fun `logAndReturn passes query type and preserves original result instance`() {
        val oldLogResult = handleLogResult
        val logged = mutableListOf<LoggedResult>()
        val task = KronosAtomicQueryTask(
            sql = "SELECT * FROM user WHERE id = :id",
            paramMap = mapOf("id" to 1),
            targetType = typeOf<Map<String, Any?>>()
        )
        val result = listOf(mapOf("id" to 1))
        handleLogResult = { atomicTask, value, queryType ->
            logged += LoggedResult(atomicTask.sql, (value as List<*>).size, queryType)
        }

        try {
            assertSame(result, task.logAndReturn(result, QueryType.ToList))
            assertEquals(
                listOf(LoggedResult("SELECT * FROM user WHERE id = :id", 1, QueryType.ToList)),
                logged
            )
        } finally {
            handleLogResult = oldLogResult
        }
    }

    @Test
    fun `default log handler covers select action and batch result shapes`() {
        val queryTask = KronosAtomicQueryTask(
            sql = "SELECT * FROM user",
            operationType = KOperationType.SELECT,
            targetType = typeOf<Map<String, Any?>>()
        )
        val actionTask = KronosAtomicActionTask(
            sql = "INSERT INTO user(name) VALUES (:name)",
            paramMap = mapOf("name" to "alpha"),
            operationType = KOperationType.INSERT
        )
        val batchTask = KronosAtomicBatchTask(
            sql = "UPDATE user SET status = :status WHERE id = :id",
            paramMapArr = arrayOf(
                mapOf("status" to "A", "id" to 1),
                mapOf("status" to null, "id" to 2)
            ),
            operationType = KOperationType.UPDATE
        )
        val actionResult = KronosOperationResult(1).apply { stash["lastInsertId"] = 9L }
        val batchResult = KronosOperationResult(3)
        val queryRows = listOf(1, 2)

        assertSame(queryRows, queryTask.logAndReturn(queryRows, QueryType.ToMapList))
        assertEquals(mapOf("id" to 1), queryTask.logAndReturn(mapOf("id" to 1), QueryType.ToMap))
        assertSame(actionResult, actionTask.logAndReturn(actionResult))
        assertSame(batchResult, batchTask.logAndReturn(batchResult))
    }

    private data class LoggedResult(val sql: String, val resultCount: Int, val queryType: QueryType?)

    private class RecordingActionWrapper(
        private val updateResult: Int = 1,
        val batchResult: IntArray = intArrayOf(1)
    ) : KronosDataSourceWrapper {
        val actions = mutableListOf<KAtomicActionTask>()
        val batchActions = mutableListOf<KronosAtomicBatchTask>()
        override val url: String = "jdbc:mysql://localhost:3306/kronos"
        override val userName: String = "kronos"
        override val dbType: DBType = DBType.Mysql

        override fun toList(task: KAtomicQueryTask): List<Any?> = emptyList()

        override fun first(task: KAtomicQueryTask): Any? = null

        override fun update(task: KAtomicActionTask): Int {
            actions += task
            return updateResult
        }

        override fun batchUpdate(task: KronosAtomicBatchTask): IntArray {
            batchActions += task
            return batchResult
        }

        override fun transact(
            isolation: TransactionIsolation?,
            timeout: Int?,
            block: TransactionScope.() -> Any?
        ): Any? = TransactionScope().block()
    }
}
