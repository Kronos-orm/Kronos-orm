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

// Verifies no-argument string match properties read values from sourceValues.

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

@Table(name = "tb_no_arg_string_match")
data class NoArgStringMatchUser(
    var id: Int? = null,
    var name: String? = null,
    var pattern: String? = null,
) : KPojo

data class CapturedNoArgStringMatch(
    val expr: SqlExpr?,
    val parameters: Map<String, Any?>,
    val fields: Map<String, Field>
)

fun noArgStringWhere(user: NoArgStringMatchUser, block: ToFilter<NoArgStringMatchUser, Boolean?>): CapturedNoArgStringMatch {
    var result: CapturedNoArgStringMatch? = null
    user.afterFilter {
        sourceValues = user.toDataMap()
        block(it)
        result = CapturedNoArgStringMatch(sqlExpr, parameterValues.toMap(), parameterFields.toMap())
    }
    return result ?: CapturedNoArgStringMatch(null, emptyMap(), emptyMap())
}

fun noArgStringParameter(actual: CapturedNoArgStringMatch, expr: SqlExpr?): Any? {
    val parameter = expr as? SqlExpr.Parameter ?: return null
    val name = (parameter.parameter as? SqlParameter.Named)?.name ?: return null
    return toDatabaseParameterValue(CompilerTestDataSourceWrapper, actual.fields, name, actual.parameters[name], expandAsList = parameter.expandAsList)
}

fun expectLikeCondition(
    label: String,
    actual: CapturedNoArgStringMatch,
    fieldName: String,
    value: Any?,
    not: Boolean = false,
    escape: SqlExpr? = null,
): String? {
    val like = actual.expr as? SqlExpr.Like
    val column = like?.expr as? SqlExpr.Column
    return when {
        like == null -> "Fail: $label condition was ${actual.expr}"
        column?.columnName != fieldName -> "Fail: $label field was ${column?.columnName}"
        like.withNot != not -> "Fail: $label not was ${like.withNot}"
        like.escape != escape -> "Fail: $label escape was ${like.escape}"
        noArgStringParameter(actual, like.pattern) != value -> "Fail: $label value was ${noArgStringParameter(actual, like.pattern)}"
        else -> null
    }
}

fun expectRegexpCondition(
    label: String,
    actual: CapturedNoArgStringMatch,
    fieldName: String,
    value: Any?,
    operator: SqlBinaryOperator,
): String? {
    val binary = actual.expr as? SqlExpr.Binary
    val column = binary?.left as? SqlExpr.Column
    return when {
        binary == null -> "Fail: $label condition was ${actual.expr}"
        column?.columnName != fieldName -> "Fail: $label field was ${column?.columnName}"
        binary.operator != operator -> "Fail: $label operator was ${binary.operator}"
        noArgStringParameter(actual, binary.right) != value -> "Fail: $label value was ${noArgStringParameter(actual, binary.right)}"
        else -> null
    }
}

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val user = NoArgStringMatchUser(name = "A%_\\", pattern = "^A.*")
    val failures = listOfNotNull(
        expectLikeCondition("like", noArgStringWhere(user) { it.name.like }, "name", "A%_\\"),
        expectLikeCondition(
            "notLike",
            noArgStringWhere(user) { it.name.notLike },
            "name",
            "A%_\\",
            not = true
        ),
        expectLikeCondition(
            "startsWith",
            noArgStringWhere(user) { it.name.startsWith },
            "name",
            "A\\%\\_\\\\%",
            escape = SqlExpr.StringLiteral("\\")
        ),
        expectLikeCondition(
            "endsWith",
            noArgStringWhere(user) { it.name.endsWith },
            "name",
            "%A\\%\\_\\\\",
            escape = SqlExpr.StringLiteral("\\")
        ),
        expectLikeCondition(
            "contains",
            noArgStringWhere(user) { it.name.contains },
            "name",
            "%A\\%\\_\\\\%",
            escape = SqlExpr.StringLiteral("\\")
        ),
        expectRegexpCondition(
            "regexp",
            noArgStringWhere(user) { it.pattern.regexp },
            "pattern",
            "^A.*",
            SqlBinaryOperator.Regexp
        ),
        expectRegexpCondition(
            "notRegexp",
            noArgStringWhere(user) { it.pattern.notRegexp },
            "pattern",
            "^A.*",
            SqlBinaryOperator.NotRegexp
        ),
    )

    return failures.firstOrNull() ?: "OK"
}
