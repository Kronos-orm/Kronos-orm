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

import com.kotlinorm.enums.ASC
import com.kotlinorm.enums.DESC
import com.kotlinorm.enums.SortType

/**
 * KTableSortable
 *
 * DSL Class of Kronos, which the compiler plugin use to generate the `order by` code.
 * @param T the type of the table
 *
 * @property it the instance of the table
 */
class KTableSortable<T : KPojo>(override val it: T) : KTable<T>(it) {
    val Any?.desc get(): Pair<Any?, SortType> = this to DESC
    val Any?.asc get(): Pair<Any?, SortType> = this to ASC
}