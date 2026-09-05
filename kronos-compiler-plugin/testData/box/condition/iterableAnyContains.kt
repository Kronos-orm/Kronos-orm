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

// Verifies Iterable.any predicates lower to an ordered OR tree with independent LIKE parameters.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableForCondition.Companion.afterFilter
import com.kotlinorm.compiler.support.CompilerTestDataSourceWrapper
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlParameter
import com.kotlinorm.types.ToFilter
import com.kotlinorm.utils.toDatabaseParameterValue

@Table(name = "tb_iterable_any_contains")
data class IterableAnyContainsUser(
    var id: Int? = null,
    var name: String? = null,
) : KPojo

data class CapturedIterableAnyContains(
    val expr: SqlExpr?,
    val parameters: Map<String, Any?>,
    val fields: Map<String, Field>,
)

fun iterableAnyContainsWhere(
    user: IterableAnyContainsUser,
    block: ToFilter<IterableAnyContainsUser, Boolean?>,
): CapturedIterableAnyContains {
    var result: CapturedIterableAnyContains? = null
    user.afterFilter {
        sourceValues = user.toDataMap()
        block(it)
        result = CapturedIterableAnyContains(sqlExpr, parameterValues.toMap(), parameterFields.toMap())
    }
    return result ?: CapturedIterableAnyContains(null, emptyMap(), emptyMap())
}

fun iterableAnyContainsParameter(actual: CapturedIterableAnyContains, expr: SqlExpr?): Any? {
    val parameter = expr as? SqlExpr.Parameter ?: return null
    val name = (parameter.parameter as? SqlParameter.Named)?.name ?: return null
    return toDatabaseParameterValue(
        CompilerTestDataSourceWrapper,
        actual.fields,
        name,
        actual.parameters[name],
        expandAsList = parameter.expandAsList,
    )
}

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val user = IterableAnyContainsUser(id = 1, name = "Ada")
    val keywords = listOf("Ada", "B%_\\")
    var receiverEvaluations = 0
    fun nextKeywords(): List<String> {
        receiverEvaluations += 1
        return keywords
    }
    val actual = iterableAnyContainsWhere(user) { nextKeywords().any { keyword -> it.name.contains(keyword) } }
    val root = actual.expr as? SqlExpr.Binary
    val first = root?.left as? SqlExpr.Like
    val second = root?.right as? SqlExpr.Like
    val firstColumn = first?.expr as? SqlExpr.Column
    val secondColumn = second?.expr as? SqlExpr.Column

    return when {
        root?.operator != SqlBinaryOperator.Or -> "Fail: root operator was ${root?.operator}"
        firstColumn?.columnName != "name" -> "Fail: first field was ${firstColumn?.columnName}"
        secondColumn?.columnName != "name" -> "Fail: second field was ${secondColumn?.columnName}"
        first?.withNot == true -> "Fail: first predicate was negated"
        second?.withNot == true -> "Fail: second predicate was negated"
        iterableAnyContainsParameter(actual, first?.pattern) != "%Ada%" ->
            "Fail: first parameter was ${iterableAnyContainsParameter(actual, first?.pattern)}"
        iterableAnyContainsParameter(actual, second?.pattern) != "%B\\%\\_\\\\%" ->
            "Fail: second parameter was ${iterableAnyContainsParameter(actual, second?.pattern)}"
        actual.parameters.keys.toList() != listOf("name", "name@1") ->
            "Fail: parameter names were ${actual.parameters.keys}"
        receiverEvaluations != 1 -> "Fail: receiver was evaluated $receiverEvaluations times"
        else -> "OK"
    }
}
