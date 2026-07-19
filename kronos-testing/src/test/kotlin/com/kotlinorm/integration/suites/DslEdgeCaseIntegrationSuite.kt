package com.kotlinorm.integration.suites

import com.kotlinorm.annotations.UnsafeProjectionOverride
import com.kotlinorm.functions.bundled.exts.PolymerizationFunctions.count
import com.kotlinorm.functions.bundled.exts.PolymerizationFunctions.sum
import com.kotlinorm.functions.bundled.exts.WindowFunctions.rowNumber
import com.kotlinorm.integration.fixtures.IntegrationAliasMatrixUser
import com.kotlinorm.integration.fixtures.IntegrationDslEdgeProjection
import com.kotlinorm.integration.fixtures.IntegrationOrder
import com.kotlinorm.integration.fixtures.IntegrationUser
import com.kotlinorm.integration.fixtures.PAID_STATUS
import com.kotlinorm.integration.profiles.IntegrationScenarioProfile
import com.kotlinorm.integration.support.IntegrationDatabaseEnvironment
import com.kotlinorm.integration.support.IntegrationSuiteSupport
import com.kotlinorm.orm.ddl.table
import com.kotlinorm.orm.insert.insert
import com.kotlinorm.orm.join.join
import com.kotlinorm.orm.select.select
import com.kotlinorm.orm.union.union
import com.kotlinorm.syntax.order.SqlOrdering
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

