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
import com.kotlinorm.ast.ColumnReference
import com.kotlinorm.ast.JoinTable
import com.kotlinorm.ast.QueryMaterializeContext
import com.kotlinorm.ast.SelectItem
import com.kotlinorm.ast.SubqueryLowering
import com.kotlinorm.ast.SubqueryTable
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.join.join
import com.kotlinorm.orm.select.select

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
    Kronos.init {}

    val orders = JoinSelectableOrder()
        .select { [it.userId, it.status.alias("rankStatus")] }
        .where { it.status == 41 }
    val params = mutableMapOf<String, Any?>()
    val statement = JoinSelectableUser()
        .join(orders) { user, order ->
            val rankStatus: Int? = order.rankStatus
            leftJoin(order) { user.id == order.userId }
            select { [user.id, order.rankStatus] }
            if (rankStatus == -1) error("unreachable")
        }
        .toStatement(parameterValues = params)
    val lowered = SubqueryLowering.lower(statement, QueryMaterializeContext(parameterValues = params))
    val join = lowered.from as? JoinTable
    val right = join?.right as? SubqueryTable
    val selectedColumns = lowered.selectList.mapNotNull {
        (it as? SelectItem.ExpressionSelectItem)?.expression as? ColumnReference
    }

    return when {
        join == null -> "Fail: from was ${lowered.from}"
        right == null -> "Fail: right table was ${join.right}"
        params["status"] != 41 -> "Fail: params were $params"
        selectedColumns.none { it.tableAlias == "q" && it.columnName == "rankStatus" } ->
            "Fail: selected columns were $selectedColumns"
        else -> "OK"
    }
}
