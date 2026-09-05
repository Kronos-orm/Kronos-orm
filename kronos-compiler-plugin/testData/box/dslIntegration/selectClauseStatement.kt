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

// Verifies select statement construction and Source/Context receivers after projection refinement.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Column
import com.kotlinorm.annotations.Table
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.group.SqlGroupingItem
import com.kotlinorm.syntax.order.SqlOrdering
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.syntax.table.SqlTable

@Table(name = "tb_integrated_user")
data class IntegratedUser(
    var id: Int? = null,
    @Column("user_name")
    var name: String? = null,
    var age: Int? = null,
) : KPojo

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val user = IntegratedUser(id = 7, name = "Ada", age = 36)
    val statement = user.select { [it.id, it.name] }
        .where { it.age >= 18 }
        .by { [it.id, it.name] }
        .groupBy { [it.id, it.name] }
        .orderBy { [it.name.desc(), it.id] }
        .distinct()
        .limit(5)
        .toSqlQuery() as SqlQuery.Select

    val inner = ((statement.from.singleOrNull() as? SqlTable.Subquery)?.query as? SqlQuery.Select) ?: statement
    val selectColumns = inner.select.mapNotNull { ((it as? SqlSelectItem.Expr)?.expr as? SqlExpr.Column)?.columnName }
    val groupColumns = inner.groupBy?.items?.mapNotNull { ((it as? SqlGroupingItem.Expr)?.item as? SqlExpr.Column)?.columnName }

    return when {
        inner.quantifier == null -> "Fail: distinct was false"
        (inner.limit?.fetch?.limit as? SqlExpr.NumberLiteral)?.number != "5" -> "Fail: limit was ${inner.limit}"
        selectColumns != listOf("id", "user_name") -> "Fail: select columns were $selectColumns"
        groupColumns != listOf("id", "user_name") -> "Fail: group columns were $groupColumns"
        statement.orderBy.map { it.ordering } != listOf(SqlOrdering.Desc, SqlOrdering.Asc) -> "Fail: orderBy was ${statement.orderBy}"
        (inner.where as? SqlExpr.Binary)?.operator != SqlBinaryOperator.And -> "Fail: where operator was ${(inner.where as? SqlExpr.Binary)?.operator}"
        else -> "OK"
    }
}
