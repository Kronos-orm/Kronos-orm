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

// Verifies that scalar subquery aliases are FIR-visible on Selected and orderBy Context receivers.

import com.kotlinorm.Kronos
import com.kotlinorm.annotations.Table
import com.kotlinorm.ast.BinaryExpression
import com.kotlinorm.ast.ColumnReference
import com.kotlinorm.ast.DeferredSubqueryExpression
import com.kotlinorm.ast.OrderByItem
import com.kotlinorm.ast.SelectItem
import com.kotlinorm.ast.SelectStatement
import com.kotlinorm.ast.SubqueryTable
import com.kotlinorm.enums.SortType
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.select
import kotlin.reflect.KClass

@Table("tb_projection_scalar_alias_user")
data class ProjectionScalarAliasUser(
    var id: Int? = null,
    var name: String? = null,
) : KPojo

@Table("tb_projection_scalar_alias_order")
data class ProjectionScalarAliasOrder(
    var id: Int? = null,
    var userId: Int? = null,
    var amount: Int? = null,
) : KPojo

fun box(): String {
    Kronos.init {
        fieldNamingStrategy = lineHumpNamingStrategy
        tableNamingStrategy = lineHumpNamingStrategy
    }

    val clause = ProjectionScalarAliasUser()
        .select {
            [
                it.id,
                ProjectionScalarAliasOrder()
                    .select { order -> order.amount }
                    .limit(1)
                    .alias("lastAmount")
            ]
        }
        .orderBy { it.lastAmount.desc() }

    @Suppress("UNREACHABLE_CODE")
    if (false) {
        val selected = clause.queryOne()
        val aliasValue: Int? = selected.lastAmount
        return "Fail: selected alias unexpectedly evaluated as $aliasValue"
    }

    val statement = clause.toStatement()

    val failures = listOfNotNull(
        expect(statement.hasScalarSubqueryAlias("lastAmount")) { "select aliases were ${statement.selectAliases()}" },
        expect(statement.hasOrderByColumn("lastAmount", SortType.DESC)) { "order by was ${statement.allOrderBy()}" },
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

fun SelectStatement.hasScalarSubqueryAlias(name: String): Boolean {
    return allSelectStatements().any { statement ->
        statement.selectList.any { item ->
            item is SelectItem.ExpressionSelectItem &&
                item.alias == name &&
                item.expression is DeferredSubqueryExpression.Scalar &&
                item.aliasMetadata(0)?.outputName == name
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
