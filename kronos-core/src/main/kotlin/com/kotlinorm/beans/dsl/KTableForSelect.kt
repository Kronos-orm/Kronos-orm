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
import com.kotlinorm.ast.SelectItem
import com.kotlinorm.ast.SelectItemAliasMetadata
import com.kotlinorm.ast.SelectItemSourceScope
import com.kotlinorm.functions.FunctionHandler
import com.kotlinorm.interfaces.KPojo

/**
 * KTable
 *
 * DSL Class of Kronos, which the compiler plugin use to generate the `select` code.
 * to add Fields, you can use following:
 * 1. `[it.<field1>, it.<field2>]`
 * 2. `[it.<field1>, it.<field2>.as_("<alias>")]`
 * 3. `addField(Field(columnName, optionalName))`
 * 4. `Field(columnName, optionalName).setAlias("<alias>")`
 * 5. `count(it.<field>)` or `count(1)` or `count(it.<field>).as_("<alias>")`
 *
 * @param T the type of the table
 */
open class KTableForSelect<T : KPojo> {
    val fields: MutableList<Field> = mutableListOf()
    val selectItems: MutableList<SelectItem> = mutableListOf()
    internal val projectionItems: MutableList<ProjectionItem> = mutableListOf()
    val f: FunctionHandler = FunctionHandler

    @Suppress("UNUSED_PARAMETER")
    operator fun get(vararg fields: Any?): Unit = Unit

    operator fun Any?.plus(@Suppress("UNUSED_PARAMETER") other: Any?): Any? = null

    operator fun Any?.minus(@Suppress("UNUSED_PARAMETER") other: Any?): Number? = null

    operator fun Any?.times(@Suppress("UNUSED_PARAMETER") other: Any?): Number? = null

    operator fun Any?.div(@Suppress("UNUSED_PARAMETER") other: Any?): Number? = null

    operator fun Any?.rem(@Suppress("UNUSED_PARAMETER") other: Any?): Number? = null

    /**
     * Overloaded operator function that minus two objects of type Any?.
     *
     * @param other the object to be added to this object.
     * @return an integer value of 1.
     */
    operator fun KPojo?.minus(@Suppress("UNUSED_PARAMETER") other: Any?) = this

    /**
     * Sets an alias for the given object.
     *
     * @param alias the alias to set for the object
     * @return the provided alias
     */
    @Suppress("UNUSED")
    infix fun Field.as_(@Suppress("UNUSED_PARAMETER") alias: String): Field = this

    @Suppress("UNUSED")
    infix fun FunctionField.as_(@Suppress("UNUSED_PARAMETER") alias: String): FunctionField = this

    @Suppress("UNUSED")
    infix fun Expression.as_(@Suppress("UNUSED_PARAMETER") alias: String): Expression = this

    @Suppress("UNUSED")
    infix fun <R> R.as_(@Suppress("UNUSED_PARAMETER") alias: String): R = this

    /**
     * Adds a SQL window `OVER (...)` clause to a function expression.
     */
    @Suppress("UNUSED")
    fun <R> R.over(block: KTableForWindow<T>.() -> Unit): R {
        KTableForWindow<T>().block()
        return this
    }

    /**
     * Adds a field to the collection of fields.
     *
     * @param property the field to be added
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun addField(property: Field) {
        fields += property
        projectionItems += ProjectionItem.FieldItem(property)
    }

    fun addScalarSubquery(query: KSelectable<*>, alias: String) {
        val expression = DeferredSubqueryExpression.Scalar(KSelectableQueryRef(query))
        val item = SelectItem.ExpressionSelectItem(
            expression = expression,
            alias = alias,
            metadata = SelectItemAliasMetadata(
                outputName = alias,
                expression = expression,
                scope = SelectItemSourceScope.SELECTED,
                sourceField = null,
                userReferenceable = true
            )
        )
        selectItems += item
        projectionItems += ProjectionItem.SelectItemValue(item)
    }

    fun Field.setAlias(alias: String): Field {
        this.name = alias
        return this
    }

    companion object {
        /**
         * Creates a KTable instance with the given KPojo object as the data source and applies the given block to it.
         *
         * @param T The type of the KPojo object.
         * @param block The block of code to be applied to the KTable instance.
         * @return The resulting KTable instance after applying the block.
         */
        fun <T : KPojo> T.afterSelect(block: KTableForSelect<T>.(T) -> Unit) = KTableForSelect<T>().block(this)
    }

    internal sealed class ProjectionItem {
        data class FieldItem(val field: Field) : ProjectionItem()
        data class SelectItemValue(val item: SelectItem) : ProjectionItem()
    }
}
