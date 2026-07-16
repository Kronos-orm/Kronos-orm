package com.kotlinorm.integration.suites

import com.kotlinorm.integration.fixtures.MutationDialectCornerCaseAccount
import com.kotlinorm.integration.fixtures.MutationDialectCornerCaseAccountRecord
import com.kotlinorm.integration.fixtures.MutationDialectCornerCaseArchiveRecord
import com.kotlinorm.integration.fixtures.MutationDialectCornerCaseCtasArchive
import com.kotlinorm.integration.fixtures.MutationDialectCornerCaseIdentityArchive
import com.kotlinorm.integration.fixtures.MutationDialectCornerCaseLedger
import com.kotlinorm.integration.fixtures.MutationDialectCornerCaseManualArchive
import com.kotlinorm.integration.profiles.IntegrationScenarioProfile
import com.kotlinorm.integration.profiles.StandardIntegrationScenarioProfile
import com.kotlinorm.integration.support.IntegrationDatabaseEnvironment
import com.kotlinorm.integration.support.IntegrationDatabaseEnvironments.mysql
import com.kotlinorm.integration.support.IntegrationDatabaseEnvironments.oracle
import com.kotlinorm.integration.support.IntegrationDatabaseEnvironments.postgres
import com.kotlinorm.integration.support.IntegrationDatabaseEnvironments.sqlite
import com.kotlinorm.integration.support.IntegrationDatabaseEnvironments.sqlServer
import com.kotlinorm.integration.support.IntegrationSuiteSupport
import com.kotlinorm.orm.ddl.table
import com.kotlinorm.orm.delete.delete
import com.kotlinorm.orm.insert.insert
import com.kotlinorm.orm.select.select
import com.kotlinorm.orm.update.update
import com.kotlinorm.orm.upsert.upsert
import kotlin.test.Test
import kotlin.test.assertEquals

