package com.kotlinorm.integration.support

import com.kotlinorm.Kronos
import com.kotlinorm.enums.DBType
import com.kotlinorm.integration.fixtures.IntegrationTableState
import com.kotlinorm.integration.fixtures.IntegrationUserRecord
import com.kotlinorm.integration.profiles.IntegrationScenarioProfile
import kotlin.test.assertEquals

abstract class IntegrationSuiteSupport(
    protected val environment: IntegrationDatabaseEnvironment,
    protected val profile: IntegrationScenarioProfile,
) {
    protected val wrapper by lazy { environment.createWrapper() }

    protected fun recreateTables() {
        requireDatabaseAvailable()
        configureKronos()
        profile.dropTables()
        profile.syncTables()
        profile.truncateTables(restartIdentity = restartIdentity)
        assertEquals(IntegrationTableState(users = true, orders = true, archives = true), profile.tableState())
        assertEquals(0, profile.countUsers())
        assertEquals(0, profile.countOrders())
        assertEquals(0, profile.countArchives())
    }

    protected fun requireDatabaseAvailable() {
        check(environment.enabled) {
            "${environment.displayName} integration environment is disabled"
        }
        try {
            wrapper.verifyConnection(environment.probeSql)
        } catch (error: Throwable) {
            throw AssertionError(
                "${environment.displayName} integration database is not reachable: ${error.message}",
                error,
            )
        }
    }

    protected fun configureKronos() {
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
            DBType.Oracle,
            DBType.DM8 -> "\"${identifier.uppercase()}\""
            DBType.Postgres,
            DBType.SQLite,
            DBType.H2 -> "\"$identifier\""
            else -> identifier
        }

    protected fun table(identifier: String): String = quote(identifier)

    protected fun Map<String, Any?>.value(label: String): Any? =
        this[label] ?: this[label.uppercase()] ?: this[label.lowercase()]
}
