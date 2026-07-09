package com.kotlinorm.integration.suites

import com.kotlinorm.functions.bundled.exts.PolymerizationFunctions.count
import com.kotlinorm.functions.bundled.exts.PolymerizationFunctions.sum
import com.kotlinorm.functions.bundled.exts.WindowFunctions.rowNumber
import com.kotlinorm.integration.fixtures.IntegrationDslEdgeProjection
import com.kotlinorm.integration.fixtures.IntegrationOrder
import com.kotlinorm.integration.fixtures.IntegrationUser
import com.kotlinorm.integration.fixtures.PAID_STATUS
import com.kotlinorm.integration.profiles.IntegrationScenarioProfile
import com.kotlinorm.integration.support.IntegrationDatabaseEnvironment
import com.kotlinorm.integration.support.IntegrationSuiteSupport
import com.kotlinorm.orm.join.join
import com.kotlinorm.orm.select.select
import com.kotlinorm.orm.union.union
import com.kotlinorm.syntax.order.SqlOrdering
import kotlin.test.Test
import kotlin.test.assertEquals

abstract class DslEdgeCaseIntegrationSuite(
    environment: IntegrationDatabaseEnvironment,
    profile: IntegrationScenarioProfile,
) : IntegrationSuiteSupport(environment, profile) {
    @Test
    fun joinProjectionCanBePagedWithTotalAgainstRealDatabase() {
        recreateTables()
        profile.seedUsersAndOrders()

        val (total, rows) = IntegrationUser()
            .join(IntegrationOrder()) { user, order ->
                innerJoin(order) { user.id == order.userId }
                select {
                    [
                        user.id.alias("userId"),
                        user.name,
                        order.amount,
                    ]
                }
                where { order.status == PAID_STATUS }
                orderBy { user.id.asc() }
                page(pi = 1, ps = 1)
            }
            .withTotal()
            .queryList<IntegrationDslEdgeProjection>()

        assertEquals(2, total)
        assertEquals(listOf(1), rows.map { it.userId })
        assertEquals(listOf("Ada"), rows.map { it.name })
        assertEquals(listOf(50), rows.map { it.amount })
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
            .page(pi = 1, ps = 1)
            .withTotal()
            .queryList<IntegrationDslEdgeProjection>()

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
            .queryList<IntegrationDslEdgeProjection>()

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
            .page(pi = 1, ps = 1)
            .withTotal()
            .queryList<IntegrationDslEdgeProjection>()

        assertEquals(2, total)
        assertEquals(listOf(1), rows.map { it.userId })
        assertEquals(listOf(70), rows.map { it.totalAmount })
        assertEquals(listOf(2), rows.map { it.orderCount })
    }

    @Test
    fun joinGroupedAggregateCanBePagedWithTotalAgainstRealDatabase() {
        recreateTables()
        profile.seedUsersAndOrders()

        val (total, rows) = IntegrationUser()
            .join(IntegrationOrder()) { user, order ->
                innerJoin(order) { user.id == order.userId }
                select {
                    [
                        user.id.alias("userId"),
                        user.name,
                        f.sum(order.amount).alias("totalAmount"),
                        f.count(order.id).alias("orderCount"),
                    ]
                }
                groupBy { [user.id, user.name] }
                having { f.sum(order.amount) > 30 }
                orderBy { user.id.asc() }
                page(pi = 1, ps = 1)
            }
            .withTotal()
            .queryList<IntegrationDslEdgeProjection>()

        assertEquals(2, total)
        assertEquals(listOf(1), rows.map { it.userId })
        assertEquals(listOf("Ada"), rows.map { it.name })
        assertEquals(listOf(70), rows.map { it.totalAmount })
        assertEquals(listOf(2), rows.map { it.orderCount })
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
            .page(pi = 1, ps = 10)
            .withTotal()
            .queryList<IntegrationDslEdgeProjection>()

        assertEquals(2, total)
        assertEquals(listOf(1, 3), rows.map { it.id })
        assertEquals(listOf(1, 2), rows.map { it.userId })
        assertEquals(listOf(50, 40), rows.map { it.amount })
        assertEquals(listOf(1, 1), rows.map { it.rn })
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
            .queryList<IntegrationDslEdgeProjection>()

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
            .page(pi = 1, ps = 1)
            .withTotal()
            .queryList<IntegrationDslEdgeProjection>()

        assertEquals(2, total)
        assertEquals(listOf(2), rows.map { it.id })
        assertEquals(listOf("Grace"), rows.map { it.name })
        assertEquals(listOf(PAID_STATUS), rows.map { it.status })
    }
}
