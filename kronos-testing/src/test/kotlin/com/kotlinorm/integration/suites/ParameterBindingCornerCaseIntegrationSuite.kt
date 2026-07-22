package com.kotlinorm.integration.suites

import com.kotlinorm.database.SqlExecutor.batchExecute
import com.kotlinorm.database.SqlExecutor.execute
import com.kotlinorm.database.SqlExecutor.query
import com.kotlinorm.database.SqlExecutor.queryOne
import com.kotlinorm.enums.DBType
import com.kotlinorm.integration.fixtures.ParameterBindingCornerCaseRecord
import com.kotlinorm.integration.fixtures.ParameterBindingCornerCaseValue
import com.kotlinorm.integration.profiles.IntegrationScenarioProfile
import com.kotlinorm.integration.profiles.StandardIntegrationScenarioProfile
import com.kotlinorm.integration.support.IntegrationDatabaseEnvironment
import com.kotlinorm.integration.support.IntegrationDatabaseEnvironments.mysql
import com.kotlinorm.integration.support.IntegrationDatabaseEnvironments.oracle
import com.kotlinorm.integration.support.IntegrationDatabaseEnvironments.postgres
import com.kotlinorm.integration.support.IntegrationDatabaseEnvironments.sqlServer
import com.kotlinorm.integration.support.IntegrationDatabaseEnvironments.sqlite
import com.kotlinorm.integration.support.IntegrationSuiteSupport
import com.kotlinorm.orm.ddl.table
import com.kotlinorm.orm.select.select
import java.sql.Statement
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

