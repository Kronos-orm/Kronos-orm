package com.kotlinorm.integration

import com.kotlinorm.database.SqlExecutor.execute
import com.kotlinorm.database.SqlExecutor.query
import com.kotlinorm.database.SqlManager
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.integration.fixtures.H2PrecisionValue
import com.kotlinorm.integration.profiles.StandardIntegrationScenarioProfile
import com.kotlinorm.integration.fixtures.CascadeInsertDepartment
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
import com.kotlinorm.integration.suites.ValueMappingIntegrationSuite
import com.kotlinorm.integration.suites.ValueTypeIntegrationSuite
import com.kotlinorm.integration.suites.WrapperSqlIntegrationSuite
import com.kotlinorm.integration.support.IntegrationDatabaseEnvironments.h2
import com.kotlinorm.integration.support.IntegrationSuiteSupport
import com.kotlinorm.interfaces.KAtomicActionTask
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.orm.ddl.queryTableColumns
import com.kotlinorm.orm.ddl.table
import com.kotlinorm.orm.insert.insert
import com.kotlinorm.syntax.SqlIdentifier
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlParameter
import com.kotlinorm.syntax.statement.SqlAssignmentTarget
import com.kotlinorm.syntax.statement.SqlDmlStatement
import com.kotlinorm.syntax.statement.SqlUpdateSetPair
import com.kotlinorm.syntax.statement.SqlUpsertAction
import com.kotlinorm.syntax.table.SqlTable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class H2SchemaIntegrationTest : SchemaIntegrationSuite(h2, StandardIntegrationScenarioProfile)
class H2SchemaSyncRegressionTest : SchemaSyncRegressionSuite(h2, StandardIntegrationScenarioProfile)
class H2CrudWhereIntegrationTest : CrudWhereIntegrationSuite(h2, StandardIntegrationScenarioProfile)
class H2QueryIntegrationTest : QueryIntegrationSuite(h2, StandardIntegrationScenarioProfile)
class H2ResultMethodEdgeIntegrationTest : ResultMethodEdgeIntegrationSuite(h2, StandardIntegrationScenarioProfile)
class H2DslEdgeCaseIntegrationTest : DslEdgeCaseIntegrationSuite(h2, StandardIntegrationScenarioProfile)
class H2ComplexQueryProjectionIntegrationTest : ComplexQueryProjectionIntegrationSuite(h2, StandardIntegrationScenarioProfile)
class H2DmlSubqueryIntegrationTest : DmlSubqueryIntegrationSuite(h2, StandardIntegrationScenarioProfile)
class H2DmlUpsertCornerCaseIntegrationTest : DmlUpsertCornerCaseIntegrationSuite(h2, StandardIntegrationScenarioProfile)
class H2UpsertIntegrationTest : UpsertIntegrationSuite(h2, StandardIntegrationScenarioProfile)
class H2ErrorIntegrationTest : ErrorIntegrationSuite(h2, StandardIntegrationScenarioProfile)
class H2WrapperSqlIntegrationTest : WrapperSqlIntegrationSuite(h2, StandardIntegrationScenarioProfile)
class H2CascadeIntegrationTest : CascadeIntegrationSuite(h2, StandardIntegrationScenarioProfile)
class H2ValueTypeIntegrationTest : ValueTypeIntegrationSuite(h2, StandardIntegrationScenarioProfile)
class H2ValueMappingIntegrationTest : ValueMappingIntegrationSuite(h2, StandardIntegrationScenarioProfile)
class H2TypeDialectDdlCornerIntegrationTest : TypeDialectDdlCornerIntegrationSuite(h2, StandardIntegrationScenarioProfile)
class H2FunctionAndParameterIntegrationTest : FunctionAndParameterIntegrationSuite(h2, StandardIntegrationScenarioProfile)
class H2TransactionIntegrationTest : TransactionIntegrationSuite(h2, StandardIntegrationScenarioProfile)
class H2SafetyCornerCaseIntegrationTest : SafetyCornerCaseIntegrationSuite(h2, StandardIntegrationScenarioProfile)
class H2EdgeCaseIntegrationTest : EdgeCaseIntegrationSuite(h2, StandardIntegrationScenarioProfile)
class H2StrategyIntegrationTest : StrategyIntegrationSuite(h2, StandardIntegrationScenarioProfile)

class H2GeneratedKeyIntegrationTest : IntegrationSuiteSupport(h2, StandardIntegrationScenarioProfile) {
    @Test
    fun identityInsertReturnsJdbcGeneratedKey() {
        requireDatabaseAvailable()
        configureKronos()

        with(wrapper.table) {
            dropTable(CascadeInsertDepartment())
            createTable(CascadeInsertDepartment())
        }
        try {
            val result = CascadeInsertDepartment(name = "H2 generated key")
                .insert()
                .withId()
                .execute()

            assertEquals(1L, result.lastInsertId)
        } finally {
            wrapper.table.dropTable(CascadeInsertDepartment())
        }
    }
}

