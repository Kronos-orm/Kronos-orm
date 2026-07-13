package com.kotlinorm.orm.pagination

import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.TransactionScope
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.TransactionIsolation
import com.kotlinorm.interfaces.KAtomicActionTask
import com.kotlinorm.interfaces.KAtomicQueryTask
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

        assertEquals("Cursor pagination requires field-based orderBy items.", error.message)
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
