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

// Verifies that `[field1, field2] in query` lowers to row-value IN subquery criteria.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.ast.CriteriaSubqueryValue
import com.kotlinorm.ast.KSelectableQueryRef
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.Criteria
import com.kotlinorm.beans.dsl.KTableForCondition.Companion.afterFilter
import com.kotlinorm.enums.ConditionType
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select
import com.kotlinorm.types.ToFilter

@Table(name = "tb_tuple_user")
data class TupleUser(
    var id: Int? = null,
    var status: Int? = null,
) : KPojo

@Table(name = "tb_tuple_order")
data class TupleOrder(
    var id: Int? = null,
    var userId: Int? = null,
    var status: Int? = null,
) : KPojo

fun tupleWhere(user: TupleUser, block: ToFilter<TupleUser, Boolean?>): Criteria? {
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

    val criteria = tupleWhere(TupleUser(id = 7, status = 1)) {
        [it.id, it.status] in TupleOrder().select { order -> [order.userId, order.status] }
    }
    val subqueryValue = criteria?.value as? CriteriaSubqueryValue.In
    val rowFields = subqueryValue?.value as? List<*>
    val queryRef = subqueryValue?.query as? KSelectableQueryRef

    val failures = listOfNotNull(
        expect(criteria?.type == ConditionType.IN) { "type was ${criteria?.type}" },
        expect(criteria?.field?.name.isNullOrEmpty()) { "field was ${criteria?.field?.name}" },
        expect(subqueryValue != null) { "value was ${criteria?.value?.let { it::class.qualifiedName }}" },
        expect(subqueryValue?.not == false) { "subquery not flag was ${subqueryValue?.not}" },
        expect(rowFields?.size == 2) { "row field size was ${rowFields?.size}" },
        expect(rowFields?.all { it is Field } == true) { "row fields were ${rowFields?.map { it?.let { v -> v::class.qualifiedName } }}" },
        expect(queryRef != null) { "query ref was ${subqueryValue?.query?.let { it::class.qualifiedName }}" },
    )

    return failures.firstOrNull() ?: "OK"
}

inline fun expect(condition: Boolean, message: () -> String): String? {
    return if (condition) null else "Fail: ${message()}"
}
