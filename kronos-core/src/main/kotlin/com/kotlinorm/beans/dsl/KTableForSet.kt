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

import com.kotlinorm.Kronos.fieldNamingStrategy
import com.kotlinorm.annotations.Column
import com.kotlinorm.interfaces.KPojo
import kotlin.reflect.KProperty

/**
 * KTable
 *
 * DSL Class of Kronos, which the compiler plugin use to generate the `set` code.
 * to set values, you can use following:
 * 1. `it.<field1> = value`
 * 2. `Field(columnName, optionalName).setValue(value)`
 * 3. `it::<field1>.setValue(value)`
 * 4. `setValue(Field(columnName, optionalName), value)`
 * 5. `it["columnName"] = value`
 *
 * for plusAssign and minusAssign, you can use
 * 1. `it.<field1> += inc` or `it.<field1> -= dec`
 * 2. `Field(columnName, optionalName) += inc` or `Field(columnName, optionalName) -= dec`
 * 3. `it["columnName"] += inc` or `it["columnName"] -= dec`
 * 4. `it::<field1> += inc` or `it::<field1> -= dec`
 *
 * @param T the type of the table
 */
open class KTableForSet<T : KPojo> {
    val fields: MutableList<Field> = mutableListOf()
    val fieldParamMap: MutableMap<Field, Any?> = mutableMapOf()
    val plusAssignFields: MutableList<Pair<Field, Number>> = mutableListOf()
    val minusAssignFields: MutableList<Pair<Field, Number>> = mutableListOf()

    operator fun Any?.plusAssign(other: Number) {}

    operator fun Any?.minusAssign(other: Number) {}


    operator fun KPojo.get(column: String) = this.kronosColumns().first { it.columnName == column }
    operator fun KPojo.set(@Suppress("UNUSED_PARAMETER") column: String, @Suppress("UNUSED_PARAMETER") value: Any?): () -> Unit = {}

    /**
     * Sets the value of a Field with the given value.
     *
     * @param value the value to set the Field to
     * @return the updated value of the Field
     */
    @JvmName("FieldSetValue")
    fun Field.setValue(value: Any?) = setValue(this, value)

    /**
     * Adds a field to the collection of fields.
     *
     * @param property the field to be added
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun addField(property: Field) {
        fields += property
    }

    /**
     * Sets the value of a Field with the given value.
     *
     * @param property the Field to set the value for
     * @param value the value to set the Field to
     */
    fun setValue(property: Field, value: Any?) {
        addField(property) // Add the property to the field list
        fieldParamMap[property] = value
    }

    @Suppress("MemberVisibilityCanBePrivate", "UNUSED")
    fun setAssign(type: String, property: Field, value: Number) {
        addField(property)
        when (type) {
            "+" -> plusAssignFields += Pair(property, value)
            "-" -> minusAssignFields += Pair(property, value)
        }
    }

    companion object {
        /**
         * Creates a KTable instance with the given KPojo object as the data source and applies the given block to it.
         *
         * @param T The type of the KPojo object.
         * @param block The block of code to be applied to the KTable instance.
         * @return The resulting KTable instance after applying the block.
         */
        fun <T : KPojo> T.afterSet(block: KTableForSet<T>.(T) -> Unit) = KTableForSet<T>().block(this)
    }
}