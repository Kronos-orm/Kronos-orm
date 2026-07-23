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

// Verifies LOWER and UPPER function expressions support equality, contains, and caller-supplied LIKE patterns.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableForCondition.Companion.afterFilter
import com.kotlinorm.compiler.support.CompilerTestDataSourceWrapper
import com.kotlinorm.functions.bundled.exts.StringFunctions.lower
import com.kotlinorm.functions.bundled.exts.StringFunctions.upper
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlInRightOperand
import com.kotlinorm.syntax.expr.SqlParameter
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.types.ToFilter
import com.kotlinorm.utils.toDatabaseParameterValue

@Table(name = "tb_lower_function_string_match")
data class LowerFunctionStringMatchUser(
    var id: Int? = null,
    var userName: String? = null,
) : KPojo

@Table(name = "tb_lower_function_string_match_lookup")
data class LowerFunctionStringMatchLookup(
    var id: Int? = null,
    var userName: String? = null,
) : KPojo

data class CapturedLowerFunctionStringMatch(
    val expr: SqlExpr?,
    val parameters: Map<String, Any?>,
    val fields: Map<String, Field>,
)

fun lowerFunctionStringMatchWhere(
    user: LowerFunctionStringMatchUser,
    block: ToFilter<LowerFunctionStringMatchUser, Boolean?>,
): CapturedLowerFunctionStringMatch {
    var result: CapturedLowerFunctionStringMatch? = null
    user.afterFilter {
        sourceValues = user.toDataMap()
        block(it)
        result = CapturedLowerFunctionStringMatch(sqlExpr, parameterValues.toMap(), parameterFields.toMap())
    }
    return result ?: CapturedLowerFunctionStringMatch(null, emptyMap(), emptyMap())
}

