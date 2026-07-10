package com.kotlinorm.integration.suites

import com.kotlinorm.database.SqlExecutor.batchExecute
import com.kotlinorm.database.SqlExecutor.query
import com.kotlinorm.database.SqlExecutor.queryList
import com.kotlinorm.database.SqlExecutor.queryOne
import com.kotlinorm.database.SqlExecutor.queryOneOrNull
import com.kotlinorm.enums.DBType
import com.kotlinorm.integration.fixtures.IntegrationTypedValue
import com.kotlinorm.integration.fixtures.IntegrationUser
import com.kotlinorm.integration.fixtures.IntegrationUserRecord
import com.kotlinorm.integration.profiles.IntegrationScenarioProfile
import com.kotlinorm.integration.support.IntegrationDatabaseEnvironment
import com.kotlinorm.integration.support.IntegrationSuiteSupport
import com.kotlinorm.orm.ddl.table
import com.kotlinorm.orm.insert.insert
import java.sql.Statement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Raw SQL integration coverage for SqlExecutor and KronosDataSourceWrapper.
 *
 * Other integration suites exercise ORM DSL behavior. This suite intentionally
 * keeps hand-written SQL here so wrapper parameter binding, typed mapping, and
 * JDBC error propagation have one explicit boundary.
 */
abstract class WrapperSqlIntegrationSuite(
    environment: IntegrationDatabaseEnvironment,
    profile: IntegrationScenarioProfile,
) : IntegrationSuiteSupport(environment, profile) {
    @Test
    fun sqlExecutorBatchListKPojoMapPrimitiveAndNullMappingExecuteAgainstRealDatabase() {
        recreateTables()

        val records = listOf(
            IntegrationUserRecord(id = 30, name = "Batch-A", score = 70, status = 7),
            IntegrationUserRecord(id = 31, name = "Batch-B", score = 80, status = 7),
            IntegrationUserRecord(id = 32, name = "Batch-C", score = null, status = 8),
        )

        assertEquals(listOf(1, 1, 1), insertUsersBySqlExecutorBatch(records))
        assertEquals(records, selectUsersBySqlExecutorKPojoList())
        assertEquals(records[1], selectUserBySqlExecutorKPojo(id = 31))
        val nullableScoreRow = selectUserMapBySqlExecutor(id = 32)
        assertEquals(true, nullableScoreRow.keys.any { it.equals("score", ignoreCase = true) })
        assertEquals(records[2], nullableScoreRow.toUserRecord())
        assertEquals(3, selectSqlExecutorPrimitiveCount())
        assertEquals(null, selectNullableScoreBySqlExecutorPrimitive(id = 32))
    }

    @Test
    fun sqlExecutorListArrayPrimitiveArrayRepeatedAndNullParametersExecuteAgainstRealDatabase() {
        recreateTypedValueTable()
        seedTypedValues()

        assertEquals(
            listOf(1, 3),
            selectIdsWhere("${quote("id")} IN (:ids)", mapOf("ids" to listOf(1, 3))),
        )
        assertEquals(
            listOf(2, 3),
            selectIdsWhere("${quote("id")} IN (:ids)", mapOf("ids" to arrayOf(2, 3))),
        )
        assertEquals(
            listOf(1, 2, 3),
            selectIdsWhere("${quote("id")} IN (:ids)", mapOf("ids" to intArrayOf(1, 2, 3))),
        )
        assertEquals(
            listOf(2, 3),
            selectIdsWhere("${quote("id")} = :value OR ${quote("optional_score")} = :value", mapOf("value" to 2)),
        )
        assertEquals(
            listOf(1),
            selectIdsWhere(
                "${quote("optional_score")} IS NULL AND ${typedNullCheck("score")}",
                mapOf("score" to null),
            ),
        )
    }

    @Test
    fun sqlExecutorMissingTableErrorsAreVisibleAgainstRealDatabase() {
        recreateTables()

        val missingTableError = assertFailsWith<Throwable> {
            wrapper.query("SELECT * FROM ${table("kt_integration_user_missing")}")
        }
        assertEquals(false, missingTableError.message.isNullOrBlank())
    }

    private fun insertUsersBySqlExecutorBatch(records: List<IntegrationUserRecord>): List<Int> =
        wrapper.batchExecute(
            """
            INSERT INTO ${table("kt_integration_user")}
                (${quote("id")}, ${quote("name")}, ${quote("score")}, ${quote("status")})
            VALUES (:id, :name, :score, :status)
            """.trimIndent(),
            records.map { it.toParamMap() }.toTypedArray(),
        ).toList().map { it.normalizedBatchCount() }

    private fun selectUsersBySqlExecutorKPojoList(): List<IntegrationUserRecord> =
        wrapper.queryList<IntegrationUser>(selectUserSql(orderBy = quote("id")))
            .map { it.toRecord() }

    private fun selectUserBySqlExecutorKPojo(id: Int): IntegrationUserRecord =
        wrapper.queryOne<IntegrationUser>(
            selectUserSql(where = "${quote("id")} = :id"),
            mapOf("id" to id),
        ).toRecord()

    private fun selectUserMapBySqlExecutor(id: Int): Map<String, Any?> =
        wrapper.query(
            selectUserSql(where = "${quote("id")} = :id"),
            mapOf("id" to id),
        ).single()

    private fun selectSqlExecutorPrimitiveCount(): Int =
        wrapper.queryOne<Number>("SELECT COUNT(*) FROM ${table("kt_integration_user")}").toInt()

    private fun selectNullableScoreBySqlExecutorPrimitive(id: Int): Int? =
        wrapper.queryOneOrNull(
            "SELECT ${quote("score")} FROM ${table("kt_integration_user")} WHERE ${quote("id")} = :id",
            mapOf("id" to id),
        )

    private fun recreateTypedValueTable() {
        assumeDatabaseAvailable()
        configureKronos()
        with(wrapper.table) {
            dropTable(IntegrationTypedValue())
            syncTable(IntegrationTypedValue())
            truncateTable(IntegrationTypedValue(), restartIdentity = restartIdentity)
        }
    }

    private fun seedTypedValues() {
        listOf(
            IntegrationTypedValue(id = 1, longValue = 10, textValue = "A", flagValue = true, optionalScore = null),
            IntegrationTypedValue(id = 2, longValue = 20, textValue = "B", flagValue = false, optionalScore = 2),
            IntegrationTypedValue(id = 3, longValue = 30, textValue = "C", flagValue = true, optionalScore = 2),
        ).forEach { value ->
            assertEquals(1, value.insert().execute().affectedRows)
        }
    }

    private fun selectIdsWhere(whereSql: String, params: Map<String, Any?>): List<Int> =
        wrapper.queryList<Number>(
            """
            SELECT ${quote("id")} AS ${quote("id")}
            FROM ${table("kt_integration_typed_value")}
            WHERE $whereSql
            ORDER BY ${quote("id")}
            """.trimIndent(),
            params,
        ).map { it.toInt() }.toList()

    private fun selectUserSql(where: String? = null, orderBy: String? = null): String =
        buildString {
            append(
                """
                SELECT ${quote("id")}, ${quote("name")}, ${quote("score")}, ${quote("status")}
                FROM ${table("kt_integration_user")}
                """.trimIndent()
            )
            if (where != null) append(" WHERE ").append(where)
            if (orderBy != null) append(" ORDER BY ").append(orderBy)
        }

    private fun typedNullCheck(parameterName: String): String =
        when (wrapper.dbType) {
            DBType.Postgres -> "CAST(:$parameterName AS INTEGER) IS NULL"
            else -> ":$parameterName IS NULL"
        }

    private fun IntegrationUserRecord.toParamMap(): Map<String, Any?> =
        mapOf(
            "id" to id,
            "name" to name,
            "score" to score,
            "status" to status,
        )

    private fun IntegrationUser.toRecord(): IntegrationUserRecord =
        IntegrationUserRecord(id = id, name = name, score = score, status = status)

    private fun Map<String, Any?>.toUserRecord(): IntegrationUserRecord =
        IntegrationUserRecord(
            id = intValue("id"),
            name = stringValue("name"),
            score = intValue("score"),
            status = intValue("status"),
        )

    private fun Map<String, Any?>.intValue(label: String): Int? =
        when (val value = value(label)) {
            null -> null
            is Number -> value.toInt()
            else -> value.toString().toInt()
        }

    private fun Map<String, Any?>.stringValue(label: String): String? =
        value(label)?.toString()

    private fun Int.normalizedBatchCount(): Int =
        if (this == Statement.SUCCESS_NO_INFO) 1 else this
}
