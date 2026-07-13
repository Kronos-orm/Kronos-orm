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
