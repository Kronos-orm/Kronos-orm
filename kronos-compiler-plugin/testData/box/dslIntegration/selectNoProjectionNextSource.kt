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

// Verifies select() without a projection exposes logical output names as the next-layer Source.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Column
import com.kotlinorm.annotations.Table
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.syntax.table.SqlTable

@Table("tb_no_projection_user")
data class NoProjectionUser(
    var id: Int? = null,
    @Column("user_name")
    var name: String? = null,
    var status: Int? = null,
) : KPojo

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val lowered = NoProjectionUser()
        .select()
        .select { [it.id, it.name] }
        .where { it.status == 1 }
        .toSqlQuery() as SqlQuery.Select

    val source = lowered.from.singleOrNull() as? SqlTable.Subquery
        ?: return "Fail: source was ${lowered.from}"
    val inner = source.query as? SqlQuery.Select
        ?: return "Fail: inner source was ${source.query}"
    val selectedColumns = lowered.select.mapNotNull { ((it as? SqlSelectItem.Expr)?.expr as? SqlExpr.Column)?.columnName }
    val innerColumns = inner.select.mapNotNull { ((it as? SqlSelectItem.Expr)?.expr as? SqlExpr.Column)?.columnName }
    val whereColumn = ((lowered.where as? SqlExpr.Binary)?.left as? SqlExpr.Column)?.columnName

    return when {
        selectedColumns != listOf("id", "name") -> "Fail: selected columns were $selectedColumns"
        innerColumns != listOf("id", "user_name", "status") -> "Fail: inner columns were $innerColumns"
        whereColumn != "status" -> "Fail: where column was $whereColumn"
        else -> "OK"
    }
}
