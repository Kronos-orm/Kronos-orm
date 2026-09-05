/**
 * Copyright 2022-2025 kronos-orm
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

package com.kotlinorm.beans.dsl

import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.order.SqlOrdering

/**
 * KTableForSort
 *
 * DSL Class of Kronos, which the compiler plugin use to generate the `order by` code.
 * @param T the type of the table
 */
class KTableForSort<T : KPojo>(
    sourceBinding: SourceBinding? = null
) : KTableForSelect<T>(sourceBinding) {
    val sortedItems = mutableListOf<SortItem>()

    @Suppress("UNCHECKED_CAST", "UNUSED")
    fun addSortField(field: Any) {
        when (field) {
            is Pair<*, *> -> {
                val value = field.first
                val ordering = field.second as SqlOrdering
                when (value) {
                    is Field -> addFieldSort(value, ordering)
                    is SqlExpr -> addExpressionSort(value, ordering)
                    is KSelectable<*> -> addSelectableSort(value, ordering)
                    else -> addFieldSort(value as Field, ordering)
                }
            }

            is String -> {
                addExpressionSort(field.toRawSqlExpr(), SqlOrdering.Asc)
            }

            is SqlExpr -> {
                addExpressionSort(field, SqlOrdering.Asc)
            }

            is KSelectable<*> -> {
                addSelectableSort(field, SqlOrdering.Asc)
            }

            else -> {
                addFieldSort(field as Field, SqlOrdering.Asc)
            }
        }
    }

    fun addSortExpression(expression: SqlExpr, ordering: SqlOrdering = SqlOrdering.Asc) {
        addExpressionSort(expression, ordering)
    }

    fun addSortSubquery(query: KSelectable<*>, ordering: SqlOrdering = SqlOrdering.Asc) {
        addSelectableSort(query, ordering)
    }

    @Suppress("UNUSED")
    fun Any?.desc(): Pair<Any?, SqlOrdering> =
        sortPair(SqlOrdering.Desc)

    @Suppress("UNUSED")
    fun Any?.asc(): Pair<Any?, SqlOrdering> =
        sortPair(SqlOrdering.Asc)

    private fun Any?.sortPair(ordering: SqlOrdering): Pair<Any?, SqlOrdering> {
        return (this.takeUnless { it is String } ?: this.toString().toRawSqlExpr()) to ordering
    }

    private fun addFieldSort(field: Field, ordering: SqlOrdering) {
        sortedItems.add(SortItem.FieldItem(field, ordering, column(field)))
    }

    private fun addExpressionSort(expression: SqlExpr, ordering: SqlOrdering) {
        sortedItems.add(SortItem.ExpressionItem(expression, ordering))
    }

    private fun addSelectableSort(query: KSelectable<*>, ordering: SqlOrdering) {
        sortedItems.add(SortItem.SelectableItem(query, ordering))
    }

    companion object {
        /**
         * Creates a KTable instance with the given KPojo object as the data source and applies the given block to it.
         *
         * @param T The type of the KPojo object.
         * @param block The block of code to be applied to the KTable instance.
         * @return The resulting KTable instance after applying the block.
         */
        fun <T : KPojo> T.afterSort(block: KTableForSort<T>.(T) -> Unit) =
            KTableForSort<T>().block(this)

        fun <T : KPojo> T.afterSort(
            sourceBinding: SourceBinding?,
            block: KTableForSort<T>.(T) -> Unit
        ) = KTableForSort<T>(sourceBinding).block(this)
    }

    sealed class SortItem {
        abstract val ordering: SqlOrdering

        data class FieldItem(
            val field: Field,
            override val ordering: SqlOrdering,
            val expr: SqlExpr.Column? = null
        ) : SortItem()
        data class ExpressionItem(val expression: SqlExpr, override val ordering: SqlOrdering) : SortItem()
        data class SelectableItem(val query: KSelectable<*>, override val ordering: SqlOrdering) : SortItem()
    }
}
