package com.kotlinorm.integration.suites

import com.kotlinorm.integration.fixtures.IntegrationTypedValue
import com.kotlinorm.integration.fixtures.IntegrationTypedValueRecord
import com.kotlinorm.integration.profiles.IntegrationScenarioProfile
import com.kotlinorm.integration.support.IntegrationDatabaseEnvironment
import com.kotlinorm.integration.support.IntegrationSuiteSupport
import com.kotlinorm.orm.ddl.table
import com.kotlinorm.orm.insert.insert
import com.kotlinorm.orm.select.select
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

abstract class ValueTypeIntegrationSuite(
    environment: IntegrationDatabaseEnvironment,
    profile: IntegrationScenarioProfile,
) : IntegrationSuiteSupport(environment, profile) {
    @Test
    fun primitiveDecimalDateTimeAndNullableValuesRoundTripAgainstRealDatabase() {
        recreateTypedValueTable()
        seedTypedValues()

        assertEquals(
            listOf(
                IntegrationTypedValueRecord(
                    id = 1,
                    longValue = 1_000_000_001L,
                    textValue = "Ada Lovelace",
                    flagValue = true,
                    decimalValue = "12.30",
                    createdAt = LocalDateTime.of(2026, 1, 2, 3, 4, 5),
                    optionalScore = null,
                ),
                IntegrationTypedValueRecord(
                    id = 2,
                    longValue = 1_000_000_002L,
                    textValue = "Grace Hopper",
                    flagValue = false,
                    decimalValue = "45.67",
                    createdAt = LocalDateTime.of(2026, 2, 3, 4, 5, 6),
                    optionalScore = 2,
                ),
                IntegrationTypedValueRecord(
                    id = 3,
                    longValue = 1_000_000_003L,
                    textValue = "Linus Torvalds",
                    flagValue = true,
                    decimalValue = "99.01",
                    createdAt = LocalDateTime.of(2026, 3, 4, 5, 6, 7),
                    optionalScore = 2,
                ),
            ),
            IntegrationTypedValue()
                .select()
                .orderBy { it.id.asc() }
                .toList<IntegrationTypedValue>()
                .map { it.toRecord() },
        )
    }

    protected fun recreateTypedValueTable() {
        assumeDatabaseAvailable()
        configureKronos()
        with(wrapper.table) {
            dropTable(IntegrationTypedValue())
            syncTable(IntegrationTypedValue())
            truncateTable(IntegrationTypedValue(), restartIdentity = restartIdentity)
        }
    }

    protected fun seedTypedValues() {
        typedValues.forEach { value ->
            assertEquals(1, value.insert().execute().affectedRows)
        }
    }

    protected fun IntegrationTypedValue.toRecord(): IntegrationTypedValueRecord =
        IntegrationTypedValueRecord(
            id = id,
            longValue = longValue,
            textValue = textValue,
            flagValue = flagValue,
            decimalValue = decimalValue?.setScale(2, RoundingMode.UNNECESSARY)?.toPlainString(),
            createdAt = createdAt,
            optionalScore = optionalScore,
        )

    protected val typedValues: List<IntegrationTypedValue>
        get() = listOf(
            IntegrationTypedValue(
                id = 1,
                longValue = 1_000_000_001L,
                textValue = "Ada Lovelace",
                flagValue = true,
                decimalValue = BigDecimal("12.30"),
                createdAt = LocalDateTime.of(2026, 1, 2, 3, 4, 5),
                optionalScore = null,
            ),
            IntegrationTypedValue(
                id = 2,
                longValue = 1_000_000_002L,
                textValue = "Grace Hopper",
                flagValue = false,
                decimalValue = BigDecimal("45.67"),
                createdAt = LocalDateTime.of(2026, 2, 3, 4, 5, 6),
                optionalScore = 2,
            ),
            IntegrationTypedValue(
                id = 3,
                longValue = 1_000_000_003L,
                textValue = "Linus Torvalds",
                flagValue = true,
                decimalValue = BigDecimal("99.01"),
                createdAt = LocalDateTime.of(2026, 3, 4, 5, 6, 7),
                optionalScore = 2,
            ),
        )
}
