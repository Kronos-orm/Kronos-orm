package com.kotlinorm.integration.suites

import com.kotlinorm.integration.fixtures.COMPLEX_CANCELLED_STATUS
import com.kotlinorm.integration.fixtures.COMPLEX_OPEN_STATUS
import com.kotlinorm.integration.fixtures.COMPLEX_PAID_STATUS
import com.kotlinorm.integration.fixtures.ComplexCustomer
import com.kotlinorm.integration.fixtures.ComplexCustomerCard
import com.kotlinorm.integration.fixtures.ComplexCustomerRow
import com.kotlinorm.integration.fixtures.ComplexCustomerSearchRow
import com.kotlinorm.integration.fixtures.ComplexInvoice
import com.kotlinorm.integration.fixtures.ComplexJoinSubqueryRow
import com.kotlinorm.integration.profiles.IntegrationScenarioProfile
import com.kotlinorm.integration.support.IntegrationDatabaseEnvironment
import com.kotlinorm.integration.support.IntegrationSuiteSupport
import com.kotlinorm.orm.ddl.table
import com.kotlinorm.orm.insert.insert
import com.kotlinorm.orm.join.join
import com.kotlinorm.orm.pagination.Cursor
import com.kotlinorm.orm.select.select
import kotlin.test.Test
import kotlin.test.assertEquals

