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

// Verifies native Kotlin string receiver calls become complete select projection expressions.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableForSelect.Companion.afterSelect
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.syntax.SqlIdentifier
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlBuiltinFunction
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.syntax.statement.SqlSelectItemAliasMetadata
import com.kotlinorm.syntax.statement.SqlSelectItemSourceScope
import com.kotlinorm.types.ToSelect

@Table(name = "tb_native_string_receiver_select")
data class NativeStringReceiverSelectUser(
    var id: Int? = null,
    var requiredName: String = "",
) : KPojo

data class NativeStringReceiverSelectCapture(
    val fields: List<Field>,
    val items: List<SqlSelectItem>,
)

fun NativeStringReceiverSelectUser.captureNativeStringReceiverSelect(
    block: ToSelect<NativeStringReceiverSelectUser, Any?>,
): NativeStringReceiverSelectCapture {
    var result: NativeStringReceiverSelectCapture? = null
    afterSelect {
        block!!(it)
        result = NativeStringReceiverSelectCapture(fields.toList(), selectItems.toList())
    }
    return result ?: error("select block did not run")
}

fun nativeStringReceiverSelectColumn(): SqlExpr.Column = SqlExpr.Column(columnName = "required_name")

fun nativeStringReceiverSelectFunction(
    name: String,
    builtin: SqlBuiltinFunction,
    vararg args: SqlExpr,
): SqlExpr.Function = SqlExpr.Function(
    name = SqlIdentifier.of(name),
    args = args.toList(),
    builtinFunction = builtin,
)

fun nativeStringReceiverSelectLength(): SqlExpr.Function =
    nativeStringReceiverSelectFunction("LENGTH", SqlBuiltinFunction.Length, nativeStringReceiverSelectColumn())

fun nativeStringReceiverSelectStart(start: Int): SqlExpr.Binary = SqlExpr.Binary(
    SqlExpr.NumberLiteral(start.toString()),
    SqlBinaryOperator.Plus,
    SqlExpr.NumberLiteral("1"),
)

fun nativeStringReceiverSelectRangeLength(end: Int, start: Int): SqlExpr.Binary = SqlExpr.Binary(
    SqlExpr.NumberLiteral(end.toString()),
    SqlBinaryOperator.Minus,
    SqlExpr.NumberLiteral(start.toString()),
)

fun nativeStringReceiverSelectToEndLength(start: Int): SqlExpr.Binary = SqlExpr.Binary(
    SqlExpr.NumberLiteral(Int.MAX_VALUE.toString()),
    SqlBinaryOperator.Minus,
    nativeStringReceiverSelectStart(start),
)

fun nativeStringReceiverSelectItem(expr: SqlExpr, alias: String): SqlSelectItem.Expr = SqlSelectItem.Expr(
    expr = expr,
    alias = alias,
    metadata = SqlSelectItemAliasMetadata(
        outputName = alias,
        expression = expr,
        scope = SqlSelectItemSourceScope.Aggregate,
    ),
)

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val source = NativeStringReceiverSelectUser(requiredName = "Ada")
    val oldValue = "d"
    val newValue = "D"
    val start = 1
    val end = 3
    val prefixEnd = 2
    val takeCount = 2

    val actual = source.captureNativeStringReceiverSelect {
        [
            it.requiredName.length.alias("nameLength"),
            it.requiredName.count().alias("nameCount"),
            it.requiredName.replace("A", "O").alias("replaceLiteral"),
            it.requiredName.replace(oldValue, newValue).alias("replaceRuntime"),
            it.requiredName.substring(1).alias("substringStart"),
            it.requiredName.substring(start, end).alias("substringRange"),
            it.requiredName.subSequence(0, prefixEnd).alias("subSequence"),
            it.requiredName.take(takeCount).alias("takeRuntime"),
            it.requiredName.takeLast(2).alias("takeLastLiteral"),
        ]
    }

    val requiredName = nativeStringReceiverSelectColumn()
    val expectedItems = listOf(
        nativeStringReceiverSelectItem(nativeStringReceiverSelectLength(), "nameLength"),
        nativeStringReceiverSelectItem(nativeStringReceiverSelectLength(), "nameCount"),
        nativeStringReceiverSelectItem(
            nativeStringReceiverSelectFunction(
                "REPLACE",
                SqlBuiltinFunction.Replace,
                requiredName,
                SqlExpr.StringLiteral("A"),
                SqlExpr.StringLiteral("O"),
            ),
            "replaceLiteral",
        ),
        nativeStringReceiverSelectItem(
            nativeStringReceiverSelectFunction(
                "REPLACE",
                SqlBuiltinFunction.Replace,
                requiredName,
                SqlExpr.StringLiteral(oldValue),
                SqlExpr.StringLiteral(newValue),
            ),
            "replaceRuntime",
        ),
        nativeStringReceiverSelectItem(
            nativeStringReceiverSelectFunction(
                "SUBSTR",
                SqlBuiltinFunction.Substring,
                requiredName,
                nativeStringReceiverSelectStart(1),
                nativeStringReceiverSelectToEndLength(1),
            ),
            "substringStart",
        ),
        nativeStringReceiverSelectItem(
            nativeStringReceiverSelectFunction(
                "SUBSTR",
                SqlBuiltinFunction.Substring,
                requiredName,
                nativeStringReceiverSelectStart(start),
                nativeStringReceiverSelectRangeLength(end, start),
            ),
            "substringRange",
        ),
        nativeStringReceiverSelectItem(
            nativeStringReceiverSelectFunction(
                "SUBSTR",
                SqlBuiltinFunction.Substring,
                requiredName,
                nativeStringReceiverSelectStart(0),
                nativeStringReceiverSelectRangeLength(prefixEnd, 0),
            ),
            "subSequence",
        ),
        nativeStringReceiverSelectItem(
            nativeStringReceiverSelectFunction(
                "LEFT",
                SqlBuiltinFunction.Left,
                requiredName,
                SqlExpr.NumberLiteral(takeCount.toString()),
            ),
            "takeRuntime",
        ),
        nativeStringReceiverSelectItem(
            nativeStringReceiverSelectFunction(
                "RIGHT",
                SqlBuiltinFunction.Right,
                requiredName,
                SqlExpr.NumberLiteral("2"),
            ),
            "takeLastLiteral",
        ),
    )
    val expected = NativeStringReceiverSelectCapture(emptyList(), expectedItems)

    return if (actual == expected) {
        "OK"
    } else {
        "Fail: projection was $actual, expected $expected"
    }
}
