@file:OptIn(com.kotlinorm.annotations.InternalKronosApi::class)

package com.kotlinorm.orm.pagination

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.KSelectable
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.beans.task.KronosQueryTask
import com.kotlinorm.beans.task.KronosQueryTask.Companion.toKronosQueryTask
import com.kotlinorm.beans.task.TransactionScope
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.enums.TransactionIsolation
import com.kotlinorm.enums.ValueCodecDirection
import com.kotlinorm.interfaces.KAtomicActionTask
import com.kotlinorm.interfaces.KAtomicQueryTask
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.interfaces.valueCodec
import com.kotlinorm.orm.sql.SqlQueryPlan
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlInRightOperand
import com.kotlinorm.syntax.expr.SqlParameter
import com.kotlinorm.syntax.limit.SqlLimit
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.syntax.table.SqlTable
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

@Table("paged_clause_stub")
data class PageQueryStubRow(var id: Int? = null) : KPojo

private enum class PreparedPageStatus { ACTIVE, BLOCKED }

class PageQueriesBehaviorTest {

    @Test
    fun `build wraps count SQL and clears count hooks`() {
        val selectable = RecordingSelectable(pageSize = 2)
        val wrapper = RecordingWrapper(objectResult = 2)

        val tasks = OffsetPageQuery(selectable, pageIndex = 1, pageSize = 2).withTotal().build(wrapper)

        assertEquals(
            "SELECT COUNT(*) FROM (SELECT `id` FROM `paged_clause_stub`) AS `total_count`",
            tasks.countTask.atomicTask.sql
        )
        assertNull(tasks.countTask.beforeQuery)
        assertNull(tasks.countTask.afterQuery)
        assertNotNull(tasks.recordsTask.beforeQuery)
        assertNotNull(tasks.recordsTask.afterQuery)
        assertEquals("SELECT id FROM paged_clause_stub", tasks.recordsTask.atomicTask.sql)
        assertEquals(listOf("build", "count"), selectable.calls)
    }

    @Test
    fun `count wrapper preserves already prepared scalar and expanded list parameters`() {
        var codecCalls = 0
        val selectable = PreparedParameterSelectable()
        val registration = Kronos.registerValueCodec(
            valueCodec(
                supports = { value, context ->
                    context.direction == ValueCodecDirection.ENCODE && value is PreparedPageStatus
                },
                convert = { value, _ ->
                    codecCalls++
                    (value as PreparedPageStatus).name
                }
            )
        )

        try {
            val task = OffsetPageQuery(selectable, pageIndex = 1, pageSize = 2)
                .withTotal()
                .build(RecordingWrapper())
                .countTask
                .atomicTask

            assertEquals(0, codecCalls)
            assertSame(selectable.status, task.paramMap["status"])
            assertSame(selectable.statuses, task.paramMap["statuses"])
            assertEquals(setOf(1), task.listParameterOccurrences)
            assertEquals(
                "SELECT COUNT(*) FROM (SELECT `id` FROM `paged_clause_stub` " +
                    "WHERE `status` = :status AND `status` IN (:statuses)) AS `total_count`",
                task.sql
            )
        } finally {
            registration.close()
        }
    }

    @Test
    fun `map result uses one wrapper and returns named pagination values`() {
        val rows = listOf(mapOf("id" to 1), mapOf("id" to 2))
        val wrapper = RecordingWrapper(listResult = rows, objectResult = 5)

        val result = OffsetPageQuery(RecordingSelectable(pageSize = 2), pageIndex = 3, pageSize = 2)
            .withTotal()
            .toMapList(wrapper)

        assertEquals(
            PageResult<Map<String, Any?>>(
                total = 5,
                records = rows,
                totalPages = 3,
                pageIndex = 3,
                pageSize = 2
            ),
            result
        )
        assertEquals(
            listOf(
                "SELECT COUNT(*) FROM (SELECT `id` FROM `paged_clause_stub`) AS `total_count`",
                "SELECT id FROM paged_clause_stub"
            ),
            wrapper.querySql
        )
    }

    @Test
    fun `typed toList uses provided wrapper for count and records`() {
        val rows = listOf(PageQueryStubRow(1), PageQueryStubRow(2))
        val wrapper = RecordingWrapper(typedListResult = rows, objectResult = 4)

        val result = OffsetPageQuery(RecordingSelectable(pageSize = 2), pageIndex = 2, pageSize = 2)
            .withTotal()
            .toList<PageQueryStubRow>(wrapper)

        assertEquals(
            PageResult(total = 4, records = rows, totalPages = 2, pageIndex = 2, pageSize = 2),
            result
        )
    }

    @Test
    fun `selected toList overload uses selected class metadata`() {
        val rows = listOf(PageQueryStubRow(1))
        val wrapper = RecordingWrapper(typedListResult = rows, objectResult = 1)

        val result = OffsetPageQuery(RecordingSelectable(pageSize = 3), pageIndex = 1, pageSize = 3)
            .withTotal()
            .toList(wrapper)

        assertEquals(
            PageResult(total = 1, records = rows, totalPages = 1, pageIndex = 1, pageSize = 3),
            result
        )
    }