abstract class DslEdgeCaseIntegrationSuite(
    environment: IntegrationDatabaseEnvironment,
    profile: IntegrationScenarioProfile,
) : IntegrationSuiteSupport(environment, profile) {
    @Test
    fun joinProjectionCanBePagedWithTotalAgainstRealDatabase() {
        recreateTables()
        profile.seedUsersAndOrders()

        val page = IntegrationUser()
            .join(IntegrationOrder()) { user, order ->
                innerJoin { user.id == order.userId }
                    .select {
                        [
                            user.id.alias("userId"),
                            user.name,
                            order.amount,
                        ]
                    }
                    .where { order.status == PAID_STATUS }
                    .orderBy { user.id.asc() }
            }
            .page(pageIndex = 1, pageSize = 1)
            .withTotal()
            .toList<IntegrationDslEdgeProjection>()

        assertEquals(2, page.total)
        assertEquals(2, page.totalPages)
        assertEquals(listOf(1), page.records.map { it.userId })
        assertEquals(listOf("Ada"), page.records.map { it.name })
        assertEquals(listOf(50), page.records.map { it.amount })
    }

    @Test
    fun subquerySourceCanBeFilteredPagedAndMappedAgainstRealDatabase() {
        recreateTables()
        profile.seedUsersAndOrders()

        val paidUsers = IntegrationUser()
            .select { [it.id, it.name, it.status] }
            .where {
                it.id in IntegrationOrder()
                    .select { order -> order.userId }
                    .where { order -> order.status == PAID_STATUS }
            }

        val (total, rows) = paidUsers
            .select { [it.id, it.name] }
            .where { it.status == PAID_STATUS }
            .orderBy { it.id.desc() }
            .page(pageIndex = 1, pageSize = 1)
            .withTotal()
            .toList<IntegrationDslEdgeProjection>()

        assertEquals(2, total)
        assertEquals(listOf(2), rows.map { it.id })
        assertEquals(listOf("Grace"), rows.map { it.name })
    }

    @Test
    fun groupByHavingAggregateProjectionMapsTypedValuesAgainstRealDatabase() {
        recreateTables()
        profile.seedUsersAndOrders()

        val rows = IntegrationOrder()
            .select {
                [
                    it.userId,
                    f.sum(it.amount).alias("totalAmount"),
                    f.count(1).alias("orderCount"),
                ]
            }
            .groupBy { it.userId }
            .having { f.sum(it.amount) > 30 }
            .orderBy { it.userId.asc() }
            .toList<IntegrationDslEdgeProjection>()

        assertEquals(listOf(1, 2), rows.map { it.userId })
        assertEquals(listOf(70, 40), rows.map { it.totalAmount })
        assertEquals(listOf(2, 1), rows.map { it.orderCount })
    }

    @Test
    fun groupByHavingAggregateProjectionCanBePagedWithTotalAgainstRealDatabase() {
        recreateTables()
        profile.seedUsersAndOrders()

        val (total, rows) = IntegrationOrder()
            .select {
                [
                    it.userId,
                    f.sum(it.amount).alias("totalAmount"),
                    f.count(1).alias("orderCount"),
                ]
            }
            .groupBy { it.userId }
            .having { f.sum(it.amount) > 30 }
            .orderBy { it.userId.asc() }
            .page(pageIndex = 1, pageSize = 1)
            .withTotal()
            .toList<IntegrationDslEdgeProjection>()

        assertEquals(2, total)
        assertEquals(listOf(1), rows.map { it.userId })
        assertEquals(listOf(70), rows.map { it.totalAmount })
        assertEquals(listOf(2), rows.map { it.orderCount })
    }

    @Test
    fun joinGroupedAggregateCanBePagedWithTotalAgainstRealDatabase() {
        recreateTables()
        profile.seedUsersAndOrders()

        val page = IntegrationUser()
            .join(IntegrationOrder()) { user, order ->
                innerJoin { user.id == order.userId }
                    .select {
                        [
                            user.id.alias("userId"),
                            user.name,
                            f.sum(order.amount).alias("totalAmount"),
                            f.count(order.id).alias("orderCount"),
                        ]
                    }
                    .groupBy { [user.id, user.name] }
                    .having { f.sum(order.amount) > 30 }
                    .orderBy { user.id.asc() }
            }
            .page(pageIndex = 1, pageSize = 1)
            .withTotal()
            .toList<IntegrationDslEdgeProjection>()

        assertEquals(2, page.total)
        assertEquals(2, page.totalPages)
        assertEquals(listOf(1), page.records.map { it.userId })
        assertEquals(listOf("Ada"), page.records.map { it.name })
        assertEquals(listOf(70), page.records.map { it.totalAmount })
        assertEquals(listOf(2), page.records.map { it.orderCount })
    }

    @OptIn(UnsafeProjectionOverride::class)
    @Test
    fun joinDuplicateProjectionNamesStayAlignedForTypedAndMapRowsAgainstRealDatabase() {
        recreateTables()
        profile.seedUsersAndOrders()

        val query = IntegrationUser()
            .join(IntegrationOrder()) { user, order ->
                innerJoin { user.id == order.userId }
                    .select { [user.id, order.id] }
                    .where { order.status == PAID_STATUS }
                    .orderBy { user.id.asc() }
            }

        val typedRows = query.toList()
        assertEquals(listOf(1, 2), typedRows.map { it.id })
        assertEquals(listOf(1, 3), typedRows.map { it.id_1 })

        val mapRows = query.toMapList()
        assertEquals(listOf(1, 2), mapRows.map { (it.cell("id") as Number).toInt() })
        assertEquals(listOf(1, 3), mapRows.map { (it.cell("id_1") as Number).toInt() })
        assertTrue(mapRows.all { row -> row.hasColumn("id") && row.hasColumn("id_1") })
    }

    @Test
    fun joinCursorUsesCompositeOrderAndContinuesFromAfterTokenAgainstRealDatabase() {
        recreateTables()
        profile.seedUsersAndOrders()

        val query = IntegrationUser()
            .join(IntegrationOrder()) { user, order ->
                innerJoin { user.id == order.userId }
                    .select { [order.id, user.id.alias("userId"), order.amount] }
                    .orderBy { [user.id.asc(), order.amount.asc()] }
            }

        val firstPage = query
            .cursor(pageSize = 2)
            .toList<IntegrationDslEdgeProjection>()

        assertTrue(firstPage.hasNext)
        assertNotNull(firstPage.nextCursor)
        assertEquals(listOf(2, 1), firstPage.records.map { it.id })
        assertEquals(listOf(1, 1), firstPage.records.map { it.userId })
        assertEquals(listOf(20, 50), firstPage.records.map { it.amount })

        val secondPage = query
            .cursor(pageSize = 2, after = firstPage.nextCursor)
            .toList<IntegrationDslEdgeProjection>()

        assertFalse(secondPage.hasNext)
        assertNull(secondPage.nextCursor)
        assertEquals(listOf(3), secondPage.records.map { it.id })
        assertEquals(listOf(2), secondPage.records.map { it.userId })
        assertEquals(listOf(40), secondPage.records.map { it.amount })
    }

    @Test
    fun joinedSelectedQueryCanBeUsedAsDerivedSourceAndUnionOperandAgainstRealDatabase() {
        recreateTables()
        profile.seedUsersAndOrders()

        val paidOrders = IntegrationUser()
            .join(IntegrationOrder()) { user, order ->
                innerJoin { user.id == order.userId }
                    .select {
                        [
                            user.id.alias("userId"),
                            user.name.alias("userName"),
                            order.amount,
                        ]
                    }
                    .where { order.status == PAID_STATUS }
            }
        val openOrders = IntegrationUser()
            .join(IntegrationOrder()) { user, order ->
                innerJoin { user.id == order.userId }
                    .select {
                        [
                            user.id.alias("userId"),
                            user.name.alias("userName"),
                            order.amount,
                        ]
                    }
                    .where { order.status != PAID_STATUS }
            }

        val derivedRows = paidOrders
            .select { [it.userId, it.userName, it.amount] }
            .where { it.amount >= 40 }
            .orderBy { it.userId.asc() }
            .toList<IntegrationDslEdgeProjection>()

        assertEquals(listOf(1, 2), derivedRows.map { it.userId })
        assertEquals(listOf("Ada", "Grace"), derivedRows.map { it.userName })
        assertEquals(listOf(50, 40), derivedRows.map { it.amount })

        val unionRows = union(paidOrders, openOrders)
            .orderBy("amount" to SqlOrdering.Asc)
            .toList<IntegrationDslEdgeProjection>()

        assertEquals(listOf(1, 2, 1), unionRows.map { it.userId })
        assertEquals(listOf("Ada", "Grace", "Ada"), unionRows.map { it.userName })
        assertEquals(listOf(20, 40, 50), unionRows.map { it.amount })
    }

    @Test
    fun windowProjectionCanBePagedWithTotalAgainstRealDatabase() {
        recreateTables()
        profile.seedUsersAndOrders()

        val rankedOrders = IntegrationOrder()
            .select {
                [
                    it.id,
                    it.userId,
                    it.amount,
                    f.rowNumber()
                        .over {
                            partitionBy(it.userId)
                            orderBy(it.amount.desc())
                        }
                        .alias("rn"),
                ]
            }

        val (total, rows) = rankedOrders
            .select { [it.id, it.userId, it.amount, it.rn] }
            .where { it.rn == 1 }
            .orderBy { it.userId.asc() }
            .page(pageIndex = 1, pageSize = 10)
            .withTotal()
            .toList<IntegrationDslEdgeProjection>()

        assertEquals(2, total)
        assertEquals(listOf(1, 3), rows.map { it.id })
        assertEquals(listOf(1, 2), rows.map { it.userId })
        assertEquals(listOf(50, 40), rows.map { it.amount })
        assertEquals(listOf(1, 1), rows.map { it.rn })
    }

    @Test
    fun derivedSourceLogicalFieldReferencesCanBeFilteredOrderedAndPagedAgainstRealDatabase() {
        recreateAliasMatrixUsers()

        val source = IntegrationAliasMatrixUser()
            .select { [it.id, it.userName, it.age, it.createTime] }

        val (total, rows) = source
            .select { [it.id, it.userName, it.age, it.createTime] }
            .where { it.userName == "Ada" }
            .orderBy { it.createTime.desc() }
            .page(pageIndex = 1, pageSize = 10)
            .withTotal()
            .toList<IntegrationDslEdgeProjection>()

        assertEquals(2, total)
        assertEquals(listOf(2, 1), rows.map { it.id })
        assertEquals(listOf("Ada", "Ada"), rows.map { it.userName })
        assertEquals(listOf(30, 30), rows.map { it.age })
        assertEquals(listOf(20, 10), rows.map { it.createTime })
    }

    @Test
    fun derivedWindowSourceLogicalFieldReferencesCanBeFilteredOrderedAndPagedAgainstRealDatabase() {
        recreateAliasMatrixUsers()

        val ranked = IntegrationAliasMatrixUser()
            .select {
                [
                    it.id,
                    it.userName,
                    it.age,
                    it.createTime,
                    it.updateTime,
                    f.rowNumber()
                        .over {
                            partitionBy(it.age)
                            orderBy(it.createTime.desc())
                        }
                        .alias("rn"),
                ]
            }

        val (total, rows) = ranked
            .select { [it.id, it.userName, it.age, it.createTime, it.updateTime, it.rn] }
            .where { it.rn == 1 }
            .orderBy { it.userName.asc() }
            .page(pageIndex = 1, pageSize = 10)
            .withTotal()
            .toList<IntegrationDslEdgeProjection>()

        assertEquals(2, total)
        assertEquals(listOf(2, 3), rows.map { it.id })
        assertEquals(listOf("Ada", "Grace"), rows.map { it.userName })
        assertEquals(listOf(20, 30), rows.map { it.createTime })
        assertEquals(listOf(200, 300), rows.map { it.updateTime })
        assertEquals(listOf(1, 1), rows.map { it.rn })
    }

    @Test
    fun derivedWindowSourceFromKPojoExpansionCanReferenceLogicalFieldNamesAgainstRealDatabase() {
        recreateAliasMatrixUsers()

        val ranked = IntegrationAliasMatrixUser()
            .select {
                [
                    it,
                    f.rowNumber()
                        .over {
                            partitionBy(it.age)
                            orderBy(it.createTime.desc())
                        }
                        .alias("rn"),
                ]
            }

        val rows = ranked
            .select { [it.id, it.userName, it.age, it.createTime, it.updateTime, it.rn] }
            .where { it.rn == 1 }
            .orderBy { it.userName.asc() }
            .toList<IntegrationDslEdgeProjection>()

        assertEquals(listOf(2, 3), rows.map { it.id })
        assertEquals(listOf("Ada", "Grace"), rows.map { it.userName })
        assertEquals(listOf(20, 30), rows.map { it.createTime })
        assertEquals(listOf(200, 300), rows.map { it.updateTime })
        assertEquals(listOf(1, 1), rows.map { it.rn })
    }

    @Test
    fun derivedSourceFromKPojoExclusionCanReferenceLogicalFieldNamesAgainstRealDatabase() {
        recreateAliasMatrixUsers()

        val source = IntegrationAliasMatrixUser()
            .select { it - listOf(it.updateTime) }

        val (total, rows) = source
            .select { [it.id, it.userName, it.age, it.createTime] }
            .where { it.userName == "Ada" }
            .orderBy { it.createTime.desc() }
            .page(pageIndex = 1, pageSize = 10)
            .withTotal()
            .toList<IntegrationDslEdgeProjection>()

        assertEquals(2, total)
        assertEquals(listOf(2, 1), rows.map { it.id })
        assertEquals(listOf("Ada", "Ada"), rows.map { it.userName })
        assertEquals(listOf(20, 10), rows.map { it.createTime })
    }

    @Test
    fun derivedAggregateSourceLogicalFieldReferencesCanBeGroupedHavingAndOrderedAgainstRealDatabase() {
        recreateAliasMatrixUsers()

        val source = IntegrationAliasMatrixUser()
            .select { [it.id, it.userName, it.age] }

        val rows = source
            .select {
                [
                    it.userName,
                    it.age,
                    f.count(it.id).alias("orderCount"),
                ]
            }
            .groupBy { [it.userName, it.age] }
            .having { f.count(it.id) > 0 }
            .orderBy { [it.userName.asc(), it.age.asc()] }
            .toList<IntegrationDslEdgeProjection>()

        assertEquals(listOf("Ada", "Grace"), rows.map { it.userName })
        assertEquals(listOf(30, 20), rows.map { it.age })
        assertEquals(listOf(2, 1), rows.map { it.orderCount })
    }

    @Test
    fun unionCanBeOrderedLimitedAndMappedAgainstRealDatabase() {
        recreateTables()
        profile.seedUsersAndOrders()

        val rows = union(
            IntegrationUser()
                .select { [it.id, it.name, it.status] }
                .where { it.status == PAID_STATUS },
            IntegrationUser()
                .select { [it.id, it.name, it.status] }
                .where { it.status == 2 },
        )
            .orderBy("id" to SqlOrdering.Desc)
            .limit(2)
            .toList<IntegrationDslEdgeProjection>()

        assertEquals(listOf(3, 2), rows.map { it.id })
        assertEquals(listOf("Linus", "Grace"), rows.map { it.name })
        assertEquals(listOf(2, PAID_STATUS), rows.map { it.status })
    }

    @Test
    fun unionSourceCanBeFilteredPagedAndMappedAgainstRealDatabase() {
        recreateTables()
        profile.seedUsersAndOrders()

        val activeUsers = union(
            IntegrationUser()
                .select { [it.id, it.name, it.status] }
                .where { it.status == PAID_STATUS },
            IntegrationUser()
                .select { [it.id, it.name, it.status] }
                .where { it.status == 2 },
        )

        val (total, rows) = activeUsers
            .select { [it.id, it.name, it.status] }
            .where { it.id > 1 }
            .orderBy { it.id.asc() }
            .page(pageIndex = 1, pageSize = 1)
            .withTotal()
            .toList<IntegrationDslEdgeProjection>()

        assertEquals(2, total)
        assertEquals(listOf(2), rows.map { it.id })
        assertEquals(listOf("Grace"), rows.map { it.name })
        assertEquals(listOf(PAID_STATUS), rows.map { it.status })
    }

    private fun recreateAliasMatrixUsers() {
        requireDatabaseAvailable()
        configureKronos()
        val table = IntegrationAliasMatrixUser()
        wrapper.table.dropTable(table)
        wrapper.table.createTable(table)
        listOf(
            IntegrationAliasMatrixUser(id = 1, userName = "Ada", age = 30, createTime = 10, updateTime = 100),
            IntegrationAliasMatrixUser(id = 2, userName = "Ada", age = 30, createTime = 20, updateTime = 200),
            IntegrationAliasMatrixUser(id = 3, userName = "Grace", age = 20, createTime = 30, updateTime = 300),
        ).forEach { it.insert().execute() }
    }

    private fun Map<String, Any?>.cell(label: String): Any? =
        entries.firstOrNull { (key, _) -> key.equals(label, ignoreCase = true) }?.value

    private fun Map<String, Any?>.hasColumn(label: String): Boolean =
        keys.any { it.equals(label, ignoreCase = true) }
}
