/**
 * Copyright 2022-2024 kronos-orm
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

import com.kotlinorm.enums.SortType
import com.kotlinorm.enums.SortType.Companion.Asc
import com.kotlinorm.enums.SortType.Companion.Desc

/**
 * KTableSortable
 *
 * DSL Class of Kronos, which the compiler plugin use to generate the `order by` code.
 * @param T the type of the table
 */
class KTableSortable<T : KPojo> : KTable<T>() {
    internal val sortFields = mutableListOf<Pair<Field, SortType>>()

    @Suppress("UNCHECKED_CAST", "UNUSED")
    fun addSortField(field: Any) {
        when (field) {
            is Pair<*, *> -> {
                sortFields.add(field as Pair<Field, SortType>)
            }

            is String -> {
                sortFields.add(Field(field, field, type = "string") to Asc)
            }

            else -> {
                sortFields.add((field to Asc) as Pair<Field, SortType>)
            }
        }
    }

    @Suppress("UNUSED")
    fun Any?.desc(): Pair<Any?, SortType> =
        (this.takeUnless { it is String } ?: Field(this.toString(), this.toString(), type = "string")) to Desc

    @Suppress("UNUSED")
    fun Any?.asc(): Pair<Any?, SortType> =
        (this.takeUnless { it is String } ?: Field(this.toString(), this.toString(), type = "string")) to Asc

    companion object {
        /**
         * Creates a KTable instance with the given KPojo object as the data source and applies the given block to it.
         *
         * @param T The type of the KPojo object.
         * @param block The block of code to be applied to the KTable instance.
         * @return The resulting KTable instance after applying the block.
         */
        fun <T : KPojo> T.sortableRun(block: KTableSortable<T>.(T) -> Unit) =
            KTableSortable<T>().block(this)
    }
}