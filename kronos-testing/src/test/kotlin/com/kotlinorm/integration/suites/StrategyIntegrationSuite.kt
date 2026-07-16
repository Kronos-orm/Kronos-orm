package com.kotlinorm.integration.suites

import com.kotlinorm.database.SqlExecutor.queryOne
import com.kotlinorm.integration.fixtures.IntegrationStrategyAccount
import com.kotlinorm.integration.profiles.IntegrationScenarioProfile
import com.kotlinorm.integration.support.IntegrationDatabaseEnvironment
import com.kotlinorm.integration.support.IntegrationSuiteSupport
import com.kotlinorm.orm.ddl.table
import com.kotlinorm.orm.delete.delete
import com.kotlinorm.orm.insert.insert
import com.kotlinorm.orm.select.select
import com.kotlinorm.orm.update.update
import com.kotlinorm.orm.upsert.upsert
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

abstract class StrategyIntegrationSuite(
    environment: IntegrationDatabaseEnvironment,
    profile: IntegrationScenarioProfile,
) : IntegrationSuiteSupport(environment, profile) {
    @Test
    fun insertUpdateAndLogicDeleteStrategiesExecuteAgainstRealDatabase() {
        recreateStrategyAccountTable()

        val beforeInsert = LocalDateTime.now().minusSeconds(2)
        assertEquals(
            1,
            IntegrationStrategyAccount(id = 1, name = "seed", balance = 10)
                .insert()
                .execute()
                .affectedRows,
        )
        val afterInsert = LocalDateTime.now().plusSeconds(2)
        val inserted = selectRawAccount(1)

        assertEquals("seed", inserted.name)
        assertEquals(10, inserted.balance)
        assertEquals(false, inserted.deleted)
        assertEquals(0, inserted.version)
        assertTimestampWithin(inserted.createdAt, beforeInsert, afterInsert, "createdAt should be initialized on insert")
        assertTimestampWithin(inserted.updatedAt, beforeInsert, afterInsert, "updatedAt should be initialized on insert")

        waitPastStoredTimestampPrecision()
        assertEquals(
            1,
            IntegrationStrategyAccount(id = 1)
                .update()
                .set {
                    it.name = "updated"
                    it.balance = 20
                }
                .by { it.id }
                .execute()
                .affectedRows,
        )
        val updated = selectRawAccount(1)

        assertEquals("updated", updated.name)
        assertEquals(20, updated.balance)
        assertEquals(inserted.createdAt, updated.createdAt)
        assertTimestampAfter(inserted.updatedAt, updated.updatedAt, "updatedAt should advance on update")
        assertEquals(false, updated.deleted)
        assertEquals(1, updated.version)

        assertEquals(
            0,
            IntegrationStrategyAccount()
                .update()
                .set { it.balance = 99 }
                .where { it.id == 1 && it.version == 0 }
                .execute()
                .affectedRows,
        )
        assertEquals(updated, selectRawAccount(1))

        waitPastStoredTimestampPrecision()
        assertEquals(
            1,
            IntegrationStrategyAccount(id = 1)
                .delete()
                .by { it.id }
                .execute()
                .affectedRows,
        )
        val deleted = selectRawAccount(1)

        assertEquals(emptyList(), selectVisibleAccounts())
        assertEquals("updated", deleted.name)
        assertEquals(20, deleted.balance)
        assertEquals(true, deleted.deleted)
        assertEquals(2, deleted.version)
        assertEquals(inserted.createdAt, deleted.createdAt)
        assertTimestampAfter(updated.updatedAt, deleted.updatedAt, "updatedAt should advance on logic delete")

        assertEquals(
            0,
            IntegrationStrategyAccount(id = 1)
                .delete()
                .by { it.id }
                .execute()
                .affectedRows,
        )
    }

    @Test
    fun conflictUpsertStrategiesExecuteAgainstRealDatabase() {
        recreateStrategyAccountTable()

        val beforeInsert = LocalDateTime.now().minusSeconds(2)
        IntegrationStrategyAccount(id = 10, name = "inserted", balance = 10)
            .upsert { [it.name, it.balance] }
            .onConflict()
            .execute()
        val afterInsert = LocalDateTime.now().plusSeconds(2)
        val inserted = selectRawAccount(10)

        assertEquals("inserted", inserted.name)
        assertEquals(10, inserted.balance)
        assertEquals(false, inserted.deleted)
        assertEquals(0, inserted.version)
        assertTimestampWithin(inserted.createdAt, beforeInsert, afterInsert, "createdAt should be initialized on conflict upsert insert")
        assertTimestampWithin(inserted.updatedAt, beforeInsert, afterInsert, "updatedAt should be initialized on conflict upsert insert")

        waitPastStoredTimestampPrecision()
        IntegrationStrategyAccount(id = 10, name = "conflict-update", balance = 30)
            .upsert { [it.name, it.balance] }
            .onConflict()
            .execute()
        val updated = selectRawAccount(10)

        assertEquals("conflict-update", updated.name)
        assertEquals(30, updated.balance)
        assertEquals(false, updated.deleted)
        assertEquals(1, updated.version)
        assertEquals(inserted.createdAt, updated.createdAt)
        assertTimestampAfter(inserted.updatedAt, updated.updatedAt, "updatedAt should advance on conflict upsert")
    }

    @Test
    fun matchFieldUpsertRestoresLogicDeletedRowsAgainstRealDatabase() {
        recreateStrategyAccountTable()

        IntegrationStrategyAccount(id = 20, name = "deleted", balance = 10)
            .insert()
            .execute()
        IntegrationStrategyAccount(id = 20)
            .delete()
            .by { it.id }
            .execute()
        val deleted = selectRawAccount(20)

        assertEquals(true, deleted.deleted)
        assertEquals(1, deleted.version)
        assertEquals(emptyList(), selectVisibleAccounts())

        waitPastStoredTimestampPrecision()
        IntegrationStrategyAccount(id = 20, name = "restored", balance = 50)
            .upsert { [it.name, it.balance] }
            .on { it.id }
            .execute()
        val restored = selectRawAccount(20)

        assertEquals("restored", restored.name)
        assertEquals(50, restored.balance)
        assertEquals(false, restored.deleted)
        assertEquals(2, restored.version)
        assertEquals(deleted.createdAt, restored.createdAt)
        assertTimestampAfter(deleted.updatedAt, restored.updatedAt, "updatedAt should advance when upsert restores a logic-deleted row")
        assertEquals(listOf(restored), selectVisibleAccounts())
    }

    private fun recreateStrategyAccountTable() {
        requireDatabaseAvailable()
        configureKronos()
        with(wrapper.table) {
            dropTable(IntegrationStrategyAccount())
            syncTable(IntegrationStrategyAccount())
            truncateTable(IntegrationStrategyAccount(), restartIdentity = restartIdentity)
        }
    }

    private fun selectVisibleAccounts(): List<IntegrationStrategyAccount> =
        IntegrationStrategyAccount()
            .select()
            .orderBy { it.id.asc() }
            .toList<IntegrationStrategyAccount>()

    private fun selectRawAccount(id: Int): IntegrationStrategyAccount =
        wrapper.queryOne(
            """
            SELECT ${quote("id")}, ${quote("name")}, ${quote("balance")},
                   ${quote("created_at")}, ${quote("updated_at")},
                   ${quote("deleted")}, ${quote("version")}
            FROM ${table("kt_integration_strategy_account")}
            WHERE ${quote("id")} = :id
            """.trimIndent(),
            mapOf("id" to id),
        )

    private fun waitPastStoredTimestampPrecision() {
        Thread.sleep(1100)
    }

    private fun assertTimestampWithin(
        actual: LocalDateTime?,
        lowerBound: LocalDateTime,
        upperBound: LocalDateTime,
        message: String,
    ) {
        val value = assertNotNull(actual, message)
        assertTrue(!value.isBefore(lowerBound) && !value.isAfter(upperBound), "$message: $value")
    }

    private fun assertTimestampAfter(previous: LocalDateTime?, actual: LocalDateTime?, message: String) {
        val previousValue = assertNotNull(previous, "$message: previous timestamp")
        val actualValue = assertNotNull(actual, message)
        assertTrue(actualValue.isAfter(previousValue), "$message: $actualValue <= $previousValue")
    }
}
