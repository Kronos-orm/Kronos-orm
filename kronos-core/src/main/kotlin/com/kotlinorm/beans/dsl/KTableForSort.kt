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

import com.kotlinorm.ast.DeferredSubqueryExpression
import com.kotlinorm.ast.Expression
import com.kotlinorm.ast.KSelectableQueryRef
import com.kotlinorm.enums.KColumnType.CUSTOM_CRITERIA_SQL
import com.kotlinorm.enums.SortType
import com.kotlinorm.enums.SortType.Companion.Asc
import com.kotlinorm.enums.SortType.Companion.Desc
import com.kotlinorm.interfaces.KPojo

/**
 * KTableForSort
 *
 * DSL Class of Kronos, which the compiler plugin use to generate the `order by` code.
 * @param T the type of the table
 */
class KTableForSort<T : KPojo>: KTableForSelect<T>() {
    val sortedFields = mutableListOf<Pair<Field, SortType>>()
    val sortedItems = mutableListOf<SortItem>()

    @Suppress("UNCHECKED_CAST", "UNUSED")
    fun addSortField(field: Any) {
        when (field) {
            is Pair<*, *> -> {
                val value = field.first
                val sortType = field.second as SortType
                when (value) {
                    is Field -> addFieldSort(value, sortType)
                    is Expression -> addExpressionSort(value, sortType)
                    is KSelectable<*> -> addSelectableSort(value, sortType)
                    else -> addFieldSort(value as Field, sortType)
                }
            }

            is String -> {
                addFieldSort(Field(field, field, type = CUSTOM_CRITERIA_SQL), Asc)
            }

            is Expression -> {
                addExpressionSort(field, Asc)
            }

            is KSelectable<*> -> {
                addSelectableSort(field, Asc)
            }

            else -> {
                addFieldSort(field as Field, Asc)
            }
        }
    }

    fun addSortExpression(expression: Expression, sortType: SortType = Asc) {
        addExpressionSort(expression, sortType)
    }

    fun addSortSubquery(query: KSelectable<*>, sortType: SortType = Asc) {
        addSelectableSort(query, sortType)
    }

    @Suppress("UNUSED")
    fun Any?.desc(): Pair<Any?, SortType> =
        sortPair(Desc)

    @Suppress("UNUSED")
    fun Any?.asc(): Pair<Any?, SortType> =
        sortPair(Asc)

    private fun Any?.sortPair(sortType: SortType): Pair<Any?, SortType> {
        return (this.takeUnless { it is String } ?: Field(
            this.toString(),
            this.toString(),
            type = CUSTOM_CRITERIA_SQL
        )) to sortType
    }

    private fun addFieldSort(field: Field, sortType: SortType) {
        sortedFields.add(field to sortType)
        sortedItems.add(SortItem.FieldItem(field, sortType))
    }

    private fun addExpressionSort(expression: Expression, sortType: SortType) {
        sortedItems.add(SortItem.ExpressionItem(expression, sortType))
    }

    private fun addSelectableSort(query: KSelectable<*>, sortType: SortType) {
        addExpressionSort(DeferredSubqueryExpression.Scalar(KSelectableQueryRef(query)), sortType)
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
    }

    sealed class SortItem {
        data class FieldItem(val field: Field, val sortType: SortType) : SortItem()
        data class ExpressionItem(val expression: Expression, val sortType: SortType) : SortItem()
    }
}
