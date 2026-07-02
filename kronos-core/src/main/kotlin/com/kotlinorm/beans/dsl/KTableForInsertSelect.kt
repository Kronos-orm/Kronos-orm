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

import com.kotlinorm.functions.FunctionHandler
import com.kotlinorm.interfaces.KPojo

/**
 * Collects values used to rewrite an INSERT SELECT source projection.
 */
open class KTableForInsertSelect<T : KPojo> {
    val values: MutableList<Any?> = mutableListOf()
    val f: FunctionHandler = FunctionHandler

    @Suppress("UNUSED_PARAMETER")
    operator fun get(vararg values: Any?): Unit = Unit

    operator fun Any?.plus(@Suppress("UNUSED_PARAMETER") other: Any?): Any? = null

    operator fun Any?.minus(@Suppress("UNUSED_PARAMETER") other: Any?): Number? = null

    operator fun Any?.times(@Suppress("UNUSED_PARAMETER") other: Any?): Number? = null

    operator fun Any?.div(@Suppress("UNUSED_PARAMETER") other: Any?): Number? = null

    operator fun Any?.rem(@Suppress("UNUSED_PARAMETER") other: Any?): Number? = null

    /**
     * Adds one source-selected value or expression to the INSERT SELECT value list.
     */
    fun addValue(value: Any?) {
        values += value
    }

    companion object {
        /**
         * Runs an INSERT SELECT mapping block against a source row instance.
         */
        fun <T : KPojo> T.afterInsertSelect(block: KTableForInsertSelect<T>.(T) -> Unit): List<Any?> {
            val table = KTableForInsertSelect<T>()
            table.block(this)
            return table.values
        }
    }
}
