package com.kotlinorm.integration.suites

import com.kotlinorm.integration.fixtures.IntegrationUserRecord
import com.kotlinorm.integration.profiles.IntegrationScenarioProfile
import com.kotlinorm.integration.support.IntegrationDatabaseEnvironment
import com.kotlinorm.integration.support.IntegrationSuiteSupport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

abstract class ErrorIntegrationSuite(
    environment: IntegrationDatabaseEnvironment,
    profile: IntegrationScenarioProfile,
) : IntegrationSuiteSupport(environment, profile) {
    @Test
    fun duplicateKeyErrorsAreVisibleAgainstRealDatabase() {
        recreateTables()

        assertEquals(1, profile.insertUser(IntegrationUserRecord(id = 1, name = "Ada", score = 10, status = 1)))
        val duplicateKeyError = assertFailsWith<Throwable> {
            profile.insertUser(IntegrationUserRecord(id = 1, name = "Again", score = 11, status = 1))
        }
        assertEquals(false, duplicateKeyError.message.isNullOrBlank())
    }
}
