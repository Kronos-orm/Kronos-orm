package com.kotlinorm.orm.pagination

import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.KSelectable
import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.TransactionScope
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.TransactionIsolation
import com.kotlinorm.interfaces.KAtomicActionTask
import com.kotlinorm.interfaces.KAtomicQueryTask
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.join.join
import com.kotlinorm.orm.select.select
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.table.SqlTable
import com.kotlinorm.testfixtures.entities.TestUser
import com.kotlinorm.testfixtures.entities.UserRelation
import com.kotlinorm.testutils.MysqlTestBase
import com.kotlinorm.wrappers.SampleMysqlJdbcWrapper
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@Table("page_stable_key_projection")
data class PageStableKeyProjection(var userId: Int? = null) : KPojo

class PaginationStageBehaviorTest : MysqlTestBase() {

    @Test
    fun `public stage types preserve selected type and expose only valid transitions`() {
        val base = TestUser().select().orderBy { it.id.asc() }
        val page: OffsetPageQuery<TestUser> = base.page(1, 20)
        val totalPage: TotalPageQuery<TestUser> = page.withTotal()
        val cursorPage: CursorPageQuery<TestUser> = base.cursor(20)
        val selectablePage: KSelectable<TestUser> = page
        val insertFromPage = page.insert<TestUser>()

        assertEquals(typeOf<TestUser>(), page.selectedType)
        assertEquals(typeOf<TestUser>(), cursorPage.selectedType)
        assertEquals(page, selectablePage)
        assertEquals(TestUser::class, insertFromPage.pojo::class)

        val offsetMethods = OffsetPageQuery::class.java.methods.map { it.name }.toSet()
        val totalMethods = TotalPageQuery::class.java.methods.map { it.name }.toSet()
        val cursorMethods = CursorPageQuery::class.java.methods.map { it.name }.toSet()
        val selectMethods = base::class.java.methods.map { it.name }.toSet()
        assertTrue(KSelectable::class.java.isAssignableFrom(base::class.java))
        assertFalse("withTotal" in selectMethods)
        assertFalse("withCursor" in selectMethods)
        assertTrue("withTotal" in offsetMethods)
        assertFalse("page" in offsetMethods)
        assertFalse("cursor" in offsetMethods)
        assertFalse("page" in totalMethods)
        assertFalse("cursor" in totalMethods)
        assertFalse("withTotal" in totalMethods)
        assertFalse("page" in cursorMethods)
        assertFalse("cursor" in cursorMethods)
        assertFalse("withTotal" in cursorMethods)
        assertTrue("alias" in offsetMethods)
        assertTrue("insert" in offsetMethods)
        assertFalse("alias" in cursorMethods)
        assertFalse("insert" in cursorMethods)
        assertTrue(KSelectable::class.java.isAssignableFrom(page::class.java))
        assertFalse(KSelectable::class.java.isAssignableFrom(totalPage::class.java))
        assertFalse(KSelectable::class.java.isAssignableFrom(cursorPage::class.java))
    }

    @Test
    fun `offset page and sibling views do not mutate their base query`() {
        val wrapper = SampleMysqlJdbcWrapper.sampleMysqlJdbcWrapper
        val base = TestUser().select { [it.id, it.username] }.orderBy { it.id.asc() }
        val secondPage = base.page(2, 10)
        val compactPage = base.page(3, 4)

        assertEquals(
            "SELECT `id`, `username` FROM `tb_user` WHERE `deleted` = 0 ORDER BY `id` ASC",
            base.build(wrapper).atomicTask.sql
        )
        assertEquals(
            "SELECT `id`, `username` FROM `tb_user` WHERE `deleted` = 0 ORDER BY `id` ASC LIMIT 10 OFFSET 10",
            secondPage.build(wrapper).atomicTask.sql
        )
        assertEquals(
            "SELECT `id`, `username` FROM `tb_user` WHERE `deleted` = 0 ORDER BY `id` ASC LIMIT 4 OFFSET 8",
            compactPage.build(wrapper).atomicTask.sql
        )
    }