abstract class ComplexQueryProjectionIntegrationSuite(
    environment: IntegrationDatabaseEnvironment,
    profile: IntegrationScenarioProfile,
) : IntegrationSuiteSupport(environment, profile) {
    @Test
    fun nestedWhereNoValueNullRangeAndMembershipPredicatesExecuteAgainstRealDatabase() {
        recreateComplexTables()

        val optionalTier: String? = null
        val rows = ComplexCustomer()
            .select { [it.id, it.name, it.tier, it.age, it.score, it.region] }
            .where {
                ((((it.score between 70..100) && (it.tier in listOf("gold", "silver"))) || it.name.isNull) &&
                    (it.id !in listOf(5, 99)))
            }
            .where { (it.tier == optionalTier).takeIf(optionalTier != null) }
            .where { (it.region != "APAC") || it.name.isNull }
            .where { it.age notBetween 18..25 }
            .orderBy { it.id.asc() }
            .toList<ComplexCustomerRow>()

        assertEquals(listOf(1, 2, 4), rows.map { it.id })
        assertEquals(listOf("Ada", "Grace", null), rows.map { it.name })
        assertEquals(listOf("gold", "silver", "gold"), rows.map { it.tier })
        assertEquals(listOf(31, 42, 35), rows.map { it.age })
        assertEquals(listOf(91, 76, 40), rows.map { it.score })
        assertEquals(listOf("NA", "EU", "APAC"), rows.map { it.region })

        val targetRegion: String? = null
        val nullRegionIds = ComplexCustomer()
            .select { it.id }
            .where {
                if (targetRegion != null) {
                    it.region == targetRegion
                } else {
                    it.region.isNull
                }
            }
            .orderBy { it.id.asc() }
            .toList<Int>()

        assertEquals(listOf(3), nullRegionIds)
    }

    @Test
    fun falseNoValueStrategyShouldRenderDialectSafePredicateAgainstRealDatabase() {
        recreateComplexTables()

        val optionalTier: String? = null
        val falseStrategyIds = ComplexCustomer()
            .select { it.id }
            .where {
                if (optionalTier != null) {
                    it.tier == optionalTier
                } else {
                    false.asSql()
                }
            }
            .orderBy { it.id.asc() }
            .toList<Int>()

        assertEquals(emptyList(), falseStrategyIds)
    }

    @Test
    fun emptyInAndNotInCollectionsRenderDialectSafePredicatesAgainstRealDatabase() {
        recreateComplexTables()

        val emptyIds = emptyList<Int>()
        val inRows = ComplexCustomer()
            .select { it.id }
            .where { it.id in emptyIds }
            .orderBy { it.id.asc() }
            .toList<Int>()

        val notInRows = ComplexCustomer()
            .select { it.id }
            .where { it.id !in emptyIds }
            .orderBy { it.id.asc() }
            .toList<Int>()

        assertEquals(emptyList(), inRows)
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7), notInRows)
    }

    @Test
    fun stringHelpersShouldEscapeLikeWildcardsExceptExplicitLikeAgainstRealDatabase() {
        recreateComplexTables()

        listOf(
            ComplexCustomer(id = 20, name = "literal%_mid", tier = "wild", archived = 0),
            ComplexCustomer(id = 21, name = "literalXYmid", tier = "wild", archived = 0),
            ComplexCustomer(id = 22, name = "prefix%_", tier = "wild", archived = 0),
            ComplexCustomer(id = 23, name = "%_suffix", tier = "wild", archived = 0),
        ).forEach { assertEquals(1, it.insert().execute().affectedRows) }

        val explicitLikeIds = ComplexCustomer()
            .select { it.id }
            .where { it.name like "literal%_mid" }
            .orderBy { it.id.asc() }
            .toList<Int>()
        assertEquals(listOf(20, 21), explicitLikeIds)

        val containsLiteralWildcardIds = ComplexCustomer()
            .select { it.id }
            .where { it.name.contains("%_") }
            .orderBy { it.id.asc() }
            .toList<Int>()
        assertEquals(listOf(20, 22, 23), containsLiteralWildcardIds)

        val startsWithLiteralWildcardIds = ComplexCustomer()
            .select { it.id }
            .where { it.name.startsWith("literal%_") }
            .orderBy { it.id.asc() }
            .toList<Int>()
        assertEquals(listOf(20), startsWithLiteralWildcardIds)

        val endsWithLiteralWildcardIds = ComplexCustomer()
            .select { it.id }
            .where { it.name.endsWith("%_mid") }
            .orderBy { it.id.asc() }
            .toList<Int>()
        assertEquals(listOf(20), endsWithLiteralWildcardIds)
    }

    @Test
    fun limitZeroReturnsEmptyRowsAgainstRealDatabase() {
        recreateComplexTables()

        val rows = ComplexCustomer()
            .select { it.id }
            .orderBy { it.id.asc() }
            .limit(0)
            .toList<Int>()

        assertEquals(emptyList(), rows)
    }

    @Test
    fun cursorPaginationWithDuplicateSortKeyShouldNotDropTiedRowsAgainstRealDatabase() {
        recreateComplexTables()

        listOf(
            ComplexCustomer(id = 8, name = "Tie-A", tier = "silver", age = 20, score = 55, region = "EU", archived = 0),
            ComplexCustomer(id = 9, name = "Tie-B", tier = "silver", age = 21, score = 55, region = "EU", archived = 0),
        ).forEach { assertEquals(1, it.insert().execute().affectedRows) }

        val expectedIds = ComplexCustomer()
            .select { [it.id, it.score] }
            .where { it.archived == 0 }
            .orderBy { it.score.asc() }
            .toList<ComplexCustomerRow>()
            .map { it.id }
            .toSet()

        val seenIds = mutableSetOf<Int?>()
        var cursor: Cursor? = null
        var hasNext: Boolean
        do {
            val page = ComplexCustomer()
                .select { [it.id, it.score] }
                .where { it.archived == 0 }
                .orderBy { it.score.asc() }
                .cursor(pageSize = 2, after = cursor)
                .toList<ComplexCustomerRow>()

            hasNext = page.hasNext
            cursor = page.nextCursor
            seenIds += page.records.map { it.id }
        } while (hasNext)

        assertEquals(expectedIds, seenIds)
    }

    @Test
    fun cursorPaginationWithHiddenTieBreakerShouldStripInternalProjectionAgainstRealDatabase() {
        recreateComplexTables()

        listOf(
            ComplexCustomer(id = 8, name = "Tie-A", tier = "silver", age = 20, score = 55, region = "EU", archived = 0),
            ComplexCustomer(id = 9, name = "Tie-B", tier = "silver", age = 21, score = 55, region = "EU", archived = 0),
        ).forEach { assertEquals(1, it.insert().execute().affectedRows) }

        val expectedCount = ComplexCustomer()
            .select { it.id }
            .where { it.archived == 0 }
            .toList<Int>()
            .size

        val rows = mutableListOf<Map<String, Any?>>()
        var cursor: Cursor? = null
        var hasNext: Boolean
        do {
            val page = ComplexCustomer()
                .select { it.score }
                .where { it.archived == 0 }
                .orderBy { it.score.asc() }
                .cursor(pageSize = 2, after = cursor)
                .toMapList()

            hasNext = page.hasNext
            cursor = page.nextCursor
            rows += page.records
        } while (hasNext)

        assertEquals(expectedCount, rows.size)
        assertEquals(true, rows.all { row -> row.keys.none { it.startsWith("__kronos_cursor_") } })
    }

    @Test
    fun pageWithoutOrderByShouldHandleDialectRequirementsAgainstRealDatabase() {
        recreateComplexTables()

        val page = ComplexCustomer()
            .select { [it.id, it.name] }
            .page(pageIndex = 1, pageSize = 2)
            .withTotal()
            .toList<ComplexCustomerRow>()

        assertEquals(7, page.total)
        assertEquals(4, page.totalPages)
        assertEquals(2, page.records.size)
    }

    @Test
    fun projectionExclusionAliasesDtoAndScalarSubqueryMapExpectedValuesAgainstRealDatabase() {
        recreateComplexTables()

        val rows = ComplexCustomer()
            .select(ComplexCustomerCard::class) {
                [
                    it.id.alias("customerId"),
                    it.name.alias("displayName"),
                    it - listOf(it.id, it.name, it.archived),
                    ComplexInvoice()
                        .select { invoice -> invoice.amount }
                        .where { invoice -> invoice.customerId == it.id && invoice.status == COMPLEX_PAID_STATUS }
                        .limit(1)
                        .alias("lastPaidAmount"),
                ]
            }
            .where { it.id in listOf(1, 2, 4) }
            .orderBy { it.id.asc() }
            .toList<ComplexCustomerCard>()

        assertEquals(
            listOf(
                ComplexCustomerCard(
                    customerId = 1,
                    displayName = "Ada",
                    tier = "gold",
                    age = 31,
                    score = 91,
                    region = "NA",
                    referrer = null,
                    lastPaidAmount = 120,
                ),
                ComplexCustomerCard(
                    customerId = 2,
                    displayName = "Grace",
                    tier = "silver",
                    age = 42,
                    score = 76,
                    region = "EU",
                    referrer = "Ada",
                    lastPaidAmount = 90,
                ),
                ComplexCustomerCard(
                    customerId = 4,
                    displayName = null,
                    tier = "gold",
                    age = 35,
                    score = 40,
                    region = "APAC",
                    referrer = "Grace",
                    lastPaidAmount = 70,
                ),
            ),
            rows,
        )
    }

    @Test
    fun derivedSourceCanBeStackedFilteredAndPagedAfterAliasingAgainstRealDatabase() {
        recreateComplexTables()

        val projectedCustomers = ComplexCustomer()
            .select {
                [
                    it.id.alias("customerId"),
                    it.name.alias("displayName"),
                    it.tier,
                    it.region,
                    it.score,
                    it.archived,
                ]
            }
            .where { it.archived == 0 }

        val optionalRegion: String? = null
        val page = projectedCustomers
            .select { [it.customerId, it.displayName, it.tier, it.region, it.score] }
            .where { it.score >= 70 }
            .where { (it.region == optionalRegion).takeIf(optionalRegion != null) }
            .where { it.tier !in listOf("bronze") }
            .orderBy { [it.score.desc(), it.customerId.asc()] }
            .page(pageIndex = 2, pageSize = 2)
            .withTotal()
            .toList<ComplexCustomerSearchRow>()

        assertEquals(3, page.total)
        assertEquals(2, page.totalPages)
        assertEquals(
            listOf(
                ComplexCustomerSearchRow(
                    customerId = 2,
                    displayName = "Grace",
                    tier = "silver",
                    region = "EU",
                    score = 76,
                ),
            ),
            page.records,
        )
    }

    @Test
    fun joinSubqueryAndCorrelatedExistsFiltersMapExpectedRowsAgainstRealDatabase() {
        recreateComplexTables()

        val customersWithLargePaidInvoices = ComplexInvoice()
            .select { it.customerId }
            .where { it.status == COMPLEX_PAID_STATUS && it.amount >= 70 }

        val rows = ComplexCustomer()
            .join(ComplexInvoice()) { customer, invoice ->
                innerJoin { customer.id == invoice.customerId }
                    .select {
                        [
                            customer.id.alias("customerId"),
                            customer.name.alias("customerName"),
                            invoice.amount.alias("openAmount"),
                            ComplexInvoice()
                                .select { paid -> paid.amount }
                                .where { paid -> paid.customerId == customer.id && paid.status == COMPLEX_PAID_STATUS }
                                .limit(1)
                                .alias("paidAmount"),
                            invoice.channel,
                            invoice.status,
                        ]
                    }
                    .where { customer.archived == 0 }
                    .where { invoice.status == COMPLEX_OPEN_STATUS && invoice.note.notNull }
                    .where { customer.id in customersWithLargePaidInvoices }
                    .where {
                        exists(
                            ComplexInvoice()
                                .select()
                                .where { paid ->
                                    paid.customerId == customer.id &&
                                        paid.status == COMPLEX_PAID_STATUS &&
                                        paid.amount > invoice.amount
                                }
                        )
                    }
                    .orderBy { [customer.id.asc(), invoice.amount.desc()] }
            }
            .toList<ComplexJoinSubqueryRow>()

        assertEquals(
            listOf(
                ComplexJoinSubqueryRow(
                    customerId = 1,
                    customerName = "Ada",
                    openAmount = 20,
                    paidAmount = 120,
                    channel = "sales",
                    status = COMPLEX_OPEN_STATUS,
                ),
                ComplexJoinSubqueryRow(
                    customerId = 2,
                    customerName = "Grace",
                    openAmount = 15,
                    paidAmount = 90,
                    channel = "web",
                    status = COMPLEX_OPEN_STATUS,
                ),
                ComplexJoinSubqueryRow(
                    customerId = 7,
                    customerName = "Katherine",
                    openAmount = 40,
                    paidAmount = 110,
                    channel = "partner",
                    status = COMPLEX_OPEN_STATUS,
                ),
            ),
            rows,
        )
    }

    private fun recreateComplexTables() {
        requireDatabaseAvailable()
        configureKronos()
        wrapper.table.dropTable(ComplexInvoice())
        wrapper.table.dropTable(ComplexCustomer())
        wrapper.table.createTable(ComplexCustomer())
        wrapper.table.createTable(ComplexInvoice())
        seedComplexRows()
    }

    private fun seedComplexRows() {
        listOf(
            ComplexCustomer(id = 1, name = "Ada", tier = "gold", age = 31, score = 91, region = "NA", archived = 0),
            ComplexCustomer(
                id = 2,
                name = "Grace",
                tier = "silver",
                age = 42,
                score = 76,
                region = "EU",
                referrer = "Ada",
                archived = 0,
            ),
            ComplexCustomer(id = 3, name = "Linus", tier = "bronze", age = 28, score = 65, region = null, archived = 0),
            ComplexCustomer(
                id = 4,
                name = null,
                tier = "gold",
                age = 35,
                score = 40,
                region = "APAC",
                referrer = "Grace",
                archived = 0,
            ),
            ComplexCustomer(
                id = 5,
                name = "Alan",
                tier = "gold",
                age = 24,
                score = 88,
                region = "NA",
                referrer = "Ada",
                archived = 1,
            ),
            ComplexCustomer(
                id = 6,
                name = "Ken",
                tier = "silver",
                age = 19,
                score = 55,
                region = "EU",
                referrer = "Grace",
                archived = 0,
            ),
            ComplexCustomer(
                id = 7,
                name = "Katherine",
                tier = "platinum",
                age = 29,
                score = 84,
                region = "EU",
                referrer = "Ada",
                archived = 0,
            ),
        ).forEach { it.insert().execute() }

        listOf(
            ComplexInvoice(
                id = 101,
                customerId = 1,
                status = COMPLEX_PAID_STATUS,
                amount = 120,
                discount = 0,
                channel = "web",
                createdDay = 10,
                note = "annual",
            ),
            ComplexInvoice(
                id = 102,
                customerId = 1,
                status = COMPLEX_OPEN_STATUS,
                amount = 20,
                discount = 5,
                channel = "sales",
                createdDay = 11,
                note = "upsell",
            ),
            ComplexInvoice(
                id = 103,
                customerId = 2,
                status = COMPLEX_PAID_STATUS,
                amount = 90,
                discount = 10,
                channel = "web",
                createdDay = 12,
                note = "annual",
            ),
            ComplexInvoice(
                id = 104,
                customerId = 3,
                status = COMPLEX_OPEN_STATUS,
                amount = 30,
                channel = "partner",
                createdDay = 13,
                note = null,
            ),
            ComplexInvoice(
                id = 105,
                customerId = 4,
                status = COMPLEX_PAID_STATUS,
                amount = 70,
                discount = 0,
                channel = "sales",
                createdDay = 14,
                note = "manual",
            ),
            ComplexInvoice(
                id = 106,
                customerId = 2,
                status = COMPLEX_OPEN_STATUS,
                amount = 15,
                channel = "web",
                createdDay = 15,
                note = "followup",
            ),
            ComplexInvoice(
                id = 107,
                customerId = 5,
                status = COMPLEX_PAID_STATUS,
                amount = 200,
                discount = 20,
                channel = "enterprise",
                createdDay = 16,
                note = "archived",
            ),
            ComplexInvoice(
                id = 108,
                customerId = 7,
                status = COMPLEX_PAID_STATUS,
                amount = 110,
                discount = 0,
                channel = "web",
                createdDay = 17,
                note = "annual",
            ),
            ComplexInvoice(
                id = 109,
                customerId = 7,
                status = COMPLEX_OPEN_STATUS,
                amount = 40,
                discount = 5,
                channel = "partner",
                createdDay = 18,
                note = "expansion",
            ),
            ComplexInvoice(
                id = 110,
                customerId = 6,
                status = COMPLEX_CANCELLED_STATUS,
                amount = 55,
                discount = 0,
                channel = "web",
                createdDay = 19,
                note = "cancelled",
            ),
        ).forEach { it.insert().execute() }
    }
}
