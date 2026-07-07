package com.kotlinorm.integration.suites

import com.kotlinorm.functions.bundled.exts.MathFunctions.mod
import com.kotlinorm.functions.bundled.exts.PolymerizationFunctions.count
import com.kotlinorm.functions.bundled.exts.PolymerizationFunctions.max
import com.kotlinorm.functions.bundled.exts.PolymerizationFunctions.min
import com.kotlinorm.functions.bundled.exts.PolymerizationFunctions.sum
import com.kotlinorm.functions.bundled.exts.StringFunctions.length
import com.kotlinorm.functions.bundled.exts.StringFunctions.lower
import com.kotlinorm.functions.bundled.exts.StringFunctions.upper
import com.kotlinorm.integration.fixtures.IntegrationAggregateRecord
import com.kotlinorm.integration.fixtures.IntegrationFunctionRecord
import com.kotlinorm.integration.fixtures.IntegrationTypedValue
import com.kotlinorm.integration.fixtures.IntegrationUser
import com.kotlinorm.integration.profiles.IntegrationScenarioProfile
import com.kotlinorm.integration.support.IntegrationDatabaseEnvironment
import com.kotlinorm.integration.support.IntegrationSuiteSupport
import com.kotlinorm.orm.ddl.table
import com.kotlinorm.orm.insert.insert
import com.kotlinorm.orm.select.select
import kotlin.test.Test
import kotlin.test.assertEquals

abstract class FunctionAndParameterIntegrationSuite(
    environment: IntegrationDatabaseEnvironment,
    profile: IntegrationScenarioProfile,
) : IntegrationSuiteSupport(environment, profile) {
    @Test
    fun stringMathAndAggregateFunctionsExecuteAgainstRealDatabase() {
        recreateTables()
        profile.seedUsersAndOrders()

        assertEquals(
            listOf(
                IntegrationFunctionRecord(id = 1, nameLength = 3, upperName = "ADA", lowerName = "ada", scoreMod = 0),
                IntegrationFunctionRecord(id = 4, nameLength = 7, upperName = "NOORDER", lowerName = "noorder", scoreMod = 1),
            ),
            IntegrationUser()
                .select {
                    [
                        it.id,
                        f.length(it.name).alias("nameLength"),
                        f.upper(it.name).alias("upperName"),
                        f.lower(it.name).alias("lowerName"),
                        (it.score % 2).alias("scoreMod")
                    ]
                }
                .where { it.id in [1, 4] }
                .orderBy { it.id.asc() }
                .queryList()
                .map {
                    IntegrationFunctionRecord(
                        id = it.id,
                        nameLength = it.nameLength.toIntValue(),
                        upperName = it.upperName?.toString(),
                        lowerName = it.lowerName?.toString(),
                        scoreMod = it.scoreMod.toIntValue(),
                    )
                },
        )

        assertEquals(
            IntegrationAggregateRecord(total = 4, scoreSum = 65, minScore = 5, maxScore = 30),
            IntegrationUser()
                .select {
                    [
                        f.count(1).alias("total"),
                        f.sum(it.score).alias("scoreSum"),
                        f.min(it.score).alias("minScore"),
                        f.max(it.score).alias("maxScore")
                    ]
                }
                .queryList()
                .single()
                .let {
                    IntegrationAggregateRecord(
                        total = it.total.toIntValue(),
                        scoreSum = it.scoreSum.toLongValue(),
                        minScore = it.minScore.toIntValue(),
                        maxScore = it.maxScore.toIntValue(),
                    )
                },
        )
    }

    private fun Any?.toIntValue(): Int? =
        when (this) {
            null -> null
            is Number -> toInt()
            else -> toString().toInt()
        }

    private fun Any?.toLongValue(): Long? =
        when (this) {
            null -> null
            is Number -> toLong()
            else -> toString().toLong()
        }
}
