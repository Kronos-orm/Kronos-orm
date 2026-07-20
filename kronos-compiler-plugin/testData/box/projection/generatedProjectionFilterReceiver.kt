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

// Verifies filter exposes generated projection aliases and creates a derived-query predicate layer.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.filter
import com.kotlinorm.orm.select.select
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.syntax.table.SqlTable

@Table("tb_projection_filter_receiver")
data class ProjectionFilterReceiverUser(
    var id: Int? = null,
    var username: String? = null,
) : KPojo

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val statement = ProjectionFilterReceiverUser()
        .select { [it.id.alias("uid"), it.username.alias("displayName")] }
        .filter { it.uid == 7 }
        .toSqlQuery() as SqlQuery.Select

    val source = statement.from.singleOrNull() as? SqlTable.Subquery
        ?: return "Fail: outer source was ${statement.from}"
    val inner = source.query as? SqlQuery.Select
        ?: return "Fail: inner query was ${source.query}"
    val where = statement.where as? SqlExpr.Binary
        ?: return "Fail: outer where was ${statement.where}"
    val whereColumn = where.left as? SqlExpr.Column
        ?: return "Fail: outer where left was ${where.left}"
    val innerOutputNames = inner.select.map { (it as? SqlSelectItem.Expr)?.metadata?.outputName }

    val failures = listOfNotNull(
        expectProjectionFilter(innerOutputNames == listOf("uid", "displayName")) {
            "inner output names were $innerOutputNames"
        },
        expectProjectionFilter(where.operator == SqlBinaryOperator.Equal) {
            "outer where operator was ${where.operator}"
        },
        expectProjectionFilter(whereColumn.tableName == "q") {
            "outer where table was ${whereColumn.tableName}"
        },
        expectProjectionFilter(whereColumn.columnName == "uid") {
            "outer where column was ${whereColumn.columnName}"
        },
        expectProjectionFilter(where.right is SqlExpr.Parameter) {
            "outer where right was ${where.right}"
        },
    )

    return failures.firstOrNull() ?: "OK"
}

inline fun expectProjectionFilter(condition: Boolean, message: () -> String): String? =
    if (condition) null else "Fail: ${message()}"
