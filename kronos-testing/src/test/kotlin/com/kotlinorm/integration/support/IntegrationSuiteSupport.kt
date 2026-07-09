package com.kotlinorm.integration.support

import com.kotlinorm.Kronos
import com.kotlinorm.enums.DBType
import com.kotlinorm.integration.fixtures.IntegrationArchive
import com.kotlinorm.integration.fixtures.IntegrationAggregateProjection
import com.kotlinorm.integration.fixtures.IntegrationAliasProjection
import com.kotlinorm.integration.fixtures.CascadeDepartment
import com.kotlinorm.integration.fixtures.CascadeEmployee
import com.kotlinorm.integration.fixtures.CascadeInsertDepartment
import com.kotlinorm.integration.fixtures.CascadeInsertEmployee
import com.kotlinorm.integration.fixtures.CascadeProject
import com.kotlinorm.integration.fixtures.CascadeTask
import com.kotlinorm.integration.fixtures.EdgeAccount
import com.kotlinorm.integration.fixtures.EdgeDefaultOnly
import com.kotlinorm.integration.fixtures.EdgeNullableOnly
import com.kotlinorm.integration.fixtures.EdgeTailNullInsertSelect
import com.kotlinorm.integration.fixtures.EdgeTupleInsertSelect
import com.kotlinorm.integration.fixtures.EdgeWideInsertSelect
import com.kotlinorm.integration.fixtures.IntegrationFunctionProjection
import com.kotlinorm.integration.fixtures.IntegrationDslEdgeProjection
import com.kotlinorm.integration.fixtures.IntegrationPageProjection
import com.kotlinorm.integration.fixtures.IntegrationTableState
import com.kotlinorm.integration.fixtures.IntegrationOrder
import com.kotlinorm.integration.fixtures.IntegrationJoinProjection
import com.kotlinorm.integration.fixtures.IntegrationTypedValue
import com.kotlinorm.integration.fixtures.IntegrationUser
import com.kotlinorm.integration.fixtures.IntegrationUserRecord
import com.kotlinorm.integration.fixtures.SchemaSyncShapeV1
import com.kotlinorm.integration.fixtures.SchemaSyncShapeV2
import com.kotlinorm.integration.profiles.IntegrationScenarioProfile
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.utils.registerKPojo
import com.kotlinorm.utils.registerKPojoFactory
import org.junit.Assume.assumeTrue
import kotlin.test.assertEquals

abstract class IntegrationSuiteSupport(
    protected val environment: IntegrationDatabaseEnvironment,
    protected val profile: IntegrationScenarioProfile,
) {
    protected val wrapper by lazy { environment.createWrapper() }

    protected fun recreateTables() {
        assumeDatabaseAvailable()
        configureKronos()
        profile.dropTables()
        profile.syncTables()
        profile.truncateTables(restartIdentity = restartIdentity)
        assertEquals(IntegrationTableState(users = true, orders = true, archives = true), profile.tableState())
        assertEquals(0, profile.countUsers())
        assertEquals(0, profile.countOrders())
        assertEquals(0, profile.countArchives())
    }

    protected fun assumeDatabaseAvailable() {
        assumeTrue("${environment.displayName} integration environment is disabled", environment.enabled)
        try {
            wrapper.verifyConnection(environment.probeSql)
        } catch (error: Throwable) {
            assumeTrue("${environment.displayName} is not reachable: ${error.message}", false)
        }
    }

    protected fun configureKronos() {
        registerIntegrationKPojoFactories()
        with(Kronos) {
            fieldNamingStrategy = lineHumpNamingStrategy
            tableNamingStrategy = lineHumpNamingStrategy
            createTimeStrategy.enabled = false
            updateTimeStrategy.enabled = false
            logicDeleteStrategy.enabled = false
            optimisticLockStrategy.enabled = false
            strictSetValue = false
            dataSource = { wrapper }
        }
    }

    protected fun executeUpsert(record: IntegrationUserRecord) {
        profile.upsertUser(record)
    }

    protected val restartIdentity: Boolean = true

    protected fun quote(identifier: String): String =
        when (wrapper.dbType) {
            DBType.Mysql -> "`$identifier`"
            DBType.Mssql -> "[$identifier]"
            DBType.Oracle -> "\"${identifier.uppercase()}\""
            DBType.Postgres,
            DBType.SQLite -> "\"$identifier\""
            else -> identifier
        }

    protected fun table(identifier: String): String = quote(identifier)

    protected fun Map<String, Any>.value(label: String): Any? =
        this[label] ?: this[label.uppercase()] ?: this[label.lowercase()]

    private fun registerIntegrationKPojoFactories() {
        if (kPojoFactoriesRegistered) return
        registerKPojo(IntegrationUser::class) { IntegrationUser() }
        registerKPojo(IntegrationOrder::class) { IntegrationOrder() }
        registerKPojo(IntegrationArchive::class) { IntegrationArchive() }
        registerKPojo(IntegrationJoinProjection::class) { IntegrationJoinProjection() }
        registerKPojo(IntegrationFunctionProjection::class) { IntegrationFunctionProjection() }
        registerKPojo(IntegrationDslEdgeProjection::class) { IntegrationDslEdgeProjection() }
        registerKPojo(IntegrationAggregateProjection::class) { IntegrationAggregateProjection() }
        registerKPojo(IntegrationAliasProjection::class) { IntegrationAliasProjection() }
        registerKPojo(IntegrationPageProjection::class) { IntegrationPageProjection() }
        registerKPojo(IntegrationTypedValue::class) { IntegrationTypedValue() }
        registerKPojo(EdgeAccount::class) { EdgeAccount() }
        registerKPojo(EdgeNullableOnly::class) { EdgeNullableOnly() }
        registerKPojo(EdgeDefaultOnly::class) { EdgeDefaultOnly() }
        registerKPojo(EdgeWideInsertSelect::class) { EdgeWideInsertSelect() }
        registerKPojo(EdgeTupleInsertSelect::class) { EdgeTupleInsertSelect() }
        registerKPojo(EdgeTailNullInsertSelect::class) { EdgeTailNullInsertSelect() }
        registerKPojo(SchemaSyncShapeV1::class) { SchemaSyncShapeV1() }
        registerKPojo(SchemaSyncShapeV2::class) { SchemaSyncShapeV2() }
        registerKPojo(CascadeInsertDepartment::class) { CascadeInsertDepartment() }
        registerKPojo(CascadeInsertEmployee::class) { CascadeInsertEmployee() }
        registerKPojo(CascadeDepartment::class) { CascadeDepartment() }
        registerKPojo(CascadeEmployee::class) { CascadeEmployee() }
        registerKPojo(CascadeProject::class) { CascadeProject() }
        registerKPojo(CascadeTask::class) { CascadeTask() }
        registerKPojoFactory { kClass ->
            if (kClass.qualifiedName?.startsWith("com.kotlinorm.generated.projection.KronosSelectContext_") == true) {
                kClass.java.getDeclaredConstructor().newInstance() as KPojo
            } else {
                null
            }
        }
        kPojoFactoriesRegistered = true
    }

    companion object {
        private var kPojoFactoriesRegistered = false
    }
}
