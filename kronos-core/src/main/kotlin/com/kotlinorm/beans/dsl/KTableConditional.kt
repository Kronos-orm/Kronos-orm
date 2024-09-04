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

import com.kotlinorm.annotations.UnsafeCriteria
import com.kotlinorm.enums.NoValueStrategy

/**
 * KTableConditional
 *
 * DSL Class of Kronos, which the compiler plugin use to generate the `where` code.
 *
 * @param T the type of the table
 */
open class KTableConditional<T : KPojo> : KTable<T>() {
    var criteria: Criteria? = null

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
    operator fun <T> Iterable<T>?.contains(
        @Suppress("UNUSED_PARAMETER") other: @Suppress(
            "INVISIBLE_MEMBER", "INVISIBLE_REFERENCE"
        ) @kotlin.internal.NoInfer T?
    ) = true

    fun <T> T?.cast() = this as Any?

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
    operator fun <T> Comparable<T>?.compareTo(@Suppress("UNUSED_PARAMETER") other: Comparable<T>?) = 1

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
    operator fun <T, R> Comparable<T>?.compareTo(@Suppress("UNUSED_PARAMETER") other: Comparable<R>?) = 1

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
    operator fun Any?.compareTo(@Suppress("UNUSED_PARAMETER") other: Any?) = 1

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
    @Suppress("UnusedReceiverParameter")
    fun Boolean?.ifNoValue(@Suppress("UNUSED_PARAMETER") strategy: NoValueStrategy) = true

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
    infix fun Comparable<*>?.like(@Suppress("UNUSED_PARAMETER") other: String?) = true

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
    infix fun Comparable<*>?.notLike(@Suppress("UNUSED_PARAMETER") other: String?) = true

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
    infix fun Comparable<*>?.between(@Suppress("UNUSED_PARAMETER") other: ClosedRange<*>?) = true

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
    infix fun Comparable<*>?.notBetween(@Suppress("UNUSED_PARAMETER") other: ClosedRange<*>?) = true

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
    infix fun Comparable<*>?.matchLeft(@Suppress("UNUSED_PARAMETER") other: String?) = true

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
    infix fun Comparable<*>?.matchRight(@Suppress("UNUSED_PARAMETER") other: String?) = true

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
    infix fun Comparable<*>?.matchBoth(@Suppress("UNUSED_PARAMETER") other: String?) = true


    infix fun Comparable<*>?.regexp(@Suppress("UNUSED_PARAMETER") other: String?) = true

    /**
     * Checks if the given value is null.
     *
     * Only for compiler plugin to parse to [Criteria]
     *
     * Return `true` whether the value is null or not
     *
     * @return `true`
     */
    @Suppress("UnusedReceiverParameter")
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
    @Suppress("UnusedReceiverParameter")
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
    @Suppress("UnusedReceiverParameter")
    val Comparable<*>?.eq get() = true

    @Suppress("UnusedReceiverParameter")
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
    @Suppress("UnusedReceiverParameter")
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
    @Suppress("UnusedReceiverParameter")
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
    @Suppress("UnusedReceiverParameter")
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
    @Suppress("UnusedReceiverParameter")
    val Comparable<*>?.matchLeft get() = true

    /**
     * Checks if the given value is null.
     *
     * Only for compiler plugin to parse to [Criteria]
     *
     * Return `true` whether the value is null or not
     *
     * @return `true`
     */
    @Suppress("UnusedReceiverParameter")
    val Comparable<*>?.matchRight get() = true

    /**
     * Checks if the given value is null.
     *
     * Only for compiler plugin to parse to [Criteria]
     *
     * Return `true` whether the value is null or not
     *
     * @return `true`
     */
    @Suppress("UnusedReceiverParameter")
    val Comparable<*>?.matchBoth get() = true

    /**
     * Checks if the given value is null.
     *
     * Only for compiler plugin to parse to [Criteria]
     *
     * Return `true` whether the value is null or not
     *
     * @return `true`
     */
    @Suppress("UnusedReceiverParameter")
    val Comparable<*>?.isNull get() = true

    /**
     * Checks if the given value is null.
     *
     * Only for compiler plugin to parse to [Criteria]
     *
     * Return `true` whether the value is null or not
     *
     * @return `true`
     */
    @Suppress("UnusedReceiverParameter")
    val Comparable<*>?.notNull get() = true

    /**
     * Checks if the given value is null.
     *
     * Only for compiler plugin to parse to [Criteria]
     *
     * Return `true` whether the value is null or not
     *
     * @return `true`
     */
    @Suppress("UnusedReceiverParameter")
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
    @Suppress("UnusedReceiverParameter")
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
    @Suppress("UnusedReceiverParameter")
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
    @Suppress("UnusedReceiverParameter")
    val Comparable<*>?.ge get() = true

    @Suppress("UnusedReceiverParameter")
    val Comparable<*>?.regexp get() = true

    companion object {
        /**
         * Runs the given block on a new instance of [KTableConditional] with the given [T] object as the data source.
         *
         * @param T The type of the KPojo object.
         * @param block The block of code to be applied to the [KTableConditional] instance.
         * @return The resulting [KTableConditional] instance after applying the block.
         */
        fun <T : KPojo> T.conditionalRun(block: KTableConditional<T>.(T) -> Unit) =
            KTableConditional<T>().block(this)
    }
}