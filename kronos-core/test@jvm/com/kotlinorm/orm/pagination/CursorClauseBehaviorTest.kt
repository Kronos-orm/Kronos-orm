package com.kotlinorm.orm.pagination

import com.kotlinorm.annotations.Table
import com.kotlinorm.annotations.TableIndex
import com.kotlinorm.annotations.NonNull
import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.TransactionScope
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.TransactionIsolation
import com.kotlinorm.interfaces.KAtomicActionTask
import com.kotlinorm.interfaces.KAtomicQueryTask
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.functions.bundled.exts.PolymerizationFunctions.count
import com.kotlinorm.orm.join.join
import com.kotlinorm.orm.select.select
import com.kotlinorm.testfixtures.entities.TestUser
import com.kotlinorm.testfixtures.entities.UserRelation
import com.kotlinorm.testutils.MysqlTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CursorClauseBehaviorTest : MysqlTestBase() {

    @Test
    fun `first cursor page fetches one extra row and returns next cursor`() {
        val wrapper = CapturingCursorWrapper(
            mapRows = listOf(
                mapOf("id" to 3, "username" to "trinity"),
                mapOf("id" to 2, "username" to "neo"),
                mapOf("id" to 1, "username" to "morpheus")
            )
        )

        val result = TestUser()
            .select { [it.id, it.username] }
            .orderBy { it.id.desc() }
            .cursor(pageSize = 2)
            .toMapList(wrapper)

        assertEquals(true, result.hasNext)
        assertNotNull(result.nextCursor)
        assertEquals(
            listOf(
                mapOf("id" to 3, "username" to "trinity"),
                mapOf("id" to 2, "username" to "neo")
            ),
            result.records
        )
        assertEquals(
            "SELECT `id`, `username` FROM `tb_user` WHERE `deleted` = 0 ORDER BY `id` DESC LIMIT 3",
            wrapper.queryTasks.single().sql
        )
    }

    @Test
    fun `next cursor page adds keyset condition and returns no cursor on last page`() {
        val cursor = mapOf<String, Any?>("id" to 2).toCursor()
        val wrapper = CapturingCursorWrapper(
            mapRows = listOf(mapOf("id" to 1, "username" to "morpheus"))
        )

        val result = TestUser()
            .select { [it.id, it.username] }
            .orderBy { it.id.desc() }
            .cursor(pageSize = 2, after = cursor)
            .toMapList(wrapper)

        assertEquals(false, result.hasNext)
        assertNull(result.nextCursor)
        assertEquals(listOf(mapOf("id" to 1, "username" to "morpheus")), result.records)
        assertEquals(
            "SELECT `id`, `username` FROM `tb_user` WHERE `deleted` = 0 AND `id` < :cursor_id ORDER BY `id` DESC LIMIT 3",
            wrapper.queryTasks.single().sql
        )
        assertEquals(mapOf("cursor_id" to 2), wrapper.queryTasks.single().paramMap.toMap())
    }

    @Test
    fun `next cursor page supports composite order fields`() {
        val cursor = mapOf<String, Any?>("id" to 2, "username" to "neo").toCursor()
        val wrapper = CapturingCursorWrapper()

        TestUser()
            .select { [it.id, it.username] }
            .orderBy { [it.id.asc(), it.username.desc()] }
            .cursor(pageSize = 2, after = cursor)
            .toMapList(wrapper)

        assertEquals(
            "SELECT `id`, `username` FROM `tb_user` WHERE `deleted` = 0 AND " +
                "(`id` > :cursor_id OR (`id` = :cursor_id AND `username` < :cursor_username)) " +
                "ORDER BY `id` ASC, `username` DESC LIMIT 3",
            wrapper.queryTasks.single().sql
        )
        assertEquals(mapOf("cursor_id" to 2, "cursor_username" to "neo"), wrapper.queryTasks.single().paramMap.toMap())
    }

    @Test
    fun `cursor page appends primary key tie breaker to non unique order`() {
        val wrapper = CapturingCursorWrapper(
            mapRows = listOf(
                mapOf("id" to 1, "score" to 55),
                mapOf("id" to 2, "score" to 55),
                mapOf("id" to 3, "score" to 60)
            )
        )

        val result = TestUser()
            .select { [it.id, it.score] }
            .orderBy { it.score.asc() }
            .cursor(pageSize = 2)
            .toMapList(wrapper)

        assertEquals(true, result.hasNext)
        assertEquals(mapOf<String, Any?>("score" to 55, "id" to 2).toCursor(), result.nextCursor)
        assertEquals(
            "SELECT `id`, `score` FROM `tb_user` WHERE `deleted` = 0 ORDER BY `score` ASC, `id` ASC LIMIT 3",
            wrapper.queryTasks.single().sql
        )
    }

    @Test
    fun `next cursor page uses appended primary key in lexicographic predicate`() {
        val cursor = mapOf<String, Any?>("score" to 55, "id" to 2).toCursor()
        val wrapper = CapturingCursorWrapper()

        TestUser()
            .select { [it.id, it.score] }
            .orderBy { it.score.asc() }
            .cursor(pageSize = 2, after = cursor)
            .toMapList(wrapper)

        assertEquals(
            "SELECT `id`, `score` FROM `tb_user` WHERE `deleted` = 0 AND " +
                "(`score` > :cursor_score OR (`score` = :cursor_score AND `id` > :cursor_id)) " +
                "ORDER BY `score` ASC, `id` ASC LIMIT 3",
            wrapper.queryTasks.single().sql
        )
        assertEquals(mapOf("cursor_score" to 55, "cursor_id" to 2), wrapper.queryTasks.single().paramMap.toMap())
    }

    @Test
    fun `next cursor page supports descending order with appended primary key tie breaker`() {
        val cursor = mapOf<String, Any?>("score" to 55, "id" to 2).toCursor()
        val wrapper = CapturingCursorWrapper()

        TestUser()
            .select { [it.id, it.score] }
            .orderBy { it.score.desc() }
            .cursor(pageSize = 2, after = cursor)
            .toMapList(wrapper)

        assertEquals(
            "SELECT `id`, `score` FROM `tb_user` WHERE `deleted` = 0 AND " +
                "(`score` < :cursor_score OR (`score` = :cursor_score AND `id` > :cursor_id)) " +
                "ORDER BY `score` DESC, `id` ASC LIMIT 3",
            wrapper.queryTasks.single().sql
        )
        assertEquals(mapOf("cursor_score" to 55, "cursor_id" to 2), wrapper.queryTasks.single().paramMap.toMap())
    }

    @Test
    fun `cursor map page strips hidden tie breaker from projection`() {
        val wrapper = CapturingCursorWrapper(
            mapRows = listOf(
                mapOf("score" to 55, "__kronos_cursor_id" to 2),
                mapOf("score" to 60, "__kronos_cursor_id" to 3)
            )
        )

        val result = TestUser()
            .select { it.score }
            .orderBy { it.score.asc() }
            .cursor(pageSize = 1)
            .toMapList(wrapper)

        assertEquals(true, result.hasNext)
        assertEquals(mapOf<String, Any?>("score" to 55, "id" to 2).toCursor(), result.nextCursor)
        assertEquals(listOf(mapOf("score" to 55)), result.records)
        assertEquals(
            "SELECT `score`, `id` AS `__kronos_cursor_id` FROM `tb_user` WHERE `deleted` = 0 " +
                "ORDER BY `score` ASC, `id` ASC LIMIT 2",
            wrapper.queryTasks.single().sql
        )
    }

    @Test
    fun `cursor reads an ordered field through its projection alias`() {
        val wrapper = CapturingCursorWrapper(
            mapRows = listOf(
                mapOf("userId" to 1),
                mapOf("userId" to 2)
            )
        )

        val result = TestUser()
            .select { it.id.alias("userId") }
            .orderBy { it.id.asc() }
            .cursor(pageSize = 1)
            .toMapList(wrapper)

        assertEquals(true, result.hasNext)
        assertEquals(mapOf<String, Any?>("id" to 1).toCursor(), result.nextCursor)
        assertEquals(listOf(mapOf("userId" to 1)), result.records)
        assertEquals(
            "SELECT `id` AS `userId` FROM `tb_user` WHERE `deleted` = 0 ORDER BY `id` ASC LIMIT 2",
            wrapper.queryTasks.single().sql
        )
    }

    @Test
    fun `cursor hidden labels do not replace a user projection alias`() {
        val wrapper = CapturingCursorWrapper(
            mapRows = listOf(
                mapOf("score" to 55, "__KRONOS_CURSOR_ID" to "kept", "__kronos_cursor_id_1" to 2),
                mapOf("score" to 60, "__KRONOS_CURSOR_ID" to "next", "__kronos_cursor_id_1" to 3)
            )
        )

        val result = TestUser()
            .select { [it.score, it.username.alias("__KRONOS_CURSOR_ID")] }
            .orderBy { it.score.asc() }
            .cursor(pageSize = 1)
            .toMapList(wrapper)

        assertEquals(true, result.hasNext)
        assertEquals(mapOf<String, Any?>("score" to 55, "id" to 2).toCursor(), result.nextCursor)
        assertEquals(listOf(mapOf("score" to 55, "__KRONOS_CURSOR_ID" to "kept")), result.records)
        assertEquals(
            "SELECT `score`, `username` AS `__KRONOS_CURSOR_ID`, `id` AS `__kronos_cursor_id_1` " +
                "FROM `tb_user` WHERE `deleted` = 0 ORDER BY `score` ASC, `id` ASC LIMIT 2",
            wrapper.queryTasks.single().sql
        )
    }

    @Test
    fun `cursor page uses unique index tie breaker when primary key is absent`() {
        val wrapper = CapturingCursorWrapper(
            mapRows = listOf(
                mapOf("score" to 55, "__kronos_cursor_code" to "A"),
                mapOf("score" to 55, "__kronos_cursor_code" to "B")
            )
        )

        val result = CursorUniqueUser()
            .select { it.score }
            .orderBy { it.score.asc() }
            .cursor(pageSize = 1)
            .toMapList(wrapper)

        assertEquals(true, result.hasNext)
        assertEquals(mapOf<String, Any?>("score" to 55, "code" to "A").toCursor(), result.nextCursor)
        assertEquals(listOf(mapOf("score" to 55)), result.records)
        assertEquals(
            "SELECT `score`, `code` AS `__kronos_cursor_code` FROM `cursor_unique_user` " +
                "ORDER BY `score` ASC, `code` ASC LIMIT 2",
            wrapper.queryTasks.single().sql
        )
    }

    @Test
    fun `cursor page fails early when no primary or unique tie breaker exists`() {
        val wrapper = CapturingCursorWrapper()

        val error = assertFailsWith<IllegalArgumentException> {
            CursorUnstableUser()
                .select { [it.score, it.name] }
                .orderBy { it.score.asc() }
                .cursor(pageSize = 1)
                .toMapList(wrapper)
        }

        assertEquals("Cursor pagination requires a primary key or unique key tie-breaker.", error.message)
        assertEquals(emptyList(), wrapper.queryTasks)
    }

    @Test
    fun `cursor ignores nullable unique indexes as unstable tie breakers`() {
        val error = assertFailsWith<IllegalArgumentException> {
            CursorNullableUniqueUser()
                .select { it.score }
                .orderBy { it.score.asc() }
                .cursor(pageSize = 1)
        }

        assertEquals("Cursor pagination requires a primary key or unique key tie-breaker.", error.message)
    }

    @Test
    fun `ordinary cursor rejects result shapes changed by hidden ordering fields`() {
        val distinctError = assertFailsWith<IllegalArgumentException> {
            TestUser()
                .select { it.username }
                .distinct()
                .orderBy { it.username.asc() }
                .cursor(pageSize = 1)
        }
        assertEquals("Cursor pagination does not support DISTINCT queries.", distinctError.message)

        val groupError = assertFailsWith<IllegalArgumentException> {
            TestUser()
                .select { it.gender }
                .groupBy { it.gender }
                .orderBy { it.gender.asc() }
                .cursor(pageSize = 1)
        }
        assertEquals("Cursor pagination does not support GROUP BY or HAVING queries.", groupError.message)

        val havingError = assertFailsWith<IllegalArgumentException> {
            TestUser()
                .select { it.gender }
                .having { it.gender > 0 }
                .orderBy { it.gender.asc() }
                .cursor(pageSize = 1)
        }
        assertEquals("Cursor pagination does not support GROUP BY or HAVING queries.", havingError.message)

        val aggregateError = assertFailsWith<IllegalArgumentException> {
            TestUser()
                .select { f.count(it.id).alias("total") }
                .orderBy { it.id.asc() }
                .cursor(pageSize = 1)
        }
        assertEquals("Cursor pagination does not support aggregate projections.", aggregateError.message)
    }

    @Test
    fun `join cursor appends a stable key for every base source`() {
        val query = TestUser().join(UserRelation()) { user, relation ->
            innerJoin { user.id == relation.id2 }
                .select { user.username }
                .orderBy { user.id.asc() }
        }

        val firstPage = query.cursor(pageSize = 1).build().atomicTask
        assertEquals(
            "SELECT `tb_user`.`username` AS `username`, `tb_user`.`id` AS `__kronos_cursor_id`, " +
                "`user_relation`.`id` AS `__kronos_cursor_id_1` FROM `tb_user` " +
                "INNER JOIN `user_relation` ON `tb_user`.`id` = `user_relation`.`id2` " +
                "WHERE `tb_user`.`deleted` = 0 ORDER BY `tb_user`.`id` ASC, `user_relation`.`id` ASC LIMIT 2",
            firstPage.sql
        )

        val cursor = mapOf<String, Any?>(
            "tb_user.id" to 1,
            "user_relation.id" to 2
        ).toCursor()
        val nextPage = query.cursor(pageSize = 1, after = cursor).build().atomicTask
        assertEquals(
            linkedMapOf(
                "cursor_tb_user_id" to 1,
                "cursor_tb_user_id@1" to 1,
                "cursor_user_relation_id" to 2
            ),
            nextPage.paramMap
        )
    }

    @Test
    fun `join cursor hidden labels avoid aliases case insensitively`() {
        val wrapper = CapturingCursorWrapper(
            mapRows = listOf(
                mapOf(
                    "__KRONOS_CURSOR_ID" to "kept",
                    "__kronos_cursor_id_1" to 1,
                    "__kronos_cursor_id_2" to 2
                ),
                mapOf(
                    "__KRONOS_CURSOR_ID" to "next",
                    "__kronos_cursor_id_1" to 3,
                    "__kronos_cursor_id_2" to 4
                )
            )
        )
        val query = TestUser().join(UserRelation()) { user, relation ->
            innerJoin { user.id == relation.id2 }
                .select { user.username.alias("__KRONOS_CURSOR_ID") }
                .orderBy { user.id.asc() }
        }

        val result = query.cursor(pageSize = 1).toMapList(wrapper)

        assertEquals(true, result.hasNext)
        assertEquals(
            mapOf<String, Any?>("tb_user.id" to 1, "user_relation.id" to 2).toCursor(),
            result.nextCursor
        )
        assertEquals(listOf(mapOf("__KRONOS_CURSOR_ID" to "kept")), result.records)
        assertEquals(
            "SELECT `tb_user`.`username` AS `__KRONOS_CURSOR_ID`, " +
                "`tb_user`.`id` AS `__kronos_cursor_id_1`, " +
                "`user_relation`.`id` AS `__kronos_cursor_id_2` FROM `tb_user` " +
                "INNER JOIN `user_relation` ON `tb_user`.`id` = `user_relation`.`id2` " +
                "WHERE `tb_user`.`deleted` = 0 ORDER BY `tb_user`.`id` ASC, `user_relation`.`id` ASC LIMIT 2",
            wrapper.queryTasks.single().sql
        )
    }

    @Test
    fun `cross join cursor appends stable keys for both base sources`() {
        val query = TestUser().join(UserRelation()) { user, relation ->
            crossJoin()
                .select { user.username }
                .orderBy { user.id.asc() }
        }

        val firstPage = query.cursor(pageSize = 1).build().atomicTask

        assertEquals(
            "SELECT `tb_user`.`username` AS `username`, `tb_user`.`id` AS `__kronos_cursor_id`, " +
                "`user_relation`.`id` AS `__kronos_cursor_id_1` FROM `tb_user` CROSS JOIN `user_relation` " +
                "WHERE `tb_user`.`deleted` = 0 ORDER BY `tb_user`.`id` ASC, `user_relation`.`id` ASC LIMIT 2",
            firstPage.sql
        )
    }

    @Test
    fun `join cursor rejects unsafe source trees before building`() {
        val noOrderQuery = TestUser().join(UserRelation()) { user, relation ->
            innerJoin { user.id == relation.id2 }.select { user.id }
        }
        assertEquals(
            "Cursor pagination requires orderBy().",
            assertFailsWith<IllegalArgumentException> { noOrderQuery.cursor(pageSize = 1) }.message
        )

        val outerQuery = TestUser().join(UserRelation()) { user, relation ->
            leftJoin { user.id == relation.id2 }
                .select { user.id }
                .orderBy { user.id.asc() }
        }
        assertEquals(
            "Cursor pagination does not support outer JOIN sources because row uniqueness cannot be proven.",
            assertFailsWith<IllegalArgumentException> { outerQuery.cursor(pageSize = 1) }.message
        )

        val derived = UserRelation().select { [it.id, it.id2] }
        val derivedQuery = TestUser().join(derived) { user, relation ->
            innerJoin { user.id == relation.id2 }
                .select { user.id }
                .orderBy { user.id.asc() }
        }
        assertEquals(
            "Cursor pagination does not support JOIN derived or union sources because row uniqueness cannot be proven.",
            assertFailsWith<IllegalArgumentException> { derivedQuery.cursor(pageSize = 1) }.message
        )

        val missingKeyQuery = TestUser().join(CursorUnstableUser()) { user, unstable ->
            innerJoin { user.score == unstable.score }
                .select { user.id }
                .orderBy { user.id.asc() }
        }
        assertEquals(
            "Cursor pagination requires every JOIN base source to define a primary key or non-null unique key; " +
                "source 'cursor_unstable_user' has no stable key.",
            assertFailsWith<IllegalArgumentException> { missingKeyQuery.cursor(pageSize = 1) }.message
        )
    }

    @Test
    fun `join cursor rejects aggregate distinct and grouped shapes`() {
        val distinctQuery = TestUser().join(UserRelation()) { user, relation ->
            innerJoin { user.id == relation.id2 }
                .select { user.id }
                .distinct()
                .orderBy { user.id.asc() }
        }
        assertEquals(
            "Cursor pagination does not support DISTINCT queries.",
            assertFailsWith<IllegalArgumentException> { distinctQuery.cursor(pageSize = 1) }.message
        )

        val groupedQuery = TestUser().join(UserRelation()) { user, relation ->
            innerJoin { user.id == relation.id2 }
                .select { user.id }
                .groupBy { user.id }
                .orderBy { user.id.asc() }
        }
        assertEquals(
            "Cursor pagination does not support GROUP BY or HAVING queries.",
            assertFailsWith<IllegalArgumentException> { groupedQuery.cursor(pageSize = 1) }.message
        )

        val aggregateQuery = TestUser().join(UserRelation()) { user, relation ->
            innerJoin { user.id == relation.id2 }
                .select { f.count(relation.id).alias("total") }
                .orderBy { user.id.asc() }
        }
        assertEquals(
            "Cursor pagination does not support aggregate projections.",
            assertFailsWith<IllegalArgumentException> { aggregateQuery.cursor(pageSize = 1) }.message
        )
    }

    @Test
    fun `typed cursor page requires hidden tie breaker to be visible in projection`() {
        val wrapper = CapturingCursorWrapper()

        val error = assertFailsWith<IllegalArgumentException> {
            TestUser()
                .select { it.score }
                .orderBy { it.score.asc() }
                .cursor(pageSize = 1)
                .toList<Int>(wrapper)
        }

        assertEquals("Cursor pagination requires selected orderBy field 'id' in result rows.", error.message)
        assertEquals(emptyList(), wrapper.queryTasks)
    }

    @Test
    fun `cursor map results read ordered values case insensitively`() {
        val wrapper = CapturingCursorWrapper(
            mapRows = listOf(
                mapOf("ID" to 3, "USERNAME" to "trinity"),
                mapOf("ID" to 2, "USERNAME" to "neo")
            )
        )

        val result = TestUser()
            .select { [it.id, it.username] }
            .orderBy { it.id.desc() }
            .cursor(pageSize = 1)
            .toMapList(wrapper)

        assertEquals(true, result.hasNext)
        assertEquals(mapOf("id" to 3).toCursor(), result.nextCursor)
        assertEquals(listOf(mapOf("ID" to 3, "USERNAME" to "trinity")), result.records)
    }

    @Test
    fun `cursor typed toList supports map rows`() {
        val rows = listOf(mapOf("id" to 1), mapOf("id" to 2))
        val wrapper = CapturingCursorWrapper(mapRows = rows)

        val result = TestUser()
            .select()
            .orderBy { it.id.asc() }
            .cursor(pageSize = 1)
            .toList<Map<String, Any?>>(wrapper)

        assertEquals(true, result.hasNext)
        assertEquals(mapOf("id" to 1).toCursor(), result.nextCursor)
        assertEquals(listOf(mapOf("id" to 1)), result.records)
    }

    @Test
    fun `typed cursor page trims the extra row`() {
        val rows = listOf(TestUser(id = 1), TestUser(id = 2), TestUser(id = 3))
        val wrapper = CapturingCursorWrapper(typedRows = rows)

        val result = TestUser()
            .select()
            .orderBy { it.id.asc() }
            .cursor(pageSize = 2)
            .toList<TestUser>(wrapper)

        assertEquals(true, result.hasNext)
        assertNotNull(result.nextCursor)
        assertEquals(rows.take(2), result.records)
    }

    @Test
    fun `selected cursor toList overload returns selected rows and cursor`() {
        val rows = listOf(TestUser(id = 1), TestUser(id = 2))
        val wrapper = CapturingCursorWrapper(typedRows = rows)

        val result = TestUser()
            .select()
            .orderBy { it.id.asc() }
            .cursor(pageSize = 1)
            .toList(wrapper)

        assertEquals(true, result.hasNext)
        assertEquals(mapOf("id" to 1).toCursor(), result.nextCursor)
        assertEquals(listOf(TestUser(id = 1)), result.records)
    }

    @Test
    fun `cursor token requires every ordered field`() {
        val wrapper = CapturingCursorWrapper()
        val cursor = mapOf<String, Any?>("username" to "neo").toCursor()

        val error = assertFailsWith<IllegalArgumentException> {
            TestUser()
                .select { [it.id, it.username] }
                .orderBy { it.id.desc() }
                .cursor(pageSize = 2, after = cursor)
                .toMapList(wrapper)
        }

        assertEquals("Cursor token is missing orderBy field 'id'.", error.message)
    }

    @Test
    fun `cursor token rejects null ordered field values`() {
        val wrapper = CapturingCursorWrapper()
        val cursor = mapOf<String, Any?>("id" to null).toCursor()

        val error = assertFailsWith<IllegalStateException> {
            TestUser()
                .select { [it.id, it.username] }
                .orderBy { it.id.desc() }
                .cursor(pageSize = 2, after = cursor)
                .toMapList(wrapper)
        }

        assertEquals("Cursor token contains null for orderBy field 'id', which is not supported.", error.message)
    }

    @Test
    fun `cursor page requires order by when producing next cursor`() {
        val wrapper = CapturingCursorWrapper(
            mapRows = listOf(mapOf("id" to 1), mapOf("id" to 2))
        )

        val error = assertFailsWith<IllegalArgumentException> {
            TestUser()
                .select { [it.id] }
                .cursor(pageSize = 1)
                .toMapList(wrapper)
        }

        assertEquals("Cursor pagination requires orderBy().", error.message)
        assertEquals(emptyList(), wrapper.queryTasks)
    }

    @Test
    fun `cursor page requires selected ordered fields in result rows`() {
        val wrapper = CapturingCursorWrapper(
            mapRows = listOf(mapOf("username" to "neo"), mapOf("username" to "trinity"))
        )

        val error = assertFailsWith<IllegalArgumentException> {
            TestUser()
                .select { [it.username] }
                .orderBy { it.id.asc() }
                .cursor(pageSize = 1)
                .toMapList(wrapper)
        }

        assertEquals("Cursor pagination requires selected orderBy field 'id' in result rows.", error.message)
    }
}

