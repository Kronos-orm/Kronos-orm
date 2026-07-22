package com.kotlinorm.integration.suites

import com.kotlinorm.Kronos
import com.kotlinorm.database.SqlExecutor.queryOne
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.ValueCodecDirection
import com.kotlinorm.enums.ValueCodecOrigin
import com.kotlinorm.exceptions.ValueMappingException
import com.kotlinorm.integration.fixtures.SafetyGuardedRow
import com.kotlinorm.integration.fixtures.SafetyGuardedRowRecord
import com.kotlinorm.integration.fixtures.SafetyVersionedAccount
import com.kotlinorm.integration.fixtures.SafetyVersionedAccountRecord
import com.kotlinorm.integration.profiles.IntegrationScenarioProfile
import com.kotlinorm.integration.support.IntegrationDatabaseEnvironment
import com.kotlinorm.integration.support.IntegrationSuiteSupport
import com.kotlinorm.orm.ddl.table
import com.kotlinorm.orm.delete.delete
import com.kotlinorm.orm.insert.insert
import com.kotlinorm.orm.select.select
import com.kotlinorm.orm.update.update
import com.kotlinorm.orm.upsert.upsert
import com.kotlinorm.plugins.DataGuardPlugin
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

abstract class SafetyCornerCaseIntegrationSuite(
    environment: IntegrationDatabaseEnvironment,
    profile: IntegrationScenarioProfile,
) : IntegrationSuiteSupport(environment, profile) {
    @Test
    fun dataGuardBlocksTableWideOperationsButAllowsQualifiedWritesAgainstRealDatabase() {
        recreateGuardedRowTable()
        seedGuardedRows()

        withDefaultDataGuard {
            assertEquals(
                "Update operation is not allowed.",
                assertFailsWith<UnsupportedOperationException> {
                    SafetyGuardedRow(name = "blocked-update", score = 99, status = 9)
                        .update { [it.name, it.score, it.status] }
                        .execute()
                }.message,
            )
            assertEquals(
                "Delete operation is not allowed.",
                assertFailsWith<UnsupportedOperationException> {
                    SafetyGuardedRow()
                        .delete()
                        .logic(false)
                        .execute()
                }.message,
            )
            assertEquals(
                "Truncate operation is not allowed.",
                assertFailsWith<UnsupportedOperationException> {
                    wrapper.table.truncateTable(SafetyGuardedRow(), restartIdentity = restartIdentity)
                }.message,
            )
            assertEquals(
                "Drop operation is not allowed.",
                assertFailsWith<UnsupportedOperationException> {
                    wrapper.table.dropTable(SafetyGuardedRow())
                }.message,
            )

            assertEquals(
                1,
                SafetyGuardedRow(id = 1)
                    .update()
                    .set {
                        it.name = "qualified-update"
                        it.score = 15
                        it.status = 5
                    }
                    .by { it.id }
                    .execute()
                    .affectedRows,
            )
            assertEquals(
                1,
                SafetyGuardedRow(id = 2)
                    .delete()
                    .logic(false)
                    .by { it.id }
                    .execute()
                    .affectedRows,
            )
        }

        assertEquals(false, DataGuardPlugin.enabled)
        assertEquals(
            listOf(
                SafetyGuardedRowRecord(id = 1, name = "qualified-update", score = 15, status = 5),
                SafetyGuardedRowRecord(id = 3, name = "gamma", score = 30, status = 3),
            ),
            selectGuardedRows(),
        )

        assertEquals(
            2,
            SafetyGuardedRow(name = "post-guard", score = 77, status = 7)
                .update { [it.name, it.score, it.status] }
                .execute()
                .affectedRows,
        )
        assertEquals(
            listOf(
                SafetyGuardedRowRecord(id = 1, name = "post-guard", score = 77, status = 7),
                SafetyGuardedRowRecord(id = 3, name = "post-guard", score = 77, status = 7),
            ),
            selectGuardedRows(),
        )
    }

    @Test
    fun savepointRollbackAfterDuplicateKeyAllowsLaterWritesInSameTransactionAgainstRealDatabase() {
        recreateGuardedRowTable()

        Kronos.transact(wrapper = wrapper) {
            assertEquals(
                1,
                SafetyGuardedRow(id = 10, name = "before-savepoint", score = 10, status = 1)
                    .insert()
                    .execute()
                    .affectedRows,
            )

            val duplicateBranch = savepoint("safety_duplicate_branch")
            assertFails {
                SafetyGuardedRow(id = 10, name = "duplicate", score = 100, status = 9)
                    .insert()
                    .execute()
            }
            rollbackToSavepoint(duplicateBranch)

            assertEquals(
                1,
                SafetyGuardedRow(id = 11, name = "after-rollback", score = 11, status = 2)
                    .insert()
                    .execute()
                    .affectedRows,
            )
            assertEquals(
                1,
                SafetyGuardedRow(id = 10)
                    .update()
                    .set {
                        it.name = "before-updated"
                        it.score = 12
                    }
                    .by { it.id }
                    .execute()
                    .affectedRows,
            )
        }

        assertEquals(
            listOf(
                SafetyGuardedRowRecord(id = 10, name = "before-updated", score = 12, status = 1),
                SafetyGuardedRowRecord(id = 11, name = "after-rollback", score = 11, status = 2),
            ),
            selectGuardedRows(),
        )
    }

    @Test
    fun conflictUpsertRestoresLogicDeletedVersionedRowAndStrictSetValueDoesNotLeakAfterFailure() {
        recreateVersionedAccountTable()

        assertEquals(
            1,
            SafetyVersionedAccount(id = 1, externalId = "acct-1", name = "seed", score = 10)
                .insert()
                .execute()
                .affectedRows,
        )
        assertEquals(
            1,
            SafetyVersionedAccount(id = 1)
                .delete()
                .by { it.id }
                .execute()
                .affectedRows,
        )

        val deleted = SafetyVersionedAccountRecord(
            id = 1,
            externalId = "acct-1",
            name = "seed",
            score = 10,
            deleted = true,
            version = 1,
        )
        assertEquals(deleted, selectRawVersionedAccount(1))
        assertEquals(emptyList(), selectVisibleVersionedAccounts())

        SafetyVersionedAccount(id = 1, externalId = "acct-1", name = "restored", score = 35)
            .upsert { [it.name, it.score] }
            .on { it.externalId }
            .onConflict()
            .execute()

        val restored = SafetyVersionedAccountRecord(
            id = 1,
            externalId = "acct-1",
            name = "restored",
            score = 35,
            deleted = false,
            version = 2,
        )
        assertEquals(restored, selectRawVersionedAccount(1))
        assertEquals(listOf(restored), selectVisibleVersionedAccounts())

        val originalStrictSetValue = Kronos.strictSetValue
        if (wrapper.dbType == DBType.SQLite) {
            val failure = assertFailsWith<ValueMappingException> {
                withStrictSetValue(true) {
                    selectRawVersionedAccount(1)
                }
            }
            assertEquals(ValueCodecDirection.DECODE, failure.direction)
            assertEquals(ValueCodecOrigin.DATABASE, failure.origin)
            assertEquals("deleted", failure.fieldName)
            assertNull(failure.declaredSourceType)
            assertEquals(typeOf<Int>(), failure.runtimeSourceType)
            assertEquals(typeOf<Boolean?>(), failure.targetType)
        } else {
            assertEquals(
                "forced strict read failure",
                assertFailsWith<IllegalStateException> {
                    withStrictSetValue(true) {
                        assertEquals(restored, selectRawVersionedAccount(1))
                        error("forced strict read failure")
                    }
                }.message,
            )
        }
        assertEquals(originalStrictSetValue, Kronos.strictSetValue)

        withStrictSetValue(false) {
            assertEquals(restored, selectRawVersionedAccount(1))
        }
        assertEquals(originalStrictSetValue, Kronos.strictSetValue)
    }

    private fun recreateGuardedRowTable() {
        requireDatabaseAvailable()
        configureKronos()
        resetDataGuard()
        with(wrapper.table) {
            dropTable(SafetyGuardedRow())
            syncTable(SafetyGuardedRow())
            truncateTable(SafetyGuardedRow(), restartIdentity = restartIdentity)
        }
        assertEquals(emptyList(), selectGuardedRows())
    }

    private fun recreateVersionedAccountTable() {
        requireDatabaseAvailable()
        configureKronos()
        resetDataGuard()
        with(wrapper.table) {
            dropTable(SafetyVersionedAccount())
            syncTable(SafetyVersionedAccount())
            truncateTable(SafetyVersionedAccount(), restartIdentity = restartIdentity)
        }
        assertEquals(emptyList(), selectVisibleVersionedAccounts())
    }

    private fun seedGuardedRows() {
        listOf(
            SafetyGuardedRow(id = 1, name = "alpha", score = 10, status = 1),
            SafetyGuardedRow(id = 2, name = "beta", score = 20, status = 2),
            SafetyGuardedRow(id = 3, name = "gamma", score = 30, status = 3),
        ).forEach { row ->
            assertEquals(1, row.insert().execute().affectedRows)
        }
    }

    private fun selectGuardedRows(): List<SafetyGuardedRowRecord> =
        SafetyGuardedRow()
            .select()
            .orderBy { it.id.asc() }
            .toList<SafetyGuardedRow>()
            .map { row ->
                SafetyGuardedRowRecord(id = row.id, name = row.name, score = row.score, status = row.status)
            }

    private fun selectVisibleVersionedAccounts(): List<SafetyVersionedAccountRecord> =
        SafetyVersionedAccount()
            .select()
            .orderBy { it.id.asc() }
            .toList<SafetyVersionedAccount>()
            .map { it.toRecord() }

    private fun selectRawVersionedAccount(id: Int): SafetyVersionedAccountRecord =
        wrapper.queryOne<SafetyVersionedAccount>(
            """
            SELECT ${quote("id")}, ${quote("external_id")}, ${quote("name")}, ${quote("score")},
                   ${quote("deleted")}, ${quote("version")}
            FROM ${table("kt_safety_versioned_account")}
            WHERE ${quote("id")} = :id
            """.trimIndent(),
            mapOf("id" to id),
        ).toRecord()

    private fun SafetyVersionedAccount.toRecord(): SafetyVersionedAccountRecord =
        SafetyVersionedAccountRecord(
            id = id,
            externalId = externalId,
            name = name,
            score = score,
            deleted = deleted,
            version = version,
        )

    private inline fun withDefaultDataGuard(block: () -> Unit) {
        DataGuardPlugin.enable {}
        try {
            block()
        } finally {
            resetDataGuard()
        }
    }

    private fun resetDataGuard() {
        DataGuardPlugin.enable {}
        DataGuardPlugin.disable()
    }

    private inline fun withStrictSetValue(value: Boolean, block: () -> Unit) {
        val previous = Kronos.strictSetValue
        Kronos.strictSetValue = value
        try {
            block()
        } finally {
            Kronos.strictSetValue = previous
        }
    }
}
