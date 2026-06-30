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

// Verifies that field comparisons against selectable queries produce scalar subquery criteria values.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.ast.CriteriaSubqueryValue
import com.kotlinorm.ast.KSelectableQueryRef
import com.kotlinorm.beans.dsl.Criteria
import com.kotlinorm.beans.dsl.KTableForCondition.Companion.afterFilter
import com.kotlinorm.enums.ConditionType
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select
import com.kotlinorm.types.ToFilter

@Table(name = "tb_scalar_cmp_order")
data class ScalarCmpOrder(
    var id: Int? = null,
    var userId: Int? = null,
    var status: Int? = null,
) : KPojo

fun scalarCmpWhere(order: ScalarCmpOrder, block: ToFilter<ScalarCmpOrder, Boolean?>): Criteria? {
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

    val criteria = scalarCmpWhere(ScalarCmpOrder(status = 3)) {
        it.status > ScalarCmpOrder()
            .select { order -> order.status }
            .where { order -> order.userId == 1 }
            .limit(1)
    }
    val scalar = criteria?.value as? CriteriaSubqueryValue.Scalar

    val failures = listOfNotNull(
        expect(criteria?.type == ConditionType.GT) { "type was ${criteria?.type}" },
        expect(criteria?.field?.name == "status") { "field was ${criteria?.field?.name}" },
        expect(scalar != null) { "value was ${criteria?.value?.let { it::class.qualifiedName }}" },
        expect(scalar?.query is KSelectableQueryRef) { "query ref was ${scalar?.query?.let { it::class.qualifiedName }}" },
    )

    return failures.firstOrNull() ?: "OK"
}

inline fun expect(condition: Boolean, message: () -> String): String? {
    return if (condition) null else "Fail: ${message()}"
}