    @Test
    fun `pagination snapshot preserves derived source binding and parameters`() {
        val wrapper = SampleMysqlJdbcWrapper.sampleMysqlJdbcWrapper
        val derived = TestUser()
            .select { [it.id, it.username] }
            .where { it.username == "neo" }
        val base = derived.select().where { it.id > 1 }
        val page = base.page(2, 3)

        assertEquals(
            "SELECT `q`.`id`, `q`.`username` FROM (SELECT `id`, `username` FROM `tb_user` " +
                "WHERE `tb_user`.`username` = :username AND `deleted` = 0) AS `q` WHERE `q`.`id` > :idMin LIMIT 3 OFFSET 3",
            page.build(wrapper).atomicTask.sql
        )
        assertEquals(mapOf("username" to "neo", "idMin" to 1), page.build(wrapper).atomicTask.paramMap.toMap())
        assertFalse(base.build(wrapper).atomicTask.sql.contains("LIMIT"))
    }

    @Test
    fun `offset page remains a finite selectable source with independent outer pagination`() {
        val wrapper = SampleMysqlJdbcWrapper.sampleMysqlJdbcWrapper
        val innerPage = TestUser()
            .select { [it.id, it.username] }
            .where { it.username == "neo" }
            .orderBy { it.id.asc() }
            .page(2, 3)
        val outer = innerPage
            .select { [it.id, it.username] }
            .orderBy { it.id.asc() }
        val outerPage = outer.page(2, 2)
        val outerCursor = outer.cursor(2)

        assertEquals(listOf(listOf("id")), innerPage.outputStableKeyCandidates())
        val innerStatement = innerPage.toSqlQuery(wrapper) as SqlQuery.Select
        val outerStatement = outer.toSqlQuery(wrapper) as SqlQuery.Select
        val outerPageStatement = outerPage.toSqlQuery(wrapper) as SqlQuery.Select
        val outerCursorStatement = outerCursor.toSqlQuery(wrapper) as SqlQuery.Select

        assertEquals(
            "SELECT `id`, `username` FROM `tb_user` WHERE `tb_user`.`username` = :username " +
                "AND `deleted` = 0 ORDER BY `id` ASC LIMIT 3 OFFSET 3",
            innerPage.build(wrapper).atomicTask.sql
        )
        assertEquals(
            "SELECT `q`.`id`, `q`.`username` FROM (SELECT `id`, `username` FROM `tb_user` " +
                "WHERE `tb_user`.`username` = :username AND `deleted` = 0 " +
                "ORDER BY `id` ASC LIMIT 3 OFFSET 3) AS `q` ORDER BY `q`.`id` ASC",
            outer.build(wrapper).atomicTask.sql
        )
        assertEquals(
            "SELECT `q`.`id`, `q`.`username` FROM (SELECT `id`, `username` FROM `tb_user` " +
                "WHERE `tb_user`.`username` = :username AND `deleted` = 0 " +
                "ORDER BY `id` ASC LIMIT 3 OFFSET 3) AS `q` ORDER BY `q`.`id` ASC LIMIT 2 OFFSET 2",
            outerPage.build(wrapper).atomicTask.sql
        )
        assertEquals(
            "SELECT `q`.`id`, `q`.`username` FROM (SELECT `id`, `username` FROM `tb_user` " +
                "WHERE `tb_user`.`username` = :username AND `deleted` = 0 " +
                "ORDER BY `id` ASC LIMIT 3 OFFSET 3) AS `q` ORDER BY `q`.`id` ASC LIMIT 3",
            outerCursor.build(wrapper).atomicTask.sql
        )
        assertEquals(innerStatement, (outerStatement.from.single() as SqlTable.Subquery).query)
        assertEquals(innerStatement, (outerPageStatement.from.single() as SqlTable.Subquery).query)
        assertEquals(innerStatement, (outerCursorStatement.from.single() as SqlTable.Subquery).query)
        assertEquals(mapOf("username" to "neo"), outerPage.build(wrapper).atomicTask.paramMap)
        assertEquals(mapOf("username" to "neo"), outerCursor.build(wrapper).atomicTask.paramMap)
    }

