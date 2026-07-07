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

// Verifies comparison operators are lowered into syntax SqlExpr conditions.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableForCondition.Companion.afterFilter
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlParameter
import com.kotlinorm.types.ToFilter
import com.kotlinorm.utils.TransformerSafeValue

@Table(name = "tb_condition_user")
data class ConditionUser(
    var id: Int? = null,
    var name: String? = null,
    var age: Int? = null,
) : KPojo {
    fun column(name: String): Field {
        return kronosColumns().single { it.name == name }
    }
}

data class CapturedCondition(
    val expr: SqlExpr?,
    val parameters: Map<String, Any?>
)

fun conditionUserWhere(user: ConditionUser, block: ToFilter<ConditionUser, Boolean?>): CapturedCondition {
    var result: CapturedCondition? = null
    user.afterFilter {
        sourceValues = user.toDataMap()
        block!!(it)
        result = CapturedCondition(sqlExpr, parameterValues.toMap())
    }
    return result ?: CapturedCondition(null, emptyMap())
}

fun assertCondition(
    actual: CapturedCondition,
    fieldName: String,
    operator: SqlBinaryOperator,
    value: Any?,
): String? {
    val binary = actual.expr as? SqlExpr.Binary
    val column = binary?.left as? SqlExpr.Column
    val parameter = (binary?.right as? SqlExpr.Parameter)?.parameter as? SqlParameter.Named
    return when {
        binary == null -> "condition was ${actual.expr}"
        column?.columnName != fieldName -> "column was ${column?.columnName}"
        binary.operator != operator -> "operator was ${binary.operator}"
        parameter == null -> "right side was ${binary.right}"
        actual.parameters[parameter.name] != value -> "value was ${actual.parameters[parameter.name]}"
        else -> null
    }
}

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val user = ConditionUser(name = "Ada", age = 36)

    fun checkCondition(
        label: String,
        actual: CapturedCondition,
        fieldName: String,
        operator: SqlBinaryOperator,
        value: Any?,
    ): String? {
        val failure = assertCondition(actual, fieldName, operator, value)
        return if (failure == null) null else "Fail: $label $failure"
    }

    val checks = listOfNotNull(
        checkCondition(
            "equality",
            conditionUserWhere(user) { it.name == "Ada" },
            "name",
            SqlBinaryOperator.Equal,
            "Ada",
        ),
        checkCondition(
            "greater-or-equal",
            conditionUserWhere(user) { it.age >= 18 },
            "age",
            SqlBinaryOperator.GreaterThanEqual,
            18,
        ),
        assertLike(
            "notLike",
            conditionUserWhere(user) { it.name notLike "%bot%" },
            "name",
            TransformerSafeValue("%bot%", "kotlin.String"),
            withNot = true
        ),
    )

    return checks.firstOrNull() ?: "OK"
}

fun assertLike(
    label: String,
    actual: CapturedCondition,
    fieldName: String,
    value: Any?,
    withNot: Boolean,
): String? {
    val like = actual.expr as? SqlExpr.Like
    val column = like?.expr as? SqlExpr.Column
    val parameter = (like?.pattern as? SqlExpr.Parameter)?.parameter as? SqlParameter.Named
    val failure = when {
        like == null -> "condition was ${actual.expr}"
        column?.columnName != fieldName -> "column was ${column?.columnName}"
        like.withNot != withNot -> "not was ${like.withNot}"
        parameter == null -> "pattern was ${like.pattern}"
        actual.parameters[parameter.name] != value -> "value was ${actual.parameters[parameter.name]}"
        else -> null
    }
    return if (failure == null) null else "Fail: $label $failure"
}
