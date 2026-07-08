package com.kotlinorm.integration.suites

import com.kotlinorm.integration.fixtures.IntegrationUserRecord
import com.kotlinorm.integration.profiles.IntegrationScenarioProfile
import com.kotlinorm.integration.support.IntegrationDatabaseEnvironment
import com.kotlinorm.integration.support.IntegrationSuiteSupport
import kotlin.test.Test
import kotlin.test.assertEquals

abstract class CrudWhereIntegrationSuite(
    environment: IntegrationDatabaseEnvironment,
    profile: IntegrationScenarioProfile,
) : IntegrationSuiteSupport(environment, profile) {
    @Test
    fun crudWhereVariantsAndOrderedLimitExecuteAgainstRealDatabase() {
        recreateTables()

        assertEquals(1, profile.insertUser(IntegrationUserRecord(id = 9, name = "Temp", score = 9, status = 9)))
        assertEquals(IntegrationUserRecord(id = 9, name = "Temp", score = 9, status = 9), profile.selectUserById(9))
        assertEquals(1, profile.updateUserScore(id = 9, score = 19))
        assertEquals(IntegrationUserRecord(id = 9, name = "Temp", score = 19, status = 9), profile.selectUserById(9))
        assertEquals(1, profile.deleteUserById(9))
        assertEquals(0, profile.countUsers())

        profile.seedUsersAndOrders()
        assertEquals(
            listOf(
                IntegrationUserRecord(id = 1, name = "Ada", score = 10, status = 1),
                IntegrationUserRecord(id = 2, name = "Grace", score = 20, status = 1),
                IntegrationUserRecord(id = 3, name = "Linus", score = 30, status = 2),
                IntegrationUserRecord(id = 4, name = "NoOrder", score = 5, status = 0),
            ),
            profile.selectAllUsers(),
        )
        assertEquals(
            listOf(
                IntegrationUserRecord(id = 1, name = "Ada", score = 10, status = 1),
                IntegrationUserRecord(id = 2, name = "Grace", score = 20, status = 1),
            ),
            profile.selectUsersByExampleStatus(status = 1),
        )
        assertEquals(
            listOf(IntegrationUserRecord(id = 3, name = "Linus", score = 30, status = 2)),
            profile.selectUsersWithLambdaWhereOverride(),
        )
        assertEquals(
            listOf(IntegrationUserRecord(id = 2, name = "Grace", score = 20, status = 1)),
            profile.selectUsersWithChainedWhere(),
        )
        assertEquals(
            listOf(
                IntegrationUserRecord(id = 2, name = "Grace", score = 20, status = 1),
                IntegrationUserRecord(id = 1, name = "Ada", score = 10, status = 1),
            ),
            profile.selectUsersByStatusOrderedByScore(status = 1, limit = 2),
        )
    }
}
