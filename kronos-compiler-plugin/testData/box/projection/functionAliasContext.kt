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

// Verifies that function and aggregate aliases are FIR-visible on generated Selected and orderBy Context receivers.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.functions.bundled.exts.PolymerizationFunctions.count
import com.kotlinorm.functions.bundled.exts.StringFunctions.length
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.order.SqlOrdering
import com.kotlinorm.syntax.order.SqlOrderingItem
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.syntax.table.SqlTable
import kotlin.reflect.KClass

@Table("tb_projection_function_alias")
data class ProjectionFunctionAliasUser(
    var id: Int? = null,
    var username: String? = null,
) : KPojo

fun box(): String {
    val clause = ProjectionFunctionAliasUser()
        .select {
            [
                it.id,
                f.length(it.username).alias("nameLength"),
                f.count(it.id).alias("totalCount")
            ]
        }
        .orderBy { it.nameLength.desc() }

    @Suppress("UNREACHABLE_CODE")
    if (false) {
        val selected = clause.queryOne()
        val nameLength: Any? = selected.nameLength
        val totalCount: Number? = selected.totalCount
        return "Fail: selected aliases unexpectedly evaluated as $nameLength/$totalCount"
    }

    val statement = clause.toSqlQuery() as SqlQuery.Select
    val failures = listOfNotNull(
        expect(statement.hasProjectionAlias("nameLength")) { "select aliases were ${statement.selectAliases()}" },
        expect(statement.hasProjectionAlias("totalCount")) { "select aliases were ${statement.selectAliases()}" },
        expect(statement.hasOrderByColumn("nameLength", SqlOrdering.Desc)) { "order by was ${statement.allOrderBy()}" },
    )

    return failures.firstOrNull() ?: "OK"
}

inline fun expect(condition: Boolean, message: () -> String): String? {
    return if (condition) null else "Fail: ${message()}"
}

fun SqlQuery.Select.selectAliases(): List<String?> {
    return allSelectStatements().flatMap { statement ->
        statement.select.map { item -> (item as? SqlSelectItem.Expr)?.metadata?.outputName }
    }
}

fun SqlQuery.Select.hasProjectionAlias(name: String): Boolean {
    return allSelectStatements().any { statement ->
        statement.select.mapNotNull { (it as? SqlSelectItem.Expr)?.metadata }.any { it.outputName == name }
    }
}

fun SqlQuery.Select.hasOrderByColumn(name: String, direction: SqlOrdering): Boolean {
    return allOrderBy().any { item ->
        item.ordering == direction &&
            item.expr.containsExpression(SqlExpr.Column::class) { it.columnName == name }
    }
}

fun SqlQuery.Select.allOrderBy(): List<SqlOrderingItem> {
    return allSelectStatements().flatMap { it.orderBy }
}

fun SqlQuery.Select.allSelectStatements(): List<SqlQuery.Select> {
    val nested = ((from.singleOrNull() as? SqlTable.Subquery)?.query as? SqlQuery.Select)?.allSelectStatements().orEmpty()
    return listOf(this) + nested
}

fun <T : Any> Any?.containsExpression(type: KClass<T>, match: (T) -> Boolean): Boolean {
    return when (this) {
        null -> false
        else -> {
            @Suppress("UNCHECKED_CAST")
            val currentMatches = type.isInstance(this) && match(this as T)
            currentMatches || when (this) {
                is SqlExpr.Binary -> left.containsExpression(type, match) || right.containsExpression(type, match)
                else -> false
            }
        }
    }
}
