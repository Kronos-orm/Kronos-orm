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
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

abstract class ResultMethodEdgeIntegrationSuite(
    environment: IntegrationDatabaseEnvironment,
    profile: IntegrationScenarioProfile,
) : IntegrationSuiteSupport(environment, profile) {
    @Test
    fun pagedQueryListExposesNamedResultWithoutExplicitKPojoFlag() {
        recreateTables()
        profile.seedUsersAndOrders()

        val page = IntegrationUser()
            .select { [it.id, it.name] }
            .orderBy { it.id.asc() }
            .page(pageIndex = 2, pageSize = 2)
            .withTotal()
            .toList<IntegrationPageProjection>()

        assertEquals(4, page.total)
        assertEquals(2, page.totalPages)
        assertEquals(2, page.pageIndex)
        assertEquals(2, page.pageSize)
        assertEquals(listOf(3, 4), page.records.map { it.id })
        assertEquals(listOf("Linus", "NoOrder"), page.records.map { it.name })
    }

    @Test
    fun pagedQueryListMapsNullableSelectedPropertiesIntoProjectionRows() {
        recreateTables()
        seedNullableUser()

        val (total, pageUsers) = IntegrationUser()
            .select { [it.id, it.name, it.score] }
            .orderBy { it.id.asc() }
            .page(pageIndex = 3, pageSize = 2)
            .withTotal()
            .toList<IntegrationPageProjection>()

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
            .toList<IntegrationAliasProjection>()

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
            .page(pageIndex = 1, pageSize = 10)
            .withTotal()
            .toMapList()

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
            .toMapList()
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
                .firstOrNull<Int>(),
        )

        assertFailsWith<NoSuchElementException> {
            IntegrationUser()
                .select { it.id }
                .where { it.id == -1 }
                .first<Int>()
        }
    }

    @Test
    fun queryListPreservesNullScalarRows() {
        recreateTables()
        seedNullableUser()

        val scores = IntegrationUser()
            .select { it.score }
            .orderBy { it.id.asc() }
            .toList<Int?>()

        assertEquals(listOf(10, 20, 30, 5, null), scores)
    }

    @Test
    fun cursorPaginationFetchesNextPageAgainstRealDatabase() {
        recreateTables()
        profile.seedUsersAndOrders()

        val firstPage = IntegrationUser()
            .select { [it.id, it.name] }
            .orderBy { it.id.asc() }
            .cursor(pageSize = 2)
            .toList<IntegrationPageProjection>()

        assertTrue(firstPage.hasNext)
        val cursor = assertNotNull(firstPage.nextCursor)
        assertEquals(listOf(1, 2), firstPage.records.map { it.id })
        assertEquals(listOf("Ada", "Grace"), firstPage.records.map { it.name })

        val secondPage = IntegrationUser()
            .select { [it.id, it.name] }
            .orderBy { it.id.asc() }
            .cursor(pageSize = 2, after = cursor)
            .toList<IntegrationPageProjection>()

        assertFalse(secondPage.hasNext)
        assertNull(secondPage.nextCursor)
        assertEquals(listOf(3, 4), secondPage.records.map { it.id })
        assertEquals(listOf("Linus", "NoOrder"), secondPage.records.map { it.name })
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
            .first<IntegrationAggregateProjection>()

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
            .first<IntegrationAggregateProjection>()

        assertNotNull(projection.total)
        assertNotNull(projection.scoreSum)
        assertEquals(4, projection.total)
        assertEquals(65L, projection.scoreSum)
    }

    private fun seedNullableUser() {
        profile.seedUsersAndOrders()
        IntegrationUser(id = 5, name = null, score = null, status = 0).insert().execute()
    }

    private fun Map<String, Any?>.cell(label: String): Any? =
        this[label] ?: this[label.uppercase()] ?: this[label.lowercase()]

    private fun Map<String, Any?>.hasColumn(label: String): Boolean =
        keys.any { it.equals(label, ignoreCase = true) }
}
