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

// Verifies window function OVER clauses are lowered into syntax window expressions.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.KTableForSelect.Companion.afterSelect
import com.kotlinorm.functions.bundled.exts.WindowFunctions.rowNumber
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.order.SqlOrdering
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.types.ToSelect

@Table(name = "tb_select_window")
data class WindowSelectOrder(
    var id: Int? = null,
    var userId: Int? = null,
    var createdAt: Int? = null,
) : KPojo

fun WindowSelectOrder.collectItems(block: ToSelect<WindowSelectOrder, Any?>): List<SqlSelectItem> {
    val result = mutableListOf<SqlSelectItem>()
    afterSelect {
        block!!(it)
        result += selectItems
    }
    return result
}

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val item = WindowSelectOrder().collectItems {
        f.rowNumber()
            .over {
                partitionBy(it.userId)
                orderBy(it.createdAt.desc())
            }
            .alias("rn")
    }.singleOrNull() as? SqlSelectItem.Expr ?: return "Fail: window item was not Expr"

    val windowExpr = item.expr as? SqlExpr.Window ?: return "Fail: expr was ${item.expr}"
    val function = windowExpr.expr as? SqlExpr.Function ?: return "Fail: window function was ${windowExpr.expr}"
    val window = windowExpr.window
    val partition = window.partitionBy.singleOrNull() as? SqlExpr.Column
        ?: return "Fail: partition was ${window.partitionBy}"
    val order = window.orderBy.singleOrNull()
        ?: return "Fail: orderBy was ${window.orderBy}"
    val orderColumn = order.expr as? SqlExpr.Column
        ?: return "Fail: order expression was ${order.expr}"

    val failures = listOfNotNull(
        expect(item.alias == "rn") { "alias was ${item.alias}" },
        expect(function.name.last == "ROW_NUMBER") { "function name was ${function.name}" },
        expect(partition.columnName == "user_id") { "partition column was ${partition.columnName}" },
        expect(orderColumn.columnName == "created_at") { "order column was ${orderColumn.columnName}" },
        expect(order.ordering == SqlOrdering.Desc) { "order direction was ${order.ordering}" },
    )

    return failures.firstOrNull() ?: "OK"
}

inline fun expect(condition: Boolean, message: () -> String): String? {
    return if (condition) null else "Fail: ${message()}"
}
