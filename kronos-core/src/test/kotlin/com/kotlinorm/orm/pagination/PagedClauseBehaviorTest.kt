package com.kotlinorm.orm.pagination

import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.KSelectable
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.beans.task.KronosQueryTask
import com.kotlinorm.beans.task.KronosQueryTask.Companion.toKronosQueryTask
import com.kotlinorm.beans.task.TransactionScope
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.enums.TransactionIsolation
import com.kotlinorm.interfaces.KAtomicActionTask
import com.kotlinorm.interfaces.KAtomicQueryTask
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.sql.SqlQueryPlan
import com.kotlinorm.syntax.statement.SqlQuery
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@Table("paged_clause_stub")
data class PagedClauseStubRow(var id: Int? = null) : KPojo

class PagedClauseBehaviorTest {

    @Test
    fun `build wraps count SQL and clears count hooks`() {
        val selectable = RecordingSelectable()
        val wrapper = RecordingWrapper(objectResult = 2)

        val (countTask, recordTask) = PagedClause<PagedClauseStubRow, PagedClauseStubRow, RecordingSelectable>(selectable)
            .build(wrapper)

        assertEquals(
            "SELECT COUNT(*) FROM (SELECT id FROM paged_clause_stub) AS total_count",
            countTask.atomicTask.sql
        )
        assertNull(countTask.beforeQuery)
        assertNull(countTask.afterQuery)
        assertEquals("SELECT id FROM paged_clause_stub", recordTask.atomicTask.sql)
        assertEquals(listOf("build", "count"), selectable.calls)
    }

    @Test
    fun `query uses provided wrapper for count and records`() {
        val wrapper = RecordingWrapper(
            listResult = listOf(mapOf("id" to 1)),
            objectResult = 2
        )

        val result = PagedClause<PagedClauseStubRow, PagedClauseStubRow, RecordingSelectable>(RecordingSelectable())
            .query(wrapper)

        assertEquals(2 to listOf(mapOf("id" to 1)), result)
        assertEquals(
            listOf(
                "SELECT COUNT(*) FROM (SELECT id FROM paged_clause_stub) AS total_count",
                "SELECT id FROM paged_clause_stub"
            ),
            wrapper.querySql
        )
    }

    @Test
    fun `typed queryList uses provided wrapper for count and typed records`() {
        val rows = listOf(PagedClauseStubRow(1), PagedClauseStubRow(2))
        val wrapper = RecordingWrapper(
            typedListResult = rows,
            objectResult = 2
        )

        val result = PagedClause<PagedClauseStubRow, PagedClauseStubRow, RecordingSelectable>(RecordingSelectable())
            .queryList<PagedClauseStubRow>(wrapper)

        assertEquals(2 to rows, result)
        assertEquals(
            listOf(
                "SELECT COUNT(*) FROM (SELECT id FROM paged_clause_stub) AS total_count",
                "SELECT id FROM paged_clause_stub"
            ),
            wrapper.querySql
        )
    }

    @Test
    fun `selected queryList overload uses selected class metadata and hooks`() {
        val rows = listOf(PagedClauseStubRow(1))
        val wrapper = RecordingWrapper(
            typedListResult = rows,
            objectResult = 1
        )
        val selectable = RecordingSelectable()

        val result = PagedClause<PagedClauseStubRow, PagedClauseStubRow, RecordingSelectable>(selectable)
            .queryList(wrapper)

        assertEquals(1 to rows, result)
        assertEquals(
            listOf(
                "SELECT COUNT(*) FROM (SELECT id FROM paged_clause_stub) AS total_count",
                "SELECT id FROM paged_clause_stub"
            ),
            wrapper.querySql
        )
    }

    private class RecordingSelectable : KSelectable<PagedClauseStubRow>(
        PagedClauseStubRow(),
        PagedClauseStubRow::class
    ) {
        val calls = mutableListOf<String>()

        override fun build(wrapper: KronosDataSourceWrapper?): KronosQueryTask {
            calls += "build"
            return KronosAtomicQueryTask(
                sql = "SELECT id FROM paged_clause_stub",
                operationType = KOperationType.SELECT
            ).toKronosQueryTask()
        }

        override fun buildTotalCountTask(wrapper: KronosDataSourceWrapper?): KronosQueryTask {
            calls += "count"
            return KronosQueryTask(
                KronosAtomicQueryTask(
                    sql = "SELECT id FROM paged_clause_stub",
                    operationType = KOperationType.SELECT
                )
            ).doBeforeQuery { "before" }
                .doAfterQuery { _, _ -> "after" }
        }

        override fun toSqlQueryPlan(wrapper: KronosDataSourceWrapper?): SqlQueryPlan =
            error("PagedClauseBehaviorTest does not materialize selectable queries.")
    }

    private class RecordingWrapper(
        private val listResult: List<Map<String, Any>> = emptyList(),
        private val typedListResult: List<Any> = emptyList(),
        private val objectResult: Any? = null
    ) : KronosDataSourceWrapper {
        val querySql = mutableListOf<String>()
        override val url: String = "jdbc:mysql://localhost:3306/kronos"
        override val userName: String = "kronos"
        override val dbType: DBType = DBType.Mysql

        override fun forList(task: KAtomicQueryTask): List<Map<String, Any>> {
            querySql += task.sql
            return listResult
        }

        override fun forList(
            task: KAtomicQueryTask,
            kClass: KClass<*>,
            isKPojo: Boolean,
            superTypes: List<String>
        ): List<Any> {
            querySql += task.sql
            return typedListResult
        }

        override fun forMap(task: KAtomicQueryTask): Map<String, Any>? = null

        override fun forObject(
            task: KAtomicQueryTask,
            kClass: KClass<*>,
            isKPojo: Boolean,
            superTypes: List<String>
        ): Any? {
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
