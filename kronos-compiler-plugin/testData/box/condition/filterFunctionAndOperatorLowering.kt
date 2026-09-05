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

// Verifies filter lowers condition functions and arithmetic operators on generated projection aliases.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.functions.bundled.exts.StringFunctions.length
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.filter
import com.kotlinorm.orm.select.select
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.table.SqlTable

@Table("tb_filter_function_operator")
data class FilterFunctionOperatorUser(
    var id: Int? = null,
    var username: String? = null,
) : KPojo

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val statement = FilterFunctionOperatorUser()
        .select { [it.id.alias("uid"), it.username.alias("displayName")] }
        .filter { f.length(it.displayName) > 2 && it.uid + 1 > 7 }
        .toSqlQuery() as SqlQuery.Select

    val source = statement.from.singleOrNull() as? SqlTable.Subquery
        ?: return "Fail: outer source was ${statement.from}"
    val where = statement.where as? SqlExpr.Binary
        ?: return "Fail: outer where was ${statement.where}"
    val lengthPredicate = where.left as? SqlExpr.Binary
        ?: return "Fail: length predicate was ${where.left}"
    val plusPredicate = where.right as? SqlExpr.Binary
        ?: return "Fail: plus predicate was ${where.right}"
    val lengthExpr = lengthPredicate.left as? SqlExpr.Function
        ?: return "Fail: length expression was ${lengthPredicate.left}"
    val lengthColumn = lengthExpr.args.singleOrNull() as? SqlExpr.Column
    val plusExpr = plusPredicate.left as? SqlExpr.Binary
        ?: return "Fail: plus expression was ${plusPredicate.left}"
    val plusColumn = plusExpr.left as? SqlExpr.Column

    val failures = listOfNotNull(
        expectFilterLowering(source.query is SqlQuery.Select) { "source query was ${source.query}" },
        expectFilterLowering(where.operator == SqlBinaryOperator.And) { "where operator was ${where.operator}" },
        expectFilterLowering(lengthPredicate.operator == SqlBinaryOperator.GreaterThan) {
            "length predicate operator was ${lengthPredicate.operator}"
        },
        expectFilterLowering(lengthExpr.name.last == "LENGTH") { "length function was ${lengthExpr.name}" },
        expectFilterLowering(lengthColumn?.tableName == "q" && lengthColumn.columnName == "displayName") {
            "length column was $lengthColumn"
        },
        expectFilterLowering(lengthPredicate.right is SqlExpr.Parameter) {
            "length predicate right was ${lengthPredicate.right}"
        },
        expectFilterLowering(plusPredicate.operator == SqlBinaryOperator.GreaterThan) {
            "plus predicate operator was ${plusPredicate.operator}"
        },
        expectFilterLowering(plusExpr.operator == SqlBinaryOperator.Plus) {
            "plus expression operator was ${plusExpr.operator}"
        },
        expectFilterLowering(plusColumn?.tableName == "q" && plusColumn.columnName == "uid") {
            "plus column was $plusColumn"
        },
        expectFilterLowering(plusExpr.right == SqlExpr.NumberLiteral("1")) {
            "plus expression right was ${plusExpr.right}"
        },
        expectFilterLowering(plusPredicate.right is SqlExpr.Parameter) {
            "plus predicate right was ${plusPredicate.right}"
        },
    )

    return failures.firstOrNull() ?: "OK"
}

inline fun expectFilterLowering(condition: Boolean, message: () -> String): String? =
    if (condition) null else "Fail: ${message()}"
