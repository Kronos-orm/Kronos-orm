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
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE", "UNUSED", "UnusedReceiverParameter")

package com.kotlinorm.beans.dsl

import com.kotlinorm.annotations.UnsafeCriteria
import com.kotlinorm.enums.NoValueStrategyType
import com.kotlinorm.functions.FunctionHandler
import com.kotlinorm.interfaces.KPojo

/**
 * kTableForCondition
 *
 * DSL Class of Kronos, which the compiler plugin use to generate the `where` code.
 *
 * @param T the type of the table
 */
open class KTableForCondition<T : KPojo> {
    var criteria: Criteria? = null
    var criteriaParamMap: MutableMap<String, Any?> = mutableMapOf()
    val f: FunctionHandler = FunctionHandler

    /**
     * Retrieves the value from the 'propParamMap' based on the provided 'fieldName'.
     *
     * @param fieldName the name of the field to retrieve the value for
     * @return the value associated with the provided 'fieldName', or null if not found
     */
    fun getValueByFieldName(fieldName: String): Any? {
        return criteriaParamMap[fieldName]
    }

    val <T : Any?> T?.value get() = this

    /**
     * Check if the iterable contains the element
     *
     * Only for compiler plugin to parse to [Criteria]
     *
     * This expression always return `true` whether the iterable contains the element or not
     *
     * @param other the element to check
     * @return `true`
     */

    operator fun <K> Iterable<K?>?.contains(other: @kotlin.internal.NoInfer K?) = true

    @JvmName("containsOutK")
    operator fun <K> Collection<K?>?.contains(other: @kotlin.internal.NoInfer K?) = true

    operator fun <K> Array<K?>?.contains(other: @kotlin.internal.NoInfer K?) = true

    @JvmName("containsOutK")
    operator fun <K> Array<out K?>?.contains(other: @kotlin.internal.NoInfer K?) = true

    operator fun IntArray?.contains(other: @kotlin.internal.NoInfer Number?) = true

    operator fun LongArray?.contains(other: @kotlin.internal.NoInfer Number?) = true

    operator fun FloatArray?.contains(other: @kotlin.internal.NoInfer Number?) = true

    operator fun DoubleArray?.contains(other: @kotlin.internal.NoInfer Number?) = true

    operator fun CharArray?.contains(other: @kotlin.internal.NoInfer Char?) = true

    operator fun BooleanArray?.contains(other: @kotlin.internal.NoInfer Boolean?) = true

    operator fun CharSequence?.contains(other: @kotlin.internal.NoInfer Char?) = true
    
    operator fun CharSequence?.contains(other: CharSequence): Boolean = true

    val CharSequence?.contains get() = true

    fun <T> T?.cast() = this as Any?

    operator fun KPojo.minus(field: Any?) = this

    /**
     * Check if the Comparable<*> is greater than the specified
     *
     * Only for compiler plugin to parse to [Criteria]
     *
     * Return 1 whether which one is greater
     *
     * @param other The Comparable<*> to compare with.
     * @param T The type of the Comparable<*> to compare with.
     * @return `1`
     */
    operator fun <T> Comparable<T>?.compareTo(other: Comparable<T>?) = 1

    /**
     * Check if the Comparable<*> is greater than the specified
     *
     * Only for compiler plugin to parse to [Criteria]
     *
     * Return 1 whether which one is greater
     *
     * @param other The Comparable<*> to compare with.
     * @param T The type of the Comparable<*> to compare with.
     * @param R The type of the Comparable<*> to compare with.
     * @return `1`
     */
    @JvmName("compareToDifferentType")
    @UnsafeCriteria("It's not safe to compare different Type, use `.cast()` to declare that the expression is safe.")
    operator fun <T, R> Comparable<T>?.compareTo(other: Comparable<R>?) = 1

    /**
     * Check if the Comparable<*> is greater than the specified
     *
     * Only for compiler plugin to parse to [Criteria]
     *
     * Return 1 whether which one is greater
     *
     * @param other The Comparable<*> to compare with.
     * @return `1`
     */
    operator fun Any?.compareTo(other: Any?) = 1

    /**
     * Set the no value strategy
     *
     * Only for compiler plugin to parse to [Criteria]
     *
     * Return 1 whether which strategy is used
     *
     * @param strategy The no value strategy
     * @return `1`
     */
    fun Boolean?.ifNoValue(strategy: NoValueStrategyType) = true

    /**
     * Checks if the given value is like the specified string.
     *
     * Only for compiler plugin to parse to [Criteria]
     *
     * Return `true` whether the value is like the string or not
     *
     * @param other The string to compare with.
     * @return `true`
     */
    infix fun Comparable<*>?.like(other: String?) = true

    /**
     * Checks if the given value is not like the specified string.
     *
     * Only for compiler plugin to parse to [Criteria]
     *
     * Return `true` whether the value is not like the string or not
     *
     * @param other The string to compare with.
     * @return `true`
     */
    infix fun Comparable<*>?.notLike(other: String?) = true

    /**
     * Checks if the given value is between the specified range.
     *
     * Only for compiler plugin to parse to [Criteria]
     *
     * Return `true` whether the value is between the range or not
     *
     * @param other The range to compare with.
     * @return `true`
     */
    infix fun Comparable<*>?.between(other: ClosedRange<*>?) = true

