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

// Verifies select projection lowering for arithmetic operators, string concat, and nested functions.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.beans.dsl.KTableForSelect.Companion.afterSelect
import com.kotlinorm.functions.bundled.exts.StringFunctions.concat
import com.kotlinorm.functions.bundled.exts.StringFunctions.upper
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.types.ToSelect

@Table(name = "tb_select_operator_matrix")
data class OperatorMatrixSelectUser(
    var id: Int? = null,
    var name: String? = null,
    var score: Int = 0,
) : KPojo

fun OperatorMatrixSelectUser.collectMatrixItems(block: ToSelect<OperatorMatrixSelectUser, Any?>): List<SqlSelectItem> {
    val result = mutableListOf<SqlSelectItem>()
    afterSelect {
        block!!(it)
        result += selectItems
    }
    return result
}

fun exprAt(items: List<SqlSelectItem>, index: Int): SqlExpr? =
    (items.getOrNull(index) as? SqlSelectItem.Expr)?.expr

fun binaryAt(items: List<SqlSelectItem>, index: Int): SqlExpr.Binary? =
    exprAt(items, index) as? SqlExpr.Binary

fun functionAt(items: List<SqlSelectItem>, index: Int): SqlExpr.Function? =
    exprAt(items, index) as? SqlExpr.Function

fun expectSelectMatrix(condition: Boolean, message: () -> String): String? =
    if (condition) null else "Fail: ${message()}"

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val items = OperatorMatrixSelectUser().collectMatrixItems {
        [
            it.score - 2,
            it.score * 3,
            it.score / 4,
            it.name + "-x",
            f.upper(it.name).alias("upperName"),
            f.concat(it.name, "-", it.name).alias("joined")
        ]
    }
    val sub = binaryAt(items, 0)
    val mul = binaryAt(items, 1)
    val div = binaryAt(items, 2)
    val concat = functionAt(items, 3)
    val upperItem = items.getOrNull(4) as? SqlSelectItem.Expr
    val upper = upperItem?.expr as? SqlExpr.Function
    val joinedItem = items.getOrNull(5) as? SqlSelectItem.Expr
    val joined = joinedItem?.expr as? SqlExpr.Function

    val failures = listOfNotNull(
        expectSelectMatrix(items.size == 6) { "size was ${items.size}" },
        expectSelectMatrix(sub?.operator == SqlBinaryOperator.Minus) { "sub operator was ${sub?.operator}" },
        expectSelectMatrix((sub?.left as? SqlExpr.Column)?.columnName == "score") { "sub field was ${sub?.left}" },
        expectSelectMatrix(mul?.operator == SqlBinaryOperator.Times) { "mul operator was ${mul?.operator}" },
        expectSelectMatrix((mul?.left as? SqlExpr.Column)?.columnName == "score") { "mul field was ${mul?.left}" },
        expectSelectMatrix(div?.operator == SqlBinaryOperator.Div) { "div operator was ${div?.operator}" },
        expectSelectMatrix((div?.left as? SqlExpr.Column)?.columnName == "score") { "div field was ${div?.left}" },
        expectSelectMatrix(concat?.name?.last == "CONCAT") { "concat function was ${concat?.name}" },
        expectSelectMatrix((concat?.args?.getOrNull(0) as? SqlExpr.Column)?.columnName == "name") {
            "concat first arg was ${concat?.args}"
        },
        expectSelectMatrix(upperItem?.alias == "upperName") { "upper alias was ${upperItem?.alias}" },
        expectSelectMatrix(upper?.name?.last == "UPPER") { "upper function was ${upper?.name}" },
        expectSelectMatrix((upper?.args?.singleOrNull() as? SqlExpr.Column)?.columnName == "name") {
            "upper arg was ${upper?.args}"
        },
        expectSelectMatrix(joinedItem?.alias == "joined") { "joined alias was ${joinedItem?.alias}" },
        expectSelectMatrix(joined?.name?.last == "CONCAT") { "joined function was ${joined?.name}" },
        expectSelectMatrix((joined?.args?.getOrNull(0) as? SqlExpr.Column)?.columnName == "name") {
            "joined first arg was ${joined?.args}"
        },
        expectSelectMatrix((joined?.args?.getOrNull(2) as? SqlExpr.Column)?.columnName == "name") {
            "joined third arg was ${joined?.args}"
        },
    )

    return failures.firstOrNull() ?: "OK"
}
