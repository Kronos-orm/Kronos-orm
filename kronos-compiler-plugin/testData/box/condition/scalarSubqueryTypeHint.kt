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

// Verifies `limit(1) as T` remains a scalar subquery type hint in syntax condition lowering.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.KTableForCondition.Companion.afterFilter
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.types.ToFilter

@Table(name = "tb_scalar_hint_order")
data class ScalarHintOrder(
    var id: Int? = null,
    var userId: Int? = null,
    var status: Int? = null,
) : KPojo

fun scalarHintWhere(order: ScalarHintOrder, block: ToFilter<ScalarHintOrder, Boolean?>): SqlExpr? {
    var result: SqlExpr? = null
    order.afterFilter {
        sourceValues = order.toDataMap()
        block!!(it)
        result = sqlExpr
    }
    return result
}

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val condition = scalarHintWhere(ScalarHintOrder(status = 3)) {
        it.status > (ScalarHintOrder()
            .select { other -> other.status }
            .where { other -> other.userId == 1 }
            .limit(1) as Int?)
    } as? SqlExpr.Binary
    val column = condition?.left as? SqlExpr.Column
    val scalar = condition?.right as? SqlExpr.Subquery

    val failures = listOfNotNull(
        expect(column?.columnName == "status") { "field was ${column?.columnName}" },
        expect(condition?.operator == SqlBinaryOperator.GreaterThan) { "operator was ${condition?.operator}" },
        expect(scalar?.query is SqlQuery.Select) { "query was ${scalar?.query?.let { it::class.qualifiedName }}" },
    )

    return failures.firstOrNull() ?: "OK"
}

inline fun expect(condition: Boolean, message: () -> String): String? {
    return if (condition) null else "Fail: ${message()}"
}