abstract class ParameterBindingCornerCaseIntegrationSuite(
    environment: IntegrationDatabaseEnvironment,
    profile: IntegrationScenarioProfile,
) : IntegrationSuiteSupport(environment, profile) {
    @Test
    fun rawSqlPatchRepeatedNamedNullAndInjectionShapedValuesStayDataAgainstRealDatabase() {
        recreateParameterBindingTable()
        val injectionShapedNote = "x') OR 1 = 1; DROP TABLE kt_parameter_binding_case; -- :id"

        assertEquals(1, insertParameterRow(1, "plain", 10, "ordinary", 10))
        assertEquals(1, insertParameterRow(2, "needle", 20, injectionShapedNote, null))
        assertEquals(1, insertParameterRow(3, "other", 30, "not a match", null))

        val noteColumn = quote("note")
        val optionalColumn = quote("optional_score")
        val matchedByRepeatedRawPatch = ParameterBindingCornerCaseValue()
            .select()
            .where { "($noteColumn = :needle OR :needle = $noteColumn)".asSql() }
            .patch("needle" to injectionShapedNote)
            .orderBy { it.id.asc() }
            .toList<ParameterBindingCornerCaseValue>()
            .map { it.toRecord() }

        assertEquals(
            listOf(ParameterBindingCornerCaseRecord(2, "needle", 20, injectionShapedNote, null)),
            matchedByRepeatedRawPatch,
        )

        val matchedByNullPatch = ParameterBindingCornerCaseValue()
            .select()
            .where { "$optionalColumn IS NULL AND ${typedNullCheck("optionalScore")} ".asSql() }
            .patch("optionalScore" to null)
            .orderBy { it.id.asc() }
            .toList<ParameterBindingCornerCaseValue>()
            .map { it.toRecord() }

        assertEquals(
            listOf(
                ParameterBindingCornerCaseRecord(2, "needle", 20, injectionShapedNote, null),
                ParameterBindingCornerCaseRecord(3, "other", 30, "not a match", null),
            ),
            matchedByNullPatch,
        )
        assertEquals(3, countParameterBindingRows())
    }

    @Test
    fun ormDslInExpandsCollectionAndArrayParametersAgainstRealDatabase() {
        recreateParameterBindingTable()
        seedCollectionRows()

        assertEquals(
            listOf(2, 3, 4),
            ParameterBindingCornerCaseValue()
                .select { it.id }
                .where { it.id in listOf(2, 4) || it.optionalScore in listOf(2, 4) }
                .orderBy { it.id.asc() }
                .toList<ParameterBindingCornerCaseValue>()
                .map { it.id },
        )
        assertEquals(
            listOf(1, 3),
            ParameterBindingCornerCaseValue()
                .select { it.id }
                .where { it.id in intArrayOf(1, 3) && it.label in arrayOf("one", "three") }
                .orderBy { it.id.asc() }
                .toList<ParameterBindingCornerCaseValue>()
                .map { it.id },
        )
    }

    @Test
    fun batchExecuteBindsRepeatedNullAndInjectionShapedValuesPerRowAgainstRealDatabase() {
        recreateParameterBindingTable()
        val injectionShapedNote = "batch'); UPDATE kt_parameter_binding_case SET score = 999; -- :score"

        val counts = wrapper.batchExecute(
            """
            INSERT INTO ${table("kt_parameter_binding_case")}
                (${quote("id")}, ${quote("label")}, ${quote("score")}, ${quote("note")}, ${quote("optional_score")})
            VALUES (:id, :label, :score, :note, :score)
            """.trimIndent(),
            arrayOf(
                mapOf("id" to 11, "label" to "batch-null", "score" to 101, "note" to null),
                mapOf("id" to 12, "label" to "batch-text", "score" to 202, "note" to injectionShapedNote),
            ),
        ).toList().map { it.normalizedBatchCount() }

        assertEquals(listOf(1, 1), counts)
        assertEquals(
            listOf(
                ParameterBindingCornerCaseRecord(11, "batch-null", 101, null, 101),
                ParameterBindingCornerCaseRecord(12, "batch-text", 202, injectionShapedNote, 202),
            ),
            selectParameterRows(),
        )
        assertEquals(2, countParameterBindingRows())
    }

    @Test
    fun byteArrayNamedParameterShouldBindAsSingleBlobValueAgainstRealDatabase() {
        recreateParameterBindingTable()
        val payload = byteArrayOf(0, 1, 2, 3, 127, -128)

        assertEquals(
            1,
            wrapper.execute(
                """
                INSERT INTO ${table("kt_parameter_binding_case")}
                    (${quote("id")}, ${quote("label")}, ${quote("binary_payload")})
                VALUES (:id, :label, :payload)
                """.trimIndent(),
                mapOf("id" to 21, "label" to "blob", "payload" to payload),
            ),
        )

        val storedPayload = wrapper.query(
            """
            SELECT ${quote("binary_payload")} AS ${quote("binary_payload")}
            FROM ${table("kt_parameter_binding_case")}
            WHERE ${quote("id")} = :id
            """.trimIndent(),
            mapOf("id" to 21),
        ).single().value("binary_payload") as ByteArray

        assertContentEquals(payload, storedPayload)
    }

    private fun recreateParameterBindingTable() {
        requireDatabaseAvailable()
        configureKronos()
        with(wrapper.table) {
            dropTable(ParameterBindingCornerCaseValue())
            syncTable(ParameterBindingCornerCaseValue())
            truncateTable(ParameterBindingCornerCaseValue(), restartIdentity = restartIdentity)
        }
    }

    private fun seedCollectionRows() {
        listOf(
            ParameterBindingCornerCaseRecord(1, "one", 10, "alpha", 9),
            ParameterBindingCornerCaseRecord(2, "two", 20, "beta", null),
            ParameterBindingCornerCaseRecord(3, "three", 30, "gamma", 2),
            ParameterBindingCornerCaseRecord(4, "four", 40, "delta", 4),
        ).forEach { record ->
            assertEquals(
                1,
                insertParameterRow(
                    id = record.id ?: error("id is required"),
                    label = record.label,
                    score = record.score,
                    note = record.note,
                    optionalScore = record.optionalScore,
                ),
            )
        }
    }

    private fun insertParameterRow(
        id: Int,
        label: String?,
        score: Int?,
        note: String?,
        optionalScore: Int?,
    ): Int =
        wrapper.execute(
            """
            INSERT INTO ${table("kt_parameter_binding_case")}
                (${quote("id")}, ${quote("label")}, ${quote("score")}, ${quote("note")}, ${quote("optional_score")})
            VALUES (:id, :label, :score, :note, :optionalScore)
            """.trimIndent(),
            mapOf(
                "id" to id,
                "label" to label,
                "score" to score,
                "note" to note,
                "optionalScore" to optionalScore,
            ),
        )

    private fun selectParameterRows(): List<ParameterBindingCornerCaseRecord> =
        wrapper.query(
            """
            SELECT ${quote("id")}, ${quote("label")}, ${quote("score")}, ${quote("note")}, ${quote("optional_score")}
            FROM ${table("kt_parameter_binding_case")}
            ORDER BY ${quote("id")}
            """.trimIndent(),
        ).map { it.toRecord() }

    private fun countParameterBindingRows(): Int =
        wrapper.queryOne<Number>("SELECT COUNT(*) FROM ${table("kt_parameter_binding_case")}").toInt()

    private fun typedNullCheck(parameterName: String): String =
        when (wrapper.dbType) {
            DBType.Postgres -> "CAST(:$parameterName AS INTEGER) IS NULL"
            else -> ":$parameterName IS NULL"
        }

    private fun ParameterBindingCornerCaseValue.toRecord(): ParameterBindingCornerCaseRecord =
        ParameterBindingCornerCaseRecord(
            id = id,
            label = label,
            score = score,
            note = note,
            optionalScore = optionalScore,
        )

    private fun Map<String, Any?>.toRecord(): ParameterBindingCornerCaseRecord =
        ParameterBindingCornerCaseRecord(
            id = intValue("id"),
            label = stringValue("label"),
            score = intValue("score"),
            note = stringValue("note"),
            optionalScore = intValue("optional_score"),
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

class MysqlParameterBindingCornerCaseIntegrationTest :
    ParameterBindingCornerCaseIntegrationSuite(mysql, StandardIntegrationScenarioProfile)

class PostgresParameterBindingCornerCaseIntegrationTest :
    ParameterBindingCornerCaseIntegrationSuite(postgres, StandardIntegrationScenarioProfile)

class SqliteParameterBindingCornerCaseIntegrationTest :
    ParameterBindingCornerCaseIntegrationSuite(sqlite, StandardIntegrationScenarioProfile)

class SqlServerParameterBindingCornerCaseIntegrationTest :
    ParameterBindingCornerCaseIntegrationSuite(sqlServer, StandardIntegrationScenarioProfile)

class OracleParameterBindingCornerCaseIntegrationTest :
    ParameterBindingCornerCaseIntegrationSuite(oracle, StandardIntegrationScenarioProfile)