fun lowerFunctionStringMatchParameter(actual: CapturedLowerFunctionStringMatch, expr: SqlExpr?): Any? {
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

fun expectCaseFunction(label: String, expectedName: String, expr: SqlExpr?): String? {
    val function = expr as? SqlExpr.Function
    val column = function?.args?.singleOrNull() as? SqlExpr.Column
    return when {
        function == null -> "Fail: $label expression was $expr"
        function.name.last != expectedName -> "Fail: $label function was ${function.name.last}"
        column?.columnName != "user_name" -> "Fail: $label field was ${column?.columnName}"
        else -> null
    }
}

inline fun expectLowerFunctionStringMatch(condition: Boolean, message: () -> String): String? =
    if (condition) null else "Fail: ${message()}"

fun expectCaseFunctionIn(
    label: String,
    actual: CapturedLowerFunctionStringMatch,
    functionName: String,
    values: List<String>,
): String? {
    val condition = actual.expr as? SqlExpr.In ?: return "Fail: $label condition was ${actual.expr}"
    expectCaseFunction(label, functionName, condition.expr)?.let { return it }
    val parameter = (condition.`in` as? SqlInRightOperand.Values)?.items?.singleOrNull() as? SqlExpr.Parameter
    return when {
        condition.withNot -> "Fail: $label not was ${condition.withNot}"
        parameter == null -> "Fail: $label values were ${condition.`in`}"
        !parameter.expandAsList -> "Fail: $label list expansion was ${parameter.expandAsList}"
        lowerFunctionStringMatchParameter(actual, parameter) != values ->
            "Fail: $label values were ${lowerFunctionStringMatchParameter(actual, parameter)}"
        else -> null
    }
}

fun expectCaseFunctionSubqueryIn(
    label: String,
    actual: CapturedLowerFunctionStringMatch,
    functionName: String,
): String? {
    val condition = actual.expr as? SqlExpr.In ?: return "Fail: $label condition was ${actual.expr}"
    expectCaseFunction(label, functionName, condition.expr)?.let { return it }
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

    val user = LowerFunctionStringMatchUser(userName = "Ada")
    val userName = "ADA"
    val wildcardPattern = "A%_\\"

    val equality = lowerFunctionStringMatchWhere(user) {
        f.lower(it.userName) == userName.lowercase()
    }
    val equalityExpr = equality.expr as? SqlExpr.Binary

    val contains = lowerFunctionStringMatchWhere(user) {
        f.lower(it.userName).contains(userName.lowercase())
    }
    val containsExpr = contains.expr as? SqlExpr.Like

    val like = lowerFunctionStringMatchWhere(user) {
        f.lower(it.userName) like wildcardPattern
    }
    val likeExpr = like.expr as? SqlExpr.Like

    val startsWith = lowerFunctionStringMatchWhere(user) {
        f.lower(it.userName).startsWith(userName.lowercase())
    }
    val startsWithExpr = startsWith.expr as? SqlExpr.Like

    val lowerEndsWith = lowerFunctionStringMatchWhere(user) {
        f.lower(it.userName).endsWith(userName.lowercase())
    }
    val lowerEndsWithExpr = lowerEndsWith.expr as? SqlExpr.Like

    val lowerStringContains = lowerFunctionStringMatchWhere(user) {
        userName.lowercase() in f.lower(it.userName)
    }
    val lowerStringContainsExpr = lowerStringContains.expr as? SqlExpr.Like

    val upperEquality = lowerFunctionStringMatchWhere(user) {
        f.upper(it.userName) == userName.uppercase()
    }
    val upperEqualityExpr = upperEquality.expr as? SqlExpr.Binary

    val upperContains = lowerFunctionStringMatchWhere(user) {
        f.upper(it.userName).contains(userName.uppercase())
    }
    val upperContainsExpr = upperContains.expr as? SqlExpr.Like

    val upperLike = lowerFunctionStringMatchWhere(user) {
        f.upper(it.userName) like wildcardPattern
    }
    val upperLikeExpr = upperLike.expr as? SqlExpr.Like

    val upperStartsWith = lowerFunctionStringMatchWhere(user) {
        f.upper(it.userName).startsWith(userName.uppercase())
    }
    val upperStartsWithExpr = upperStartsWith.expr as? SqlExpr.Like

    val upperEndsWith = lowerFunctionStringMatchWhere(user) {
        f.upper(it.userName).endsWith(userName.uppercase())
    }
    val upperEndsWithExpr = upperEndsWith.expr as? SqlExpr.Like

    val upperStringContains = lowerFunctionStringMatchWhere(user) {
        userName.uppercase() in f.upper(it.userName)
    }
    val upperStringContainsExpr = upperStringContains.expr as? SqlExpr.Like

    val lowerValues = listOf("ada", "bob")
    val lowerCollectionIn = lowerFunctionStringMatchWhere(user) {
        f.lower(it.userName) in lowerValues
    }
    val upperValues = listOf("ADA", "BOB")
    val upperCollectionContains = lowerFunctionStringMatchWhere(user) {
        upperValues.contains(f.upper(it.userName))
    }
    val lowerSelectableIn = lowerFunctionStringMatchWhere(user) {
        f.lower(it.userName) in LowerFunctionStringMatchLookup().select { lookup -> lookup.userName }
    }

    val failures = listOfNotNull(
        expectCaseFunction("lower equality", "LOWER", equalityExpr?.left),
        expectLowerFunctionStringMatch(equalityExpr?.operator == SqlBinaryOperator.Equal) {
            "equality operator was ${equalityExpr?.operator}"
        },
        expectLowerFunctionStringMatch(equalityExpr?.right is SqlExpr.Parameter) {
            "equality value expression was ${equalityExpr?.right}"
        },
        expectLowerFunctionStringMatch(lowerFunctionStringMatchParameter(equality, equalityExpr?.right) == "ada") {
            "equality value was ${lowerFunctionStringMatchParameter(equality, equalityExpr?.right)}"
        },
        expectCaseFunction("lower contains", "LOWER", containsExpr?.expr),
        expectLowerFunctionStringMatch(containsExpr?.withNot == false) {
            "contains not was ${containsExpr?.withNot}"
        },
        expectLowerFunctionStringMatch(containsExpr?.escape == SqlExpr.StringLiteral("\\")) {
            "contains escape was ${containsExpr?.escape}"
        },
        expectLowerFunctionStringMatch(containsExpr?.pattern is SqlExpr.Parameter) {
            "contains pattern expression was ${containsExpr?.pattern}"
        },
        expectLowerFunctionStringMatch(lowerFunctionStringMatchParameter(contains, containsExpr?.pattern) == "%ada%") {
            "contains value was ${lowerFunctionStringMatchParameter(contains, containsExpr?.pattern)}"
        },
        expectCaseFunction("lower like", "LOWER", likeExpr?.expr),
        expectLowerFunctionStringMatch(likeExpr?.withNot == false) {
            "like not was ${likeExpr?.withNot}"
        },
        expectLowerFunctionStringMatch(likeExpr?.escape == null) {
            "like escape was ${likeExpr?.escape}"
        },
        expectLowerFunctionStringMatch(likeExpr?.pattern is SqlExpr.Parameter) {
            "like pattern expression was ${likeExpr?.pattern}"
        },
        expectLowerFunctionStringMatch(lowerFunctionStringMatchParameter(like, likeExpr?.pattern) == wildcardPattern) {
            "like value was ${lowerFunctionStringMatchParameter(like, likeExpr?.pattern)}"
        },
        expectCaseFunction("lower startsWith", "LOWER", startsWithExpr?.expr),
        expectLowerFunctionStringMatch(startsWithExpr?.withNot == false) {
            "startsWith not was ${startsWithExpr?.withNot}"
        },
        expectLowerFunctionStringMatch(startsWithExpr?.escape == SqlExpr.StringLiteral("\\")) {
            "startsWith escape was ${startsWithExpr?.escape}"
        },
        expectLowerFunctionStringMatch(startsWithExpr?.pattern is SqlExpr.Parameter) {
            "startsWith pattern expression was ${startsWithExpr?.pattern}"
        },
        expectLowerFunctionStringMatch(
            lowerFunctionStringMatchParameter(startsWith, startsWithExpr?.pattern) == "ada%"
        ) {
            "startsWith value was ${lowerFunctionStringMatchParameter(startsWith, startsWithExpr?.pattern)}"
        },
        expectCaseFunction("lower endsWith", "LOWER", lowerEndsWithExpr?.expr),
        expectLowerFunctionStringMatch(lowerEndsWithExpr?.withNot == false) {
            "lower endsWith not was ${lowerEndsWithExpr?.withNot}"
        },
        expectLowerFunctionStringMatch(lowerEndsWithExpr?.escape == SqlExpr.StringLiteral("\\")) {
            "lower endsWith escape was ${lowerEndsWithExpr?.escape}"
        },
        expectLowerFunctionStringMatch(lowerEndsWithExpr?.pattern is SqlExpr.Parameter) {
            "lower endsWith pattern expression was ${lowerEndsWithExpr?.pattern}"
        },
        expectLowerFunctionStringMatch(
            lowerFunctionStringMatchParameter(lowerEndsWith, lowerEndsWithExpr?.pattern) == "%ada"
        ) {
            "lower endsWith value was ${lowerFunctionStringMatchParameter(lowerEndsWith, lowerEndsWithExpr?.pattern)}"
        },
        expectCaseFunction("lower string contains", "LOWER", lowerStringContainsExpr?.expr),
        expectLowerFunctionStringMatch(lowerStringContainsExpr?.withNot == false) {
            "lower string contains not was ${lowerStringContainsExpr?.withNot}"
        },
        expectLowerFunctionStringMatch(lowerStringContainsExpr?.escape == SqlExpr.StringLiteral("\\")) {
            "lower string contains escape was ${lowerStringContainsExpr?.escape}"
        },
        expectLowerFunctionStringMatch(lowerStringContainsExpr?.pattern is SqlExpr.Parameter) {
            "lower string contains pattern expression was ${lowerStringContainsExpr?.pattern}"
        },
        expectLowerFunctionStringMatch(
            lowerFunctionStringMatchParameter(lowerStringContains, lowerStringContainsExpr?.pattern) == "%ada%"
        ) {
            "lower string contains value was ${lowerFunctionStringMatchParameter(lowerStringContains, lowerStringContainsExpr?.pattern)}"
        },
        expectCaseFunction("upper equality", "UPPER", upperEqualityExpr?.left),
        expectLowerFunctionStringMatch(upperEqualityExpr?.operator == SqlBinaryOperator.Equal) {
            "upper equality operator was ${upperEqualityExpr?.operator}"
        },
        expectLowerFunctionStringMatch(upperEqualityExpr?.right is SqlExpr.Parameter) {
            "upper equality value expression was ${upperEqualityExpr?.right}"
        },
        expectLowerFunctionStringMatch(
            lowerFunctionStringMatchParameter(upperEquality, upperEqualityExpr?.right) == "ADA"
        ) {
            "upper equality value was ${lowerFunctionStringMatchParameter(upperEquality, upperEqualityExpr?.right)}"
        },
        expectCaseFunction("upper contains", "UPPER", upperContainsExpr?.expr),
        expectLowerFunctionStringMatch(upperContainsExpr?.withNot == false) {
            "upper contains not was ${upperContainsExpr?.withNot}"
        },
        expectLowerFunctionStringMatch(upperContainsExpr?.escape == SqlExpr.StringLiteral("\\")) {
            "upper contains escape was ${upperContainsExpr?.escape}"
        },
        expectLowerFunctionStringMatch(upperContainsExpr?.pattern is SqlExpr.Parameter) {
            "upper contains pattern expression was ${upperContainsExpr?.pattern}"
        },
        expectLowerFunctionStringMatch(
            lowerFunctionStringMatchParameter(upperContains, upperContainsExpr?.pattern) == "%ADA%"
        ) {
            "upper contains value was ${lowerFunctionStringMatchParameter(upperContains, upperContainsExpr?.pattern)}"
        },
        expectCaseFunction("upper like", "UPPER", upperLikeExpr?.expr),
        expectLowerFunctionStringMatch(upperLikeExpr?.withNot == false) {
            "upper like not was ${upperLikeExpr?.withNot}"
        },
        expectLowerFunctionStringMatch(upperLikeExpr?.escape == null) {
            "upper like escape was ${upperLikeExpr?.escape}"
        },
        expectLowerFunctionStringMatch(upperLikeExpr?.pattern is SqlExpr.Parameter) {
            "upper like pattern expression was ${upperLikeExpr?.pattern}"
        },
        expectLowerFunctionStringMatch(
            lowerFunctionStringMatchParameter(upperLike, upperLikeExpr?.pattern) == wildcardPattern
        ) {
            "upper like value was ${lowerFunctionStringMatchParameter(upperLike, upperLikeExpr?.pattern)}"
        },
        expectCaseFunction("upper startsWith", "UPPER", upperStartsWithExpr?.expr),
        expectLowerFunctionStringMatch(upperStartsWithExpr?.withNot == false) {
            "upper startsWith not was ${upperStartsWithExpr?.withNot}"
        },
        expectLowerFunctionStringMatch(upperStartsWithExpr?.escape == SqlExpr.StringLiteral("\\")) {
            "upper startsWith escape was ${upperStartsWithExpr?.escape}"
        },
        expectLowerFunctionStringMatch(upperStartsWithExpr?.pattern is SqlExpr.Parameter) {
            "upper startsWith pattern expression was ${upperStartsWithExpr?.pattern}"
        },
        expectLowerFunctionStringMatch(
            lowerFunctionStringMatchParameter(upperStartsWith, upperStartsWithExpr?.pattern) == "ADA%"
        ) {
            "upper startsWith value was ${lowerFunctionStringMatchParameter(upperStartsWith, upperStartsWithExpr?.pattern)}"
        },
        expectCaseFunction("upper endsWith", "UPPER", upperEndsWithExpr?.expr),
        expectLowerFunctionStringMatch(upperEndsWithExpr?.withNot == false) {
            "upper endsWith not was ${upperEndsWithExpr?.withNot}"
        },
        expectLowerFunctionStringMatch(upperEndsWithExpr?.escape == SqlExpr.StringLiteral("\\")) {
            "upper endsWith escape was ${upperEndsWithExpr?.escape}"
        },
        expectLowerFunctionStringMatch(upperEndsWithExpr?.pattern is SqlExpr.Parameter) {
            "upper endsWith pattern expression was ${upperEndsWithExpr?.pattern}"
        },
        expectLowerFunctionStringMatch(
            lowerFunctionStringMatchParameter(upperEndsWith, upperEndsWithExpr?.pattern) == "%ADA"
        ) {
            "upper endsWith value was ${lowerFunctionStringMatchParameter(upperEndsWith, upperEndsWithExpr?.pattern)}"
        },
        expectCaseFunction("upper string contains", "UPPER", upperStringContainsExpr?.expr),
        expectLowerFunctionStringMatch(upperStringContainsExpr?.withNot == false) {
            "upper string contains not was ${upperStringContainsExpr?.withNot}"
        },
        expectLowerFunctionStringMatch(upperStringContainsExpr?.escape == SqlExpr.StringLiteral("\\")) {
            "upper string contains escape was ${upperStringContainsExpr?.escape}"
        },
        expectLowerFunctionStringMatch(upperStringContainsExpr?.pattern is SqlExpr.Parameter) {
            "upper string contains pattern expression was ${upperStringContainsExpr?.pattern}"
        },
        expectLowerFunctionStringMatch(
            lowerFunctionStringMatchParameter(upperStringContains, upperStringContainsExpr?.pattern) == "%ADA%"
        ) {
            "upper string contains value was ${lowerFunctionStringMatchParameter(upperStringContains, upperStringContainsExpr?.pattern)}"
        },
        expectCaseFunctionIn("lower collection in", lowerCollectionIn, "LOWER", lowerValues),
        expectCaseFunctionIn("upper collection contains", upperCollectionContains, "UPPER", upperValues),
        expectCaseFunctionSubqueryIn("lower selectable in", lowerSelectableIn, "LOWER"),
    )

    return failures.firstOrNull() ?: "OK"
}
