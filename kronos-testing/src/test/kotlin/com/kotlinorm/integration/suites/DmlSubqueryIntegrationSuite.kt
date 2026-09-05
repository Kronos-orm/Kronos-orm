package com.kotlinorm.integration.suites

import com.kotlinorm.integration.fixtures.IntegrationArchiveRecord
import com.kotlinorm.integration.fixtures.IntegrationUserRecord
import com.kotlinorm.integration.profiles.IntegrationScenarioProfile
import com.kotlinorm.integration.support.IntegrationDatabaseEnvironment
import com.kotlinorm.integration.support.IntegrationSuiteSupport
import kotlin.test.Test
import kotlin.test.assertEquals

abstract class DmlSubqueryIntegrationSuite(
    environment: IntegrationDatabaseEnvironment,
    profile: IntegrationScenarioProfile,
) : IntegrationSuiteSupport(environment, profile) {
    @Test
    fun updateAndDeleteDmlSubqueriesExecuteAgainstRealDatabase() {
        recreateTables()
        profile.seedUsersAndOrders()

        assertEquals(2, profile.updateScoresFromPaidOrders())
        assertEquals(
            listOf(
                IntegrationUserRecord(id = 1, name = "Ada", score = 50, status = 1),
                IntegrationUserRecord(id = 2, name = "Grace", score = 40, status = 1),
                IntegrationUserRecord(id = 3, name = "Linus", score = 30, status = 2),
                IntegrationUserRecord(id = 4, name = "NoOrder", score = 5, status = 0),
            ),
            profile.selectAllUsers(),
        )

        assertEquals(2, profile.deleteUsersWithoutOrders())
        assertEquals(
            listOf(
                IntegrationUserRecord(id = 1, name = "Ada", score = 50, status = 1),
                IntegrationUserRecord(id = 2, name = "Grace", score = 40, status = 1),
            ),
            profile.selectAllUsers(),
        )
    }

    @Test
    fun insertSelectArchivesPaidOrdersAgainstRealDatabase() {
        recreateTables()
        profile.seedUsersAndOrders()

        assertEquals(2, profile.insertPaidOrdersToArchive())
        assertEquals(
            listOf(
                IntegrationArchiveRecord(id = 1, userId = 1, amount = 50, status = 1),
                IntegrationArchiveRecord(id = 3, userId = 2, amount = 40, status = 1),
            ),
            profile.selectArchives(),
        )
    }
}
