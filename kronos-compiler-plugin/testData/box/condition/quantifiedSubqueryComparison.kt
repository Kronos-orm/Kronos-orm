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

// Verifies that quantified selectable comparisons produce structured Criteria handoff values.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.ast.CriteriaSubqueryValue
import com.kotlinorm.ast.KSelectableQueryRef
import com.kotlinorm.ast.SubqueryExpression
import com.kotlinorm.beans.dsl.Criteria
import com.kotlinorm.beans.dsl.KTableForCondition.Companion.afterFilter
import com.kotlinorm.enums.ConditionType
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select
import com.kotlinorm.types.ToFilter

@Table(name = "tb_quantified_cmp_order")
data class QuantifiedCmpOrder(
    var id: Int? = null,
    var userId: Int? = null,
    var status: Int? = null,
) : KPojo

fun quantifiedCmpWhere(order: QuantifiedCmpOrder, block: ToFilter<QuantifiedCmpOrder, Boolean?>): Criteria? {
    var result: Criteria? = null
    order.afterFilter {
        criteriaParamMap = order.toDataMap()
        block!!(it)
        result = criteria?.children?.singleOrNull()
    }
    return result
}

fun box(): String {
    Kronos.init {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val criteria = quantifiedCmpWhere(QuantifiedCmpOrder(status = 3)) {
        it.status > any<Int>(
            QuantifiedCmpOrder()
                .select { order -> order.status }
                .where { order -> order.userId == 1 }
        )
    }
    val quantified = criteria?.value as? CriteriaSubqueryValue.QuantifiedComparison

    val failures = listOfNotNull(
        expect(criteria?.type == ConditionType.GT) { "type was ${criteria?.type}" },
        expect(criteria?.field?.name == "status") { "field was ${criteria?.field?.name}" },
        expect(quantified != null) { "value was ${criteria?.value?.let { it::class.qualifiedName }}" },
        expect(quantified?.query is KSelectableQueryRef) { "query ref was ${quantified?.query?.let { it::class.qualifiedName }}" },
        expect(quantified?.quantifier == SubqueryExpression.Quantifier.ANY) { "quantifier was ${quantified?.quantifier}" },
    )

    return failures.firstOrNull() ?: "OK"
}

inline fun expect(condition: Boolean, message: () -> String): String? {
    return if (condition) null else "Fail: ${message()}"
}
