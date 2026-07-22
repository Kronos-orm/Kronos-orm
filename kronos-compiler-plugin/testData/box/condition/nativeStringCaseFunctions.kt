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

// Verifies native Kotlin string-case calls map only from source fields and respect runtime value boundaries.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableForCondition.Companion.afterFilter
import com.kotlinorm.compiler.support.CompilerTestDataSourceWrapper
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlInRightOperand
import com.kotlinorm.syntax.expr.SqlParameter
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.types.ToFilter
import com.kotlinorm.utils.toDatabaseParameterValue

@Table(name = "tb_native_string_case")
data class NativeStringCaseUser(
    var id: Int? = null,
    var userName: String? = null,
    var initial: Char? = null,
) : KPojo

@Table(name = "tb_native_string_case_lookup")
data class NativeStringCaseLookup(
    var id: Int? = null,
    var userName: String? = null,
) : KPojo

data class CapturedNativeStringCase(
    val expr: SqlExpr?,
    val parameters: Map<String, Any?>,
    val fields: Map<String, Field>,
)

fun nativeStringCaseWhere(
    user: NativeStringCaseUser,
    block: ToFilter<NativeStringCaseUser, Boolean?>,
): CapturedNativeStringCase {
    var result: CapturedNativeStringCase? = null
    user.afterFilter {
        sourceValues = user.toDataMap()
        block(it)
        result = CapturedNativeStringCase(sqlExpr, parameterValues.toMap(), parameterFields.toMap())
    }
    return result ?: CapturedNativeStringCase(null, emptyMap(), emptyMap())
}

