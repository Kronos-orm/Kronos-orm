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

// Verifies join(KSelectable) exposes the source query's Selected type to the right lambda parameter.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.compiler.support.CompilerTestDataSourceWrapper
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.join.join
import com.kotlinorm.orm.select.select
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.syntax.table.SqlTable

@Table("tb_join_selectable_user")
data class JoinSelectableUser(
    var id: Int? = null,
    var name: String? = null,
) : KPojo

@Table("tb_join_selectable_order")
data class JoinSelectableOrder(
    var id: Int? = null,
    var userId: Int? = null,
    var status: Int? = null,
) : KPojo

fun box(): String {
    with(Kronos) {}

    val orders = JoinSelectableOrder()
        .select { [it.userId, it.status.alias("rankStatus")] }
        .where { it.status == 41 }
    val statement = JoinSelectableUser()
        .join(orders) { user, order ->
            val rankStatus: Int? = order.rankStatus
            if (rankStatus == -1) error("unreachable")
            leftJoin { user.id == order.userId }
                .select { [user.id, order.rankStatus] }
        }
        .toSqlQuery(CompilerTestDataSourceWrapper) as SqlQuery.Select
    val join = statement.from.singleOrNull() as? SqlTable.Join
    val right = join?.right as? SqlTable.Subquery
    val selectedColumns = statement.select.mapNotNull {
        (it as? SqlSelectItem.Expr)?.expr as? SqlExpr.Column
    }

    return when {
        join == null -> "Fail: from was ${statement.from}"
        right == null -> "Fail: right table was ${join.right}"
        selectedColumns.none { it.tableName == "q" && it.columnName == "rankStatus" } ->
            "Fail: selected columns were $selectedColumns"
        else -> "OK"
    }
}
