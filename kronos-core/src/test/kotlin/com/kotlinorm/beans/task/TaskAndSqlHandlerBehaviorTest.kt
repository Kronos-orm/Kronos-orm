package com.kotlinorm.beans.task

import com.kotlinorm.database.SqlExecutor
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.enums.TransactionIsolation
import com.kotlinorm.interfaces.KAtomicActionTask
import com.kotlinorm.interfaces.KAtomicQueryTask
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import kotlin.reflect.KType
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TaskAndSqlExecutorBehaviorTest {

    @Test
    fun `query task executes all query variants with hooks and events`() {
        val wrapper = RecordingWrapper(
            listResult = listOf(mapOf("id" to 1)),
            typedListResult = listOf("row"),
            mapResult = mapOf("id" to 1),
            objectResult = "one"
        )
        val atomic = KronosAtomicQueryTask(
            "SELECT * FROM user WHERE id = :id",
            mapOf("id" to 1),
            targetType = typeOf<Map<String, Any?>>()
        )
        val task = KronosQueryTask(atomic)
        val events = mutableListOf<String>()
        val oldBeforeEvents = QueryEvent.beforeQueryEvents.toList()
        val oldAfterEvents = QueryEvent.afterQueryEvents.toList()
        QueryEvent.beforeQueryEvents.clear()
        QueryEvent.afterQueryEvents.clear()
        QueryEvent.beforeQueryEvents += { queryTask, _ -> events += "globalBefore:${queryTask.sql}" }
        QueryEvent.afterQueryEvents += { queryTask, _ -> events += "globalAfter:${queryTask.sql}" }

        try {
            task.doBeforeQuery { events += "before:${component1()}" }
                .doAfterQuery { queryType, _ -> events += "after:$queryType" }

            assertEquals(listOf(mapOf("id" to 1)), task.toMapList(wrapper))
            assertEquals(listOf("row"), task.toList<String>(wrapper))
            assertEquals(mapOf("id" to 1), task.toMap(wrapper))
            assertEquals(mapOf("id" to 1), task.toMapOrNull(wrapper))
            assertEquals("one", task.first<String>(wrapper))
            assertEquals("one", task.firstOrNull<String>(wrapper))
            assertEquals(
                listOf(
                    "before:SELECT * FROM user WHERE id = :id",
                    "globalBefore:SELECT * FROM user WHERE id = :id",
                    "globalAfter:SELECT * FROM user WHERE id = :id",
                    "after:ToMapList",
                    "before:SELECT * FROM user WHERE id = :id",
                    "globalBefore:SELECT * FROM user WHERE id = :id",
                    "globalAfter:SELECT * FROM user WHERE id = :id",
                    "after:ToList",
                    "before:SELECT * FROM user WHERE id = :id",
                    "globalBefore:SELECT * FROM user WHERE id = :id",
                    "globalAfter:SELECT * FROM user WHERE id = :id",
                    "after:ToMap",
                    "before:SELECT * FROM user WHERE id = :id",
                    "globalBefore:SELECT * FROM user WHERE id = :id",
                    "globalAfter:SELECT * FROM user WHERE id = :id",
                    "after:ToMap",
                    "before:SELECT * FROM user WHERE id = :id",
                    "globalBefore:SELECT * FROM user WHERE id = :id",
                    "globalAfter:SELECT * FROM user WHERE id = :id",
                    "after:First",
                    "before:SELECT * FROM user WHERE id = :id",
                    "globalBefore:SELECT * FROM user WHERE id = :id",
                    "globalAfter:SELECT * FROM user WHERE id = :id",
                    "after:First"
                ),
                events
            )
            assertEquals("SELECT * FROM user WHERE id = :id", task.component1())
            assertEquals(mapOf("id" to 1), task.component2())
            assertEquals(task, task.component3())
        } finally {
            QueryEvent.beforeQueryEvents.clear()
            QueryEvent.beforeQueryEvents.addAll(oldBeforeEvents)
            QueryEvent.afterQueryEvents.clear()
            QueryEvent.afterQueryEvents.addAll(oldAfterEvents)
        }
    }

    @Test
    fun `query task throws exact no result errors`() {
        val wrapper = RecordingWrapper()
        val task = KronosQueryTask(KronosAtomicQueryTask("SELECT * FROM missing", targetType = typeOf<String>()))

        assertEquals(
            "No result found for query: SELECT * FROM missing",
            assertFailsWith<NoSuchElementException> { task.toMap(wrapper) }.message
        )
        assertEquals(
            "No result found for query: SELECT * FROM missing",
            assertFailsWith<NoSuchElementException> { task.first<String>(wrapper) }.message
        )
    }

    @Test
    fun `batch task parses arrays and rejects single parsed access`() {
        val task = KronosAtomicBatchTask(
            sql = "UPDATE user SET name = :name WHERE id = :id",
            paramMapArr = arrayOf(
                linkedMapOf("name" to "alpha", "id" to 1),
                linkedMapOf("name" to "beta", "id" to 2)
            ),
            operationType = KOperationType.UPDATE
        )
        val same = KronosAtomicBatchTask(
            sql = "UPDATE user SET name = :name WHERE id = :id",
            paramMapArr = arrayOf(
                linkedMapOf("name" to "alpha", "id" to 1),
                linkedMapOf("name" to "beta", "id" to 2)
            ),
            operationType = KOperationType.UPDATE
        )

        val (jdbcSql, jdbcParams) = task.parsedArr()

        assertEquals("UPDATE user SET name = ? WHERE id = ?", jdbcSql)
        assertEquals(
            listOf(listOf("alpha", 1), listOf("beta", 2)),
            jdbcParams.map { it.toList() }
        )
        assertEquals(emptyMap(), task.paramMap)
        assertEquals(task, same)
        assertEquals(task.hashCode(), same.hashCode())
        assertEquals(false, task == same.copy(sql = "DELETE FROM user WHERE id = :id"))
        assertEquals(
            "Please use `parsedArr()` instead of `parsed()`",
            assertFailsWith<UnsupportedOperationException> { task.parsed() }.message
        )
    }

    @Test
    fun `sql executor extension methods create exact atomic tasks`() {
        val wrapper = RecordingWrapper(
            listResult = listOf(mapOf("id" to 1, "score" to null)),
            typedListResult = listOf("row"),
            mapResult = mapOf("id" to 1, "score" to null),
            objectResult = "one",
            updateResult = 3,
            batchResult = intArrayOf(1, 2)
        )

        with(SqlExecutor) {
            val expectedRows = listOf(mapOf("id" to 1, "score" to null))
            val expectedRow = mapOf("id" to 1, "score" to null)
            assertEquals(expectedRows, wrapper.toMapList("SELECT * FROM user", mapOf("id" to 1)))
            assertEquals(expectedRow, wrapper.toMap("SELECT * FROM user WHERE id = :id", mapOf("id" to 1)))
            assertEquals(expectedRow, wrapper.toMapOrNull("SELECT * FROM user WHERE id = :id", mapOf("id" to 1)))
            assertEquals(listOf("row"), wrapper.toList<String>("SELECT name FROM user"))
            assertEquals("one", wrapper.first<String>("SELECT name FROM user LIMIT 1"))
            assertEquals("one", wrapper.firstOrNull<String>("SELECT name FROM user LIMIT 1"))
            assertEquals(expectedRows, wrapper.query("SELECT * FROM user", mapOf("id" to 1)))
            assertEquals(expectedRow, wrapper.queryMap("SELECT * FROM user WHERE id = :id", mapOf("id" to 1)))
            assertEquals(expectedRow, wrapper.queryMapOrNull("SELECT * FROM user WHERE id = :id", mapOf("id" to 1)))
            assertEquals(listOf("row"), wrapper.queryList<String>("SELECT name FROM user"))
            assertEquals("one", wrapper.queryOne<String>("SELECT name FROM user LIMIT 1"))
            assertEquals("one", wrapper.queryOneOrNull<String>("SELECT name FROM user LIMIT 1"))
            assertEquals(3, wrapper.execute("UPDATE user SET name = :name", mapOf("name" to "alpha")))
            assertContentEquals(
                intArrayOf(1, 2),
                wrapper.batchExecute(
                    "UPDATE user SET name = :name WHERE id = :id",
                    arrayOf(mapOf("name" to "alpha", "id" to 1), mapOf("name" to "beta", "id" to 2))
                )
            )
        }

        assertEquals(
            listOf(
                QueryTaskShape("SELECT * FROM user", mapOf("id" to 1), typeOf<Map<String, Any?>>()),
                QueryTaskShape("SELECT * FROM user WHERE id = :id", mapOf("id" to 1), typeOf<Map<String, Any?>>()),
                QueryTaskShape("SELECT * FROM user WHERE id = :id", mapOf("id" to 1), typeOf<Map<String, Any?>?>()),
                QueryTaskShape("SELECT name FROM user", emptyMap(), typeOf<String>()),
                QueryTaskShape("SELECT name FROM user LIMIT 1", emptyMap(), typeOf<String>()),
                QueryTaskShape("SELECT name FROM user LIMIT 1", emptyMap(), typeOf<String?>()),
                QueryTaskShape("SELECT * FROM user", mapOf("id" to 1), typeOf<Map<String, Any?>>()),
                QueryTaskShape("SELECT * FROM user WHERE id = :id", mapOf("id" to 1), typeOf<Map<String, Any?>>()),
                QueryTaskShape("SELECT * FROM user WHERE id = :id", mapOf("id" to 1), typeOf<Map<String, Any?>?>()),
                QueryTaskShape("SELECT name FROM user", emptyMap(), typeOf<String>()),
                QueryTaskShape("SELECT name FROM user LIMIT 1", emptyMap(), typeOf<String>()),
                QueryTaskShape("SELECT name FROM user LIMIT 1", emptyMap(), typeOf<String?>())
            ),
            wrapper.queries.map { QueryTaskShape(it.sql, it.paramMap, it.targetType) }
        )
        assertEquals(
            listOf(ActionTaskShape("UPDATE user SET name = :name", mapOf("name" to "alpha"), KOperationType.UPDATE)),
            wrapper.actions.map { ActionTaskShape(it.sql, it.paramMap, it.operationType) }
        )
        assertEquals(
            listOf(
                BatchTaskShape(
                    "UPDATE user SET name = :name WHERE id = :id",
                    listOf(mapOf("name" to "alpha", "id" to 1), mapOf("name" to "beta", "id" to 2)),
                    KOperationType.UPDATE
                )
            ),
            wrapper.batchActions.map {
                BatchTaskShape(it.sql, it.paramMapArr?.toList().orEmpty(), it.operationType)
            }
        )
    }

    private data class QueryTaskShape(val sql: String, val params: Map<String, Any?>, val targetType: KType)
    private data class ActionTaskShape(val sql: String, val params: Map<String, Any?>, val operationType: KOperationType)
    private data class BatchTaskShape(
        val sql: String,
        val params: List<Map<String, Any?>>,
        val operationType: KOperationType
    )

    private class RecordingWrapper(
        private val listResult: List<Map<String, Any?>> = emptyList(),
        private val typedListResult: List<Any?> = emptyList(),
        private val mapResult: Map<String, Any?>? = null,
        private val objectResult: Any? = null,
        private val updateResult: Int = 1,
        private val batchResult: IntArray = intArrayOf(1)
    ) : KronosDataSourceWrapper {
        val queries = mutableListOf<KAtomicQueryTask>()
        val actions = mutableListOf<KAtomicActionTask>()
        val batchActions = mutableListOf<KronosAtomicBatchTask>()
        override val url: String = "jdbc:mysql://localhost:3306/kronos"
        override val userName: String = "kronos"
        override val dbType: DBType = DBType.Mysql

        override fun toList(task: KAtomicQueryTask): List<Any?> {
            queries += task
            return if (task.targetType == typeOf<Map<String, Any?>>()) listResult else typedListResult
        }

        override fun first(task: KAtomicQueryTask): Any? {
            queries += task
            return if (task.targetType == typeOf<Map<String, Any?>>() ||
                task.targetType == typeOf<Map<String, Any?>?>()
            ) {
                mapResult
            } else {
                objectResult
            }
        }

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
