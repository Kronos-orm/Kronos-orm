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

import com.kotlinorm.annotations.Column
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.utils.fieldK2db
import kotlin.reflect.KProperty
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.findAnnotation

/**
 * KTable
 *
 * DSL Class of Kronos, which the compiler plugin use to generate the `select` or `set` code.
 * to add Fields, you can use `it.<field1> + it.<field2>`
 * or `addField(Field(columnName, optionalName))`
 *
 * to set values, you can use `it.<field1> = value`
 * or `Field(columnName, optionalName).setValue(value)`
 * or `it::<field1>.setValue(value)`
 * or `setValue(Field(columnName, optionalName), value)`
 *
 * @param T the type of the table
 *
 * @property it the instance of the table
 */
open class KTable<T : KPojo>(open val it: T) {
    val fields: MutableList<Field> = mutableListOf()
    var propParamMap: MutableMap<String, Any?> = mutableMapOf()
    val fieldParamMap: MutableMap<Field, Any?> = mutableMapOf()

    /**
     * Retrieves the value from the 'propParamMap' based on the provided 'fieldName'.
     *
     * @param fieldName the name of the field to retrieve the value for
     * @return the value associated with the provided 'fieldName', or null if not found
     */
    fun getValueByFieldName(fieldName: String): Any? {
        return propParamMap[fieldName]
    }

    /**
     * Overloaded operator function that adds two objects of type Any?.
     *
     * @param other the object to be added to this object.
     * @return an integer value of 1.
     */
    operator fun Any?.plus(@Suppress("UNUSED_PARAMETER") other: Any?): Int = 1

    /**
     * Overloaded unary plus operator that returns an integer value of 1.
     *
     * @return an integer value of 1
     */
    operator fun Any?.unaryPlus(): Int = 1

    /**
     * Sets the value of a KProperty with the given value.
     *
     * @param value the value to set the property to
     * @return the updated value of the property
     */
    @JvmName("KPropertySetValue")
    fun KProperty<*>.setValue(value: Any?) = setValue(this.toField(), value)

    /**
     * Sets the value of a Field with the given value.
     *
     * @param value the value to set the Field to
     * @return the updated value of the Field
     */
    @JvmName("FieldSetValue")
    fun Field.setValue(value: Any?) = setValue(this, value)

    /**
     * Sets an alias for the given object.
     *
     * @param alias the alias to set for the object
     * @return the provided alias
     */
    @Suppress("UNUSED")
    fun Any?.alias(alias: String): String = alias

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

    /**
     * Converts a Kotlin property to a Field object.
     *
     * @return The converted Field object.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun KProperty<*>.toField(): Field {
        return Field(this.findAnnotation<Column>()?.name ?: fieldK2db(this.name), this.name)
    }

    companion object {
        /**
         * Creates a KTable instance with the given KPojo object as the data source.
         *
         * @param T The type of the KPojo object.
         * @return A KTable instance with the given KPojo object as the data source.
         */
        fun <T : KPojo> T.table(): KTable<T> = KTable(this::class.createInstance())

        /**
         * Creates a KTable instance with the given KPojo object as the data source and applies the given block to it.
         *
         * @param T The type of the KPojo object.
         * @param block The block of code to be applied to the KTable instance.
         * @return The resulting KTable instance after applying the block.
         */
        fun <T : KPojo> T.tableRun(block: KTable<T>.() -> Unit): KTable<T> =
            KTable(this::class.createInstance()).apply(block)
    }
}