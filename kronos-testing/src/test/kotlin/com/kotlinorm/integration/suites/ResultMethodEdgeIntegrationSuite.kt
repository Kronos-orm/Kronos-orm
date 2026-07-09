package com.kotlinorm.integration.suites

import com.kotlinorm.functions.bundled.exts.PolymerizationFunctions.count
import com.kotlinorm.functions.bundled.exts.PolymerizationFunctions.sum
import com.kotlinorm.integration.fixtures.IntegrationAggregateProjection
import com.kotlinorm.integration.fixtures.IntegrationAliasProjection
import com.kotlinorm.integration.fixtures.IntegrationPageProjection
import com.kotlinorm.integration.fixtures.IntegrationUser
import com.kotlinorm.integration.profiles.IntegrationScenarioProfile
import com.kotlinorm.integration.support.IntegrationDatabaseEnvironment
import com.kotlinorm.integration.support.IntegrationSuiteSupport
import com.kotlinorm.orm.insert.insert
import com.kotlinorm.orm.select.select
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

abstract class ResultMethodEdgeIntegrationSuite(
    environment: IntegrationDatabaseEnvironment,
    profile: IntegrationScenarioProfile,
) : IntegrationSuiteSupport(environment, profile) {
    @Test
    fun pagedQueryListSupportsDestructuringWithoutExplicitKPojoFlag() {
        recreateTables()
        profile.seedUsersAndOrders()

        val (total, pageUsers) = IntegrationUser()
            .select { [it.id, it.name] }
            .orderBy { it.id.asc() }
            .page(pi = 2, ps = 2)
            .withTotal()
            .queryList<IntegrationPageProjection>()

        assertEquals(4, total)
        assertEquals(listOf(3, 4), pageUsers.map { it.id })
        assertEquals(listOf("Linus", "NoOrder"), pageUsers.map { it.name })
    }

    @Test
    fun pagedQueryListMapsNullableSelectedPropertiesIntoProjectionRows() {
        recreateTables()
        seedNullableUser()

        val (total, pageUsers) = IntegrationUser()
            .select { [it.id, it.name, it.score] }
            .orderBy { it.id.asc() }
            .page(pi = 3, ps = 2)
            .withTotal()
            .queryList<IntegrationPageProjection>()

        assertEquals(5, total)
        assertEquals(5, pageUsers.single().id)
        assertNull(pageUsers.single().name)
        assertNull(pageUsers.single().score)
    }

    @Test
    fun camelCaseAliasesMapIntoProjectionPropertiesWithoutOrderByReference() {
        recreateTables()
        profile.seedUsersAndOrders()

        val rows = IntegrationUser()
            .select { [it.id, it.name.alias("userName"), it.score.alias("userScore")] }
            .where { it.id in [1, 2] }
            .orderBy { it.id.asc() }
            .queryList<IntegrationAliasProjection>()

        assertEquals(listOf("Ada", "Grace"), rows.map { it.userName })
        assertEquals(listOf(10, 20), rows.map { it.userScore })
    }

    @Test
    fun pagedQueryReturnsNullSelectedValuesInMapRows() {
        recreateTables()
        seedNullableUser()

        val (total, rows) = IntegrationUser()
            .select { [it.id, it.name, it.score] }
            .orderBy { it.id.asc() }
            .page(pi = 1, ps = 10)
            .withTotal()
            .query()

        assertEquals(5, total)
        val row = rows.single { it.cell("id") == 5 }
        assertTrue(row.hasColumn("name"))
        assertTrue(row.hasColumn("score"))
        assertNull(row.cell("name"))
        assertNull(row.cell("score"))
    }

    @Test
    fun queryReturnsNullSelectedValuesInMapRows() {
        recreateTables()
        seedNullableUser()

        val row = IntegrationUser()
            .select { [it.id, it.name, it.score] }
            .where { it.id == 5 }
            .query()
            .single()

        assertTrue(row.hasColumn("name"))
        assertTrue(row.hasColumn("score"))
        assertNull(row.cell("name"))
        assertNull(row.cell("score"))
    }

    @Test
    fun queryOneOrNullReturnsNullForNoRowsButQueryOneThrows() {
        recreateTables()
        profile.seedUsersAndOrders()

        assertNull(
            IntegrationUser()
                .select { it.id }
                .where { it.id == -1 }
                .queryOneOrNull<Int>(),
        )

        assertFailsWith<NoSuchElementException> {
            IntegrationUser()
                .select { it.id }
                .where { it.id == -1 }
                .queryOne<Int>()
        }
    }

    @Test
    fun queryListPreservesNullScalarRows() {
        recreateTables()
        seedNullableUser()

        val scores = IntegrationUser()
            .select { it.score }
            .orderBy { it.id.asc() }
            .queryList<Int?>()

        assertEquals(listOf(10, 20, 30, 5, null), scores)
    }

    @Test
    fun aggregateProjectionMapsZeroCountAndNullSumOnEmptyInput() {
        recreateTables()
        profile.seedUsersAndOrders()

        val projection = IntegrationUser()
            .select {
                [
                    f.count(1).alias("total"),
                    f.sum(it.score).alias("scoreSum")
                ]
            }
            .where { it.id == -1 }
            .queryOne<IntegrationAggregateProjection>()

        assertEquals(0, projection.total)
        assertNull(projection.scoreSum)
    }

    @Test
    fun generatedProjectionPropertiesCanBeReadDirectlyAfterScalarMapping() {
        recreateTables()
        profile.seedUsersAndOrders()

        val projection = IntegrationUser()
            .select {
                [
                    f.count(1).alias("total"),
                    f.sum(it.score).alias("scoreSum")
                ]
            }
            .queryOne<IntegrationAggregateProjection>()

        assertNotNull(projection.total)
        assertNotNull(projection.scoreSum)
        assertEquals(4, projection.total)
        assertEquals(65L, projection.scoreSum)
    }

    private fun seedNullableUser() {
        profile.seedUsersAndOrders()
        IntegrationUser(id = 5, name = null, score = null, status = 0).insert().execute()
    }

    private fun Map<String, Any>.cell(label: String): Any? =
        this[label] ?: this[label.uppercase()] ?: this[label.lowercase()]

    private fun Map<String, Any>.hasColumn(label: String): Boolean =
        keys.any { it.equals(label, ignoreCase = true) }
}
