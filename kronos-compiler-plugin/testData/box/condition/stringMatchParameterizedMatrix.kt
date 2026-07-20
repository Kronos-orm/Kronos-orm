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

// Verifies parameterized string-match helpers and negated forms lower to exact SqlExpr shapes.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.KTableForCondition.Companion.afterFilter
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlParameter
import com.kotlinorm.types.ToFilter
import com.kotlinorm.utils.TransformerSafeValue
import kotlin.reflect.typeOf

@Table(name = "tb_condition_string_matrix")
data class StringMatrixUser(
    var id: Int? = null,
    var name: String? = null,
    var pattern: String? = null,
) : KPojo

data class CapturedStringMatrix(
    val expr: SqlExpr?,
    val parameters: Map<String, Any?>
)

fun stringMatrixWhere(user: StringMatrixUser, block: ToFilter<StringMatrixUser, Boolean?>): CapturedStringMatrix {
    var result: CapturedStringMatrix? = null
    user.afterFilter {
        sourceValues = user.toDataMap()
        block(it)
        result = CapturedStringMatrix(sqlExpr, parameterValues.toMap())
    }
    return result ?: CapturedStringMatrix(null, emptyMap())
}

fun stringMatrixParameter(actual: CapturedStringMatrix, expr: SqlExpr?): Any? {
    val name = ((expr as? SqlExpr.Parameter)?.parameter as? SqlParameter.Named)?.name ?: return null
    return actual.parameters[name]
}

fun expectParameterizedLike(
    label: String,
    actual: CapturedStringMatrix,
    value: Any?,
    not: Boolean,
    escape: SqlExpr? = null,
): String? {
    val like = actual.expr as? SqlExpr.Like
    val column = like?.expr as? SqlExpr.Column
    return when {
        like == null -> "Fail: $label condition was ${actual.expr}"
        column?.columnName != "name" -> "Fail: $label field was ${column?.columnName}"
        like.withNot != not -> "Fail: $label not was ${like.withNot}"
        like.escape != escape -> "Fail: $label escape was ${like.escape}"
        stringMatrixParameter(actual, like.pattern) != value ->
            "Fail: $label value was ${stringMatrixParameter(actual, like.pattern)}"
        else -> null
    }
}

fun expectParameterizedRegexp(
    label: String,
    actual: CapturedStringMatrix,
    operator: SqlBinaryOperator,
    value: Any?,
): String? {
    val binary = actual.expr as? SqlExpr.Binary
    val column = binary?.left as? SqlExpr.Column
    return when {
        binary == null -> "Fail: $label condition was ${actual.expr}"
        column?.columnName != "pattern" -> "Fail: $label field was ${column?.columnName}"
        binary.operator != operator -> "Fail: $label operator was ${binary.operator}"
        stringMatrixParameter(actual, binary.right) != value ->
            "Fail: $label value was ${stringMatrixParameter(actual, binary.right)}"
        else -> null
    }
}

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val user = StringMatrixUser(name = "Ada", pattern = "^A.*")
    val failures = listOfNotNull(
        expectParameterizedLike(
            "like",
            stringMatrixWhere(user) { it.name like "A%" },
            TransformerSafeValue("A%", typeOf<String>()),
            not = false
        ),
        expectParameterizedLike(
            "notLike",
            stringMatrixWhere(user) { it.name notLike "A%" },
            TransformerSafeValue("A%", typeOf<String>()),
            not = true
        ),
        expectParameterizedLike(
            "startsWith",
            stringMatrixWhere(user) { it.name.startsWith("A%_\\") },
            TransformerSafeValue("A\\%\\_\\\\%", typeOf<String>()),
            not = false,
            escape = SqlExpr.StringLiteral("\\")
        ),
        expectParameterizedLike(
            "negatedLike",
            stringMatrixWhere(user) { !(it.name like "A%") },
            TransformerSafeValue("A%", typeOf<String>()),
            not = true
        ),
        expectParameterizedLike(
            "endsWith",
            stringMatrixWhere(user) { it.name.endsWith("%_a\\") },
            TransformerSafeValue("%\\%\\_a\\\\", typeOf<String>()),
            not = false,
            escape = SqlExpr.StringLiteral("\\")
        ),
        expectParameterizedLike(
            "contains",
            stringMatrixWhere(user) { it.name.contains("%d_\\") },
            TransformerSafeValue("%\\%d\\_\\\\%", typeOf<String>()),
            not = false,
            escape = SqlExpr.StringLiteral("\\")
        ),
        expectParameterizedRegexp(
            "regexp",
            stringMatrixWhere(user) { it.pattern regexp "^A.*" },
            SqlBinaryOperator.Regexp,
            "^A.*"
        ),
        expectParameterizedRegexp(
            "notRegexp",
            stringMatrixWhere(user) { it.pattern notRegexp "^B.*" },
            SqlBinaryOperator.NotRegexp,
            "^B.*"
        ),
        expectParameterizedRegexp(
            "negatedRegexp",
            stringMatrixWhere(user) { !(it.pattern regexp "^A.*") },
            SqlBinaryOperator.NotRegexp,
            "^A.*"
        ),
    )

    return failures.firstOrNull() ?: "OK"
}
