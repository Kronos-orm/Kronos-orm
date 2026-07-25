package com.kotlinorm.integration.suites

import com.kotlinorm.functions.bundled.exts.MathFunctions.mod
import com.kotlinorm.functions.bundled.exts.MathFunctions.log
import com.kotlinorm.functions.bundled.exts.MathFunctions.trunc
import com.kotlinorm.functions.bundled.exts.PolymerizationFunctions.count
import com.kotlinorm.functions.bundled.exts.PolymerizationFunctions.groupConcat
import com.kotlinorm.functions.bundled.exts.PolymerizationFunctions.max
import com.kotlinorm.functions.bundled.exts.PolymerizationFunctions.min
import com.kotlinorm.functions.bundled.exts.PolymerizationFunctions.sum
import com.kotlinorm.functions.bundled.exts.StringFunctions.join
import com.kotlinorm.functions.bundled.exts.StringFunctions.length
import com.kotlinorm.functions.bundled.exts.StringFunctions.lower
import com.kotlinorm.functions.bundled.exts.StringFunctions.right
import com.kotlinorm.functions.bundled.exts.StringFunctions.upper
import com.kotlinorm.functions.bundled.exts.WindowFunctions.rowNumber
import com.kotlinorm.integration.fixtures.IntegrationAggregateProjection
import com.kotlinorm.integration.fixtures.IntegrationAggregateRecord
import com.kotlinorm.integration.fixtures.IntegrationFunctionProjection
import com.kotlinorm.integration.fixtures.IntegrationFunctionRecord
import com.kotlinorm.integration.fixtures.IntegrationUser
import com.kotlinorm.integration.profiles.IntegrationScenarioProfile
import com.kotlinorm.integration.support.IntegrationDatabaseEnvironment
import com.kotlinorm.integration.support.IntegrationSuiteSupport
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
                .toList<IntegrationFunctionProjection>()
                .map {
                    IntegrationFunctionRecord(
                        id = it.id,
                        nameLength = it.nameLength,
                        upperName = it.upperName,
                        lowerName = it.lowerName,
                        scoreMod = it.scoreMod,
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
                .toList<IntegrationAggregateProjection>()
                .single()
                .let {
                    IntegrationAggregateRecord(
                        total = it.total,
                        scoreSum = it.scoreSum,
                        minScore = it.minScore,
                        maxScore = it.maxScore,
                    )
                },
        )
    }

    @Test
    fun selectedAliasesCanBeUsedInOrderByAgainstRealDatabase() {
        recreateTables()
        profile.seedUsersAndOrders()

        assertEquals(
            listOf(3, 5, 5, 7),
            IntegrationUser()
                .select { f.length(x = it.name).alias("nameLength") }
                .orderBy { it.nameLength }
                .toList<IntegrationFunctionProjection>()
                .map { it.nameLength },
        )

        assertEquals(
            listOf("Ada", "Grace", "Linus", "NoOrder"),
            IntegrationUser()
                .select { [it.id, it.name.alias("displayName")] }
                .orderBy { it.displayName.asc() }
                .toList<IntegrationFunctionProjection>()
                .map { it.displayName },
        )

        assertEquals(
            listOf(4, 1, 2, 3),
            IntegrationUser()
                .select { [it.id, (it.score % 2).alias("scoreMod"), f.length(it.name).alias("nameLength")] }
                .orderBy { [it.scoreMod.desc(), it.nameLength.asc(), it.id.asc()] }
                .toList<IntegrationFunctionProjection>()
                .map { it.id },
        )
    }

    @Test
    fun aggregateScalarFunctionsMapDirectlyToRequestedTypesAgainstRealDatabase() {
        recreateTables()
        profile.seedUsersAndOrders()

        assertEquals(
            4,
            IntegrationUser()
                .select { f.count(1).alias("total") }
                .first<Int>(),
        )
        assertEquals(
            65L,
            IntegrationUser()
                .select { f.sum(it.score).alias("scoreSum") }
                .first<Long>(),
        )
        assertEquals(
            5,
            IntegrationUser()
                .select { f.min(it.score).alias("minScore") }
                .first<Int>(),
        )
        assertEquals(
            30,
            IntegrationUser()
                .select { f.max(it.score).alias("maxScore") }
                .first<Int>(),
        )
    }

    @Test
    fun dialectLoweredFunctionsPreserveTheirPublicSemantics() {
        recreateTables()
        profile.seedUsersAndOrders()

        assertEquals(
            "def",
            IntegrationUser()
                .select { f.right("abcdef", 3).alias("rightValue") }
                .first<String>(),
        )
        assertEquals(
            "a-b",
            IntegrationUser()
                .select { f.join("-", "a", null, "b").alias("joinedValue") }
                .first<String>(),
        )
        assertEquals(
            2.0,
            IntegrationUser()
                .select { f.log(100, 10).alias("logValue") }
                .first<Double>(),
            absoluteTolerance = 0.000001,
        )
        assertEquals(
            12.34,
            IntegrationUser()
                .select { f.trunc(12.349, 2).alias("truncatedValue") }
                .first<Double>(),
            absoluteTolerance = 0.000001,
        )
        assertEquals(
            "Ada",
            IntegrationUser()
                .select { f.groupConcat(it.name).alias("groupedName") }
                .where { it.id == 1 }
                .first<String>(),
        )
        assertEquals(
            listOf(1, 2, 3, 4),
            IntegrationUser()
                .select {
                    [
                        it.id,
                        f.rowNumber()
                            .over { orderBy(it.id.asc()) }
                            .alias("rn")
                    ]
                }
                .orderBy { it.rn.asc() }
                .toList()
                .map { it.rn },
        )
    }
}