    @Test
    fun `derived cursor rejects a source projection without its complete stable key`() {
        val innerPage = TestUser()
            .select { it.username }
            .orderBy { it.username.asc() }
            .page(1, 3)
        val outer = innerPage
            .select { it.username }
            .orderBy { it.username.asc() }

        assertEquals(emptyList(), innerPage.outputStableKeyCandidates())
        assertEquals(
            "Cursor pagination requires a primary key or unique key tie-breaker.",
            assertFailsWith<IllegalArgumentException> { outer.cursor(2) }.message
        )
    }

    @Test
    fun `derived cursor propagates a stable key by its projected alias`() {
        val wrapper = SampleMysqlJdbcWrapper.sampleMysqlJdbcWrapper
        val innerPage = TestUser()
            .select(PageStableKeyProjection::class) { it.id.alias("userId") }
            .orderBy { it.id.asc() }
            .page(1, 3)
        val outer = innerPage
            .select { it.userId }
            .orderBy { it.userId.asc() }
        val cursor = outer.cursor(2)

        assertEquals(listOf(listOf("userId")), innerPage.outputStableKeyCandidates())
        assertEquals(
            "SELECT `q`.`userId` FROM (SELECT `id` AS `userId` FROM `tb_user` " +
                "WHERE `deleted` = 0 ORDER BY `id` ASC LIMIT 3 OFFSET 0) AS `q` " +
                "ORDER BY `q`.`userId` ASC LIMIT 3",
            cursor.build(wrapper).atomicTask.sql
        )
        val cursorStatement = cursor.toSqlQuery(wrapper) as SqlQuery.Select
        assertEquals(
            innerPage.toSqlQuery(wrapper),
            (cursorStatement.from.single() as SqlTable.Subquery).query
        )
    }

    @Test
    fun `total removes only the pagination owned by its current query layer`() {
        val wrapper = SampleMysqlJdbcWrapper.sampleMysqlJdbcWrapper
        val base = TestUser()
            .select { [it.id, it.username] }
            .where { it.username == "neo" }
            .orderBy { it.id.asc() }
        val innerPage = base.page(2, 3)

        val directCount = innerPage.withTotal().build(wrapper).countTask.atomicTask
        assertEquals(
            "SELECT COUNT(*) FROM (SELECT 1 AS count_value FROM `tb_user` " +
                "WHERE `tb_user`.`username` = :username AND `deleted` = 0) AS `total_count`",
            directCount.sql
        )
        assertEquals(mapOf("username" to "neo"), directCount.paramMap)

        val outerCount = innerPage
            .select { [it.id, it.username] }
            .where { it.id > 1 }
            .orderBy { it.id.asc() }
            .page(2, 2)
            .withTotal()
            .build(wrapper)
            .countTask
            .atomicTask
        assertEquals(
            "SELECT COUNT(*) FROM (SELECT 1 AS count_value FROM " +
                "(SELECT `id`, `username` FROM `tb_user` WHERE `tb_user`.`username` = :username " +
                "AND `deleted` = 0 ORDER BY `id` ASC LIMIT 3 OFFSET 3) AS `q` " +
                "WHERE `q`.`id` > :idMin) AS `total_count`",
            outerCount.sql
        )
        assertEquals(mapOf("idMin" to 1, "username" to "neo"), outerCount.paramMap)

        val totalStatement = outerCount.statement as SqlQuery.Select
        val countedRelation = (totalStatement.from.single() as SqlTable.Subquery).query as SqlQuery.Select
        val finiteSource = countedRelation.from.single() as SqlTable.Subquery
        assertEquals(innerPage.toSqlQuery(wrapper), finiteSource.query)
    }