abstract class MutationDialectCornerCaseIntegrationSuite(
    environment: IntegrationDatabaseEnvironment,
    profile: IntegrationScenarioProfile,
) : IntegrationSuiteSupport(environment, profile) {
    @Test
    fun onConflictInfersNullableUniqueKeyWhenValueIsPresentAgainstRealDatabase() {
        recreateMutationTables()

        MutationDialectCornerCaseAccount(
            externalCode = "nullable-unique-1",
            tenantId = 10,
            region = "apac",
            email = "nullable-unique-1@example.com",
            label = "first",
            score = 10,
            quota = 1,
            status = 1,
        )
            .upsert { [it.label, it.score, it.quota, it.status] }
            .onConflict()
            .execute()

        MutationDialectCornerCaseAccount(
            externalCode = "nullable-unique-1",
            tenantId = 10,
            region = "apac",
            email = "nullable-unique-1@example.com",
            label = "second",
            score = 20,
            quota = 2,
            status = 2,
        )
            .upsert { [it.label, it.score, it.quota, it.status] }
            .onConflict()
            .execute()

        assertEquals(
            listOf(
                MutationDialectCornerCaseAccountRecord(
                    externalCode = "nullable-unique-1",
                    tenantId = 10,
                    region = "apac",
                    email = "nullable-unique-1@example.com",
                    label = "second",
                    score = 20,
                    quota = 2,
                    status = 2,
                ),
            ),
            selectAccounts(),
        )
    }

    @Test
    fun onConflictUsesCompositeNullableUniqueKeyWhenAllPartsArePresentAgainstRealDatabase() {
        recreateMutationTables()

        MutationDialectCornerCaseAccount(
            externalCode = "composite-1a",
            tenantId = 20,
            region = "emea",
            email = "shared@example.com",
            label = "first composite",
            score = 11,
            quota = 3,
            status = 1,
        )
            .upsert { [it.label, it.score, it.quota, it.status] }
            .on { [it.tenantId, it.region, it.email] }
            .onConflict()
            .execute()

        MutationDialectCornerCaseAccount(
            externalCode = "composite-1b",
            tenantId = 20,
            region = "emea",
            email = "shared@example.com",
            label = "updated composite",
            score = 22,
            quota = 4,
            status = 2,
        )
            .upsert { [it.label, it.score, it.quota, it.status] }
            .on { [it.tenantId, it.region, it.email] }
            .onConflict()
            .execute()

        MutationDialectCornerCaseAccount(
            externalCode = "composite-2",
            tenantId = 20,
            region = "amer",
            email = "shared@example.com",
            label = "other region",
            score = 33,
            quota = 5,
            status = 3,
        )
            .upsert { [it.label, it.score, it.quota, it.status] }
            .on { [it.tenantId, it.region, it.email] }
            .onConflict()
            .execute()

        assertEquals(
            listOf(
                MutationDialectCornerCaseAccountRecord(
                    externalCode = "composite-1a",
                    tenantId = 20,
                    region = "emea",
                    email = "shared@example.com",
                    label = "updated composite",
                    score = 22,
                    quota = 4,
                    status = 2,
                ),
                MutationDialectCornerCaseAccountRecord(
                    externalCode = "composite-2",
                    tenantId = 20,
                    region = "amer",
                    email = "shared@example.com",
                    label = "other region",
                    score = 33,
                    quota = 5,
                    status = 3,
                ),
            ),
            selectAccounts(),
        )
    }

    @Test
    fun onConflictPatchAcceptsFieldScalarSelectableAndScalarValuesAgainstRealDatabase() {
        recreateMutationTables()
        insertAccount(
            externalCode = "patch-1",
            tenantId = 30,
            region = "apac",
            email = "patch-1@example.com",
            label = "before patch",
            score = 12,
            quota = 1,
            status = 1,
        )
        insertLedger(
            id = 1,
            externalCode = "patch-1",
            tenantId = 30,
            region = "apac",
            email = "patch-1@example.com",
            amount = 64,
            score = 9,
            status = 8,
        )

        val scoreField = MutationDialectCornerCaseAccount().__columns.single { it.name == "score" }
        MutationDialectCornerCaseAccount(
            externalCode = "patch-1",
            tenantId = 30,
            region = "apac",
            email = "patch-1@example.com",
            label = "after patch",
            score = 99,
            quota = 99,
            status = 99,
        )
            .upsert { it.label }
            .on { it.externalCode }
            .patch(
                "quota" to scoreField,
                "status" to MutationDialectCornerCaseLedger()
                    .select { ledger -> ledger.status }
                    .where { ledger -> ledger.externalCode == "patch-1" }
                    .limit(1),
                "score" to 99,
            )
            .onConflict()
            .execute()

        assertEquals(
            listOf(
                MutationDialectCornerCaseAccountRecord(
                    externalCode = "patch-1",
                    tenantId = 30,
                    region = "apac",
                    email = "patch-1@example.com",
                    label = "after patch",
                    score = 99,
                    quota = 12,
                    status = 8,
                ),
            ),
            selectAccounts(),
        )
    }

    @Test
    fun updateAndDeleteUseIndependentSubqueriesAgainstRealDatabase() {
        recreateMutationTables()
        insertAccount("update-1", 40, "apac", "update-1@example.com", "Update One", 10, 1, 0)
        insertAccount("update-2", 40, "apac", "update-2@example.com", "Update Two", 20, 2, 0)
        insertAccount("delete-1", 40, "apac", "delete-1@example.com", "Delete One", 30, 3, 9)
        insertAccount("keep-1", 40, "apac", "keep-1@example.com", "Keep One", 40, 4, 9)
        insertLedger(101, "update-1", 40, "apac", "update-1@example.com", 70, 0, 1)
        insertLedger(102, "update-2", 40, "apac", "update-2@example.com", 0, 0, 1)
        insertLedger(103, "keep-1", 40, "apac", "keep-1@example.com", 5, 0, 9)

        assertEquals(
            1,
            MutationDialectCornerCaseAccount()
                .update()
                .set {
                    it.score = (MutationDialectCornerCaseLedger()
                        .select { ledger -> ledger.amount }
                        .where { ledger -> ledger.externalCode == it.externalCode && ledger.status == 1 }
                        .limit(1) as Int?)
                    it.status = 2
                }
                .where { account ->
                    exists(
                        MutationDialectCornerCaseLedger()
                            .select()
                            .where { ledger ->
                                ledger.externalCode == account.externalCode &&
                                    ledger.status == 1 &&
                                    ledger.amount > 0
                            }
                    )
                }
                .execute()
                .affectedRows,
        )

        assertEquals(
            1,
            MutationDialectCornerCaseAccount()
                .delete()
                .logic(false)
                .where { account ->
                    account.status == 9 &&
                        !exists(
                            MutationDialectCornerCaseLedger()
                                .select()
                                .where { ledger ->
                                    ledger.externalCode == account.externalCode &&
                                        ledger.amount > 0
                                }
                        )
                }
                .execute()
                .affectedRows,
        )

        assertEquals(
            listOf(
                MutationDialectCornerCaseAccountRecord(
                    externalCode = "keep-1",
                    tenantId = 40,
                    region = "apac",
                    email = "keep-1@example.com",
                    label = "Keep One",
                    score = 40,
                    quota = 4,
                    status = 9,
                ),
                MutationDialectCornerCaseAccountRecord(
                    externalCode = "update-1",
                    tenantId = 40,
                    region = "apac",
                    email = "update-1@example.com",
                    label = "Update One",
                    score = 70,
                    quota = 1,
                    status = 2,
                ),
                MutationDialectCornerCaseAccountRecord(
                    externalCode = "update-2",
                    tenantId = 40,
                    region = "apac",
                    email = "update-2@example.com",
                    label = "Update Two",
                    score = 20,
                    quota = 2,
                    status = 0,
                ),
            ),
            selectAccounts(),
        )
    }

    @Test
    fun insertSelectMapsShuffledSourceIntoIdentityAndManualTargetsAgainstRealDatabase() {
        recreateMutationTables()
        insertAccount("identity-source", 50, "apac", "identity@example.com", "Identity Source", 55, 5, 1)
        insertAccount("manual-source", 50, "emea", "manual@example.com", "Manual Source", 66, 6, 2)

        assertEquals(
            1,
            MutationDialectCornerCaseAccount()
                .select { [it.status, it.email, it.tenantId, it.externalCode, it.score, it.region] }
                .where { it.status == 1 }
                .insert<MutationDialectCornerCaseIdentityArchive> {
                    [it.externalCode, it.tenantId, it.region, it.email, it.score, it.status]
                }
                .execute()
                .affectedRows,
        )

        assertEquals(
            1,
            MutationDialectCornerCaseAccount()
                .select { [it.status, it.email, it.id.alias("archiveId"), it.tenantId, it.externalCode, it.score, it.region] }
                .where { it.status == 2 }
                .insert<MutationDialectCornerCaseManualArchive> {
                    [it.archiveId, it.externalCode, it.tenantId, it.region, it.email, it.score, it.status]
                }
                .execute()
                .affectedRows,
        )

        assertEquals(
            listOf(
                MutationDialectCornerCaseArchiveRecord(
                    externalCode = "identity-source",
                    tenantId = 50,
                    region = "apac",
                    email = "identity@example.com",
                    score = 55,
                    status = 1,
                ),
            ),
            selectIdentityArchives(),
        )
        assertEquals(
            listOf(
                MutationDialectCornerCaseArchiveRecord(
                    externalCode = "manual-source",
                    tenantId = 50,
                    region = "emea",
                    email = "manual@example.com",
                    score = 66,
                    status = 2,
                ),
            ),
            selectManualArchives(),
        )
    }

    @Test
    fun ctasUsesQueryOutputNamesWhenSelectOrderDiffersFromTargetModelAgainstRealDatabase() {
        recreateMutationTables()
        insertAccount("ctas-source", 60, "amer", "ctas@example.com", "CTAS Source", 77, 7, 3)

        wrapper.table.createTable(
            MutationDialectCornerCaseCtasArchive(),
            MutationDialectCornerCaseAccount()
                .select { [it.status, it.email, it.externalCode, it.tenantId] }
                .where { it.externalCode == "ctas-source" },
        )

        assertEquals(
            listOf(MutationDialectCornerCaseCtasArchive("ctas-source", 60, "ctas@example.com", 3)),
            MutationDialectCornerCaseCtasArchive()
                .select()
                .orderBy { it.externalCode.asc() }
                .toList<MutationDialectCornerCaseCtasArchive>(),
        )
    }

    private fun recreateMutationTables() {
        requireDatabaseAvailable()
        configureKronos()
        with(wrapper.table) {
            dropTable(MutationDialectCornerCaseCtasArchive())
            dropTable(MutationDialectCornerCaseManualArchive())
            dropTable(MutationDialectCornerCaseIdentityArchive())
            dropTable(MutationDialectCornerCaseLedger())
            dropTable(MutationDialectCornerCaseAccount())
            syncTable(MutationDialectCornerCaseAccount())
            syncTable(MutationDialectCornerCaseLedger())
            syncTable(MutationDialectCornerCaseIdentityArchive())
            syncTable(MutationDialectCornerCaseManualArchive())
            truncateTable(MutationDialectCornerCaseAccount(), restartIdentity = restartIdentity)
            truncateTable(MutationDialectCornerCaseLedger(), restartIdentity = restartIdentity)
            truncateTable(MutationDialectCornerCaseIdentityArchive(), restartIdentity = restartIdentity)
            truncateTable(MutationDialectCornerCaseManualArchive(), restartIdentity = restartIdentity)
        }
    }

    private fun insertAccount(
        externalCode: String,
        tenantId: Int,
        region: String,
        email: String,
        label: String,
        score: Int,
        quota: Int,
        status: Int,
    ) {
        assertEquals(
            1,
            MutationDialectCornerCaseAccount(
                externalCode = externalCode,
                tenantId = tenantId,
                region = region,
                email = email,
                label = label,
                score = score,
                quota = quota,
                status = status,
            ).insert().execute().affectedRows,
        )
    }

    private fun insertLedger(
        id: Int,
        externalCode: String,
        tenantId: Int,
        region: String,
        email: String,
        amount: Int,
        score: Int,
        status: Int,
    ) {
        assertEquals(
            1,
            MutationDialectCornerCaseLedger(
                id = id,
                externalCode = externalCode,
                tenantId = tenantId,
                region = region,
                email = email,
                amount = amount,
                score = score,
                status = status,
            ).insert().execute().affectedRows,
        )
    }

    private fun selectAccounts(): List<MutationDialectCornerCaseAccountRecord> =
        MutationDialectCornerCaseAccount()
            .select()
            .orderBy { [it.externalCode.asc(), it.region.asc()] }
            .toList<MutationDialectCornerCaseAccount>()
            .map { it.toRecord() }

    private fun selectIdentityArchives(): List<MutationDialectCornerCaseArchiveRecord> =
        MutationDialectCornerCaseIdentityArchive()
            .select()
            .orderBy { it.externalCode.asc() }
            .toList<MutationDialectCornerCaseIdentityArchive>()
            .map { it.toRecord() }

    private fun selectManualArchives(): List<MutationDialectCornerCaseArchiveRecord> =
        MutationDialectCornerCaseManualArchive()
            .select()
            .orderBy { it.externalCode.asc() }
            .toList<MutationDialectCornerCaseManualArchive>()
            .map { it.toRecord() }

    private fun MutationDialectCornerCaseAccount.toRecord(): MutationDialectCornerCaseAccountRecord =
        MutationDialectCornerCaseAccountRecord(
            externalCode = externalCode,
            tenantId = tenantId,
            region = region,
            email = email,
            label = label,
            score = score,
            quota = quota,
            status = status,
        )

    private fun MutationDialectCornerCaseIdentityArchive.toRecord(): MutationDialectCornerCaseArchiveRecord =
        MutationDialectCornerCaseArchiveRecord(
            externalCode = externalCode,
            tenantId = tenantId,
            region = region,
            email = email,
            score = score,
            status = status,
        )

    private fun MutationDialectCornerCaseManualArchive.toRecord(): MutationDialectCornerCaseArchiveRecord =
        MutationDialectCornerCaseArchiveRecord(
            externalCode = externalCode,
            tenantId = tenantId,
            region = region,
            email = email,
            score = score,
            status = status,
        )
}

class MysqlMutationDialectCornerCaseIntegrationTest :
    MutationDialectCornerCaseIntegrationSuite(mysql, StandardIntegrationScenarioProfile)

class PostgresMutationDialectCornerCaseIntegrationTest :
    MutationDialectCornerCaseIntegrationSuite(postgres, StandardIntegrationScenarioProfile)

class SqliteMutationDialectCornerCaseIntegrationTest :
    MutationDialectCornerCaseIntegrationSuite(sqlite, StandardIntegrationScenarioProfile)

class SqlServerMutationDialectCornerCaseIntegrationTest :
    MutationDialectCornerCaseIntegrationSuite(sqlServer, StandardIntegrationScenarioProfile)

class OracleMutationDialectCornerCaseIntegrationTest :
    MutationDialectCornerCaseIntegrationSuite(oracle, StandardIntegrationScenarioProfile)
