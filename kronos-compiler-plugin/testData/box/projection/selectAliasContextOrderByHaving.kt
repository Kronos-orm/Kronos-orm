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

// Verifies that select aliases are FIR-visible on Context receivers for orderBy and having.

import com.kotlinorm.Kronos
import com.kotlinorm.ast.BinaryExpression
import com.kotlinorm.ast.ColumnReference
import com.kotlinorm.ast.OrderByItem
import com.kotlinorm.ast.Parameter
import com.kotlinorm.ast.SelectStatement
import com.kotlinorm.ast.SubqueryTable
import com.kotlinorm.annotations.Table
import com.kotlinorm.enums.SortType
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select
import kotlin.reflect.KClass

@Table("tb_projection_context_order")
data class ProjectionContextOrderRow(
    var id: Int? = null,
    var name: String? = null,
) : KPojo

fun box(): String {
    val params = mutableMapOf<String, Any?>()

    Kronos.init {}

    val statement = ProjectionContextOrderRow()
        .select { [it.id, it.name.as_("xx")] }
        .having { it.xx == "Ada" }
        .orderBy { it.xx.desc() }
        .toStatement(null, params)

    val failures = listOfNotNull(
        expect(statement.hasProjectionAlias("xx")) { "select aliases were ${statement.selectAliases()}" },
        expect(statement.hasFilterColumn("xx")) { "filter expressions were ${statement.allFilterExpressions()}" },
        expect(statement.hasFilterParameter("xx")) { "filter expressions were ${statement.allFilterExpressions()}" },
        expect(statement.hasOrderByColumn("xx", SortType.DESC)) { "order by was ${statement.allOrderBy()}" },
        expect(params["xx"] == "Ada") { "alias having param was $params" },
    )

    return failures.firstOrNull() ?: "OK"
}

inline fun expect(condition: Boolean, message: () -> String): String? {
    return if (condition) null else "Fail: ${message()}"
}

fun SelectStatement.selectAliases(): List<String?> {
    return allSelectStatements().flatMap { statement ->
        statement.selectList.mapIndexed { index, item -> item.aliasMetadata(index)?.outputName }
    }
}

fun SelectStatement.hasProjectionAlias(name: String): Boolean {
    return allSelectStatements().any { statement ->
        statement.selectItemMetadata().any { it.outputName == name }
    }
}

fun SelectStatement.hasFilterColumn(name: String): Boolean {
    return allSelectStatements().any { statement ->
        statement.allFilterExpressions().any { expression ->
            expression.containsExpression(ColumnReference::class) { it.columnName == name }
        }
    }
}

fun SelectStatement.hasFilterParameter(name: String): Boolean {
    return allSelectStatements().any { statement ->
        statement.allFilterExpressions().any { expression ->
            expression.containsExpression(Parameter.NamedParameter::class) { it.name == name }
        }
    }
}

fun SelectStatement.hasOrderByColumn(name: String, direction: SortType): Boolean {
    return allOrderBy().any { item ->
        item.direction == direction &&
            item.expression.containsExpression(ColumnReference::class) { it.columnName == name }
    }
}

fun SelectStatement.allOrderBy(): List<OrderByItem> {
    return allSelectStatements().flatMap { it.orderBy.orEmpty() }
}

fun SelectStatement.allFilterExpressions(): List<Any?> {
    return allSelectStatements().flatMap { listOf(it.where, it.having) }
}

fun SelectStatement.allSelectStatements(): List<SelectStatement> {
    val nested = (from as? SubqueryTable)?.subquery?.allSelectStatements().orEmpty()
    return listOf(this) + nested
}

fun <T : Any> Any?.containsExpression(type: KClass<T>, match: (T) -> Boolean): Boolean {
    return when (this) {
        null -> false
        else -> {
            @Suppress("UNCHECKED_CAST")
            val currentMatches = type.isInstance(this) && match(this as T)
            currentMatches || when (this) {
                is BinaryExpression -> left.containsExpression(type, match) || right.containsExpression(type, match)
                else -> false
            }
        }
    }
}
