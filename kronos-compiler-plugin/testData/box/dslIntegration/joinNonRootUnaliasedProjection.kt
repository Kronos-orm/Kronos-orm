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

// Verifies non-root JOIN fields use their captured receiver type in Selected and derived projections.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.TransactionScope
import com.kotlinorm.compiler.support.CompilerTestDataSourceWrapper
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.TransactionIsolation
import com.kotlinorm.interfaces.KAtomicActionTask
import com.kotlinorm.interfaces.KAtomicQueryTask
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.join.join
import com.kotlinorm.orm.select.select
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.syntax.table.SqlTable
import com.kotlinorm.utils.Extensions.mapperTo
import kotlin.reflect.typeOf

@Table("tb_join_non_root_user")
data class JoinNonRootUser(
    var id: Int? = null,
    var name: String? = null,
    var amount: String? = null,
) : KPojo

@Table("tb_join_non_root_order")
data class JoinNonRootOrder(
    var id: Int? = null,
    var userId: Int? = null,
    var amount: Int? = null,
) : KPojo

private val joinNonRootRow = linkedMapOf<String, Any?>(
    "userId" to 7,
    "userName" to "Ada",
    "amount" to 50,
)

class JoinNonRootWrapper : KronosDataSourceWrapper {
    override val url: String = "jdbc:join-non-root-unaliased-projection"
    override val userName: String = ""
    override val dbType: DBType = DBType.Mysql

    override fun toList(task: KAtomicQueryTask): List<Any?> = listOf(mapResult(task))

    override fun first(task: KAtomicQueryTask): Any? = mapResult(task)

    private fun mapResult(task: KAtomicQueryTask): Any {
        if (task.targetType == typeOf<Map<String, Any?>>()) return joinNonRootRow
        return joinNonRootRow.mapperTo(task.targetType)
    }

    override fun update(task: KAtomicActionTask): Int = 0
    override fun batchUpdate(task: KronosAtomicBatchTask): IntArray = intArrayOf()
    override fun transact(
        isolation: TransactionIsolation?,
        timeout: Int?,
        block: TransactionScope.() -> Any?,
    ): Any? = null
}

private fun configureJoinNonRootNaming() {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }
}

fun box(): String {
    configureJoinNonRootNaming()

    val joined = JoinNonRootUser().join(JoinNonRootOrder()) { user, order ->
        innerJoin { user.id == order.userId }
            .select {
                [
                    user.id.alias("userId"),
                    user.name.alias("userName"),
                    order.amount,
                ]
            }
    }
    val derived = joined.select { [it.userId, it.userName, it.amount] }
    val wrapper = JoinNonRootWrapper()
    val row = joined.toList(wrapper).single()
    val derivedRow = derived.toList(wrapper).single()
    val amount: Int? = row.amount
    val derivedAmount: Int? = derivedRow.amount

    val statement = joined.toSqlQuery(CompilerTestDataSourceWrapper) as SqlQuery.Select
    val derivedStatement = derived.toSqlQuery(CompilerTestDataSourceWrapper) as SqlQuery.Select
    val derivedSource = (derivedStatement.from.singleOrNull() as? SqlTable.Subquery)?.query as? SqlQuery.Select
    val amountColumn = statement.outputColumn("amount")

    val failures = listOfNotNull(
        expect(statement.outputNames() == listOf("userId", "userName", "amount")) {
            "JOIN output names were ${statement.outputNames()}"
        },
        expect(amountColumn?.tableName == "tb_join_non_root_order" && amountColumn?.columnName == "amount") {
            "non-root amount column was $amountColumn"
        },
        expect(row.__columns.map { it.name } == listOf("userId", "userName", "amount")) {
            "JOIN Selected columns were ${row.__columns.map { it.name }}"
        },
        expect(row.userId == 7 && row.userName == "Ada" && amount == 50) {
            "JOIN Selected values were ${row.userId}/${row.userName}/$amount"
        },
        expect(derivedStatement.outputNames() == listOf("userId", "userName", "amount")) {
            "derived output names were ${derivedStatement.outputNames()}"
        },
        expect(derivedSource?.outputNames() == listOf("userId", "userName", "amount")) {
            "derived source output names were ${derivedSource?.outputNames()}"
        },
        expect(derivedRow.__columns.map { it.name } == listOf("userId", "userName", "amount")) {
            "derived Selected columns were ${derivedRow.__columns.map { it.name }}"
        },
        expect(derivedRow.userId == 7 && derivedRow.userName == "Ada" && derivedAmount == 50) {
            "derived Selected values were ${derivedRow.userId}/${derivedRow.userName}/$derivedAmount"
        },
    )
    return failures.firstOrNull() ?: "OK"
}

private fun SqlSelectItem.Expr.outputName(): String? =
    metadata?.outputName ?: alias ?: (expr as? SqlExpr.Column)?.columnName

private fun SqlQuery.Select.outputNames(): List<String> =
    select.filterIsInstance<SqlSelectItem.Expr>().mapNotNull { it.outputName() }

private fun SqlQuery.Select.outputColumn(outputName: String): SqlExpr.Column? =
    select.filterIsInstance<SqlSelectItem.Expr>()
        .singleOrNull { it.outputName() == outputName }
        ?.expr as? SqlExpr.Column

private inline fun expect(condition: Boolean, message: () -> String): String? =
    if (condition) null else "Fail: ${message()}"
