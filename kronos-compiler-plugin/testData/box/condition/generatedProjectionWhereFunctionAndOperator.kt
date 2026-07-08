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

// Verifies generated projection fields work in where functions and operators.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.functions.bundled.exts.StringFunctions.length
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.table.SqlTable

@Table("tb_generated_projection_where")
data class GeneratedProjectionWhereUser(
    var id: Int? = null,
    var username: String? = null,
) : KPojo

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val query = GeneratedProjectionWhereUser()
        .select { [it.id.alias("uid"), it.username.alias("displayName")] }
        .select { [it.uid, it.displayName] }
        .where { f.length(it.displayName) > 2 && it.uid + 1 > 7 }
        .toSqlQuery() as SqlQuery.Select

    val source = query.from.singleOrNull() as? SqlTable.Subquery
        ?: return "Fail: source was ${query.from}"
    val where = query.where as? SqlExpr.Binary
        ?: return "Fail: where was ${query.where}"
    val lengthPredicate = where.left as? SqlExpr.Binary
        ?: return "Fail: length predicate was ${where.left}"
    val plusPredicate = where.right as? SqlExpr.Binary
        ?: return "Fail: plus predicate was ${where.right}"
    val lengthExpr = lengthPredicate.left as? SqlExpr.Function
        ?: return "Fail: length expr was ${lengthPredicate.left}"
    val lengthColumn = lengthExpr.args.singleOrNull() as? SqlExpr.Column
    val plusExpr = plusPredicate.left as? SqlExpr.Binary
        ?: return "Fail: plus expr was ${plusPredicate.left}"
    val plusColumn = plusExpr.left as? SqlExpr.Column

    val failures = listOfNotNull(
        expectGeneratedProjectionWhere((source.query as? SqlQuery.Select) != null) { "source query was ${source.query}" },
        expectGeneratedProjectionWhere(where.operator == SqlBinaryOperator.And) { "where operator was ${where.operator}" },
        expectGeneratedProjectionWhere(lengthPredicate.operator == SqlBinaryOperator.GreaterThan) {
            "length operator was ${lengthPredicate.operator}"
        },
        expectGeneratedProjectionWhere(lengthExpr.name.last == "LENGTH") { "length function was ${lengthExpr.name}" },
        expectGeneratedProjectionWhere(lengthColumn?.columnName == "displayName") { "length column was $lengthColumn" },
        expectGeneratedProjectionWhere(lengthPredicate.right is SqlExpr.Parameter) { "length value was ${lengthPredicate.right}" },
        expectGeneratedProjectionWhere(plusPredicate.operator == SqlBinaryOperator.GreaterThan) {
            "plus operator was ${plusPredicate.operator}"
        },
        expectGeneratedProjectionWhere(plusExpr.operator == SqlBinaryOperator.Plus) { "plus expr operator was ${plusExpr.operator}" },
        expectGeneratedProjectionWhere(plusColumn?.columnName == "uid") { "plus column was $plusColumn" },
        expectGeneratedProjectionWhere(plusPredicate.right is SqlExpr.Parameter) { "plus value was ${plusPredicate.right}" },
    )

    return failures.firstOrNull() ?: "OK"
}

inline fun expectGeneratedProjectionWhere(condition: Boolean, message: () -> String): String? =
    if (condition) null else "Fail: ${message()}"
