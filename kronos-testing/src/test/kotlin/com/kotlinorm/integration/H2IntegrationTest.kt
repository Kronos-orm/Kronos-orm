package com.kotlinorm.integration

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
import com.kotlinorm.orm.ddl.table
import com.kotlinorm.orm.insert.insert
import kotlin.test.Test
import kotlin.test.assertEquals

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
