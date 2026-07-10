package com.kotlinorm.plugins

import com.kotlinorm.beans.task.ActionEvent
import com.kotlinorm.beans.task.KronosActionTask.Companion.toKronosActionTask
import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.KronosAtomicActionTask
import com.kotlinorm.beans.task.KronosOperationResult
import com.kotlinorm.beans.task.TransactionScope
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.enums.TransactionIsolation
import com.kotlinorm.interfaces.KAtomicActionTask
import com.kotlinorm.interfaces.KAtomicQueryTask
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.plugins.LastInsertIdPlugin.lastInsertId
import com.kotlinorm.utils.execute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LastInsertIdTest {
    @Test
    fun lastInsertIdObtainSqlReturnsCorrectSqlForSupportedDbTypes() {
        assertEquals("SELECT LAST_INSERT_ID()", LastInsertIdPlugin.lastInsertIdObtainSql(DBType.Mysql))
        assertEquals("SELECT LAST_INSERT_ID()", LastInsertIdPlugin.lastInsertIdObtainSql(DBType.H2))
        assertEquals("SELECT LAST_INSERT_ID()", LastInsertIdPlugin.lastInsertIdObtainSql(DBType.OceanBase))
        assertEquals("SELECT * FROM DUAL", LastInsertIdPlugin.lastInsertIdObtainSql(DBType.Oracle))
        assertEquals("SELECT SCOPE_IDENTITY()", LastInsertIdPlugin.lastInsertIdObtainSql(DBType.Mssql))
        assertEquals("SELECT LASTVAL()", LastInsertIdPlugin.lastInsertIdObtainSql(DBType.Postgres))
        assertEquals(
            "SELECT IDENTITY_VAL_LOCAL() FROM SYSIBM.SYSDUMMY1", LastInsertIdPlugin.lastInsertIdObtainSql(
                DBType.DB2
            )
        )
        assertEquals("SELECT @@IDENTITY", LastInsertIdPlugin.lastInsertIdObtainSql(DBType.Sybase))
        assertEquals("SELECT last_insert_rowid()", LastInsertIdPlugin.lastInsertIdObtainSql(DBType.SQLite))
    }

    @Test
    fun lastInsertIdObtainSqlThrowsExceptionForUnsupportedDbType() {
        assertFailsWith<UnsupportedOperationException> { LastInsertIdPlugin.lastInsertIdObtainSql(DBType.Unknown) }
    }

    @Test
    fun lastInsertIdPluginRegistersAndUnregistersActionEvent() {
        val wasEnabled = LastInsertIdPlugin.enabled
        LastInsertIdPlugin.enabled = false
        val baseEvents = ActionEvent.afterActionEvents.toList()

        try {
            LastInsertIdPlugin.enabled = true
            assertEquals(baseEvents + LastInsertIdPlugin.doAfterAction, ActionEvent.afterActionEvents)
            LastInsertIdPlugin.enabled = false
            assertEquals(baseEvents, ActionEvent.afterActionEvents)
        } finally {
            LastInsertIdPlugin.enabled = false
            ActionEvent.afterActionEvents.clear()
            ActionEvent.afterActionEvents.addAll(baseEvents)
            LastInsertIdPlugin.enabled = wasEnabled
        }
    }

    @Test
    fun doAfterActionQueriesLastInsertIdOnlyForInsertIdentityTasks() {
        val wrapper = LastInsertRecordingWrapper(objectResult = 99L)
        val explicitTask = KronosAtomicActionTask(
            sql = "INSERT INTO user(name) VALUES(:name)",
            paramMap = mapOf("name" to "alpha"),
            operationType = KOperationType.INSERT
        ).also {
            it.stash["queryId"] = true
            it.stash["useIdentity"] = true
        }
        val disabledImplicitTask = KronosAtomicActionTask(
            sql = "INSERT INTO user(name) VALUES(:name)",
            paramMap = mapOf("name" to "beta"),
            operationType = KOperationType.INSERT
        ).also {
            it.stash["useIdentity"] = true
        }
        val updateTask = KronosAtomicActionTask(
            sql = "UPDATE user SET name = :name",
            paramMap = mapOf("name" to "gamma"),
            operationType = KOperationType.UPDATE
        ).also {
            it.stash["queryId"] = true
            it.stash["useIdentity"] = true
        }
        val oldEnabled = LastInsertIdPlugin.enabled
        LastInsertIdPlugin.enabled = false

        try {
            LastInsertIdPlugin.doAfterAction(explicitTask, wrapper)
            LastInsertIdPlugin.doAfterAction(disabledImplicitTask, wrapper)
            LastInsertIdPlugin.doAfterAction(updateTask, wrapper)

            assertEquals(99L, explicitTask.stash["lastInsertId"])
            assertEquals(null, disabledImplicitTask.stash["lastInsertId"])
            assertEquals(null, updateTask.stash["lastInsertId"])
            assertEquals(
                listOf("SELECT LAST_INSERT_ID()"),
                wrapper.querySql
            )
            assertEquals(99L, KronosOperationResult(1).also { it.stash["lastInsertId"] = 99L }.lastInsertId)
        } finally {
            LastInsertIdPlugin.enabled = oldEnabled
        }
    }

    @Test
    fun doAfterActionUsesEnabledPluginForImplicitQueryId() {
        val wrapper = LastInsertRecordingWrapper(objectResult = 101L)
        val task = KronosAtomicActionTask(
            sql = "INSERT INTO user(name) VALUES(:name)",
            paramMap = mapOf("name" to "alpha"),
            operationType = KOperationType.INSERT
        ).also {
            it.stash["useIdentity"] = true
        }
        val oldEnabled = LastInsertIdPlugin.enabled

        try {
            LastInsertIdPlugin.enabled = true
            LastInsertIdPlugin.doAfterAction(task, wrapper)

            assertEquals(101L, task.stash["lastInsertId"])
            assertEquals(listOf("SELECT LAST_INSERT_ID()"), wrapper.querySql)
        } finally {
            LastInsertIdPlugin.enabled = oldEnabled
        }
    }

    @Test
    fun executeQueriesLastInsertIdForExplicitQueryIdWithoutGlobalPlugin() {
        val wrapper = LastInsertRecordingWrapper(objectResult = 202L)
        val task = KronosAtomicActionTask(
            sql = "INSERT INTO user(name) VALUES(:name)",
            paramMap = mapOf("name" to "alpha"),
            operationType = KOperationType.INSERT
        ).also {
            it.stash["queryId"] = true
            it.stash["useIdentity"] = true
        }
        val oldEnabled = LastInsertIdPlugin.enabled

        try {
            LastInsertIdPlugin.enabled = false

            val result = task.execute(wrapper)

            assertEquals(202L, result.stash["lastInsertId"])
            assertEquals(
                listOf("SELECT LAST_INSERT_ID()"),
                wrapper.querySql
            )
        } finally {
            LastInsertIdPlugin.enabled = oldEnabled
        }
    }

    @Test
    fun actionTaskAfterExecuteCanReadLastAtomicTaskStash() {
        val wrapper = LastInsertRecordingWrapper(objectResult = 303L)
        val task = KronosAtomicActionTask(
            sql = "INSERT INTO user(name) VALUES(:name)",
            paramMap = mapOf("name" to "alpha"),
            operationType = KOperationType.INSERT
        ).also {
            it.stash["queryId"] = true
            it.stash["useIdentity"] = true
        }
        val oldEnabled = LastInsertIdPlugin.enabled
        var observedLastInsertId: Any? = null

        try {
            LastInsertIdPlugin.enabled = false

            val result = task.toKronosActionTask()
                .doAfterExecute {
                    observedLastInsertId = stash["lastInsertId"]
                }
                .execute(wrapper)

            assertEquals(303L, observedLastInsertId)
            assertEquals(303L, result.stash["lastInsertId"])
            assertEquals(
                listOf("SELECT LAST_INSERT_ID()"),
                wrapper.querySql
            )
        } finally {
            LastInsertIdPlugin.enabled = oldEnabled
        }
    }

    private class LastInsertRecordingWrapper(
        private val objectResult: Any? = null
    ) : KronosDataSourceWrapper {
        val querySql = mutableListOf<String>()
        override val url: String = "jdbc:mysql://localhost:3306/kronos"
        override val userName: String = "kronos"
        override val dbType: DBType = DBType.Mysql

        override fun toList(task: KAtomicQueryTask): List<Any?> = emptyList()

        override fun first(task: KAtomicQueryTask): Any? {
            querySql += task.sql
            return objectResult
        }

        override fun update(task: KAtomicActionTask): Int = 1
        override fun batchUpdate(task: KronosAtomicBatchTask): IntArray = intArrayOf(1)
        override fun transact(
            isolation: TransactionIsolation?,
            timeout: Int?,
            block: TransactionScope.() -> Any?
        ): Any? = TransactionScope().block()
    }
}
