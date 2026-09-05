package com.kotlinorm.integration.suites

import com.kotlinorm.integration.fixtures.IntegrationJoinRecord
import com.kotlinorm.integration.fixtures.IntegrationUserRecord
import com.kotlinorm.integration.profiles.IntegrationScenarioProfile
import com.kotlinorm.integration.support.IntegrationDatabaseEnvironment
import com.kotlinorm.integration.support.IntegrationSuiteSupport
import kotlin.test.Test
import kotlin.test.assertEquals

abstract class QueryIntegrationSuite(
    environment: IntegrationDatabaseEnvironment,
    profile: IntegrationScenarioProfile,
) : IntegrationSuiteSupport(environment, profile) {
    @Test
    fun aggregateSubqueriesAndJoinProjectionExecuteAgainstRealDatabase() {
        recreateTables()
        profile.seedUsersAndOrders()

        assertEquals(2, profile.countUsersByStatus(status = 1))
        assertEquals(
            listOf(
                IntegrationUserRecord(id = 2, name = "Grace", score = 20, status = 1),
                IntegrationUserRecord(id = 3, name = "Linus", score = 30, status = 2),
            ),
            profile.selectUsersScoreHigherThanUser(1),
        )
        assertEquals(
            listOf(
                IntegrationUserRecord(id = 1, name = "Ada", score = 10, status = 1),
                IntegrationUserRecord(id = 2, name = "Grace", score = 20, status = 1),
            ),
            profile.selectUsersWithPaidOrdersByInSubquery(),
        )
        assertEquals(
            listOf(IntegrationUserRecord(id = 1, name = "Ada", score = 10, status = 1)),
            profile.selectUsersWithLargePaidOrdersByExistsSubquery(),
        )
        assertEquals(
            listOf(
                IntegrationJoinRecord(userId = 1, userName = "Ada", amount = 50),
                IntegrationJoinRecord(userId = 2, userName = "Grace", amount = 40),
            ),
            profile.selectPaidOrderJoinRecords(),
        )
    }
}
