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

// Verifies window aliases are FIR-visible on post-select orderBy Context receivers.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.ast.ColumnReference
import com.kotlinorm.enums.SortType
import com.kotlinorm.functions.bundled.exts.WindowFunctions.rowNumber
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select

@Table("tb_projection_window_alias_order")
data class ProjectionWindowAliasOrder(
    var id: Int? = null,
    var userId: Int? = null,
    var status: Int? = null,
) : KPojo

fun box(): String {
    Kronos.init {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val statement = ProjectionWindowAliasOrder()
        .select {
            [
                it.id,
                f.rowNumber()
                    .over {
                        partitionBy(it.userId)
                        orderBy(it.status.desc())
                    }
                    .alias("rn")
            ]
        }
        .orderBy { it.rn.asc() }
        .toStatement()

    val orderBy = statement.orderBy?.singleOrNull()
        ?: return "Fail: orderBy was ${statement.orderBy}"
    val orderColumn = orderBy.expression as? ColumnReference
        ?: return "Fail: orderBy expression was ${orderBy.expression}"

    val failures = listOfNotNull(
        expect(statement.selectItemMetadata().any { it.outputName == "rn" }) {
            "select metadata was ${statement.selectItemMetadata()}"
        },
        expect(orderColumn.columnName == "rn") { "orderBy column was ${orderColumn.columnName}" },
        expect(orderBy.direction == SortType.ASC) { "order direction was ${orderBy.direction}" },
    )

    return failures.firstOrNull() ?: "OK"
}

inline fun expect(condition: Boolean, message: () -> String): String? {
    return if (condition) null else "Fail: ${message()}"
}
