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

// Verifies listOf projection forms feed generated Selected and Context receivers.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
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

@Table("tb_projection_collection_forms")
data class ProjectionCollectionFormsUser(
    var id: Int? = null,
    var username: String? = null,
    var status: Int? = null,
) : KPojo

fun box(): String {
    with(Kronos) {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val listClause = ProjectionCollectionFormsUser()
        .select { listOf(it.id.alias("uid"), f.length(it.username).alias("nameLength")) }
        .orderBy { it.uid.asc() }

    @Suppress("UNREACHABLE_CODE")
    if (false) {
        val listSelected = listClause.queryOne()
        val uid: Int? = listSelected.uid
        val nameLength: Any? = listSelected.nameLength
        return "Fail: selected values unexpectedly evaluated as $uid/$nameLength"
    }

    val listStatement = listClause.toSqlQuery() as SqlQuery.Select

    val failures = listOfNotNull(
        expect(listStatement.selectAliases() == listOf("uid", "nameLength")) {
            "list aliases were ${listStatement.selectAliases()}"
        },
        expect(listStatement.hasOrderByColumn("uid", SqlOrdering.Asc)) {
            "list order by was ${listStatement.allOrderBy()}"
        },
    )

    return failures.firstOrNull() ?: "OK"
}

inline fun expect(condition: Boolean, message: () -> String): String? =
    if (condition) null else "Fail: ${message()}"

fun SqlQuery.Select.selectAliases(): List<String?> {
    return allSelectStatements().last().select.map { item ->
        val expr = item as? SqlSelectItem.Expr
        expr?.metadata?.outputName ?: expr?.alias
    }
}

fun SqlQuery.Select.hasOrderByColumn(name: String, direction: SqlOrdering): Boolean {
    return allOrderBy().any { item ->
        item.ordering == direction &&
            item.expr.containsExpression(SqlExpr.Column::class) { it.columnName == name }
    }
}

fun SqlQuery.Select.allOrderBy(): List<SqlOrderingItem> =
    allSelectStatements().flatMap { it.orderBy }

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
