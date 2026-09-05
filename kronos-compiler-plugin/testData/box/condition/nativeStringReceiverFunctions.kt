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

// Verifies native Kotlin string receiver calls lower to complete SQL expressions in condition DSL blocks.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.KTableForCondition.Companion.afterFilter
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.syntax.SqlIdentifier
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlBuiltinFunction
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlParameter
import com.kotlinorm.types.ToFilter

@Table(name = "tb_native_string_receiver_condition")
data class NativeStringReceiverConditionUser(
    var id: Int? = null,
    var requiredName: String = "",
    var nullableName: String? = null,
) : KPojo

data class NativeStringReceiverConditionCapture(
    val expr: SqlExpr?,
    val parameters: Map<String, Any?>,
)

data class NativeStringReceiverConditionCase(
    val label: String,
    val actual: NativeStringReceiverConditionCapture,
    val expected: NativeStringReceiverConditionCapture,
)

fun NativeStringReceiverConditionUser.captureNativeStringReceiverCondition(
    block: ToFilter<NativeStringReceiverConditionUser, Boolean?>,
): NativeStringReceiverConditionCapture {
    var result: NativeStringReceiverConditionCapture? = null
    afterFilter {
        sourceValues = toDataMap()
        block(it)
        result = NativeStringReceiverConditionCapture(sqlExpr, parameterValues.toMap())
    }
    return result ?: error("condition block did not run")
}

fun nativeStringReceiverColumn(columnName: String = "required_name"): SqlExpr.Column =
    SqlExpr.Column(columnName = columnName)

fun nativeStringReceiverFunction(
    name: String,
    builtin: SqlBuiltinFunction,
    vararg args: SqlExpr,
): SqlExpr.Function = SqlExpr.Function(
    name = SqlIdentifier.of(name),
    args = args.toList(),
    builtinFunction = builtin,
)

fun nativeStringReceiverLength(columnName: String = "required_name"): SqlExpr.Function =
    nativeStringReceiverFunction("LENGTH", SqlBuiltinFunction.Length, nativeStringReceiverColumn(columnName))

fun nativeStringReceiverStart(start: Int): SqlExpr.Binary = SqlExpr.Binary(
    SqlExpr.NumberLiteral(start.toString()),
    SqlBinaryOperator.Plus,
    SqlExpr.NumberLiteral("1"),
)

fun nativeStringReceiverRangeLength(end: Int, start: Int): SqlExpr.Binary = SqlExpr.Binary(
    SqlExpr.NumberLiteral(end.toString()),
    SqlBinaryOperator.Minus,
    SqlExpr.NumberLiteral(start.toString()),
)

fun nativeStringReceiverToEndLength(start: Int): SqlExpr.Binary = SqlExpr.Binary(
    SqlExpr.NumberLiteral(Int.MAX_VALUE.toString()),
    SqlBinaryOperator.Minus,
    nativeStringReceiverStart(start),
)

