package com.kotlinorm.integration.suites

import com.kotlinorm.Kronos
import com.kotlinorm.integration.fixtures.IntegrationUser
import com.kotlinorm.integration.fixtures.IntegrationUserRecord
import com.kotlinorm.integration.profiles.IntegrationScenarioProfile
import com.kotlinorm.integration.support.IntegrationDatabaseEnvironment
import com.kotlinorm.integration.support.IntegrationSuiteSupport
import com.kotlinorm.orm.insert.insert
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

abstract class TransactionIntegrationSuite(
    environment: IntegrationDatabaseEnvironment,
    profile: IntegrationScenarioProfile,
) : IntegrationSuiteSupport(environment, profile) {
    @Test
    fun commitRollbackNestedRollbackAndSavepointExecuteAgainstRealDatabase() {
        recreateTables()

        Kronos.transact(wrapper = wrapper) {
            assertEquals(1, profile.insertUser(IntegrationUserRecord(id = 40, name = "Commit-A", score = 40, status = 4)))
            assertEquals(1, profile.insertUser(IntegrationUserRecord(id = 41, name = "Commit-B", score = 41, status = 4)))
        }
        assertEquals(
            listOf(
                IntegrationUserRecord(id = 40, name = "Commit-A", score = 40, status = 4),
                IntegrationUserRecord(id = 41, name = "Commit-B", score = 41, status = 4),
            ),
            profile.selectAllUsers(),
        )

        assertFailsWith<IllegalStateException> {
            Kronos.transact(wrapper = wrapper) {
                assertEquals(1, profile.insertUser(IntegrationUserRecord(id = 42, name = "Rollback", score = 42, status = 4)))
                error("force rollback")
            }
        }
        assertEquals(
            listOf(
                IntegrationUserRecord(id = 40, name = "Commit-A", score = 40, status = 4),
                IntegrationUserRecord(id = 41, name = "Commit-B", score = 41, status = 4),
            ),
            profile.selectAllUsers(),
        )

        assertFailsWith<IllegalArgumentException> {
            Kronos.transact(wrapper = wrapper) {
                assertEquals(1, profile.insertUser(IntegrationUserRecord(id = 43, name = "Outer", score = 43, status = 4)))
                Kronos.transact(wrapper = wrapper) {
                    assertEquals(1, profile.insertUser(IntegrationUserRecord(id = 44, name = "Inner", score = 44, status = 4)))
                }
                throw IllegalArgumentException("force outer rollback")
            }
        }
        assertEquals(
            listOf(
                IntegrationUserRecord(id = 40, name = "Commit-A", score = 40, status = 4),
                IntegrationUserRecord(id = 41, name = "Commit-B", score = 41, status = 4),
            ),
            profile.selectAllUsers(),
        )

        Kronos.transact(wrapper = wrapper) {
            assertEquals(1, profile.insertUser(IntegrationUserRecord(id = 45, name = "Before-Savepoint", score = 45, status = 4)))
            val savepoint = savepoint("after_before_savepoint")
            assertEquals(1, profile.insertUser(IntegrationUserRecord(id = 46, name = "After-Savepoint", score = 46, status = 4)))
            rollbackToSavepoint(savepoint)
        }
        assertEquals(
            listOf(
                IntegrationUserRecord(id = 40, name = "Commit-A", score = 40, status = 4),
                IntegrationUserRecord(id = 41, name = "Commit-B", score = 41, status = 4),
                IntegrationUserRecord(id = 45, name = "Before-Savepoint", score = 45, status = 4),
            ),
            profile.selectAllUsers(),
        )
    }

    @Test
    fun actionTaskAfterExecuteFailureRollsBackMainAndAfterActions() {
        recreateTables()

        val actionTask = IntegrationUser(id = 47, name = "Main-Action", score = 47, status = 4)
            .insert()
            .build(wrapper)
            .doAfterExecute { _ ->
                assertEquals(
                    1,
                    profile.insertUser(IntegrationUserRecord(id = 48, name = "After-Action", score = 48, status = 4))
                )
                error("force afterExecute rollback")
            }

        assertEquals(
            "force afterExecute rollback",
            assertFailsWith<IllegalStateException> {
                actionTask.execute(wrapper)
            }.message
        )
        assertEquals(emptyList(), profile.selectAllUsers())
    }
}
