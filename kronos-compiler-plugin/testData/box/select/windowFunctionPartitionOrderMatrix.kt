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

// Verifies window partition and order variants are lowered into syntax window expressions.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.KTableForSelect.Companion.afterSelect
import com.kotlinorm.functions.bundled.exts.MathFunctions.abs
import com.kotlinorm.functions.bundled.exts.WindowFunctions.rowNumber
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.order.SqlOrdering
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.types.ToSelect

@Table(name = "tb_select_window_matrix")
data class WindowMatrixOrder(
    var id: Int? = null,
    var userId: Int? = null,
    var createdAt: Int? = null,
    var amount: Int? = null,
) : KPojo

fun WindowMatrixOrder.collectWindowMatrixItems(block: ToSelect<WindowMatrixOrder, Any?>): List<SqlSelectItem> {
    val result = mutableListOf<SqlSelectItem>()
    afterSelect {
        block!!(it)
        result += selectItems
    }
    return result
}

fun expectWindowMatrix(condition: Boolean, message: () -> String): String? =
    if (condition) null else "Fail: ${message()}"

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val item = WindowMatrixOrder().collectWindowMatrixItems {
        f.rowNumber()
            .over {
                partitionBy(it.userId, f.abs(it.amount))
                orderBy(it.createdAt.asc(), it.id)
            }
            .alias("rn")
    }.singleOrNull() as? SqlSelectItem.Expr ?: return "Fail: window item was not Expr"

    val windowExpr = item.expr as? SqlExpr.Window ?: return "Fail: expr was ${item.expr}"
    val window = windowExpr.window
    val partitionUser = window.partitionBy.getOrNull(0) as? SqlExpr.Column
    val partitionFunction = window.partitionBy.getOrNull(1) as? SqlExpr.Function
    val partitionFunctionColumn = partitionFunction?.args?.singleOrNull() as? SqlExpr.Column
    val ascOrder = window.orderBy.getOrNull(0)
    val ascColumn = ascOrder?.expr as? SqlExpr.Column
    val defaultOrder = window.orderBy.getOrNull(1)
    val defaultColumn = defaultOrder?.expr as? SqlExpr.Column

    val failures = listOfNotNull(
        expectWindowMatrix(item.alias == "rn") { "alias was ${item.alias}" },
        expectWindowMatrix(window.partitionBy.size == 2) { "partition size was ${window.partitionBy.size}" },
        expectWindowMatrix(partitionUser?.columnName == "user_id") { "partition user was ${partitionUser?.columnName}" },
        expectWindowMatrix(partitionFunction?.name?.last == "ABS") { "partition function was ${partitionFunction?.name}" },
        expectWindowMatrix(partitionFunctionColumn?.columnName == "amount") {
            "partition function column was ${partitionFunctionColumn?.columnName}"
        },
        expectWindowMatrix(window.orderBy.size == 2) { "order size was ${window.orderBy.size}" },
        expectWindowMatrix(ascColumn?.columnName == "created_at") { "asc column was ${ascColumn?.columnName}" },
        expectWindowMatrix(ascOrder?.ordering == SqlOrdering.Asc) { "asc ordering was ${ascOrder?.ordering}" },
        expectWindowMatrix(defaultColumn?.columnName == "id") { "default column was ${defaultColumn?.columnName}" },
        expectWindowMatrix(defaultOrder?.ordering == SqlOrdering.Asc) { "default ordering was ${defaultOrder?.ordering}" },
    )

    return failures.firstOrNull() ?: "OK"
}
