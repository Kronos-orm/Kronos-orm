package com.kotlinorm.orm.insert

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.task.ActionEvent
import com.kotlinorm.beans.task.KronosActionTask.Companion.toKronosActionTask
import com.kotlinorm.beans.task.KronosAtomicActionTask
import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.TransactionScope
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.enums.PrimaryKeyType
import com.kotlinorm.enums.TransactionIsolation
import com.kotlinorm.interfaces.KAtomicActionTask
import com.kotlinorm.interfaces.KAtomicQueryTask
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.syntax.SqlIdentifier
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlParameter
import com.kotlinorm.syntax.statement.SqlDmlStatement
import com.kotlinorm.syntax.statement.SqlInsertMode
import com.kotlinorm.syntax.table.SqlTable
import com.kotlinorm.testfixtures.entities.TestUser
import com.kotlinorm.testutils.MysqlTestBase
import com.kotlinorm.utils.execute
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class GeneratedKeyResultTest : MysqlTestBase() {
    @Test
    fun withIdAddsGeneratedKeyFieldOnlyForDatabaseGeneratedIdentity() {
        val generatedTask = TestUser(username = "alpha")
            .insert()
            .withId()
            .build()
            .component3()
            .single()
        val assignedTask = TestUser(id = 1, username = "beta")
            .insert()
            .withId()
            .build()
            .component3()
            .single()
        val plainTask = TestUser(username = "gamma")
            .insert()
            .build()
            .component3()
            .single()

        assertEquals(identityField(), generatedTask.generatedKeyField)
        assertEquals(null, assignedTask.generatedKeyField)
        assertEquals(null, plainTask.generatedKeyField)
    }

    @Test
    fun executeQueriesGeneratedKeyFallbackOnlyForRequestedInsertTasks() {
        val wrapper = GeneratedKeyRecordingWrapper(fallbackResult = 99L)
        val requestedInsert = KronosAtomicActionTask(
            sql = "INSERT INTO user(name) VALUES(:name)",
            paramMap = mapOf("name" to "alpha"),
            operationType = KOperationType.INSERT,
            statement = generatedKeyInsert(),
            generatedKeyField = identityField()
        )
        val plainInsert = KronosAtomicActionTask(
            sql = "INSERT INTO user(name) VALUES(:name)",
            paramMap = mapOf("name" to "beta"),
            operationType = KOperationType.INSERT
        )
        val requestedUpdate = KronosAtomicActionTask(
            sql = "UPDATE user SET name = :name",
            paramMap = mapOf("name" to "gamma"),
            operationType = KOperationType.UPDATE,
            generatedKeyField = identityField()
        )

        val requestedResult = requestedInsert.execute(wrapper)
        val plainResult = plainInsert.execute(wrapper)
        val updateResult = requestedUpdate.execute(wrapper)

        assertEquals(99L, requestedInsert.lastInsertId)
        assertEquals(99L, requestedResult.lastInsertId)
        assertEquals(emptyMap(), requestedResult.stash)
        assertEquals(null, plainResult.lastInsertId)
        assertEquals(null, updateResult.lastInsertId)
        assertEquals(listOf("SELECT LAST_INSERT_ID()"), wrapper.querySql)
    }

    @Test
    fun executeUsesWrapperGeneratedKeyWithoutFallbackQuery() {
        val wrapper = GeneratedKeyRecordingWrapper(generatedKeyFromUpdate = 303L, fallbackResult = 99L)
        val task = KronosAtomicActionTask(
            sql = "INSERT INTO user(name) VALUES(:name)",
            paramMap = mapOf("name" to "alpha"),
            operationType = KOperationType.INSERT,
            statement = generatedKeyInsert(),
            generatedKeyField = identityField()
        )

        val result = task.execute(wrapper)

        assertContentEquals(listOf<Any?>(303L), task.generatedKeys)
        assertEquals(303L, task.lastInsertId)
        assertEquals(303L, result.lastInsertId)
        assertEquals(emptyList(), wrapper.querySql)
    }

    @Test
    fun executeSkipsUnsafeGeneratedKeyFallbackForDM8() {
        val wrapper = GeneratedKeyRecordingWrapper(databaseType = DBType.DM8, fallbackResult = 404L)
        val task = KronosAtomicActionTask(
            sql = "INSERT INTO user(name) VALUES(:name)",
            paramMap = mapOf("name" to "alpha"),
            operationType = KOperationType.INSERT,
            statement = generatedKeyInsert(),
            generatedKeyField = identityField()
        )

        val result = task.execute(wrapper)

        assertEquals(null, result.lastInsertId)
        assertEquals(emptyList(), wrapper.querySql)
    }

    @Test
    fun executeSkipsFallbackWhenH2HasNoSafeGeneratedKeyQuery() {
        val wrapper = GeneratedKeyRecordingWrapper(databaseType = DBType.H2, fallbackResult = 404L)
        val task = KronosAtomicActionTask(
            sql = "INSERT INTO user(name) VALUES(:name)",
            paramMap = mapOf("name" to "alpha"),
            operationType = KOperationType.INSERT,
            statement = generatedKeyInsert(),
            generatedKeyField = identityField()
        )

        val result = task.execute(wrapper)

        assertEquals(null, result.lastInsertId)
        assertEquals(emptyList(), wrapper.querySql)
    }

    @Test
    fun afterActionAndAfterExecuteCanReadGeneratedKeyProperty() {
        val wrapper = GeneratedKeyRecordingWrapper(fallbackResult = 202L)
        val task = KronosAtomicActionTask(
            sql = "INSERT INTO user(name) VALUES(:name)",
            paramMap = mapOf("name" to "alpha"),
            operationType = KOperationType.INSERT,
            statement = generatedKeyInsert(),
            generatedKeyField = identityField()
        )
        val oldAfterEvents = ActionEvent.afterActionEvents.toList()
        var eventObservedLastInsertId: Long? = null
        var afterExecuteObservedLastInsertId: Long? = null
        ActionEvent.afterActionEvents.clear()
        ActionEvent.afterActionEvents += { actionTask, _ -> eventObservedLastInsertId = actionTask.lastInsertId }

        try {
            val result = task.toKronosActionTask()
                .doAfterExecute { afterExecuteObservedLastInsertId = lastInsertId }
                .execute(wrapper)

            assertEquals(202L, eventObservedLastInsertId)
            assertEquals(202L, afterExecuteObservedLastInsertId)
            assertEquals(202L, result.lastInsertId)
            assertEquals(listOf("SELECT LAST_INSERT_ID()"), wrapper.querySql)
        } finally {
            ActionEvent.afterActionEvents.clear()
            ActionEvent.afterActionEvents.addAll(oldAfterEvents)
        }
    }

    private fun identityField(): Field = Field(
        columnName = "id",
        primaryKey = PrimaryKeyType.IDENTITY,
        tableName = "tb_user"
    )

    private fun generatedKeyInsert(): SqlDmlStatement.Insert = SqlDmlStatement.Insert(
        table = SqlTable.Ident("tb_user"),
        columns = listOf(SqlIdentifier.of("name")),
        mode = SqlInsertMode.Values(
            listOf(listOf(SqlExpr.Parameter(SqlParameter.Named("name"))))
        )
    )

    private class GeneratedKeyRecordingWrapper(
        private val databaseType: DBType = DBType.Mysql,
        private val generatedKeyFromUpdate: Long? = null,
        private val fallbackResult: Any? = null
    ) : KronosDataSourceWrapper {
        val querySql = mutableListOf<String>()
        override val url: String = "jdbc:mysql://localhost:3306/kronos"
        override val userName: String = "kronos"
        override val dbType: DBType = databaseType

        override fun toList(task: KAtomicQueryTask): List<Any?> = emptyList()

        override fun first(task: KAtomicQueryTask): Any? {
            querySql += task.sql
            return fallbackResult
        }

        override fun update(task: KAtomicActionTask): Int {
            generatedKeyFromUpdate?.let {
                task.generatedKeys += it
                task.lastInsertId = it
            }
            return 1
        }

        override fun batchUpdate(task: KronosAtomicBatchTask): IntArray = intArrayOf(1)

        override fun transact(
            isolation: TransactionIsolation?,
            timeout: Int?,
            block: TransactionScope.() -> Any?
        ): Any? = TransactionScope().block()
    }
}
