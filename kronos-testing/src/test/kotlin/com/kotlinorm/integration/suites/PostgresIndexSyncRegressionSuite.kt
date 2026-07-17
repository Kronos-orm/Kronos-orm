package com.kotlinorm.integration.suites

import com.kotlinorm.beans.dsl.KTableIndex
import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.integration.fixtures.PostgresIndexSyncProbe
import com.kotlinorm.integration.profiles.IntegrationScenarioProfile
import com.kotlinorm.integration.support.IntegrationDatabaseEnvironment
import com.kotlinorm.integration.support.IntegrationSuiteSupport
import com.kotlinorm.interfaces.KAtomicActionTask
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.ddl.queryTableIndexes
import com.kotlinorm.orm.ddl.table
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

abstract class PostgresIndexSyncRegressionSuite(
    environment: IntegrationDatabaseEnvironment,
    profile: IntegrationScenarioProfile,
) : IntegrationSuiteSupport(environment, profile) {
    @Test
    fun repeatedSyncKeepsExplicitAndDefaultBtreeIndexes() {
        requireDatabaseAvailable()
        configureKronos()

        wrapper.table.dropTable(PostgresIndexSyncProbe())
        wrapper.table.createTable(PostgresIndexSyncProbe())

        assertEquals(
            listOf(
                KTableIndex("idx_postgres_index_sync_category", arrayOf("category"), "NORMAL", "btree"),
                KTableIndex("idx_postgres_index_sync_value", arrayOf("value"), "NORMAL", "btree"),
            ),
            queryTableIndexes("kt_postgres_index_sync", wrapper).sortedBy { it.name }
        )

        val recordingWrapper = RecordingWrapper(wrapper)
        val existing = recordingWrapper.table.syncTable(PostgresIndexSyncProbe())

        assertTrue(existing)
        assertEquals(emptyList(), recordingWrapper.actions)
        assertEquals(emptyList(), recordingWrapper.batchActions)
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
