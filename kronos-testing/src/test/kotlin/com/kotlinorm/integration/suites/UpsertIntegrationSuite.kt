package com.kotlinorm.integration.suites

import com.kotlinorm.integration.fixtures.IntegrationCompositeUpsertUser
import com.kotlinorm.integration.fixtures.IntegrationUniqueUpsertUser
import com.kotlinorm.integration.fixtures.IntegrationUserRecord
import com.kotlinorm.integration.profiles.IntegrationScenarioProfile
import com.kotlinorm.integration.support.IntegrationDatabaseEnvironment
import com.kotlinorm.integration.support.IntegrationSuiteSupport
import com.kotlinorm.orm.ddl.table
import com.kotlinorm.orm.select.select
import com.kotlinorm.orm.upsert.upsert
import kotlin.test.Test
import kotlin.test.assertEquals

abstract class UpsertIntegrationSuite(
    environment: IntegrationDatabaseEnvironment,
    profile: IntegrationScenarioProfile,
) : IntegrationSuiteSupport(environment, profile) {
    @Test
    fun upsertInsertAndUpdateExecuteAgainstRealDatabase() {
        recreateTables()

        assertEquals(1, profile.insertUser(IntegrationUserRecord(id = 10, name = "first", score = 10, status = 1)))
        executeUpsert(IntegrationUserRecord(id = 10, name = "second", score = 20, status = 2))
        assertEquals(
            IntegrationUserRecord(id = 10, name = "second", score = 20, status = 2),
            profile.selectUserById(10),
        )

        executeUpsert(IntegrationUserRecord(id = 11, name = "inserted", score = 30, status = 1))
        assertEquals(
            listOf(
                IntegrationUserRecord(id = 10, name = "second", score = 20, status = 2),
                IntegrationUserRecord(id = 11, name = "inserted", score = 30, status = 1),
            ),
            profile.selectAllUsers(),
        )
    }

    @Test
    fun onConflictInfersSingleUniqueIndexAgainstRealDatabase() {
        recreateUniqueUpsertUserTable()

        IntegrationUniqueUpsertUser(email = "ada@example.com", name = "first", score = 10)
            .upsert { [it.name, it.score] }
            .onConflict()
            .execute()
        IntegrationUniqueUpsertUser(email = "ada@example.com", name = "second", score = 20)
            .upsert { [it.name, it.score] }
            .onConflict()
            .execute()

        val rows = IntegrationUniqueUpsertUser()
            .select()
            .orderBy { it.email.asc() }
            .toList<IntegrationUniqueUpsertUser>()

        assertEquals(1, rows.size)
        assertEquals("ada@example.com", rows.single().email)
        assertEquals("second", rows.single().name)
        assertEquals(20, rows.single().score)
    }

    @Test
    fun onConflictInfersCompositeUniqueIndexAgainstRealDatabase() {
        recreateCompositeUpsertUserTable()

        IntegrationCompositeUpsertUser(tenantId = 7, email = "ada@example.com", name = "first", score = 10)
            .upsert { [it.name, it.score] }
            .onConflict()
            .execute()
        IntegrationCompositeUpsertUser(tenantId = 7, email = "ada@example.com", name = "second", score = 20)
            .upsert { [it.name, it.score] }
            .onConflict()
            .execute()
        IntegrationCompositeUpsertUser(tenantId = 8, email = "ada@example.com", name = "other tenant", score = 30)
            .upsert { [it.name, it.score] }
            .onConflict()
            .execute()

        val rows = IntegrationCompositeUpsertUser()
            .select()
            .orderBy { [it.tenantId.asc(), it.email.asc()] }
            .toList<IntegrationCompositeUpsertUser>()

        assertEquals(2, rows.size)
        assertEquals(7, rows[0].tenantId)
        assertEquals("second", rows[0].name)
        assertEquals(20, rows[0].score)
        assertEquals(8, rows[1].tenantId)
        assertEquals("other tenant", rows[1].name)
        assertEquals(30, rows[1].score)
    }

    private fun recreateUniqueUpsertUserTable() {
        requireDatabaseAvailable()
        configureKronos()
        with(wrapper.table) {
            dropTable(IntegrationUniqueUpsertUser())
            syncTable(IntegrationUniqueUpsertUser())
            truncateTable(IntegrationUniqueUpsertUser(), restartIdentity = restartIdentity)
        }
    }

    private fun recreateCompositeUpsertUserTable() {
        requireDatabaseAvailable()
        configureKronos()
        with(wrapper.table) {
            dropTable(IntegrationCompositeUpsertUser())
            syncTable(IntegrationCompositeUpsertUser())
            truncateTable(IntegrationCompositeUpsertUser(), restartIdentity = restartIdentity)
        }
    }
}
