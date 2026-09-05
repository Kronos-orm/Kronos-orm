/**
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Verifies JOIN Selected types survive offset, total, and cursor pagination stages.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.TransactionScope
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.TransactionIsolation
import com.kotlinorm.interfaces.KAtomicActionTask
import com.kotlinorm.interfaces.KAtomicQueryTask
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.join.join
import com.kotlinorm.orm.pagination.CursorResult
import com.kotlinorm.orm.pagination.PageResult
import com.kotlinorm.orm.select.select
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.table.SqlTable
import com.kotlinorm.utils.Extensions.mapperTo
import kotlin.reflect.typeOf

@Table("tb_join_page_user")
data class JoinPageUser(
    @PrimaryKey var id: Int? = null,
) : KPojo

@Table("tb_join_page_order")
data class JoinPageOrder(
    @PrimaryKey var id: Int? = null,
    var userId: Int? = null,
    var status: Int? = null,
) : KPojo

private val joinPageRow = linkedMapOf<String, Any?>(
    "id" to 7,
    "orderStatus" to 9,
)

class JoinPageWrapper : KronosDataSourceWrapper {
    override val url: String = "jdbc:join-page"
    override val userName: String = ""
    override val dbType: DBType = DBType.Mysql

    override fun toList(task: KAtomicQueryTask): List<Any?> = listOf(mapResult(task))

    override fun first(task: KAtomicQueryTask): Any? = mapResult(task)

    private fun mapResult(task: KAtomicQueryTask): Any {
        if (task.targetType == typeOf<Map<String, Any?>>()) return joinPageRow
        return joinPageRow.mapperTo(task.targetType)
    }

    override fun update(task: KAtomicActionTask): Int = 0
    override fun batchUpdate(task: KronosAtomicBatchTask): IntArray = intArrayOf()
    override fun transact(
        isolation: TransactionIsolation?,
        timeout: Int?,
        block: TransactionScope.() -> Any?,
    ): Any? = null
}

private fun joinedPageQuery() = JoinPageUser().join(JoinPageOrder()) { user, order ->
    innerJoin { user.id == order.userId }
        .select { [user.id, order.status.alias("orderStatus")] }
}

private fun configureJoinPageNaming() {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }
}

private fun compilePaginationResultTypes() {
    val page = joinedPageQuery().page(pageIndex = 2, pageSize = 3)
    val totalPage = page.withTotal()
    val cursor = joinedPageQuery()
        .orderBy { it.id.asc() }
        .cursor(pageSize = 2)

    @Suppress("UNREACHABLE_CODE")
    if (false) {
        val pageRows = page.toList()
        val totalRows = totalPage.toList()
        val cursorRows = cursor.toList()
        val pageMapRows: List<Map<String, Any?>> = page.toMapList()
        val totalMapRows: PageResult<Map<String, Any?>> = totalPage.toMapList()
        val cursorMapRows: CursorResult<Map<String, Any?>> = cursor.toMapList()
        val pageStatus: Int? = pageRows.first().orderStatus
        val totalStatus: Int? = totalRows.records.first().orderStatus
        val cursorStatus: Int? = cursorRows.records.first().orderStatus
        error(
            "pagination values unexpectedly evaluated as " +
                "$pageStatus/$totalStatus/$cursorStatus/$pageMapRows/$totalMapRows/$cursorMapRows"
        )
    }
}

fun box(): String {
    configureJoinPageNaming()
    compilePaginationResultTypes()

    val wrapper = JoinPageWrapper()
    val page = joinedPageQuery().page(pageIndex = 2, pageSize = 3)
    val totalPage = page.withTotal()
    val derived = page.select { [it.id, it.orderStatus] }
    val cursor = joinedPageQuery()
        .orderBy { it.id.asc() }
        .cursor(pageSize = 2)

    val derivedRow = derived.toList(wrapper).single()
    val derivedId: Int? = derivedRow.id
    val derivedStatus: Int? = derivedRow.orderStatus
    val derivedSql = derived.build(wrapper).atomicTask.sql

    val pageStatement = page.toSqlQuery(wrapper) as? SqlQuery.Select
        ?: return "Fail: page query was ${page.toSqlQuery(wrapper)}"
    val derivedStatement = derived.toSqlQuery(wrapper) as? SqlQuery.Select
        ?: return "Fail: derived query was ${derived.toSqlQuery(wrapper)}"
    val cursorStatement = cursor.toSqlQuery(wrapper) as? SqlQuery.Select
        ?: return "Fail: cursor query was ${cursor.toSqlQuery(wrapper)}"
    val derivedSource = derivedStatement.from.singleOrNull() as? SqlTable.Subquery
    val pageLimit = pageStatement.limit
    val cursorLimit = cursorStatement.limit
    val failures = listOfNotNull(
        expect(derivedId == 7) { "derived id was $derivedId" },
        expect(derivedStatus == 9) { "derived orderStatus was $derivedStatus" },
        expect(derivedRow.__columns.map { it.name } == listOf("id", "orderStatus")) {
            "derived columns were ${derivedRow.__columns.map { it.name }}"
        },
        expect(
            derivedSql ==
                "SELECT `q`.`id`, `q`.`orderStatus` FROM " +
                "(SELECT `tb_join_page_user`.`id` AS `id`, " +
                "`tb_join_page_order`.`status` AS `orderStatus` FROM `tb_join_page_user` " +
                "INNER JOIN `tb_join_page_order` ON `tb_join_page_user`.`id` = " +
                "`tb_join_page_order`.`user_id` LIMIT 3 OFFSET 3) AS `q`"
        ) { "derived SQL was $derivedSql" },
        expect(derivedSource?.query == pageStatement) { "derived source was $derivedSource" },
        expect(pageLimit?.offset.numberValue() == 3) { "page offset was ${pageLimit?.offset}" },
        expect(pageLimit?.fetch?.limit.numberValue() == 3) { "page fetch was ${pageLimit?.fetch}" },
        expect(cursorLimit?.offset == null) { "cursor offset was ${cursorLimit?.offset}" },
        expect(cursorLimit?.fetch?.limit.numberValue() == 3) { "cursor fetch was ${cursorLimit?.fetch}" },
        expect(cursorStatement.orderBy.size == 2) { "cursor order was ${cursorStatement.orderBy}" },
    )
    return failures.firstOrNull() ?: "OK"
}

private fun SqlExpr?.numberValue(): Int? = (this as? SqlExpr.NumberLiteral)?.number?.toIntOrNull()

private inline fun expect(condition: Boolean, message: () -> String): String? =
    if (condition) null else "Fail: ${message()}"
