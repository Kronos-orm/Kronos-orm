package com.kotlinorm.integration

import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.integration.fixtures.CascadeInsertDepartment
import com.kotlinorm.integration.profiles.StandardIntegrationScenarioProfile
import com.kotlinorm.integration.suites.CascadeIntegrationSuite
import com.kotlinorm.integration.suites.ComplexQueryProjectionIntegrationSuite
import com.kotlinorm.integration.suites.CrudWhereIntegrationSuite
import com.kotlinorm.integration.suites.DmlSubqueryIntegrationSuite
import com.kotlinorm.integration.suites.DmlUpsertCornerCaseIntegrationSuite
import com.kotlinorm.integration.suites.DslEdgeCaseIntegrationSuite
import com.kotlinorm.integration.suites.EdgeCaseIntegrationSuite
import com.kotlinorm.integration.suites.ErrorIntegrationSuite
import com.kotlinorm.integration.suites.FunctionAndParameterIntegrationSuite
import com.kotlinorm.integration.suites.QueryIntegrationSuite
import com.kotlinorm.integration.suites.ResultMethodEdgeIntegrationSuite
import com.kotlinorm.integration.suites.SafetyCornerCaseIntegrationSuite
import com.kotlinorm.integration.suites.SchemaIntegrationSuite
import com.kotlinorm.integration.suites.SchemaSyncRegressionSuite
import com.kotlinorm.integration.suites.StrategyIntegrationSuite
import com.kotlinorm.integration.suites.TransactionIntegrationSuite
import com.kotlinorm.integration.suites.TypeDialectDdlCornerIntegrationSuite
import com.kotlinorm.integration.suites.UpsertIntegrationSuite
import com.kotlinorm.integration.suites.ValueTypeIntegrationSuite
import com.kotlinorm.integration.suites.WrapperSqlIntegrationSuite
import com.kotlinorm.integration.support.IntegrationDatabaseEnvironments.dm8
import com.kotlinorm.integration.support.IntegrationSuiteSupport
import com.kotlinorm.interfaces.KAtomicActionTask
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.ddl.table
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Dm8SchemaIntegrationTest : SchemaIntegrationSuite(dm8, StandardIntegrationScenarioProfile)

class Dm8SchemaSyncRegressionTest : SchemaSyncRegressionSuite(dm8, StandardIntegrationScenarioProfile)

class Dm8NativeIdentitySchemaSyncIntegrationTest : IntegrationSuiteSupport(dm8, StandardIntegrationScenarioProfile) {
    @Test
    fun secondSyncOfNativeIdentityTableDoesNotScheduleDdl() {
        requireDatabaseAvailable()
        configureKronos()

        with(wrapper.table) {
            dropTable(CascadeInsertDepartment())
            syncTable(CascadeInsertDepartment())
        }
        try {
            val recordingWrapper = RecordingWrapper(wrapper)

            assertTrue(recordingWrapper.table.syncTable(CascadeInsertDepartment()))
            assertEquals(emptyList(), recordingWrapper.actions)
            assertEquals(emptyList(), recordingWrapper.batchActions)
        } finally {
            wrapper.table.dropTable(CascadeInsertDepartment())
        }
    }

    private class RecordingWrapper(
        private val delegate: KronosDataSourceWrapper,
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

class Dm8CrudWhereIntegrationTest : CrudWhereIntegrationSuite(dm8, StandardIntegrationScenarioProfile)

class Dm8QueryIntegrationTest : QueryIntegrationSuite(dm8, StandardIntegrationScenarioProfile)

class Dm8ResultMethodEdgeIntegrationTest : ResultMethodEdgeIntegrationSuite(dm8, StandardIntegrationScenarioProfile)

class Dm8DslEdgeCaseIntegrationTest : DslEdgeCaseIntegrationSuite(dm8, StandardIntegrationScenarioProfile)

class Dm8ComplexQueryProjectionIntegrationTest : ComplexQueryProjectionIntegrationSuite(dm8, StandardIntegrationScenarioProfile)

class Dm8DmlSubqueryIntegrationTest : DmlSubqueryIntegrationSuite(dm8, StandardIntegrationScenarioProfile)

class Dm8DmlUpsertCornerCaseIntegrationTest : DmlUpsertCornerCaseIntegrationSuite(dm8, StandardIntegrationScenarioProfile)

class Dm8UpsertIntegrationTest : UpsertIntegrationSuite(dm8, StandardIntegrationScenarioProfile)

class Dm8ErrorIntegrationTest : ErrorIntegrationSuite(dm8, StandardIntegrationScenarioProfile)

class Dm8WrapperSqlIntegrationTest : WrapperSqlIntegrationSuite(dm8, StandardIntegrationScenarioProfile)

class Dm8CascadeIntegrationTest : CascadeIntegrationSuite(dm8, StandardIntegrationScenarioProfile)

class Dm8ValueTypeIntegrationTest : ValueTypeIntegrationSuite(dm8, StandardIntegrationScenarioProfile)

class Dm8TypeDialectDdlCornerIntegrationTest : TypeDialectDdlCornerIntegrationSuite(dm8, StandardIntegrationScenarioProfile)

class Dm8FunctionAndParameterIntegrationTest : FunctionAndParameterIntegrationSuite(dm8, StandardIntegrationScenarioProfile)

class Dm8TransactionIntegrationTest : TransactionIntegrationSuite(dm8, StandardIntegrationScenarioProfile)

class Dm8SafetyCornerCaseIntegrationTest : SafetyCornerCaseIntegrationSuite(dm8, StandardIntegrationScenarioProfile)

class Dm8EdgeCaseIntegrationTest : EdgeCaseIntegrationSuite(dm8, StandardIntegrationScenarioProfile)

class Dm8StrategyIntegrationTest : StrategyIntegrationSuite(dm8, StandardIntegrationScenarioProfile)
