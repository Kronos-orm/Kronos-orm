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

// Verifies condition RHS expressions can resolve to Field values and raw `.value` expressions.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.Criteria
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableForCondition.Companion.afterFilter
import com.kotlinorm.enums.ConditionType
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.types.ToFilter

@Table(name = "tb_field_value_criteria")
data class FieldValueCriteriaUser(
    var id: Int? = null,
    var age: Int? = null,
    var otherAge: Int? = null,
) : KPojo

fun fieldValueWhere(user: FieldValueCriteriaUser, block: ToFilter<FieldValueCriteriaUser, Boolean?>): Criteria? {
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

    val user = FieldValueCriteriaUser(id = 1, age = 36, otherAge = 40)
    val fieldRhs = fieldValueWhere(user) { it.age == it.otherAge }
    val rawValue = fieldValueWhere(user) { it.age == it.otherAge.value }
    val rhsField = fieldRhs?.value as? Field

    return when {
        fieldRhs?.field?.name != "age" -> "Fail: fieldRhs field was ${fieldRhs?.field?.name}"
        rhsField?.name != "otherAge" -> "Fail: rhs field was ${rhsField?.name}"
        rawValue?.type != ConditionType.EQUAL -> "Fail: rawValue type was ${rawValue?.type}"
        rawValue.value != 40 -> "Fail: rawValue value was ${rawValue.value}"
        else -> "OK"
    }
}