    @Test
    fun `total page returns named result and runs count before records on one wrapper`() {
        val wrapper = RecordingPageWrapper(
            mapRows = listOf(mapOf("id" to 1), mapOf("id" to 2)),
            total = 5
        )

        val result = TestUser()
            .select { it.id }
            .page(1, 2)
            .withTotal()
            .toMapList(wrapper)

        assertEquals(
            PageResult<Map<String, Any?>>(
                total = 5,
                records = listOf(mapOf("id" to 1), mapOf("id" to 2)),
                totalPages = 3,
                pageIndex = 1,
                pageSize = 2
            ),
            result
        )
        assertEquals(listOf("count", "records"), wrapper.calls)
        assertEquals(2, wrapper.tasks.size)
    }

    @Test
    fun `total count preserves distinct outputs and group cardinality for select and join queries`() {
        val wrapper = SampleMysqlJdbcWrapper.sampleMysqlJdbcWrapper

        val simpleSelect = TestUser()
            .select { it.username }
            .where { it.username == "neo" }
            .page(1, 10)
            .withTotal()
            .build(wrapper)
            .countTask
            .atomicTask
        assertEquals(
            "SELECT COUNT(*) FROM (SELECT 1 AS count_value FROM `tb_user` " +
                "WHERE `tb_user`.`username` = :username AND `deleted` = 0) AS `total_count`",
            simpleSelect.sql
        )
        assertEquals(mapOf("username" to "neo"), simpleSelect.paramMap)

        val distinctSelect = TestUser()
            .select { it.username }
            .where { it.username == "neo" }
            .distinct()
            .page(1, 10)
            .withTotal()
            .build(wrapper)
            .countTask
            .atomicTask
        assertEquals(
            "SELECT COUNT(*) FROM (SELECT DISTINCT `username` FROM `tb_user` " +
                "WHERE `tb_user`.`username` = :username AND `deleted` = 0) AS `total_count`",
            distinctSelect.sql
        )
        assertEquals(mapOf("username" to "neo"), distinctSelect.paramMap)

        val groupedSelect = TestUser()
            .select { it.gender }
            .groupBy { it.gender }
            .page(1, 10)
            .withTotal()
            .build(wrapper)
            .countTask
            .atomicTask
        assertEquals(
            "SELECT COUNT(*) FROM (SELECT 1 AS count_value FROM `tb_user` WHERE `deleted` = 0 " +
                "GROUP BY `gender`) AS `total_count`",
            groupedSelect.sql
        )
        assertEquals(emptyMap(), groupedSelect.paramMap)

        val distinctJoin = TestUser().join(UserRelation()) { user, relation ->
            leftJoin { user.id == relation.id2 }
                .select { user.id }
                .where { user.username == "neo" }
                .distinct()
        }.page(1, 10).withTotal().build(wrapper).countTask.atomicTask
        assertEquals(
            "SELECT COUNT(*) FROM (SELECT DISTINCT `tb_user`.`id` AS `id` FROM `tb_user` " +
                "LEFT JOIN `user_relation` ON `tb_user`.`id` = `user_relation`.`id2` " +
                "WHERE `tb_user`.`username` = :username AND `tb_user`.`deleted` = 0) AS `total_count`",
            distinctJoin.sql
        )
        assertEquals(mapOf("username" to "neo"), distinctJoin.paramMap)

        val groupedJoin = TestUser().join(UserRelation()) { user, relation ->
            leftJoin { user.id == relation.id2 }
                .select { relation.gender }
                .groupBy { relation.gender }
        }.page(1, 10).withTotal().build(wrapper).countTask.atomicTask
        assertEquals(
            "SELECT COUNT(*) FROM (SELECT 1 AS count_value FROM `tb_user` " +
                "LEFT JOIN `user_relation` ON `tb_user`.`id` = `user_relation`.`id2` " +
                "WHERE `tb_user`.`deleted` = 0 GROUP BY `user_relation`.`gender`) AS `total_count`",
            groupedJoin.sql
        )
        assertEquals(emptyMap(), groupedJoin.paramMap)
    }