    @Test
    fun `total pages returns zero for empty totals and rounds positive totals up`() {
        assertEquals(0, TotalPageQuery.totalPages(total = 0, pageSize = 10))
        assertEquals(0, TotalPageQuery.totalPages(total = -1, pageSize = 10))
        assertEquals(3, TotalPageQuery.totalPages(total = 5, pageSize = 2))
        assertEquals(1_073_741_824, TotalPageQuery.totalPages(total = Int.MAX_VALUE, pageSize = 2))
    }

    @Test
    fun `total page build rejects count tasks that lost their query AST`() {
        val page = OffsetPageQuery(
            RecordingSelectable(pageSize = 2, retainCountStatement = false),
            pageIndex = 1,
            pageSize = 2
        )

        val error = assertFailsWith<IllegalArgumentException> {
            page.withTotal().build(RecordingWrapper())
        }

        assertEquals("Total-page count tasks must retain their SQL query AST.", error.message)
    }

    @Test
    fun `offset and cursor stage methods resolve the default wrapper once`() {
        val wrapper = RecordingWrapper()
        val selectable = RecordingSelectable(pageSize = 2)
        val page = OffsetPageQuery(selectable, pageIndex = 1, pageSize = 2)
        val cursor = CursorPageQuery(selectable, pageSize = 2, fields = emptyList())
        val previousDataSource = Kronos.dataSource

        try {
            assertSingleDefaultResolution(wrapper) { page.build() }
            assertSingleDefaultResolution(wrapper) { page.toSqlQuery() }
            assertSingleDefaultResolution(wrapper) { page.toMapList() }
            assertSingleDefaultResolution(wrapper) { page.toList<PageQueryStubRow>() }
            assertSingleDefaultResolution(wrapper) { cursor.build() }
            assertSingleDefaultResolution(wrapper) { cursor.toSqlQuery() }
            assertSingleDefaultResolution(wrapper) { cursor.toMapList() }
            assertSingleDefaultResolution(wrapper) { cursor.toList<PageQueryStubRow>() }
        } finally {
            Kronos.dataSource = previousDataSource
        }
    }

    @Test
    fun `typed cursor map rows resolve case insensitive result labels`() {
        @Suppress("UNCHECKED_CAST")
        val rows = listOf(
            linkedMapOf<Any, Any?>(1 to "ignored", "ID" to 1),
            linkedMapOf<Any, Any?>(1 to "ignored", "ID" to 2),
            linkedMapOf<Any, Any?>(1 to "ignored", "ID" to 3),
        ) as List<Any?>
        val wrapper = RecordingWrapper(listResult = rows)
        val cursor = CursorPageQuery(
            RecordingSelectable(pageSize = 2),
            pageSize = 2,
            fields = listOf(CursorPageField(name = "id", resultLabel = "id", hidden = false))
        )

        val result = cursor.toList<Map<String, Any?>>(wrapper)

        assertEquals(true, result.hasNext)
        assertEquals(2, result.records.size)
        assertEquals(2, result.nextCursor?.decodeCursorValuesForTest()?.get("id"))
    }

    @Test
    fun `generic cursor reads KPojo values and trims the lookahead row`() {
        val rows = listOf(PageQueryStubRow(1), PageQueryStubRow(2), PageQueryStubRow(3))
        val cursor = CursorPageQuery(
            RecordingSelectable(pageSize = 2),
            pageSize = 2,
            fields = listOf(CursorPageField(name = "id", resultLabel = "id", hidden = false))
        )

        val result = cursor.toList<PageQueryStubRow>(RecordingWrapper(typedListResult = rows))

        assertEquals(true, result.hasNext)
        assertEquals(listOf(PageQueryStubRow(1), PageQueryStubRow(2)), result.records)
        assertEquals(2, result.nextCursor?.decodeCursorValuesForTest()?.get("id"))
    }

    @Test
    fun `generic cursor rejects rows that cannot expose the order field`() {
        val cursor = CursorPageQuery(
            RecordingSelectable(pageSize = 1),
            pageSize = 1,
            fields = listOf(CursorPageField(name = "id", resultLabel = "id", hidden = false))
        )

        val error = assertFailsWith<IllegalArgumentException> {
            cursor.toList<String>(RecordingWrapper(typedListResult = listOf("first", "lookahead")))
        }

        assertEquals("Cursor pagination requires selected orderBy field 'id' in result rows.", error.message)
    }

    private fun assertSingleDefaultResolution(wrapper: KronosDataSourceWrapper, action: () -> Unit) {
        var resolutions = 0
        Kronos.dataSource = {
            resolutions++
            wrapper
        }

        action()

        assertEquals(1, resolutions)
    }

    private fun Cursor.decodeCursorValuesForTest(): Map<String, Any?> {
        val encoded = java.util.Base64.getUrlDecoder().decode(value)
        return String(encoded, Charsets.UTF_8).lineSequence().associate { line ->
            val index = line.indexOf('=')
            line.substring(0, index) to line.substring(index + 1).removePrefix("i:").toInt()
        }
    }