@Table("cursor_unique_user")
@TableIndex("uk_cursor_unique_user_code", ["code"], type = "UNIQUE")
data class CursorUniqueUser(
    var score: Int? = null,
    @NonNull
    var code: String? = null
) : KPojo

@Table("cursor_nullable_unique_user")
@TableIndex("uk_cursor_nullable_unique_user_code", ["code"], type = "UNIQUE")
data class CursorNullableUniqueUser(
    var score: Int? = null,
    var code: String? = null
) : KPojo

@Table("cursor_unstable_user")
data class CursorUnstableUser(
    var score: Int? = null,
    var name: String? = null
) : KPojo

private class CapturingCursorWrapper(
    private val mapRows: List<Map<String, Any?>> = emptyList(),
    private val typedRows: List<Any?> = emptyList()
) : KronosDataSourceWrapper {
    val queryTasks = mutableListOf<KAtomicQueryTask>()
    override val url: String = "jdbc:mysql://localhost:3306/kronos"
    override val userName: String = "kronos"
    override val dbType: DBType = DBType.Mysql

    override fun toList(task: KAtomicQueryTask): List<Any?> {
        queryTasks += task
        return if (task.targetType.toString().contains("Map")) mapRows else typedRows
    }

    override fun first(task: KAtomicQueryTask): Any? = null
    override fun update(task: KAtomicActionTask): Int = 1
    override fun batchUpdate(task: KronosAtomicBatchTask): IntArray = intArrayOf(1)
    override fun transact(
        isolation: TransactionIsolation?,
        timeout: Int?,
        block: TransactionScope.() -> Any?
    ): Any? = TransactionScope().block()
}