    /**
     * Checks if the given value is not between the specified range.
     *
     * Only for compiler plugin to parse to [Criteria]
     *
     * Return `true` whether the value is not between the range or not
     *
     * @param other The range to compare with.
     * @return `true`
     */
    infix fun Comparable<*>?.notBetween(other: ClosedRange<*>?) = true

    /**
     * Checks if the given value matches the specified string.
     *
     * Only for compiler plugin to parse to [Criteria]
     *
     * Return `true` whether the value matches the string or not
     *
     * @param other The string to compare with.
     * @return `true`
     */
    infix fun Comparable<*>?.startsWith(other: String?) = true

    /**
     * Checks if the given value matches the specified string.
     *
     * Only for compiler plugin to parse to [Criteria]
     *
     * Return `true` whether the value matches the string or not
     *
     * @param other The string to compare with.
     * @return `true`
     */
    infix fun Comparable<*>?.endsWith(other: String?) = true

    infix fun Comparable<*>?.regexp(other: String?) = true

    infix fun Comparable<*>?.notRegexp(other: String?) = true

    /**
     * Checks if the given value is null.
     *
     * Only for compiler plugin to parse to [Criteria]
     *
     * Return `true` whether the value is null or not
     *
     * @return `true`
     */
    fun String?.asSql() = true

    /**
     * Checks if the given value is null.
     *
     * Only for compiler plugin to parse to [Criteria]
     *
     * Return `true` whether the value is null or not
     *
     * @return `true`
     */
    fun Boolean?.asSql() = true

    /**
     * Checks if the given value is null.
     *
     * Only for compiler plugin to parse to [Criteria]
     *
     * Return `true` whether the value is null or not
     *
     * @return `true`
     */
    val Comparable<*>?.eq get() = true

    val KPojo.eq get() = true

    /**
     * Checks if the given value is null.
     *
     * Only for compiler plugin to parse to [Criteria]
     *
     * Return `true` whether the value is null or not
     *
     * @return `true`
     */
    val Comparable<*>?.neq get() = true

    /**
     * Checks if the given value is null.
     *
     * Only for compiler plugin to parse to [Criteria]
     *
     * Return `true` whether the value is null or not
     *
     * @return `true`
     */
    val Comparable<*>?.like get() = true

    /**
     * Checks if the given value is null.
     *
     * Only for compiler plugin to parse to [Criteria]
     *
     * Return `true` whether the value is null or not
     *
     * @return `true`
     */
    val Comparable<*>?.notLike get() = true

    /**
     * Checks if the given value is null.
     *
     * Only for compiler plugin to parse to [Criteria]
     *
     * Return `true` whether the value is null or not
     *
     * @return `true`
     */
    val Comparable<*>?.startsWith get() = true

    /**
     * Checks if the given value is null.
     *
     * Only for compiler plugin to parse to [Criteria]
     *
     * Return `true` whether the value is null or not
     *
     * @return `true`
     */
    val Comparable<*>?.endsWith get() = true

    /**
     * Checks if the given value is null.
     *
     * Only for compiler plugin to parse to [Criteria]
     *
     * Return `true` whether the value is null or not
     *
     * @return `true`
     */
    val Any?.isNull get() = true

    /**
     * Checks if the given value is null.
     *
     * Only for compiler plugin to parse to [Criteria]
     *
     * Return `true` whether the value is null or not
     *
     * @return `true`
     */
    val Any?.notNull get() = true

    /**
     * Checks if the given value is null.
     *
     * Only for compiler plugin to parse to [Criteria]
     *
     * Return `true` whether the value is null or not
     *
     * @return `true`
     */
    val Comparable<*>?.lt get() = true

    /**
     * Checks if the given value is null.
     *
     * Only for compiler plugin to parse to [Criteria]
     *
     * Return `true` whether the value is null or not
     *
     * @return `true`
     */
    val Comparable<*>?.gt get() = true

    /**
     * Checks if the given value is null.
     *
     * Only for compiler plugin to parse to [Criteria]
     *
     * Return `true` whether the value is null or not
     *
     * @return `true`
     */
    val Comparable<*>?.le get() = true

    /**
     * Checks if the given value is null.
     *
     * Only for compiler plugin to parse to [Criteria]
     *
     * Return `true` whether the value is null or not
     *
     * @return `true`
     */
    val Comparable<*>?.ge get() = true

    val Comparable<*>?.regexp get() = true

    val Comparable<*>?.notRegexp get() = true

    fun buildContainsStr(str: String?): String? {
        return if(str == null) null
        else "%$str%"
    }

    companion object {
        /**
         * Runs the given block on a new instance of [KTableForCondition] with the given [T] object as the data source.
         *
         * @param T The type of the KPojo object.
         * @param block The block of code to be applied to the [KTableForCondition] instance.
         * @return The resulting [KTableForCondition] instance after applying the block.
         */
        fun <T : KPojo> T.afterFilter(block: KTableForCondition<T>.(T) -> Unit) =
            KTableForCondition<T>().block(this)
    }
}