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

// Verifies method-style condition helpers are lowered into precise Criteria nodes.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.Criteria
import com.kotlinorm.beans.dsl.KTableForCondition.Companion.afterFilter
import com.kotlinorm.enums.ConditionType
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.types.ToFilter

@Table(name = "tb_method_criteria")
data class MethodCriteriaUser(
    var id: Int? = null,
    var name: String? = null,
    var age: Int? = null,
    var deletedAt: String? = null,
) : KPojo

fun methodWhere(user: MethodCriteriaUser, block: ToFilter<MethodCriteriaUser, Boolean?>): Criteria? {
    var result: Criteria? = null
    user.afterFilter {
        criteriaParamMap = user.toDataMap()
        block!!(it)
        result = criteria?.children?.singleOrNull()
    }
    return result
}

fun expectMethod(condition: Boolean, message: () -> String): String? {
    return if (condition) null else "Fail: ${message()}"
}

fun box(): String {
    Kronos.init {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val user = MethodCriteriaUser(id = 1, name = "Ada", age = 36)
    val like = methodWhere(user) { it.name.like("A%") }
    val notBetween = methodWhere(user) { it.age notBetween 1..17 }
    val isNull = methodWhere(user) { it.deletedAt.isNull }
    val notNull = methodWhere(user) { it.name.notNull }
    val sql = methodWhere(user) { "age > 18".asSql() }

    val failures = listOfNotNull(
        expectMethod(like?.type == ConditionType.LIKE) { "like type was ${like?.type}" },
        expectMethod(like?.value == "A%") { "like value was ${like?.value}" },
        expectMethod(notBetween?.type == ConditionType.BETWEEN) { "notBetween type was ${notBetween?.type}" },
        expectMethod(notBetween?.not == true) { "notBetween not was ${notBetween?.not}" },
        expectMethod(isNull?.type == ConditionType.ISNULL) { "isNull type was ${isNull?.type}" },
        expectMethod(isNull?.not == false) { "isNull not was ${isNull?.not}" },
        expectMethod(notNull?.type == ConditionType.ISNULL) { "notNull type was ${notNull?.type}" },
        expectMethod(notNull?.not == true) { "notNull not was ${notNull?.not}" },
        expectMethod(sql?.type == ConditionType.SQL) { "asSql type was ${sql?.type}" },
        expectMethod(sql?.value == "age > 18") { "asSql value was ${sql?.value}" },
    )

    return failures.firstOrNull() ?: "OK"
}
