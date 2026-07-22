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

// Verifies two generic JoinSource bounds still rewrite generated JOIN projections.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.compiler.support.CompilerTestDataSourceWrapper
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.join.JoinSource
import com.kotlinorm.orm.join.JoinSource2
import com.kotlinorm.orm.join.join
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.syntax.table.SqlJoinType
import com.kotlinorm.syntax.table.SqlTable

@Table("tb_indirect_join_user")
data class IndirectJoinUser(
    var id: Int? = null,
    var name: String? = null,
) : KPojo

@Table("tb_indirect_join_order")
data class IndirectJoinOrder(
    var id: Int? = null,
    var userId: Int? = null,
    var amount: Int? = null,
) : KPojo

private fun <Base, S> generatedJoinProjection(
    source: S,
    user: IndirectJoinUser,
    order: IndirectJoinOrder,
) where Base : JoinSource<IndirectJoinUser, Base>, S : Base =
    source.select { [user.id, order.amount.alias("amount")] }

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val query = IndirectJoinUser().join(IndirectJoinOrder()) { user, order ->
        val completed = innerJoin { user.id == order.userId }
        generatedJoinProjection<
            JoinSource2<IndirectJoinUser, IndirectJoinOrder>,
            JoinSource2<IndirectJoinUser, IndirectJoinOrder>
        >(
            completed,
            user,
            order,
        )
    }

    val statement = query.toSqlQuery(CompilerTestDataSourceWrapper) as SqlQuery.Select
    val join = statement.from.singleOrNull() as? SqlTable.Join
    val failures = listOfNotNull(
        expect(statement.outputNames() == listOf("id", "amount")) {
            "outputs were ${statement.outputNames()}"
        },
        expect(join?.joinType == SqlJoinType.Inner) { "join type was ${join?.joinType}" },
        expect(listOf(join?.left is SqlTable.Ident, join?.right is SqlTable.Ident) == listOf(true, true)) {
            "join operand kinds were ${listOf(join?.left?.let { it::class.qualifiedName }, join?.right?.let { it::class.qualifiedName })}"
        },
    )
    return failures.firstOrNull() ?: "OK"
}

private fun SqlQuery.Select.outputNames(): List<String> = select.mapNotNull { item ->
    val expression = item as? SqlSelectItem.Expr ?: return@mapNotNull null
    expression.metadata?.outputName
        ?: expression.alias
        ?: (expression.expr as? SqlExpr.Column)?.columnName
}

private inline fun expect(condition: Boolean, message: () -> String): String? =
    if (condition) null else "Fail: ${message()}"
