package com.kotlinorm.integration.suites

import com.kotlinorm.integration.fixtures.IntegrationArchiveRecord
import com.kotlinorm.integration.fixtures.IntegrationOrderRecord
import com.kotlinorm.integration.fixtures.IntegrationTableState
import com.kotlinorm.integration.fixtures.IntegrationUserRecord
import com.kotlinorm.integration.profiles.IntegrationScenarioProfile
import com.kotlinorm.integration.support.IntegrationDatabaseEnvironment
import com.kotlinorm.integration.support.IntegrationSuiteSupport
import kotlin.test.Test
import kotlin.test.assertEquals

abstract class SchemaIntegrationSuite(
    environment: IntegrationDatabaseEnvironment,
    profile: IntegrationScenarioProfile,
) : IntegrationSuiteSupport(environment, profile) {
    @Test
    fun schemaLifecycleExecutesAgainstRealDatabase() {
        requireDatabaseAvailable()
        configureKronos()

        profile.dropTables()
        assertEquals(IntegrationTableState(users = false, orders = false, archives = false), profile.tableState())

        profile.syncTables()
        assertEquals(IntegrationTableState(users = true, orders = true, archives = true), profile.tableState())

        profile.dropTables()
        assertEquals(IntegrationTableState(users = false, orders = false, archives = false), profile.tableState())

        profile.createTables()
        assertEquals(IntegrationTableState(users = true, orders = true, archives = true), profile.tableState())

        profile.syncTables()
        assertEquals(IntegrationTableState(users = true, orders = true, archives = true), profile.tableState())

        assertEquals(1, profile.insertUser(IntegrationUserRecord(id = 100, name = "Lifecycle", score = 9, status = 1)))
        assertEquals(1, profile.insertOrder(IntegrationOrderRecord(id = 100, userId = 100, status = 1, amount = 90)))
        assertEquals(1, profile.insertPaidOrdersToArchive())
        assertEquals(1, profile.countUsers())
        assertEquals(1, profile.countOrders())
        assertEquals(1, profile.countArchives())
        assertEquals(listOf(IntegrationArchiveRecord(id = 100, userId = 100, amount = 90, status = 1)), profile.selectArchives())

        profile.truncateTables(restartIdentity = restartIdentity)
        assertEquals(0, profile.countUsers())
        assertEquals(0, profile.countOrders())
        assertEquals(0, profile.countArchives())

        profile.dropTables()
        assertEquals(IntegrationTableState(users = false, orders = false, archives = false), profile.tableState())
    }
}