    @Test
    fun `cursor views do not add tie breakers or limits to base and sibling queries`() {
        val wrapper = SampleMysqlJdbcWrapper.sampleMysqlJdbcWrapper
        val base = TestUser().select { it.score }.orderBy { it.score.asc() }
        val firstPage = base.cursor(pageSize = 2)
        val compactPage = base.cursor(pageSize = 1)

        assertEquals(
            "SELECT `score` FROM `tb_user` WHERE `deleted` = 0 ORDER BY `score` ASC",
            base.build(wrapper).atomicTask.sql
        )
        assertEquals(
            "SELECT `score`, `id` AS `__kronos_cursor_id` FROM `tb_user` WHERE `deleted` = 0 " +
                "ORDER BY `score` ASC, `id` ASC LIMIT 3",
            firstPage.build(wrapper).atomicTask.sql
        )
        assertEquals(
            "SELECT `score`, `id` AS `__kronos_cursor_id` FROM `tb_user` WHERE `deleted` = 0 " +
                "ORDER BY `score` ASC, `id` ASC LIMIT 2",
            compactPage.build(wrapper).atomicTask.sql
        )
    }

    @Test
    fun `invalid page and cursor sizes fail before creating a view`() {
        val base = TestUser().select().orderBy { it.id.asc() }

        assertEquals(
            "Page index must be greater than zero.",
            assertFailsWith<IllegalArgumentException> { base.page(0, 20) }.message
        )
        assertEquals(
            "Page size must be greater than zero.",
            assertFailsWith<IllegalArgumentException> { base.page(1, 0) }.message
        )
        assertEquals(
            "Page size must be greater than zero.",
            assertFailsWith<IllegalArgumentException> { base.cursor(0) }.message
        )

        val offsetMessage = "Page offset exceeds the supported Int range."
        assertEquals(
            offsetMessage,
            assertFailsWith<IllegalArgumentException> { base.page(Int.MAX_VALUE, 2) }.message
        )
        val joined = TestUser().join(UserRelation()) { user, relation ->
            innerJoin { user.id == relation.id2 }.select { user.id }
        }
        assertEquals(
            offsetMessage,
            assertFailsWith<IllegalArgumentException> { joined.page(Int.MAX_VALUE, 2) }.message
        )

        val cursorMessage = "Cursor page size exceeds the supported Int range."
        assertEquals(
            cursorMessage,
            assertFailsWith<IllegalArgumentException> { base.cursor(Int.MAX_VALUE) }.message
        )
        assertEquals(
            cursorMessage,
            assertFailsWith<IllegalArgumentException> { joined.cursor(Int.MAX_VALUE) }.message
        )
    }
}

private class RecordingPageWrapper(
    private val mapRows: List<Map<String, Any?>>,
    private val total: Int
) : KronosDataSourceWrapper {
    val calls = mutableListOf<String>()
    val tasks = mutableListOf<KAtomicQueryTask>()
    override val url: String = "jdbc:mysql://localhost:3306/kronos"
    override val userName: String = "kronos"
    override val dbType: DBType = DBType.Mysql

    override fun toList(task: KAtomicQueryTask): List<Any?> {
        calls += "records"
        tasks += task
        return mapRows
    }

    override fun first(task: KAtomicQueryTask): Any? {
        calls += "count"
        tasks += task
        return total
    }

    override fun update(task: KAtomicActionTask): Int = 1

    override fun batchUpdate(task: KronosAtomicBatchTask): IntArray = intArrayOf(1)

    override fun transact(
        isolation: TransactionIsolation?,
        timeout: Int?,
        block: TransactionScope.() -> Any?
    ): Any? = TransactionScope().block()
}
