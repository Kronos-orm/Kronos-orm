package com.kotlinorm.integration.suites

import com.kotlinorm.integration.fixtures.IntegrationUserRecord
import com.kotlinorm.integration.profiles.IntegrationScenarioProfile
import com.kotlinorm.integration.support.IntegrationDatabaseEnvironment
import com.kotlinorm.integration.support.IntegrationSuiteSupport
import org.junit.Assume.assumeTrue
import kotlin.test.Test
import kotlin.test.assertEquals

abstract class UpsertIntegrationSuite(
    environment: IntegrationDatabaseEnvironment,
    profile: IntegrationScenarioProfile,
) : IntegrationSuiteSupport(environment, profile) {
    @Test
    fun upsertInsertAndUpdateExecuteAgainstRealDatabase() {
        assumeTrue("Native conflict upsert is not supported by ${environment.displayName}", environment.supportsConflictUpsert)
        recreateTables()

        assertEquals(1, profile.insertUser(IntegrationUserRecord(id = 10, name = "first", score = 10, status = 1)))
        executeUpsert(IntegrationUserRecord(id = 10, name = "second", score = 20, status = 2))
        assertEquals(
            IntegrationUserRecord(id = 10, name = "second", score = 20, status = 2),
            profile.selectUserById(10),
        )

        executeUpsert(IntegrationUserRecord(id = 11, name = "inserted", score = 30, status = 1))
        assertEquals(
            listOf(
                IntegrationUserRecord(id = 10, name = "second", score = 20, status = 2),
                IntegrationUserRecord(id = 11, name = "inserted", score = 30, status = 1),
            ),
            profile.selectAllUsers(),
        )
    }
}
