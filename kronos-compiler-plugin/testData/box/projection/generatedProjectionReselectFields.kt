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

// Verifies generated projection properties can be selected again as field expressions.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.syntax.table.SqlTable

@Table("tb_generated_projection_reselect")
data class GeneratedProjectionReselectUser(
    var id: Int? = null,
    var username: String? = null,
    var status: Int? = null,
) : KPojo

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val statement = GeneratedProjectionReselectUser()
        .select { [it.id.alias("uid"), it.username.alias("displayName"), it.status] }
        .select { [it.uid, it.displayName] }
        .where { it.uid == 7 }
        .toSqlQuery() as SqlQuery.Select

    val source = statement.from.singleOrNull() as? SqlTable.Subquery
        ?: return "Fail: source was ${statement.from}"
    val inner = source.query as? SqlQuery.Select
        ?: return "Fail: inner was ${source.query}"
    val selected = statement.select.mapNotNull { ((it as? SqlSelectItem.Expr)?.expr as? SqlExpr.Column)?.columnName }
    val innerAliases = inner.select.map { (it as? SqlSelectItem.Expr)?.alias }
    val whereColumn = ((statement.where as? SqlExpr.Binary)?.left as? SqlExpr.Column)?.columnName

    return when {
        selected != listOf("uid", "displayName") -> "Fail: selected columns were $selected"
        innerAliases != listOf("uid", "displayName", null) -> "Fail: inner aliases were $innerAliases"
        whereColumn != "uid" -> "Fail: where column was $whereColumn"
        else -> "OK"
    }
}
