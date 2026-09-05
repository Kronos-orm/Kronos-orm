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

// Verifies quantified selectable comparisons lower to syntax quantified comparison predicates.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.KTableForCondition.Companion.afterFilter
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlQuantifiedComparisonOperator
import com.kotlinorm.syntax.expr.SqlSubqueryQuantifier
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.types.ToFilter

@Table(name = "tb_quantified_cmp_order")
data class QuantifiedCmpOrder(
    var id: Int? = null,
    var userId: Int? = null,
    var status: Int? = null,
) : KPojo

fun quantifiedCmpWhere(order: QuantifiedCmpOrder, block: ToFilter<QuantifiedCmpOrder, Boolean?>): SqlExpr? {
    var result: SqlExpr? = null
    order.afterFilter {
        sourceValues = order.toDataMap()
        block(it)
        result = sqlExpr
    }
    return result
}

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val anyCondition = quantifiedCmpWhere(QuantifiedCmpOrder(status = 3)) {
        it.status > any<Int>(
            QuantifiedCmpOrder()
                .select { order -> order.status }
                .where { order -> order.userId == 1 }
        )
    } as? SqlExpr.QuantifiedComparisonPredicate
    val allCondition = quantifiedCmpWhere(QuantifiedCmpOrder(status = 3)) {
        it.status > all<Int>(
            QuantifiedCmpOrder()
                .select { order -> order.status }
                .where { order -> order.userId == 1 }
        )
    } as? SqlExpr.QuantifiedComparisonPredicate
    val anyColumn = anyCondition?.expr as? SqlExpr.Column
    val allColumn = allCondition?.expr as? SqlExpr.Column

    val failures = listOfNotNull(
        expect(anyColumn?.columnName == "status") { "ANY field was ${anyColumn?.columnName}" },
        expect(anyCondition?.operator == SqlQuantifiedComparisonOperator.GreaterThan) {
            "ANY operator was ${anyCondition?.operator}"
        },
        expect(anyCondition?.quantifier == SqlSubqueryQuantifier.Any) {
            "ANY quantifier was ${anyCondition?.quantifier}"
        },
        expect(anyCondition?.query is SqlQuery.Select) {
            "ANY query was ${anyCondition?.query?.let { it::class.qualifiedName }}"
        },
        expect(allColumn?.columnName == "status") { "ALL field was ${allColumn?.columnName}" },
        expect(allCondition?.operator == SqlQuantifiedComparisonOperator.GreaterThan) {
            "ALL operator was ${allCondition?.operator}"
        },
        expect(allCondition?.quantifier == SqlSubqueryQuantifier.All) {
            "ALL quantifier was ${allCondition?.quantifier}"
        },
        expect(allCondition?.query is SqlQuery.Select) {
            "ALL query was ${allCondition?.query?.let { it::class.qualifiedName }}"
        },
    )

    return failures.firstOrNull() ?: "OK"
}

inline fun expect(condition: Boolean, message: () -> String): String? {
    return if (condition) null else "Fail: ${message()}"
}
