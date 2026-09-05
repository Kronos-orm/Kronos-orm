package com.kotlinorm.integration.suites

import com.kotlinorm.database.SqlExecutor.execute
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.integration.fixtures.TypeDialectBooleanDefaultValue
import com.kotlinorm.integration.fixtures.TypeDialectCornerRecord
import com.kotlinorm.integration.fixtures.TypeDialectCornerValue
import com.kotlinorm.integration.fixtures.TypeDialectSyncCornerRecord
import com.kotlinorm.integration.fixtures.TypeDialectSyncCornerV1
import com.kotlinorm.integration.fixtures.TypeDialectSyncCornerV2
import com.kotlinorm.integration.profiles.IntegrationScenarioProfile
import com.kotlinorm.integration.support.IntegrationDatabaseEnvironment
import com.kotlinorm.integration.support.IntegrationSuiteSupport
import com.kotlinorm.orm.ddl.queryTableColumns
import com.kotlinorm.orm.ddl.table
import com.kotlinorm.orm.insert.insert
import com.kotlinorm.orm.select.select
import com.kotlinorm.wrappers.KronosBadSqlGrammarException
import java.math.BigDecimal
import java.math.RoundingMode
import java.sql.SQLException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

abstract class TypeDialectDdlCornerIntegrationSuite(
    environment: IntegrationDatabaseEnvironment,
    profile: IntegrationScenarioProfile,
) : IntegrationSuiteSupport(environment, profile) {
    protected fun verifyPostgresRejectsNumericBooleanDefaultsAgainstRealDatabase() {
        requireDatabaseAvailable()
        requireDatabaseType(DBType.Postgres)
        configureKronos()

        with(wrapper.table) {
            dropTable(TypeDialectBooleanDefaultValue())
        }

        val error = assertFailsWith<KronosBadSqlGrammarException> {
            wrapper.table.syncTable(TypeDialectBooleanDefaultValue())
        }

        assertEquals("42804", (error.cause as SQLException).sqlState)
        assertEquals(
            "CREATE TABLE IF NOT EXISTS \"public\".\"kt_type_dialect_boolean_default\" " +
                "(\"id\" INTEGER NOT NULL PRIMARY KEY, " +
                "\"default_false\" BOOLEAN DEFAULT 0, " +
                "\"default_true\" BOOLEAN DEFAULT 1, " +
                "\"numeric_zero\" INTEGER DEFAULT 0, " +
                "\"numeric_one\" INTEGER DEFAULT 1)",
            error.sql,
        )
    }

    @Test
    fun quotedIdentifiersDefaultsTextAndNumericValuesRoundTripAgainstRealDatabase() {
        recreateCornerValueTable()

        cornerSeedRows().forEach { value ->
            assertEquals(1, insertCornerSeedRow(value))
        }
        insertExplicitNullDefaultRow()

        assertEquals(expectedCornerRecords(), selectCornerRecords())
    }

    @Test
    fun byteArrayBlobInsertShouldBindAsSingleJdbcParameterAgainstRealDatabase() {
        recreateCornerValueTable()

        assertEquals(
            1,
            TypeDialectCornerValue(
                id = 99,
                reservedWord = "blob",
                binaryPayload = byteArrayOf(0, 1, 2, 3, 127, -128),
            ).insert().execute().affectedRows,
        )
    }

    protected fun verifySqliteTextDateAndTimeColumnsMapToLocalDateAndLocalTimeAgainstRealDatabase() {
        requireDatabaseType(DBType.SQLite)
        recreateCornerValueTable()

        val expectedDate = LocalDate.of(2026, 7, 14)
        val expectedTime = LocalTime.of(12, 34, 56)
        assertEquals(
            1,
            TypeDialectCornerValue(
                id = 88,
                reservedWord = "temporal",
                localDate = expectedDate,
                localTime = expectedTime,
                localDateTime = LocalDateTime.of(2026, 7, 14, 12, 34, 56),
            ).insert().execute().affectedRows,
        )

        val actual = TypeDialectCornerValue()
            .select()
            .where { it.id == 88 }
            .first<TypeDialectCornerValue>()
        assertEquals(
            expectedDate,
            actual.localDate,
        )
        assertEquals(
            expectedTime,
            actual.localTime?.withNano(0),
        )
    }

    protected fun verifySqlServerNullTemporalColumnsBindAsTypedNullsAgainstRealDatabase() {
        requireDatabaseType(DBType.Mssql)
        recreateCornerValueTable()

        assertEquals(
            1,
            TypeDialectCornerValue(
                id = 89,
                reservedWord = "null-temporal",
                localDate = null,
                localTime = null,
                localDateTime = null,
            ).insert().execute().affectedRows,
        )
    }

    protected fun verifySqlServerTimeColumnMapsToLocalTimeAgainstRealDatabase() {
        requireDatabaseType(DBType.Mssql)
        recreateCornerValueTable()

        assertEquals(
            1,
            TypeDialectCornerValue(
                id = 90,
                reservedWord = "time",
                localTime = LocalTime.of(12, 34, 56),
            ).insert().execute().affectedRows,
        )

        assertEquals(
            LocalTime.of(12, 34, 56),
            TypeDialectCornerValue()
                .select()
                .where { it.id == 90 }
                .first<TypeDialectCornerValue>()
                .localTime,
        )
    }

    @Test
    fun syncTableAddsNullableAndDefaultColumnsAfterReservedIdentifierColumnAgainstRealDatabase() {
        requireDatabaseAvailable()
        configureKronos()

        wrapper.table.dropTable(TypeDialectSyncCornerV1())
        wrapper.table.createTable(TypeDialectSyncCornerV1())
        assertEquals(1, TypeDialectSyncCornerV1(id = 1, orderValue = "before-sync").insert().execute().affectedRows)

        assertTrue(wrapper.table.syncTable(TypeDialectSyncCornerV2()))
        assertTrue(wrapper.table.syncTable(TypeDialectSyncCornerV2()))

        val columns = queryTableColumns("kt_type_dialect_sync_corner", wrapper)
        val columnNames = columns.map { it.columnName.lowercase() }.toSet()
        val addedDefault = columns.single { it.columnName.equals("added_default", ignoreCase = true) }
        val addedNullable = columns.single { it.columnName.equals("added_nullable", ignoreCase = true) }
        val snakeValue = columns.single { it.columnName.equals("snake_value", ignoreCase = true) }
        val orderValue = columns.single { it.columnName.equals("order", ignoreCase = true) }

        assertEquals(setOf("id", "order", "added_default", "added_nullable", "snake_value"), columnNames)
        assertEquals(expectedVarcharMetadataType(), addedDefault.type)
        assertEquals(KColumnType.INT, addedNullable.type)
        assertEquals(expectedBigIntMetadataType(), snakeValue.type)
        assertEquals(true, addedDefault.nullable)
        assertEquals(true, addedNullable.nullable)
        assertEquals(true, snakeValue.nullable)
        assertEquals("'fresh'", addedDefault.defaultValue)
        assertEquals("42", snakeValue.defaultValue)
        assertEquals(expectedVarcharMetadataType(), orderValue.type)

        assertEquals(
            1,
            TypeDialectSyncCornerV2(id = 2, orderValue = "after-sync")
                .insert()
                .execute()
                .affectedRows,
        )

        assertEquals(
            listOf(
                TypeDialectSyncCornerRecord(
                    id = 1,
                    orderValue = "before-sync",
                    addedDefault = existingRowDefaultValueAfterAdd("fresh"),
                    addedNullable = null,
                    snakeValue = existingRowDefaultValueAfterAdd(42L),
                ),
                TypeDialectSyncCornerRecord(
                    id = 2,
                    orderValue = "after-sync",
                    addedDefault = "fresh",
                    addedNullable = null,
                    snakeValue = 42L,
                ),
            ),
            selectSyncCornerRecords(),
        )
    }

    private fun recreateCornerValueTable() {
        requireDatabaseAvailable()
        configureKronos()
        with(wrapper.table) {
            dropTable(TypeDialectCornerValue())
            syncTable(TypeDialectCornerValue())
            truncateTable(TypeDialectCornerValue(), restartIdentity = restartIdentity)
        }
    }

    private fun requireDatabaseType(dbType: DBType) {
        requireDatabaseAvailable()
        check(wrapper.dbType == dbType) {
            "${environment.displayName} expected database type $dbType but was ${wrapper.dbType}"
        }
    }

    private fun insertExplicitNullDefaultRow() {
        assertEquals(
            1,
            wrapper.execute(
                """
                INSERT INTO ${table("kt_type_dialect_corner")}
                    (${quote("id")}, ${quote("select")}, ${quote("defaulted_text")}, ${quote("defaulted_number")})
                VALUES (:id, :reservedWord, :defaultedText, :defaultedNumber)
                """.trimIndent(),
                mapOf(
                    "id" to 3,
                    "reservedWord" to "explicit-null-default",
                    "defaultedText" to null,
                    "defaultedNumber" to null,
                ),
            ),
        )
    }

    private fun cornerSeedRows(): List<TypeDialectCornerValue> =
        listOf(
            TypeDialectCornerValue(
                id = 1,
                reservedWord = "line1\nline2 'single' \"double\" \\ slash % _ :param",
                mixedCaseName = "MiXeD-Case-Value",
                underScoreName = "under_score_value",
                externalId = 9_223_372_036_854_000L,
                enabled = true,
                exactAmount = BigDecimal("1234.5000"),
                localDate = null,
                localTime = null,
                localDateTime = null,
                binaryPayload = null,
                longText = longTextPayload,
            ),
            TypeDialectCornerValue(
                id = 2,
                reservedWord = "",
                mixedCaseName = "Case-Two",
                underScoreName = "",
                externalId = 123_456_789_012_345L,
                enabled = false,
                exactAmount = BigDecimal("0.1250"),
                localDate = null,
                localTime = null,
                localDateTime = null,
                binaryPayload = null,
                longText = "",
                defaultedText = "manual",
                defaultedNumber = 99,
            ),
        )

    private fun expectedCornerRecords(): List<TypeDialectCornerRecord> =
        listOf(
            TypeDialectCornerRecord(
                id = 1,
                reservedWord = "line1\nline2 'single' \"double\" \\ slash % _ :param",
                mixedCaseName = "MiXeD-Case-Value",
                underScoreName = "under_score_value",
                externalId = 9_223_372_036_854_000L,
                enabled = true,
                exactAmount = "1234.5000",
                localDate = null,
                localTime = null,
                localDateTime = null,
                binaryPayload = null,
                longText = longTextPayload,
                defaultedText = "fallback",
                defaultedNumber = 7,
            ),
            TypeDialectCornerRecord(
                id = 2,
                reservedWord = expectedEmptyString(),
                mixedCaseName = "Case-Two",
                underScoreName = expectedEmptyString(),
                externalId = 123_456_789_012_345L,
                enabled = false,
                exactAmount = "0.1250",
                localDate = null,
                localTime = null,
                localDateTime = null,
                binaryPayload = null,
                longText = expectedEmptyString(),
                defaultedText = "manual",
                defaultedNumber = 99,
            ),
            TypeDialectCornerRecord(
                id = 3,
                reservedWord = "explicit-null-default",
                mixedCaseName = null,
                underScoreName = null,
                externalId = null,
                enabled = null,
                exactAmount = null,
                localDate = null,
                localTime = null,
                localDateTime = null,
                binaryPayload = null,
                longText = null,
                defaultedText = null,
                defaultedNumber = null,
            ),
        )

    private fun insertCornerSeedRow(value: TypeDialectCornerValue): Int {
        val includesExplicitDefaults = value.defaultedText != null || value.defaultedNumber != null
        val defaultColumns = if (includesExplicitDefaults) {
            ", ${quote("defaulted_text")}, ${quote("defaulted_number")}"
        } else {
            ""
        }
        val defaultValues = if (includesExplicitDefaults) {
            ", :defaultedText, :defaultedNumber"
        } else {
            ""
        }
        return wrapper.execute(
            """
            INSERT INTO ${table("kt_type_dialect_corner")}
                (${quote("id")}, ${quote("select")}, ${quote("CamelCase")}, ${quote("under_score")},
                 ${quote("external_id")}, ${quote("enabled")}, ${quote("exact_amount")}, ${quote("long_text")}$defaultColumns)
            VALUES (:id, :reservedWord, :mixedCaseName, :underScoreName,
                    :externalId, :enabled, :exactAmount, :longText$defaultValues)
            """.trimIndent(),
            mapOf(
                "id" to value.id,
                "reservedWord" to value.reservedWord,
                "mixedCaseName" to value.mixedCaseName,
                "underScoreName" to value.underScoreName,
                "externalId" to value.externalId,
                "enabled" to value.enabled,
                "exactAmount" to value.exactAmount,
                "longText" to value.longText,
                "defaultedText" to value.defaultedText,
                "defaultedNumber" to value.defaultedNumber,
            ),
        )
    }

    private fun selectCornerRecords(): List<TypeDialectCornerRecord> =
        TypeDialectCornerValue()
            .select()
            .orderBy { it.id.asc() }
            .toList<TypeDialectCornerValue>()
            .map { it.toRecord() }

    private fun selectSyncCornerRecords(): List<TypeDialectSyncCornerRecord> =
        TypeDialectSyncCornerV2()
            .select()
            .orderBy { it.id.asc() }
            .toList<TypeDialectSyncCornerV2>()
            .map { it.toRecord() }

    private fun TypeDialectCornerValue.toRecord(): TypeDialectCornerRecord =
        TypeDialectCornerRecord(
            id = id,
            reservedWord = reservedWord,
            mixedCaseName = mixedCaseName,
            underScoreName = underScoreName,
            externalId = externalId,
            enabled = enabled,
            exactAmount = exactAmount?.setScale(4, RoundingMode.UNNECESSARY)?.toPlainString(),
            localDate = localDate,
            localTime = localTime?.withNano(0),
            localDateTime = localDateTime?.withNano(0),
            binaryPayload = binaryPayload?.map { it.toInt() },
            longText = longText,
            defaultedText = defaultedText,
            defaultedNumber = defaultedNumber,
        )

    private fun TypeDialectSyncCornerV2.toRecord(): TypeDialectSyncCornerRecord =
        TypeDialectSyncCornerRecord(
            id = id,
            orderValue = orderValue,
            addedDefault = addedDefault,
            addedNullable = addedNullable,
            snakeValue = mappedSnakeValue,
        )

    private fun expectedEmptyString(): String? =
        if (wrapper.dbType == DBType.Oracle) null else ""

    private fun expectedVarcharMetadataType(): KColumnType =
        if (wrapper.dbType == DBType.SQLite) KColumnType.TEXT else KColumnType.VARCHAR

    private fun expectedBigIntMetadataType(): KColumnType =
        if (wrapper.dbType == DBType.SQLite) KColumnType.INT else KColumnType.BIGINT

    private fun <T> existingRowDefaultValueAfterAdd(value: T): T? =
        if (wrapper.dbType == DBType.Mssql) null else value

    private val longTextPayload: String =
        (1..48).joinToString(separator = "\n") { index ->
            "long-text-line-$index: alpha beta gamma 0123456789 'quote' % _"
        }
}
