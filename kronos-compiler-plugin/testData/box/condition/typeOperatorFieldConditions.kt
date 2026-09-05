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

// Verifies cast-wrapped condition fields still lower to column expressions.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.KTableForCondition.Companion.afterFilter
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlParameter
import com.kotlinorm.types.ToFilter

@Table(name = "tb_condition_type_operator")
data class TypeOperatorConditionUser(
    var id: Int? = null,
    var name: String? = null,
    var age: Int? = null,
) : KPojo

data class TypeOperatorConditionCapture(
    val expr: SqlExpr?,
    val parameters: Map<String, Any?>
)

fun typeOperatorWhere(
    user: TypeOperatorConditionUser,
    block: ToFilter<TypeOperatorConditionUser, Boolean?>,
): TypeOperatorConditionCapture {
    var result: TypeOperatorConditionCapture? = null
    user.afterFilter {
        sourceValues = user.toDataMap()
        block(it)
        result = TypeOperatorConditionCapture(sqlExpr, parameterValues.toMap())
    }
    return result ?: TypeOperatorConditionCapture(null, emptyMap())
}

fun typeOperatorParameter(actual: TypeOperatorConditionCapture, expr: SqlExpr?): Any? {
    val name = ((expr as? SqlExpr.Parameter)?.parameter as? SqlParameter.Named)?.name ?: return null
    return actual.parameters[name]
}

fun expectTypeOperator(
    label: String,
    actual: TypeOperatorConditionCapture,
    column: String,
    operator: SqlBinaryOperator,
    value: Any?,
): String? {
    val binary = actual.expr as? SqlExpr.Binary
    val field = binary?.left as? SqlExpr.Column
    return when {
        binary == null -> "Fail: $label expr was ${actual.expr}"
        field?.columnName != column -> "Fail: $label column was ${field?.columnName}"
        binary.operator != operator -> "Fail: $label operator was ${binary.operator}"
        typeOperatorParameter(actual, binary.right) != value ->
            "Fail: $label value was ${typeOperatorParameter(actual, binary.right)}"
        else -> null
    }
}

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val user = TypeOperatorConditionUser(id = 1, name = "Ada", age = 36)
    val unsafeCast = typeOperatorWhere(user) { (it.name as String) == "Ada" }
    val nullableCast = typeOperatorWhere(user) { (it.id as Int?) == 1 }

    val failures = listOfNotNull(
        expectTypeOperator("unsafeCast", unsafeCast, "name", SqlBinaryOperator.Equal, "Ada"),
        expectTypeOperator("nullableCast", nullableCast, "id", SqlBinaryOperator.Equal, 1),
    )

    return failures.firstOrNull() ?: "OK"
}