class H2DialectIntegrationTest : IntegrationSuiteSupport(h2, StandardIntegrationScenarioProfile) {
    @Test
    fun precisionMetadataIsCanonicalAndSecondSyncIsNoop() {
        requireDatabaseAvailable()
        configureKronos()

        with(wrapper.table) {
            dropTable(H2PrecisionValue())
            syncTable(H2PrecisionValue())
        }
        try {
            val columns = queryTableColumns("kt_h2_precision_value", wrapper).associateBy {
                it.columnName.lowercase()
            }

            assertEquals(KColumnType.DOUBLE, columns.getValue("approximate_value").type)
            assertEquals(KColumnType.NUMERIC, columns.getValue("exact_value").type)
            assertEquals(12, columns.getValue("exact_value").length)
            assertEquals(4, columns.getValue("exact_value").scale)
            assertEquals(KColumnType.TIME, columns.getValue("local_time").type)
            assertEquals(3, columns.getValue("local_time").scale)
            assertEquals(KColumnType.TIMESTAMP, columns.getValue("local_date_time").type)
            assertEquals(6, columns.getValue("local_date_time").scale)
            assertEquals(KColumnType.TIMESTAMP, columns.getValue("timestamp_value").type)
            assertEquals(6, columns.getValue("timestamp_value").scale)

            val recordingWrapper = RecordingWrapper(wrapper)
            assertTrue(recordingWrapper.table.syncTable(H2PrecisionValue()))
            assertEquals(emptyList(), recordingWrapper.actions)
            assertEquals(emptyList(), recordingWrapper.batchActions)
        } finally {
            wrapper.table.dropTable(H2PrecisionValue())
        }
    }

    @Test
    fun conditionalMergeUpdatesOnlyWhenTheMatchedPredicateIsTrue() {
        requireDatabaseAvailable()
        configureKronos()
        val table = table("kt_h2_conditional_merge")
        val id = quote("id")
        val name = quote("name")
        val status = quote("status")
        wrapper.execute("DROP TABLE IF EXISTS $table")
        wrapper.execute("CREATE TABLE $table ($id INTEGER PRIMARY KEY, $name VARCHAR(80), $status INTEGER)")
        wrapper.execute("INSERT INTO $table ($id, $name, $status) VALUES (1, 'initial', 1)")
        try {
            executeConditionalMerge("updated", 2)
            assertEquals("updated", wrapper.query("SELECT $name, $status FROM $table").single().value("NAME"))
            assertEquals(2, wrapper.query("SELECT $name, $status FROM $table").single().value("STATUS"))

            executeConditionalMerge("ignored", 3)
            val row = wrapper.query("SELECT $name, $status FROM $table").single()
            assertEquals("updated", row.value("NAME"))
            assertEquals(2, row.value("STATUS"))
        } finally {
            wrapper.execute("DROP TABLE IF EXISTS $table")
        }
    }

    private fun executeConditionalMerge(name: String, status: Int) {
        val id = SqlIdentifier.of("id")
        val nameColumn = SqlIdentifier.of("name")
        val statusColumn = SqlIdentifier.of("status")
        val statement = SqlDmlStatement.Upsert(
            table = SqlTable.Ident("kt_h2_conditional_merge"),
            columns = listOf(id, nameColumn, statusColumn),
            values = listOf(
                SqlExpr.Parameter(SqlParameter.Named("id")),
                SqlExpr.Parameter(SqlParameter.Named("name")),
                SqlExpr.Parameter(SqlParameter.Named("status"))
            ),
            primaryKeys = listOf(id),
            action = SqlUpsertAction.Update(
                setPairs = listOf(
                    SqlUpdateSetPair(
                        SqlAssignmentTarget.Column(nameColumn),
                        SqlExpr.Parameter(SqlParameter.Named("name"))
                    ),
                    SqlUpdateSetPair(
                        SqlAssignmentTarget.Column(statusColumn),
                        SqlExpr.Parameter(SqlParameter.Named("status"))
                    )
                ),
                where = SqlExpr.Binary(
                    SqlExpr.Column(tableName = "kt_h2_conditional_merge", columnName = "status"),
                    SqlBinaryOperator.Equal,
                    SqlExpr.NumberLiteral("1")
                )
            )
        )
        val rendered = SqlManager.renderStatement(
            wrapper,
            statement,
            mapOf("id" to 1, "name" to name, "status" to status)
        )
        wrapper.execute(rendered.sql, rendered.parameters)
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
