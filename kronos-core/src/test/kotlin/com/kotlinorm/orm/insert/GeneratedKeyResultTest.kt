package com.kotlinorm.orm.insert

import com.kotlinorm.beans.task.ActionEvent
import com.kotlinorm.beans.task.GeneratedKeyRequest
import com.kotlinorm.beans.task.KronosActionTask.Companion.toKronosActionTask
import com.kotlinorm.beans.task.KronosAtomicActionTask
import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.TransactionScope
import com.kotlinorm.beans.task.lastInsertIdFallbackSql
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.enums.TransactionIsolation
import com.kotlinorm.interfaces.KAtomicActionTask
import com.kotlinorm.interfaces.KAtomicQueryTask
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.testfixtures.entities.TestUser
import com.kotlinorm.testutils.MysqlTestBase
import com.kotlinorm.utils.execute
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GeneratedKeyResultTest : MysqlTestBase() {
    @Test
    fun generatedKeyFallbackSqlReturnsExactSqlForSupportedDbTypes() {
        val request = GeneratedKeyRequest("tb_user", "id")

        assertEquals("SELECT LAST_INSERT_ID()", request.lastInsertIdFallbackSql(DBType.Mysql))
        assertEquals("SELECT LAST_INSERT_ID()", request.lastInsertIdFallbackSql(DBType.H2))
        assertEquals("SELECT LAST_INSERT_ID()", request.lastInsertIdFallbackSql(DBType.OceanBase))
        assertEquals("SELECT MAX(\"ID\") FROM \"TB_USER\"", request.lastInsertIdFallbackSql(DBType.Oracle))
        assertEquals("SELECT SCOPE_IDENTITY()", request.lastInsertIdFallbackSql(DBType.Mssql))
        assertEquals("SELECT LASTVAL()", request.lastInsertIdFallbackSql(DBType.Postgres))
        assertEquals("SELECT IDENTITY_VAL_LOCAL() FROM SYSIBM.SYSDUMMY1", request.lastInsertIdFallbackSql(DBType.DB2))
        assertEquals("SELECT @@IDENTITY", request.lastInsertIdFallbackSql(DBType.Sybase))
        assertEquals("SELECT last_insert_rowid()", request.lastInsertIdFallbackSql(DBType.SQLite))
    }

    @Test
    fun generatedKeyFallbackSqlQuotesOracleQualifiedIdentifiers() {
        val request = GeneratedKeyRequest("app_user.tb_user", "user_id")

        assertEquals(
            "SELECT MAX(\"USER_ID\") FROM \"APP_USER\".\"TB_USER\"",
            request.lastInsertIdFallbackSql(DBType.Oracle)
        )
    }

    @Test
    fun generatedKeyFallbackSqlThrowsForUnsupportedDbType() {
        assertEquals(
            "Unsupported database type: Unknown",
            assertFailsWith<UnsupportedOperationException> {
                GeneratedKeyRequest("tb_user", "id").lastInsertIdFallbackSql(DBType.Unknown)
            }.message
        )
    }

    @Test
    fun withIdAddsGeneratedKeyRequestOnlyForDatabaseGeneratedIdentity() {
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

        assertEquals(GeneratedKeyRequest("tb_user", "id"), generatedTask.generatedKeyRequest)
        assertEquals(null, assignedTask.generatedKeyRequest)
        assertEquals(null, plainTask.generatedKeyRequest)
    }

    @Test
    fun executeQueriesGeneratedKeyFallbackOnlyForRequestedInsertTasks() {
        val wrapper = GeneratedKeyRecordingWrapper(fallbackResult = 99L)
        val requestedInsert = KronosAtomicActionTask(
            sql = "INSERT INTO user(name) VALUES(:name)",
            paramMap = mapOf("name" to "alpha"),
            operationType = KOperationType.INSERT,
            generatedKeyRequest = GeneratedKeyRequest("tb_user", "id")
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
            generatedKeyRequest = GeneratedKeyRequest("tb_user", "id")
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
            generatedKeyRequest = GeneratedKeyRequest("tb_user", "id")
        )

        val result = task.execute(wrapper)

        assertContentEquals(listOf<Any?>(303L), task.generatedKeys)
        assertEquals(303L, task.lastInsertId)
        assertEquals(303L, result.lastInsertId)
        assertEquals(emptyList(), wrapper.querySql)
    }

    @Test
    fun afterActionAndAfterExecuteCanReadGeneratedKeyProperty() {
        val wrapper = GeneratedKeyRecordingWrapper(fallbackResult = 202L)
        val task = KronosAtomicActionTask(
            sql = "INSERT INTO user(name) VALUES(:name)",
            paramMap = mapOf("name" to "alpha"),
            operationType = KOperationType.INSERT,
            generatedKeyRequest = GeneratedKeyRequest("tb_user", "id")
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

    private class GeneratedKeyRecordingWrapper(
        private val generatedKeyFromUpdate: Long? = null,
        private val fallbackResult: Any? = null
    ) : KronosDataSourceWrapper {
        val querySql = mutableListOf<String>()
        override val url: String = "jdbc:mysql://localhost:3306/kronos"
        override val userName: String = "kronos"
        override val dbType: DBType = DBType.Mysql

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
