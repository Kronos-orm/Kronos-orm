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

package com.kotlinorm.ast

fun SelectStatement.asDerivedTable(alias: String): SubqueryTable {
    require(alias.isNotBlank()) { "Derived table alias must not be blank." }
    return SubqueryTable(this, alias)
}

fun SelectStatement.projectFromAlias(alias: String): MutableList<SelectItem> {
    require(alias.isNotBlank()) { "Derived table alias must not be blank." }
    return selectList.mapIndexed { index, item ->
        val outputName = item.outputNameForDerivedProjection(index)
        SelectItem.ColumnSelectItem(
            ColumnReference(database = null, tableAlias = alias, columnName = outputName),
            alias = null
        )
    }.toMutableList()
}

fun SelectStatement.wrapWithOuterFilter(
    alias: String,
    outerWhere: Expression? = null,
    outerHaving: Expression? = null,
    outerOrderBy: MutableList<OrderByItem>? = null
): SelectStatement {
    return SelectStatement(
        selectList = projectFromAlias(alias),
        from = asDerivedTable(alias),
        where = outerWhere,
        groupBy = null,
        having = outerHaving,
        orderBy = outerOrderBy,
        limit = null,
        distinct = false,
        lock = null
    )
}

private fun SelectItem.outputNameForDerivedProjection(index: Int): String {
    return when (this) {
        is SelectItem.ColumnSelectItem -> aliasMetadata(index)?.outputName ?: alias ?: column.columnName
        is SelectItem.ExpressionSelectItem ->
            aliasMetadata(index)?.takeIf { it.userReferenceable }?.outputName
                ?: error("Expression select item used by derived wrapper must have alias.")
        is SelectItem.AllColumnsSelectItem ->
            error("All-columns select item cannot be projected from a derived table without expansion.")
    }
}
