package com.kotlinorm.integration.suites

import com.kotlinorm.database.SqlExecutor.batchExecute
import com.kotlinorm.database.SqlExecutor.toList
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.integration.fixtures.EdgeAccount
import com.kotlinorm.integration.fixtures.EdgeAccountRecord
import com.kotlinorm.integration.fixtures.EdgeTailNullInsertSelect
import com.kotlinorm.integration.fixtures.EdgeTupleInsertSelect
import com.kotlinorm.integration.fixtures.EdgeWideInsertSelect
import com.kotlinorm.integration.fixtures.IntegrationTypedValue
import com.kotlinorm.integration.fixtures.SchemaSyncShapeV1
import com.kotlinorm.integration.fixtures.SchemaSyncShapeV2
import com.kotlinorm.integration.profiles.IntegrationScenarioProfile
import com.kotlinorm.integration.support.IntegrationDatabaseEnvironment
import com.kotlinorm.integration.support.IntegrationSuiteSupport
import com.kotlinorm.orm.ddl.queryTableColumns
import com.kotlinorm.orm.ddl.table
import com.kotlinorm.orm.delete.delete
import com.kotlinorm.orm.insert.insert
import com.kotlinorm.orm.select.select
import com.kotlinorm.orm.update.update
import com.kotlinorm.orm.upsert.upsert
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