fun nativeStringCaseParameter(actual: CapturedNativeStringCase, expr: SqlExpr?): Any? {
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

fun expectNativeStringCaseFunction(label: String, expectedName: String, expr: SqlExpr?): String? {
    val function = expr as? SqlExpr.Function
    val column = function?.args?.singleOrNull() as? SqlExpr.Column
    return when {
        function == null -> "Fail: $label expression was $expr"
        function.name.last != expectedName -> "Fail: $label function was ${function.name.last}"
        column?.columnName != "user_name" -> "Fail: $label field was ${column?.columnName}"
        else -> null
    }
}

fun expectNativeStringCaseEquality(
    label: String,
    actual: CapturedNativeStringCase,
    functionName: String,
    value: String,
): String? {
    val binary = actual.expr as? SqlExpr.Binary ?: return "Fail: $label condition was ${actual.expr}"
    expectNativeStringCaseFunction(label, functionName, binary.left)?.let { return it }
    return when {
        binary.operator != SqlBinaryOperator.Equal -> "Fail: $label operator was ${binary.operator}"
        binary.right !is SqlExpr.Parameter -> "Fail: $label value expression was ${binary.right}"
        nativeStringCaseParameter(actual, binary.right) != value ->
            "Fail: $label value was ${nativeStringCaseParameter(actual, binary.right)}"
        else -> null
    }
}

fun expectNativeStringCaseLike(
    label: String,
    actual: CapturedNativeStringCase,
    functionName: String,
    value: String,
    escape: SqlExpr?,
): String? {
    val like = actual.expr as? SqlExpr.Like ?: return "Fail: $label condition was ${actual.expr}"
    expectNativeStringCaseFunction(label, functionName, like.expr)?.let { return it }
    return when {
        like.withNot != false -> "Fail: $label not was ${like.withNot}"
        like.escape != escape -> "Fail: $label escape was ${like.escape}"
        like.pattern !is SqlExpr.Parameter -> "Fail: $label pattern expression was ${like.pattern}"
        nativeStringCaseParameter(actual, like.pattern) != value ->
            "Fail: $label value was ${nativeStringCaseParameter(actual, like.pattern)}"
        else -> null
    }
}

fun expectNativeStringCaseRuntimeValue(
    label: String,
    actual: CapturedNativeStringCase,
    value: String,
): String? {
    val binary = actual.expr as? SqlExpr.Binary ?: return "Fail: $label condition was ${actual.expr}"
    val column = binary.left as? SqlExpr.Column
    return when {
        column?.columnName != "user_name" -> "Fail: $label field was $column"
        binary.operator != SqlBinaryOperator.Equal -> "Fail: $label operator was ${binary.operator}"
        binary.right is SqlExpr.Function -> "Fail: $label became SQL function ${binary.right}"
        binary.right !is SqlExpr.Parameter -> "Fail: $label value expression was ${binary.right}"
        nativeStringCaseParameter(actual, binary.right) != value ->
            "Fail: $label value was ${nativeStringCaseParameter(actual, binary.right)}"
        else -> null
    }
}

fun expectNativeStringCaseIn(
    label: String,
    actual: CapturedNativeStringCase,
    functionName: String,
    values: List<String>,
): String? {
    val condition = actual.expr as? SqlExpr.In ?: return "Fail: $label condition was ${actual.expr}"
    expectNativeStringCaseFunction(label, functionName, condition.expr)?.let { return it }
    val parameter = (condition.`in` as? SqlInRightOperand.Values)?.items?.singleOrNull() as? SqlExpr.Parameter
    return when {
        condition.withNot -> "Fail: $label not was ${condition.withNot}"
        parameter == null -> "Fail: $label values were ${condition.`in`}"
        !parameter.expandAsList -> "Fail: $label list expansion was ${parameter.expandAsList}"
        nativeStringCaseParameter(actual, parameter) != values ->
            "Fail: $label values were ${nativeStringCaseParameter(actual, parameter)}"
        else -> null
    }
}

fun expectNativeStringCaseSubqueryIn(
    label: String,
    actual: CapturedNativeStringCase,
    functionName: String,
): String? {
    val condition = actual.expr as? SqlExpr.In ?: return "Fail: $label condition was ${actual.expr}"
    expectNativeStringCaseFunction(label, functionName, condition.expr)?.let { return it }
    val subquery = condition.`in` as? SqlInRightOperand.Subquery
    return when {
        condition.withNot -> "Fail: $label not was ${condition.withNot}"
        subquery?.query !is SqlQuery.Select -> "Fail: $label query was ${subquery?.query}"
        else -> null
    }
}

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val user = NativeStringCaseUser(userName = "Ada", initial = 'A')
    val name = "Ada"
    val runtimeName = "Ada"
    val wildcardPattern = "A%_\\"
    val escape = SqlExpr.StringLiteral("\\")

    val uppercaseEquality = nativeStringCaseWhere(user) {
        it.userName?.uppercase() == "ADA"
    }
    val lowercaseEquality = nativeStringCaseWhere(user) {
        it.userName?.lowercase() == "ada"
    }
    val lowercaseContains = nativeStringCaseWhere(user) {
        it.userName?.lowercase().contains(runtimeName.lowercase())
    }
    val uppercaseContains = nativeStringCaseWhere(user) {
        it.userName?.uppercase().contains(runtimeName.uppercase())
    }
    val lowercaseLike = nativeStringCaseWhere(user) {
        it.userName?.lowercase() like wildcardPattern
    }
    val uppercaseLike = nativeStringCaseWhere(user) {
        it.userName?.uppercase() like wildcardPattern
    }
    val lowercaseStartsWith = nativeStringCaseWhere(user) {
        it.userName?.lowercase().startsWith(runtimeName.lowercase())
    }
    val uppercaseStartsWith = nativeStringCaseWhere(user) {
        it.userName?.uppercase().startsWith(runtimeName.uppercase())
    }
    val lowercaseEndsWith = nativeStringCaseWhere(user) {
        it.userName?.lowercase().endsWith(runtimeName.lowercase())
    }
    val uppercaseEndsWith = nativeStringCaseWhere(user) {
        it.userName?.uppercase().endsWith(runtimeName.uppercase())
    }
    val lowercaseStringContains = nativeStringCaseWhere(user) {
        runtimeName.lowercase() in it.userName?.lowercase()
    }
    val uppercaseStringContains = nativeStringCaseWhere(user) {
        runtimeName.uppercase() in it.userName?.uppercase()
    }
    val lowercaseValues = listOf("ada", "bob")
    val lowercaseCollectionIn = nativeStringCaseWhere(user) {
        it.userName?.lowercase() in lowercaseValues
    }
    val uppercaseValues = listOf("ADA", "BOB")
    val uppercaseCollectionContains = nativeStringCaseWhere(user) {
        uppercaseValues.contains(it.userName?.uppercase())
    }
    val uppercaseSelectable = NativeStringCaseLookup().select { lookup -> lookup.userName }
    val uppercaseSelectableContains = nativeStringCaseWhere(user) {
        uppercaseSelectable.contains(it.userName?.uppercase())
    }
    val localLowercase = nativeStringCaseWhere(user) {
        it.userName == name.lowercase()
    }
    val charLowercase = nativeStringCaseWhere(user) {
        it.userName == it.initial?.lowercase()
    }
    val valueBeforeLowercase = nativeStringCaseWhere(user) {
        it.userName == it.userName.value?.lowercase()
    }
    val valueAfterUppercase = nativeStringCaseWhere(user) {
        it.userName == it.userName?.uppercase().value
    }

    val failures = listOfNotNull(
        expectNativeStringCaseEquality("uppercase equality", uppercaseEquality, "UPPER", "ADA"),
        expectNativeStringCaseEquality("lowercase equality", lowercaseEquality, "LOWER", "ada"),
        expectNativeStringCaseLike("lowercase contains", lowercaseContains, "LOWER", "%ada%", escape),
        expectNativeStringCaseLike("uppercase contains", uppercaseContains, "UPPER", "%ADA%", escape),
        expectNativeStringCaseLike("lowercase like", lowercaseLike, "LOWER", wildcardPattern, escape = null),
        expectNativeStringCaseLike("uppercase like", uppercaseLike, "UPPER", wildcardPattern, escape = null),
        expectNativeStringCaseLike("lowercase startsWith", lowercaseStartsWith, "LOWER", "ada%", escape),
        expectNativeStringCaseLike("uppercase startsWith", uppercaseStartsWith, "UPPER", "ADA%", escape),
        expectNativeStringCaseLike("lowercase endsWith", lowercaseEndsWith, "LOWER", "%ada", escape),
        expectNativeStringCaseLike("uppercase endsWith", uppercaseEndsWith, "UPPER", "%ADA", escape),
        expectNativeStringCaseLike("lowercase string contains", lowercaseStringContains, "LOWER", "%ada%", escape),
        expectNativeStringCaseLike("uppercase string contains", uppercaseStringContains, "UPPER", "%ADA%", escape),
        expectNativeStringCaseIn("lowercase collection in", lowercaseCollectionIn, "LOWER", lowercaseValues),
        expectNativeStringCaseIn("uppercase collection contains", uppercaseCollectionContains, "UPPER", uppercaseValues),
        expectNativeStringCaseSubqueryIn("uppercase selectable contains", uppercaseSelectableContains, "UPPER"),
        expectNativeStringCaseRuntimeValue("local lowercase", localLowercase, "ada"),
        expectNativeStringCaseRuntimeValue("char lowercase", charLowercase, "a"),
        expectNativeStringCaseRuntimeValue("value-before lowercase", valueBeforeLowercase, "ada"),
        expectNativeStringCaseRuntimeValue("value-after uppercase", valueAfterUppercase, "ADA"),
    )

    return failures.firstOrNull() ?: "OK"
}
