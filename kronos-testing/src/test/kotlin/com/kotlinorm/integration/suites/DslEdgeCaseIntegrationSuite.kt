package com.kotlinorm.integration.suites

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
            .toList<IntegrationDslEdgeProjection>()

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
            .withTotal()
            .page(pi = 1, ps = 1)
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
            .withTotal()
            .page(pi = 1, ps = 1)
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
            .toList<IntegrationDslEdgeProjection>()

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
            .withTotal()
            .page(pi = 1, ps = 10)
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
            .withTotal()
            .page(pi = 1, ps = 10)
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
            .withTotal()
            .page(pi = 1, ps = 10)
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
            .withTotal()
            .page(pi = 1, ps = 10)
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
            .withTotal()
            .page(pi = 1, ps = 1)
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
}
