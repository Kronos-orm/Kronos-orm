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

import com.kotlinorm.interfaces.KPojo
import kotlin.reflect.KProperty

/**
 * KTable
 *
 * DSL Class of Kronos, which the compiler plugin use to generate the `select` code.
 * to add Fields, you can use following:
 * 1. `it.<field1> + it.<field2>`
 * 2. `it.<field1> + it.<field2>.as_("<alias>")`
 * 3. `addField(Field(columnName, optionalName))`
 * 4. `Field(columnName, optionalName).setAlias("<alias>")`
 * 5. `count(it.<field>)` or `count(1)` or `count(it.<field>).as_("<alias>")`
 *
 * @param T the type of the table
 */
open class KTableForReference<T : KPojo> {
    val fields: MutableList<Field> = mutableListOf()

    operator fun KProperty<*>.plus(@Suppress("UNUSED_PARAMETER") other: KProperty<*>) = false
    operator fun KProperty<*>.plus(@Suppress("UNUSED_PARAMETER") other: Boolean?) = false
    operator fun Boolean.plus(@Suppress("UNUSED_PARAMETER") other: KProperty<*>) = false

    operator fun KProperty<*>.unaryPlus() = false

    /**
     * Adds a field to the collection of fields.
     *
     * @param property the field to be added
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun addField(property: Field) {
        fields += property
    }

    companion object {
        /**
         * Creates a KTable instance with the given KPojo object as the data source and applies the given block to it.
         *
         * @param T The type of the KPojo object.
         * @param block The block of code to be applied to the KTable instance.
         * @return The resulting KTable instance after applying the block.
         */
        fun <T : KPojo> T.afterReference(block: KTableForReference<T>.(T) -> Unit) = KTableForReference<T>().block(this)
    }
}