package com.kotlinorm.integration.suites

import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.PrimaryKeyType
import com.kotlinorm.integration.fixtures.SchemaSyncUserV1
import com.kotlinorm.integration.fixtures.SchemaSyncUserV2
import com.kotlinorm.integration.profiles.IntegrationScenarioProfile
import com.kotlinorm.integration.support.IntegrationDatabaseEnvironment
import com.kotlinorm.integration.support.IntegrationSuiteSupport
import com.kotlinorm.interfaces.KAtomicActionTask
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.ddl.queryTableColumns
import com.kotlinorm.orm.ddl.table
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

abstract class SchemaSyncRegressionSuite(
    environment: IntegrationDatabaseEnvironment,
    profile: IntegrationScenarioProfile,
) : IntegrationSuiteSupport(environment, profile) {
    @Test
    fun syncTableAddsColumnAfterCustomStringPrimaryKeyAndTimestampColumns() {
        requireDatabaseAvailable()
        configureKronos()

        wrapper.table.dropTable(SchemaSyncUserV1())
        wrapper.table.createTable(SchemaSyncUserV1())

        val existing = wrapper.table.syncTable(SchemaSyncUserV2())
        val recordingWrapper = RecordingWrapper(wrapper)
        val existingAfterSecondSync = recordingWrapper.table.syncTable(SchemaSyncUserV2())

        assertTrue(existing)
        assertTrue(existingAfterSecondSync)
        assertEquals(emptyList(), recordingWrapper.actions)
        assertEquals(emptyList(), recordingWrapper.batchActions)
        val columns = queryTableColumns("kt_schema_sync_user", wrapper)
        val columnNames = columns.map { it.columnName.lowercase() }.toSet()
        val age = columns.single { it.columnName.equals("age", ignoreCase = true) }
        val id = columns.single { it.columnName.equals("id", ignoreCase = true) }

        assertEquals(setOf("id", "name", "description", "age", "create_time", "update_time"), columnNames)
        assertEquals(if (wrapper.dbType == DBType.SQLite) KColumnType.INT else KColumnType.BIGINT, age.type)
        assertEquals(true, age.nullable)
        assertEquals(PrimaryKeyType.NOT, age.primaryKey)
        assertTrue(id.primaryKey != PrimaryKeyType.NOT)
    }

    private class RecordingWrapper(
        private val delegate: KronosDataSourceWrapper
    ) : KronosDataSourceWrapper by delegate {
        val actions = mutableListOf<KAtomicActionTask>()
        val batchActions = mutableListOf<KronosAtomicBatchTask>()

        override fun update(task: KAtomicActionTask): Int {
            actions += task
            return delegate.update(task)
        }

        override fun batchUpdate(task: KronosAtomicBatchTask): IntArray {
            batchActions += task
            return delegate.batchUpdate(task)
        }
    }
}
