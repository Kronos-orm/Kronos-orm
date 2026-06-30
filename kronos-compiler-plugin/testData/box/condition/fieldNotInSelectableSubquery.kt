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

// Verifies that `field !in query` lowers to a negated IN Criteria carrying a deferred selectable query ref.

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

@Table(name = "tb_subquery_not_in_user")
data class SubqueryNotInUser(
    var id: Int? = null,
    var name: String? = null,
) : KPojo

@Table(name = "tb_subquery_not_in_order")
data class SubqueryNotInOrder(
    var id: Int? = null,
    var userId: Int? = null,
) : KPojo

fun subqueryNotInWhere(user: SubqueryNotInUser, block: ToFilter<SubqueryNotInUser, Boolean?>): Criteria? {
    var result: Criteria? = null
    user.afterFilter {
        criteriaParamMap = user.toDataMap()
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

    val criteria = subqueryNotInWhere(SubqueryNotInUser(id = 7)) {
        it.id !in SubqueryNotInOrder().select { order -> order.userId }
    }
    val subqueryValue = criteria?.value as? CriteriaSubqueryValue.In
    val queryRef = subqueryValue?.query as? KSelectableQueryRef

    val failures = listOfNotNull(
        expect(criteria?.type == ConditionType.IN) { "type was ${criteria?.type}" },
        expect(criteria?.not == true) { "not flag was ${criteria?.not}" },
        expect(criteria?.field?.name == "id") { "field was ${criteria?.field?.name}" },
        expect(subqueryValue != null) { "value was ${criteria?.value?.let { it::class.qualifiedName }}" },
        expect(subqueryValue?.not == true) { "subquery not flag was ${subqueryValue?.not}" },
        expect(queryRef != null) { "query ref was ${subqueryValue?.query?.let { it::class.qualifiedName }}" },
    )

    return failures.firstOrNull() ?: "OK"
}

inline fun expect(condition: Boolean, message: () -> String): String? {
    return if (condition) null else "Fail: ${message()}"
}