    private class RecordingSelectable(
        private val pageSize: Int,
        private val retainCountStatement: Boolean = true
    ) : KSelectable<PageQueryStubRow>(PageQueryStubRow()) {
        override val selectedType = typeOf<PageQueryStubRow>()
        val calls = mutableListOf<String>()

        override fun build(wrapper: KronosDataSourceWrapper?): KronosQueryTask {
            calls += "build"
            return KronosAtomicQueryTask(
                sql = "SELECT id FROM paged_clause_stub",
                operationType = KOperationType.SELECT,
                statement = statement(),
                targetType = typeOf<PageQueryStubRow>()
            ).toKronosQueryTask()
                .doBeforeQuery { "records-before" }
                .doAfterQuery { _, _ -> "records-after" }
        }

        override fun buildTotalCountTask(wrapper: KronosDataSourceWrapper?): KronosQueryTask {
            calls += "count"
            val statement = SqlQuery.Select(
                select = listOf(SqlSelectItem.Expr(SqlExpr.Column(columnName = "id"))),
                from = listOf(SqlTable.Ident("paged_clause_stub"))
            )
            return KronosQueryTask(
                KronosAtomicQueryTask(
                    sql = "SELECT id FROM paged_clause_stub",
                    operationType = KOperationType.SELECT,
                    statement = statement.takeIf { retainCountStatement },
                    targetType = typeOf<Int>()
                )
            ).doBeforeQuery { "before" }
                .doAfterQuery { _, _ -> "after" }
        }

        override fun toSqlQueryPlan(wrapper: KronosDataSourceWrapper?): SqlQueryPlan =
            SqlQueryPlan(statement(), emptyMap())

        private fun statement(): SqlQuery.Select = SqlQuery.Select(
            select = listOf(SqlSelectItem.Expr(SqlExpr.Column(columnName = "id"))),
            from = listOf(SqlTable.Ident("paged_clause_stub")),
            limit = SqlLimit.limit(pageSize, 0)
        )
    }

    private class PreparedParameterSelectable : KSelectable<PageQueryStubRow>(PageQueryStubRow()) {
        override val selectedType = typeOf<PageQueryStubRow>()
        val status = PreparedPageStatus.ACTIVE
        val statuses = listOf(PreparedPageStatus.ACTIVE, PreparedPageStatus.BLOCKED)

        override fun build(wrapper: KronosDataSourceWrapper?): KronosQueryTask =
            KronosQueryTask(
                KronosAtomicQueryTask(
                    sql = "SELECT id FROM paged_clause_stub",
                    targetType = selectedType
                )
            )

        override fun buildTotalCountTask(wrapper: KronosDataSourceWrapper?): KronosQueryTask =
            KronosQueryTask(
                KronosAtomicQueryTask(
                    sql = "SELECT id FROM paged_clause_stub WHERE status = :status AND status IN (:statuses)",
                    paramMap = linkedMapOf("status" to status, "statuses" to statuses),
                    statement = statement(),
                    targetType = typeOf<Int>(),
                    listParameterOccurrences = setOf(1)
                )
            )

        override fun toSqlQueryPlan(wrapper: KronosDataSourceWrapper?): SqlQueryPlan =
            SqlQueryPlan(statement(), emptyMap())

        private fun statement(): SqlQuery.Select = SqlQuery.Select(
            select = listOf(SqlSelectItem.Expr(SqlExpr.Column(columnName = "id"))),
            from = listOf(SqlTable.Ident("paged_clause_stub")),
            where = SqlExpr.Binary(
                SqlExpr.Binary(
                    SqlExpr.Column(columnName = "status"),
                    SqlBinaryOperator.Equal,
                    SqlExpr.Parameter(SqlParameter.Named("status"))
                ),
                SqlBinaryOperator.And,
                SqlExpr.In(
                    SqlExpr.Column(columnName = "status"),
                    SqlInRightOperand.Values(
                        listOf(SqlExpr.Parameter(SqlParameter.Named("statuses"), expandAsList = true))
                    )
                )
            )
        )
    }

    private class RecordingWrapper(
        private val listResult: List<Any?> = emptyList(),
        private val typedListResult: List<Any?> = emptyList(),
        private val objectResult: Any? = null
    ) : KronosDataSourceWrapper {
        val querySql = mutableListOf<String>()
        override val url: String = "jdbc:mysql://localhost:3306/kronos"
        override val userName: String = "kronos"
        override val dbType: DBType = DBType.Mysql

        override fun toList(task: KAtomicQueryTask): List<Any?> {
            querySql += task.sql
            return if (task.targetType == typeOf<Map<String, Any?>>()) listResult else typedListResult
        }

        override fun first(task: KAtomicQueryTask): Any? {
            querySql += task.sql
            return objectResult
        }

        override fun update(task: KAtomicActionTask): Int = 1
        override fun batchUpdate(task: com.kotlinorm.beans.task.KronosAtomicBatchTask): IntArray = intArrayOf(1)
        override fun transact(
            isolation: TransactionIsolation?,
            timeout: Int?,
            block: TransactionScope.() -> Any?
        ): Any? = TransactionScope().block()
    }
}
