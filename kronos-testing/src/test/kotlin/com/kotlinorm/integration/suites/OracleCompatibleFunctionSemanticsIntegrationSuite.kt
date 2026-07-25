package com.kotlinorm.integration.suites

import com.kotlinorm.functions.bundled.exts.StringFunctions.repeat
import com.kotlinorm.integration.fixtures.IntegrationUser
import com.kotlinorm.integration.profiles.IntegrationScenarioProfile
import com.kotlinorm.integration.support.IntegrationDatabaseEnvironment
import com.kotlinorm.integration.support.IntegrationSuiteSupport
import com.kotlinorm.orm.select.select
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

abstract class OracleCompatibleFunctionSemanticsIntegrationSuite(
    environment: IntegrationDatabaseEnvironment,
    profile: IntegrationScenarioProfile,
) : IntegrationSuiteSupport(environment, profile) {
    @Test
    fun repeatWithZeroPreservesNativeNullSemanticsAgainstRealDatabase() {
        recreateTables()
        profile.seedUsersAndOrders()

        val zeroRepeatValues = IntegrationUser()
            .select { f.repeat("x", 0).alias("repeatValue") }
            .toMapList()
            .map { it.value("repeatValue") }

        assertTrue(zeroRepeatValues.isNotEmpty())
        assertEquals(List(zeroRepeatValues.size) { null }, zeroRepeatValues)

        val positiveRepeatValues = IntegrationUser()
            .select { f.repeat("x", 3).alias("repeatValue") }
            .toMapList()
            .map { it.value("repeatValue") }

        assertEquals(List(positiveRepeatValues.size) { "xxx" }, positiveRepeatValues)
    }
}
