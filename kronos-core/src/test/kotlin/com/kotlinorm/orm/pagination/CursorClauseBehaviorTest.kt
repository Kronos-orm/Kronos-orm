package com.kotlinorm.orm.pagination

import com.kotlinorm.annotations.Table
import com.kotlinorm.annotations.TableIndex
import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.TransactionScope
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.TransactionIsolation
import com.kotlinorm.interfaces.KAtomicActionTask
import com.kotlinorm.interfaces.KAtomicQueryTask
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.select.select
import com.kotlinorm.testfixtures.entities.TestUser
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
            .withCursor()
            .cursor(offset = 2)
            .toMapList(wrapper)

        assertEquals(true, result.first)
        assertNotNull(result.second)
        assertEquals(
            listOf(
                mapOf("id" to 3, "username" to "trinity"),
                mapOf("id" to 2, "username" to "neo")
            ),
            result.third
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
            .withCursor()
            .cursor(cursor, offset = 2)
            .toMapList(wrapper)

        assertEquals(false, result.first)
        assertNull(result.second)
        assertEquals(listOf(mapOf("id" to 1, "username" to "morpheus")), result.third)
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
            .withCursor()
            .cursor(cursor, offset = 2)
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
            .withCursor()
            .cursor(offset = 2)
            .toMapList(wrapper)

        assertEquals(true, result.first)
        assertEquals(mapOf<String, Any?>("score" to 55, "id" to 2).toCursor(), result.second)
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
            .withCursor()
            .cursor(cursor, offset = 2)
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
            .withCursor()
            .cursor(cursor, offset = 2)
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
            .withCursor()
            .cursor(offset = 1)
            .toMapList(wrapper)

        assertEquals(true, result.first)
        assertEquals(mapOf<String, Any?>("score" to 55, "id" to 2).toCursor(), result.second)
        assertEquals(listOf(mapOf("score" to 55)), result.third)
        assertEquals(
            "SELECT `score`, `id` AS `__kronos_cursor_id` FROM `tb_user` WHERE `deleted` = 0 " +
                "ORDER BY `score` ASC, `id` ASC LIMIT 2",
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
            .withCursor()
            .cursor(offset = 1)
            .toMapList(wrapper)

        assertEquals(true, result.first)
        assertEquals(mapOf<String, Any?>("score" to 55, "code" to "A").toCursor(), result.second)
        assertEquals(listOf(mapOf("score" to 55)), result.third)
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
                .withCursor()
                .cursor(offset = 1)
                .toMapList(wrapper)
        }

        assertEquals("Cursor pagination requires a primary key or unique key tie-breaker.", error.message)
        assertEquals(emptyList(), wrapper.queryTasks)
    }

    @Test
    fun `typed cursor page requires hidden tie breaker to be visible in projection`() {
        val wrapper = CapturingCursorWrapper()

        val error = assertFailsWith<IllegalArgumentException> {
            TestUser()
                .select { it.score }
                .orderBy { it.score.asc() }
                .withCursor()
                .cursor(offset = 1)
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
            .withCursor()
            .cursor(offset = 1)
            .toMapList(wrapper)

        assertEquals(true, result.first)
        assertEquals(mapOf("id" to 3).toCursor(), result.second)
        assertEquals(listOf(mapOf("ID" to 3, "USERNAME" to "trinity")), result.third)
    }

    @Test
    fun `cursor typed toList supports map rows`() {
        val rows = listOf(mapOf("id" to 1), mapOf("id" to 2))
        val wrapper = CapturingCursorWrapper(mapRows = rows)

        val result = TestUser()
            .select()
            .orderBy { it.id.asc() }
            .withCursor()
            .cursor(offset = 1)
            .toList<Map<String, Any?>>(wrapper)

        assertEquals(true, result.first)
        assertEquals(mapOf("id" to 1).toCursor(), result.second)
        assertEquals(listOf(mapOf("id" to 1)), result.third)
    }

    @Test
    fun `typed cursor page trims the extra row`() {
        val rows = listOf(TestUser(id = 1), TestUser(id = 2), TestUser(id = 3))
        val wrapper = CapturingCursorWrapper(typedRows = rows)

        val result = TestUser()
            .select()
            .orderBy { it.id.asc() }
            .withCursor()
            .cursor(offset = 2)
            .toList<TestUser>(wrapper)

        assertEquals(true, result.first)
        assertNotNull(result.second)
        assertEquals(rows.take(2), result.third)
    }

    @Test
    fun `selected cursor toList overload returns selected rows and cursor`() {
        val rows = listOf(TestUser(id = 1), TestUser(id = 2))
        val wrapper = CapturingCursorWrapper(typedRows = rows)

        val result = TestUser()
            .select()
            .orderBy { it.id.asc() }
            .withCursor()
            .cursor(offset = 1)
            .toList(wrapper)

        assertEquals(true, result.first)
        assertEquals(mapOf("id" to 1).toCursor(), result.second)
        assertEquals(listOf(TestUser(id = 1)), result.third)
    }

    @Test
    fun `cursor token requires every ordered field`() {
        val wrapper = CapturingCursorWrapper()
        val cursor = mapOf<String, Any?>("username" to "neo").toCursor()

        val error = assertFailsWith<IllegalArgumentException> {
            TestUser()
                .select { [it.id, it.username] }
                .orderBy { it.id.desc() }
                .withCursor()
                .cursor(cursor, offset = 2)
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
                .withCursor()
                .cursor(cursor, offset = 2)
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
                .withCursor()
                .cursor(offset = 1)
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
                .withCursor()
                .cursor(offset = 1)
                .toMapList(wrapper)
        }

        assertEquals("Cursor pagination requires selected orderBy field 'id' in result rows.", error.message)
    }
}

@Table("cursor_unique_user")
@TableIndex("uk_cursor_unique_user_code", ["code"], type = "UNIQUE")
data class CursorUniqueUser(
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