fun nativeStringReceiverComparison(
    left: SqlExpr,
    operator: SqlBinaryOperator,
    parameterName: String,
    value: Any?,
): NativeStringReceiverConditionCapture = NativeStringReceiverConditionCapture(
    expr = SqlExpr.Binary(
        left,
        operator,
        SqlExpr.Parameter(SqlParameter.Named(parameterName)),
    ),
    parameters = mapOf(parameterName to value),
)

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val user = NativeStringReceiverConditionUser(requiredName = "Ada", nullableName = "Ada")
    val runtimeLength = 3
    val oldValue = "d"
    val newValue = "D"
    val replacement = "ADa"
    val runtimeName = "Ada"
    val start = 1
    val end = 3
    val prefixEnd = 2
    val takeCount = 2

    val length = user.captureNativeStringReceiverCondition { it.requiredName.length > 2 }
    val count = user.captureNativeStringReceiverCondition { it.requiredName.count() >= runtimeLength }
    val replaceLiteral = user.captureNativeStringReceiverCondition {
        it.requiredName.replace("A", "O") == "Oda"
    }
    val replaceRuntime = user.captureNativeStringReceiverCondition {
        it.requiredName.replace(oldValue, newValue) == replacement
    }
    val replaceRuntimeReceiver = user.captureNativeStringReceiverCondition {
        it.requiredName.replace(runtimeName.take(2), "x") == "xa"
    }
    val substringStart = user.captureNativeStringReceiverCondition { it.requiredName.substring(1) == "da" }
    val substringRange = user.captureNativeStringReceiverCondition { it.requiredName.substring(start, end) == "da" }
    val subSequence = user.captureNativeStringReceiverCondition { it.requiredName.subSequence(0, prefixEnd) == "Ad" }
    val take = user.captureNativeStringReceiverCondition { it.requiredName.take(takeCount) == "Ad" }
    val takeLast = user.captureNativeStringReceiverCondition { it.requiredName.takeLast(2) == "da" }
    val safeLength = user.captureNativeStringReceiverCondition { it.nullableName?.length == 2 }
    val safeSubSequence = user.captureNativeStringReceiverCondition { it.nullableName?.subSequence(0, 2) == "Ad" }
    val safeTake = user.captureNativeStringReceiverCondition { it.nullableName?.take(2) == "Ad" }

    val requiredName = nativeStringReceiverColumn()
    val nullableName = nativeStringReceiverColumn("nullable_name")
    val cases = listOf(
        NativeStringReceiverConditionCase(
            "length",
            length,
            nativeStringReceiverComparison(
                nativeStringReceiverLength(),
                SqlBinaryOperator.GreaterThan,
                "requiredNameMin",
                2,
            ),
        ),
        NativeStringReceiverConditionCase(
            "count",
            count,
            nativeStringReceiverComparison(
                nativeStringReceiverLength(),
                SqlBinaryOperator.GreaterThanEqual,
                "requiredNameMin",
                runtimeLength,
            ),
        ),
        NativeStringReceiverConditionCase(
            "replace literal",
            replaceLiteral,
            nativeStringReceiverComparison(
                nativeStringReceiverFunction(
                    "REPLACE",
                    SqlBuiltinFunction.Replace,
                    requiredName,
                    SqlExpr.StringLiteral("A"),
                    SqlExpr.StringLiteral("O"),
                ),
                SqlBinaryOperator.Equal,
                "requiredName",
                "Oda",
            ),
        ),
        NativeStringReceiverConditionCase(
            "replace runtime",
            replaceRuntime,
            nativeStringReceiverComparison(
                nativeStringReceiverFunction(
                    "REPLACE",
                    SqlBuiltinFunction.Replace,
                    requiredName,
                    SqlExpr.StringLiteral(oldValue),
                    SqlExpr.StringLiteral(newValue),
                ),
                SqlBinaryOperator.Equal,
                "requiredName",
                replacement,
            ),
        ),
        NativeStringReceiverConditionCase(
            "replace runtime receiver",
            replaceRuntimeReceiver,
            nativeStringReceiverComparison(
                nativeStringReceiverFunction(
                    "REPLACE",
                    SqlBuiltinFunction.Replace,
                    requiredName,
                    SqlExpr.StringLiteral("Ad"),
                    SqlExpr.StringLiteral("x"),
                ),
                SqlBinaryOperator.Equal,
                "requiredName",
                "xa",
            ),
        ),
        NativeStringReceiverConditionCase(
            "substring start",
            substringStart,
            nativeStringReceiverComparison(
                nativeStringReceiverFunction(
                    "SUBSTR",
                    SqlBuiltinFunction.Substring,
                    requiredName,
                    nativeStringReceiverStart(1),
                    nativeStringReceiverToEndLength(1),
                ),
                SqlBinaryOperator.Equal,
                "requiredName",
                "da",
            ),
        ),
        NativeStringReceiverConditionCase(
            "substring range",
            substringRange,
            nativeStringReceiverComparison(
                nativeStringReceiverFunction(
                    "SUBSTR",
                    SqlBuiltinFunction.Substring,
                    requiredName,
                    nativeStringReceiverStart(start),
                    nativeStringReceiverRangeLength(end, start),
                ),
                SqlBinaryOperator.Equal,
                "requiredName",
                "da",
            ),
        ),
        NativeStringReceiverConditionCase(
            "subSequence",
            subSequence,
            nativeStringReceiverComparison(
                nativeStringReceiverFunction(
                    "SUBSTR",
                    SqlBuiltinFunction.Substring,
                    requiredName,
                    nativeStringReceiverStart(0),
                    nativeStringReceiverRangeLength(prefixEnd, 0),
                ),
                SqlBinaryOperator.Equal,
                "requiredName",
                "Ad",
            ),
        ),
        NativeStringReceiverConditionCase(
            "take runtime",
            take,
            nativeStringReceiverComparison(
                nativeStringReceiverFunction(
                    "LEFT",
                    SqlBuiltinFunction.Left,
                    requiredName,
                    SqlExpr.NumberLiteral(takeCount.toString()),
                ),
                SqlBinaryOperator.Equal,
                "requiredName",
                "Ad",
            ),
        ),
        NativeStringReceiverConditionCase(
            "takeLast literal",
            takeLast,
            nativeStringReceiverComparison(
                nativeStringReceiverFunction(
                    "RIGHT",
                    SqlBuiltinFunction.Right,
                    requiredName,
                    SqlExpr.NumberLiteral("2"),
                ),
                SqlBinaryOperator.Equal,
                "requiredName",
                "da",
            ),
        ),
        NativeStringReceiverConditionCase(
            "safe length",
            safeLength,
            nativeStringReceiverComparison(
                nativeStringReceiverLength("nullable_name"),
                SqlBinaryOperator.Equal,
                "nullableName",
                2,
            ),
        ),
        NativeStringReceiverConditionCase(
            "safe subSequence",
            safeSubSequence,
            nativeStringReceiverComparison(
                nativeStringReceiverFunction(
                    "SUBSTR",
                    SqlBuiltinFunction.Substring,
                    nullableName,
                    nativeStringReceiverStart(0),
                    nativeStringReceiverRangeLength(2, 0),
                ),
                SqlBinaryOperator.Equal,
                "nullableName",
                "Ad",
            ),
        ),
        NativeStringReceiverConditionCase(
            "safe take",
            safeTake,
            nativeStringReceiverComparison(
                nativeStringReceiverFunction(
                    "LEFT",
                    SqlBuiltinFunction.Left,
                    nullableName,
                    SqlExpr.NumberLiteral("2"),
                ),
                SqlBinaryOperator.Equal,
                "nullableName",
                "Ad",
            ),
        ),
    )

    val failed = cases.firstOrNull { it.actual != it.expected }
    return if (failed == null) {
        "OK"
    } else {
        "Fail: ${failed.label} was ${failed.actual}, expected ${failed.expected}"
    }
}
