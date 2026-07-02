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

// Verifies window function OVER clauses are lowered into FunctionField metadata.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.ast.ColumnReference
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.FunctionField
import com.kotlinorm.beans.dsl.KTableForSelect.Companion.afterSelect
import com.kotlinorm.enums.SortType
import com.kotlinorm.functions.bundled.exts.WindowFunctions.rowNumber
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.types.ToSelect

@Table(name = "tb_select_window")
data class WindowSelectOrder(
    var id: Int? = null,
    var userId: Int? = null,
    var createdAt: Int? = null,
) : KPojo

fun WindowSelectOrder.collectFields(block: ToSelect<WindowSelectOrder, Any?>): List<Field> {
    val result = mutableListOf<Field>()
    afterSelect {
        block!!(it)
        result += fields
    }
    return result
}

fun box(): String {
    Kronos.init {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val field = WindowSelectOrder().collectFields {
        f.rowNumber()
            .over {
                partitionBy(it.userId)
                orderBy(it.createdAt.desc())
            }
            .alias("rn")
    }.singleOrNull() as? FunctionField ?: return "Fail: window field was not FunctionField"

    val window = field.over ?: return "Fail: over was null"
    val partition = window.partitionBy?.singleOrNull() as? ColumnReference
        ?: return "Fail: partition was ${window.partitionBy}"
    val order = window.orderBy?.singleOrNull()
        ?: return "Fail: orderBy was ${window.orderBy}"
    val orderColumn = order.expression as? ColumnReference
        ?: return "Fail: order expression was ${order.expression}"

    val failures = listOfNotNull(
        expect(field.name == "rn") { "alias was ${field.name}" },
        expect(field.functionName == "rowNumber") { "function name was ${field.functionName}" },
        expect(partition.columnName == "user_id") { "partition column was ${partition.columnName}" },
        expect(orderColumn.columnName == "created_at") { "order column was ${orderColumn.columnName}" },
        expect(order.direction == SortType.DESC) { "order direction was ${order.direction}" },
    )

    return failures.firstOrNull() ?: "OK"
}

inline fun expect(condition: Boolean, message: () -> String): String? {
    return if (condition) null else "Fail: ${message()}"
}
