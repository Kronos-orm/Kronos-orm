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

// Verifies source-minus removes the original Context field before a same-name alias is merged.

import com.kotlinorm.annotations.Table
import com.kotlinorm.functions.bundled.exts.StringFunctions.length
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.syntax.order.SqlOrdering

@Table("tb_projection_source_minus_alias")
data class SourceMinusAliasCoverageUser(
    var id: Int? = null,
    var username: String? = null,
) : KPojo

fun box(): String {
    val clause = SourceMinusAliasCoverageUser()
        .select { [it - it.username, f.length(it.username).alias("username")] }
        .orderBy { it.username.desc() }

    @Suppress("UNREACHABLE_CODE")
    if (false) {
        val selectedUsername: Int? = clause.first().username
        return "Fail: selected username was $selectedUsername"
    }

    val statement = clause.toSqlQuery() as SqlQuery.Select
    val aliases = statement.select.mapNotNull { item ->
        val expression = item as? SqlSelectItem.Expr
        expression?.metadata?.outputName ?: expression?.alias
    }
    val order = statement.orderBy.singleOrNull()
        ?: return "Fail: expected one orderBy item, got ${statement.orderBy}"
    val orderColumn = order.expr as? SqlExpr.Column
        ?: return "Fail: expected a column orderBy expression, got ${order.expr}"

    return when {
        aliases != listOf("id", "username") -> "Fail: selected aliases were $aliases"
        aliases.count { it == "username" } != 1 -> "Fail: username alias count was ${aliases.count { it == "username" }}"
        orderColumn.columnName != "username" -> "Fail: orderBy column was ${orderColumn.columnName}"
        order.ordering != SqlOrdering.Desc -> "Fail: orderBy direction was ${order.ordering}"
        else -> "OK"
    }
}