abstract class EdgeCaseIntegrationSuite(
    environment: IntegrationDatabaseEnvironment,
    profile: IntegrationScenarioProfile,
) : IntegrationSuiteSupport(environment, profile) {
    @Test
    fun syncTableDropsRemovedColumnAndAppliesTypeAndDefaultChangesAgainstRealDatabase() {
        assumeDatabaseAvailable()
        configureKronos()

        wrapper.table.dropTable(SchemaSyncShapeV1())
        wrapper.table.createTable(SchemaSyncShapeV1())

        assertEquals(true, wrapper.table.syncTable(SchemaSyncShapeV2()))

        val columns = queryTableColumns("kt_schema_sync_shape", wrapper)
        val columnNames = columns.map { it.columnName.lowercase() }.toSet()
        val mutableScore = columns.single { it.columnName.equals("mutable_score", ignoreCase = true) }
        val status = columns.single { it.columnName.equals("status", ignoreCase = true) }

        assertEquals(setOf("id", "mutable_score", "status"), columnNames)
        assertFalse("legacy_score" in columnNames, "legacy_score should be removed by syncTable")
        assertEquals(if (wrapper.dbType == DBType.SQLite) KColumnType.INT else KColumnType.BIGINT, mutableScore.type)
        assertEquals("'active'", status.defaultValue)
    }

    @Test
    fun insertUpdateUpsertDeleteAndBatchDslPreserveTypedValuesAgainstRealDatabase() {
        recreateEdgeAccountTable()

        assertEquals(1, EdgeAccount(id = 1, name = "Ada", balance = 10).insert().execute().affectedRows)
        assertEquals(1, EdgeAccount(id = 1).update().set { it.balance += 5 }.by { it.id }.execute().affectedRows)
        assertEquals(1, EdgeAccount(id = 2, name = "Grace", balance = 20).insert().execute().affectedRows)

        EdgeAccount(id = 2, name = "Grace Hopper", balance = null)
            .upsert { [it.name] }
            .on { it.id }
            .onConflict()
            .execute()

        with(com.kotlinorm.database.SqlExecutor) {
            wrapper.batchExecute(
                "INSERT INTO ${table("kt_edge_account")} (${quote("id")}, ${quote("name")}, ${quote("balance")}, ${quote("state")}) " +
                    "VALUES (:id, :name, :balance, :state)",
                arrayOf(
                    mapOf("id" to 3, "name" to "Linus", "balance" to 30, "state" to "active"),
                    mapOf("id" to 4, "name" to "Barbara", "balance" to 40, "state" to "inactive"),
                ),
            )
        }

        assertEquals(1, EdgeAccount(id = 4).delete().logic(false).by { it.id }.execute().affectedRows)

        assertEquals(
            listOf(
                EdgeAccountRecord(id = 1, name = "Ada", balance = 15, state = "active"),
                EdgeAccountRecord(id = 2, name = "Grace Hopper", balance = 20, state = "active"),
                EdgeAccountRecord(id = 3, name = "Linus", balance = 30, state = "active"),
            ),
            selectEdgeAccounts(),
        )
    }

    @Test
    fun pagedWithTotalQueryListSupportsDestructuringAndTypedRowsAgainstRealDatabase() {
        recreateEdgeAccountTable()
        (1..5).forEach { id ->
            assertEquals(
                1,
                EdgeAccount(id = id, name = "user-$id", balance = id * 10, state = "active").insert().execute().affectedRows,
            )
        }

        val (total, rows) = EdgeAccount()
            .select()
            .orderBy { it.id.asc() }
            .page(pi = 2, ps = 2)
            .withTotal()
            .toList<EdgeAccount>()

        assertEquals(5, total)
        assertEquals(
            listOf(
                EdgeAccountRecord(id = 3, name = "user-3", balance = 30, state = "active"),
                EdgeAccountRecord(id = 4, name = "user-4", balance = 40, state = "active"),
            ),
            rows.map { it.toRecord() },
        )
    }

    @Test
    fun jdbcWrapperKPojoMappingAcceptsColumnLabelsWithoutTestSideConversion() {
        assumeDatabaseAvailable()
        configureKronos()
        with(wrapper.table) {
            dropTable(IntegrationTypedValue())
            syncTable(IntegrationTypedValue())
            truncateTable(IntegrationTypedValue(), restartIdentity = restartIdentity)
        }
        assertEquals(
            1,
            IntegrationTypedValue(id = 1, longValue = 1_000_000_001L, optionalScore = 7)
                .insert()
                .execute()
                .affectedRows,
        )

        val value = wrapper.toList<IntegrationTypedValue>(
            """
            SELECT ${quote("id")}, ${quote("long_value")}, ${quote("optional_score")}
            FROM ${table("kt_integration_typed_value")}
            WHERE ${quote("id")} = :id
            """.trimIndent(),
            mapOf("id" to 1),
        ).single()

        assertEquals(1, value.id)
        assertEquals(1_000_000_001L, value.longValue)
        assertEquals(7, value.optionalScore)
    }

    @Test
    fun insertSelectMapsMiddleNullPlaceholderByTargetTupleOrderAgainstRealDatabase() {
        recreateInsertSelectEdgeTables()
        assertEquals(1, EdgeAccount(id = 1, name = "Ada", balance = 10, state = "active").insert().execute().affectedRows)

        assertEquals(
            1,
            EdgeAccount()
                .select { [it.id, it.name] }
                .insert<EdgeTupleInsertSelect> { [it.id, null, it.name] }
                .execute()
                .affectedRows,
        )

        assertEquals(
            listOf(EdgeTupleInsertSelect(id = 1, age = null, name = "Ada")),
            EdgeTupleInsertSelect().select().orderBy { it.id.asc() }.toList<EdgeTupleInsertSelect>(),
        )
    }

    @Test
    fun insertSelectMapsTrailingNullPlaceholdersByTargetTupleOrderAgainstRealDatabase() {
        recreateInsertSelectEdgeTables()
        assertEquals(1, EdgeAccount(id = 1, name = "Ada", balance = 10, state = "active").insert().execute().affectedRows)

        assertEquals(
            1,
            EdgeAccount()
                .select { it.id }
                .insert<EdgeTailNullInsertSelect> { [it.id, null, null, null] }
                .execute()
                .affectedRows,
        )

        assertEquals(
            listOf(EdgeTailNullInsertSelect(id = 1, firstNull = null, secondNull = null, thirdNull = null)),
            EdgeTailNullInsertSelect().select().orderBy { it.id.asc() }.toList<EdgeTailNullInsertSelect>(),
        )
    }

    @Test
    fun insertSelectUsesExplicitNullToPadSourceTupleToTargetColumnCountAgainstRealDatabase() {
        recreateInsertSelectEdgeTables()
        assertEquals(1, EdgeAccount(id = 1, name = "Ada", balance = 10, state = "active").insert().execute().affectedRows)

        assertEquals(
            1,
            EdgeAccount()
                .select { [it.id, it.name, it.balance, it.state] }
                .insert<EdgeWideInsertSelect> { [it.id, it.name, it.balance, it.state, null] }
                .execute()
                .affectedRows,
        )

        assertEquals(
            listOf(EdgeWideInsertSelect(id = 1, name = "Ada", balance = 10, state = "active", extraNote = null)),
            EdgeWideInsertSelect().select().orderBy { it.id.asc() }.toList<EdgeWideInsertSelect>(),
        )
    }

    private fun recreateEdgeAccountTable() {
        assumeDatabaseAvailable()
        configureKronos()
        with(wrapper.table) {
            dropTable(EdgeAccount())
            syncTable(EdgeAccount())
            truncateTable(EdgeAccount(), restartIdentity = restartIdentity)
        }
    }

    private fun recreateInsertSelectEdgeTables() {
        assumeDatabaseAvailable()
        configureKronos()
        with(wrapper.table) {
            dropTable(EdgeTailNullInsertSelect())
            dropTable(EdgeTupleInsertSelect())
            dropTable(EdgeWideInsertSelect())
            dropTable(EdgeAccount())
            syncTable(EdgeAccount())
            syncTable(EdgeWideInsertSelect())
            syncTable(EdgeTupleInsertSelect())
            syncTable(EdgeTailNullInsertSelect())
            truncateTable(EdgeAccount(), restartIdentity = restartIdentity)
            truncateTable(EdgeWideInsertSelect(), restartIdentity = restartIdentity)
            truncateTable(EdgeTupleInsertSelect(), restartIdentity = restartIdentity)
            truncateTable(EdgeTailNullInsertSelect(), restartIdentity = restartIdentity)
        }
    }

    private fun selectEdgeAccounts(): List<EdgeAccountRecord> =
        EdgeAccount()
            .select()
            .orderBy { it.id.asc() }
            .toList<EdgeAccount>()
            .map { it.toRecord() }

    private fun EdgeAccount.toRecord(): EdgeAccountRecord =
        EdgeAccountRecord(id = id, name = name, balance = balance, state = state)
}
