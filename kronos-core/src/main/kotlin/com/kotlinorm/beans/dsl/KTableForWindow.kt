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

package com.kotlinorm.beans.dsl

import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.syntax.order.SqlOrdering

/**
 * Collects expressions used by a window function `OVER (...)` clause.
 */
class KTableForWindow<T : KPojo> {
    @Suppress("UNUSED_PARAMETER")
    fun partitionBy(vararg fields: Any?): Unit = Unit

    @Suppress("UNUSED_PARAMETER")
    fun orderBy(vararg fields: Any?): Unit = Unit

    @Suppress("UNUSED")
    fun Any?.asc(): Pair<Any?, SqlOrdering> = this to SqlOrdering.Asc

    @Suppress("UNUSED")
    fun Any?.desc(): Pair<Any?, SqlOrdering> = this to SqlOrdering.Desc
}
