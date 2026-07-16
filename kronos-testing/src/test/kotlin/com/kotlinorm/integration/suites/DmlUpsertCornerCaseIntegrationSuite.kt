package com.kotlinorm.integration.suites

import com.kotlinorm.integration.fixtures.DmlCornerAccount
import com.kotlinorm.integration.fixtures.DmlCornerAccountRecord
import com.kotlinorm.integration.fixtures.DmlCornerLedger
import com.kotlinorm.integration.fixtures.DmlCornerSnapshot
import com.kotlinorm.integration.fixtures.DmlCornerSnapshotRecord
import com.kotlinorm.integration.profiles.IntegrationScenarioProfile
import com.kotlinorm.integration.support.IntegrationDatabaseEnvironment
import com.kotlinorm.integration.support.IntegrationSuiteSupport
import com.kotlinorm.orm.ddl.table
import com.kotlinorm.orm.delete.delete
import com.kotlinorm.orm.insert.insert
import com.kotlinorm.orm.select.select
import com.kotlinorm.orm.update.update
import com.kotlinorm.orm.upsert.upsert
import kotlin.test.Test
import kotlin.test.assertEquals

abstract class DmlUpsertCornerCaseIntegrationSuite(
    environment: IntegrationDatabaseEnvironment,
    profile: IntegrationScenarioProfile,
) : IntegrationSuiteSupport(environment, profile) {
    @Test
    fun updateSetMixesIncrementDecrementNullableAssignmentByAndSubqueryWhereAgainstRealDatabase() {
        recreateCornerTables()
        seedCornerRows()

        val clearedNote: String? = null
        assertEquals(
            1,
            DmlCornerAccount(tenantId = 10)
                .update()
                .set {
                    it.balance += 7
                    it.quota -= 2
                    it.note = clearedNote
                    it.status = 4
                }
                .by { it.tenantId }
                .where { account ->
                    account.id in DmlCornerLedger()
                        .select { ledger -> ledger.accountId }
                        .where { ledger -> ledger.code == "bonus" && ledger.amount > 0 }
                }
                .execute()
                .affectedRows,
        )

        assertEquals(
            listOf(
                DmlCornerAccountRecord(
                    id = 1,
                    tenantId = 10,
                    email = "ada@example.com",
                    label = "Ada",
                    balance = 47,
                    quota = 3,
                    priority = 1,
                    note = null,
                    status = 4,
                ),
                DmlCornerAccountRecord(
                    id = 2,
                    tenantId = 10,
                    email = "grace@example.com",
                    label = "Grace",
                    balance = 15,
                    quota = 8,
                    priority = 2,
                    note = "keep",
                    status = 1,
                ),
                DmlCornerAccountRecord(
                    id = 3,
                    tenantId = 20,
                    email = "linus@example.com",
                    label = "Linus",
                    balance = 30,
                    quota = 4,
                    priority = 3,
                    note = "other",
                    status = 2,
                ),
            ),
            selectCornerAccounts(),
        )
    }

    @Test
    fun deleteCombinesObjectByConditionAndNotExistsSubqueryAgainstRealDatabase() {
        recreateCornerTables()
        seedCornerRows()

        assertEquals(
            1,
            DmlCornerAccount(tenantId = 10)
                .delete()
                .logic(false)
                .by { it.tenantId }
                .where { account ->
                    !exists(
                        DmlCornerLedger()
                            .select()
                            .where { ledger -> ledger.accountId == account.id && ledger.amount > 0 }
                    )
                }
                .execute()
                .affectedRows,
        )

        assertEquals(
            listOf(
                DmlCornerAccountRecord(
                    id = 1,
                    tenantId = 10,
                    email = "ada@example.com",
                    label = "Ada",
                    balance = 40,
                    quota = 5,
                    priority = 1,
                    note = "keep",
                    status = 1,
                ),
                DmlCornerAccountRecord(
                    id = 3,
                    tenantId = 20,
                    email = "linus@example.com",
                    label = "Linus",
                    balance = 30,
                    quota = 4,
                    priority = 3,
                    note = "other",
                    status = 2,
                ),
            ),
            selectCornerAccounts(),
        )
    }

    @Test
    fun onConflictPatchMixesExplicitUniqueKeyFieldReferenceAndScalarSubqueryAgainstRealDatabase() {
        recreateCornerTables()
        seedCornerRows()

        val balanceField = DmlCornerAccount().__columns.single { it.name == "balance" }
        DmlCornerAccount(
            id = 99,
            tenantId = 10,
            email = "ada@example.com",
            label = "Ada patched",
            balance = 99,
            quota = null,
            priority = null,
            note = "incoming",
            status = 7,
        )
            .upsert { [it.label, it.status] }
            .on { [it.tenantId, it.email] }
            .patch(
                "quota" to balanceField,
                "status" to DmlCornerLedger()
                    .select { ledger -> ledger.score }
                    .where { ledger -> ledger.accountId == 1 && ledger.code == "bonus" }
                    .limit(1),
            )
            .onConflict()
            .execute()

        assertEquals(
            listOf(
                DmlCornerAccountRecord(
                    id = 1,
                    tenantId = 10,
                    email = "ada@example.com",
                    label = "Ada patched",
                    balance = 40,
                    quota = 40,
                    priority = 1,
                    note = "keep",
                    status = 88,
                ),
                DmlCornerAccountRecord(
                    id = 2,
                    tenantId = 10,
                    email = "grace@example.com",
                    label = "Grace",
                    balance = 15,
                    quota = 8,
                    priority = 2,
                    note = "keep",
                    status = 1,
                ),
                DmlCornerAccountRecord(
                    id = 3,
                    tenantId = 20,
                    email = "linus@example.com",
                    label = "Linus",
                    balance = 30,
                    quota = 4,
                    priority = 3,
                    note = "other",
                    status = 2,
                ),
            ),
            selectCornerAccounts(),
        )
    }

    @Test
    fun insertSelectOmitsIdentityAndDefaultColumnsWhileMappingByTargetOrderAgainstRealDatabase() {
        recreateCornerTables()
        seedCornerRows()

        assertEquals(
            1,
            DmlCornerAccount()
                .select { [it.id, it.tenantId, it.email, it.label, it.status] }
                .where { it.id == 2 }
                .insert<DmlCornerSnapshot>()
                .execute()
                .affectedRows,
        )

        assertEquals(
            listOf(
                DmlCornerSnapshotRecord(
                    id = 1,
                    accountId = 2,
                    tenantId = 10,
                    email = "grace@example.com",
                    label = "Grace",
                    status = 1,
                    note = "snapshot-default",
                )
            ),
            selectCornerSnapshots(),
        )
    }

    private fun recreateCornerTables() {
        requireDatabaseAvailable()
        configureKronos()
        with(wrapper.table) {
            dropTable(DmlCornerSnapshot())
            dropTable(DmlCornerLedger())
            dropTable(DmlCornerAccount())
            syncTable(DmlCornerAccount())
            syncTable(DmlCornerLedger())
            syncTable(DmlCornerSnapshot())
            truncateTable(DmlCornerAccount(), restartIdentity = restartIdentity)
            truncateTable(DmlCornerLedger(), restartIdentity = restartIdentity)
            truncateTable(DmlCornerSnapshot(), restartIdentity = restartIdentity)
        }
    }

    private fun seedCornerRows() {
        listOf(
            DmlCornerAccount(
                id = 1,
                tenantId = 10,
                email = "ada@example.com",
                label = "Ada",
                balance = 40,
                quota = 5,
                priority = 1,
                note = "keep",
                status = 1,
            ),
            DmlCornerAccount(
                id = 2,
                tenantId = 10,
                email = "grace@example.com",
                label = "Grace",
                balance = 15,
                quota = 8,
                priority = 2,
                note = "keep",
                status = 1,
            ),
            DmlCornerAccount(
                id = 3,
                tenantId = 20,
                email = "linus@example.com",
                label = "Linus",
                balance = 30,
                quota = 4,
                priority = 3,
                note = "other",
                status = 2,
            ),
        ).forEach { account ->
            assertEquals(1, account.insert().execute().affectedRows)
        }

        listOf(
            DmlCornerLedger(id = 101, accountId = 1, tenantId = 10, code = "bonus", amount = 6, score = 88),
            DmlCornerLedger(id = 102, accountId = 2, tenantId = 10, code = "zero", amount = 0, score = 22),
            DmlCornerLedger(id = 103, accountId = 3, tenantId = 20, code = "bonus", amount = 9, score = 33),
        ).forEach { ledger ->
            assertEquals(1, ledger.insert().execute().affectedRows)
        }
    }

    private fun selectCornerAccounts(): List<DmlCornerAccountRecord> =
        DmlCornerAccount()
            .select()
            .orderBy { it.id.asc() }
            .toList<DmlCornerAccount>()
            .map { it.toRecord() }

    private fun selectCornerSnapshots(): List<DmlCornerSnapshotRecord> =
        DmlCornerSnapshot()
            .select()
            .orderBy { it.id.asc() }
            .toList<DmlCornerSnapshot>()
            .map { it.toRecord() }

    private fun DmlCornerAccount.toRecord(): DmlCornerAccountRecord =
        DmlCornerAccountRecord(
            id = id,
            tenantId = tenantId,
            email = email,
            label = label,
            balance = balance,
            quota = quota,
            priority = priority,
            note = note,
            status = status,
        )

    private fun DmlCornerSnapshot.toRecord(): DmlCornerSnapshotRecord =
        DmlCornerSnapshotRecord(
            id = id,
            accountId = accountId,
            tenantId = tenantId,
            email = email,
            label = label,
            status = status,
            note = note,
        )
}
