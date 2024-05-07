package com.kotlinorm.beans.dsl

import com.kotlinorm.annotations.UnsafeCriteria
import com.kotlinorm.enums.NoValueStrategy
import com.kotlinorm.interfaces.KPojo
import java.util.*

open class KTableConditional<T : KPojo>(override val it: T) : KTable<T>(it) {
    var criteria: Criteria? = null
    operator fun <T> Iterable<T>?.contains(
        @Suppress("UNUSED_PARAMETER") other: @Suppress(
            "INVISIBLE_MEMBER", "INVISIBLE_REFERENCE"
        ) @kotlin.internal.NoInfer T?
    ): Boolean = true

    operator fun Number?.compareTo(@Suppress("UNUSED_PARAMETER") other: Number?): Int = 1
    operator fun Date?.compareTo(@Suppress("UNUSED_PARAMETER") other: Date?): Int = 1
    fun Boolean?.ifNoValue(@Suppress("UNUSED_PARAMETER") strategy: NoValueStrategy): Boolean = true

    @UnsafeCriteria("It's not safe to compare String with other Type.")
    operator fun <T> String?.compareTo(@Suppress("UNUSED_PARAMETER") other: T?): Int = 1

    infix fun Comparable<*>?.like(@Suppress("UNUSED_PARAMETER") other: String?): Boolean = true
    infix fun Comparable<*>?.notLike(@Suppress("UNUSED_PARAMETER") other: String?): Boolean = true
    infix fun Comparable<*>?.between(@Suppress("UNUSED_PARAMETER") other: ClosedRange<*>?): Boolean = true
    infix fun Comparable<*>?.notBetween(@Suppress("UNUSED_PARAMETER") other: ClosedRange<*>?): Boolean = true
    infix fun Comparable<*>?.matchLeft(@Suppress("UNUSED_PARAMETER") other: String?): Boolean = true
    infix fun Comparable<*>?.matchRight(@Suppress("UNUSED_PARAMETER") other: String?): Boolean = true
    infix fun Comparable<*>?.matchBoth(@Suppress("UNUSED_PARAMETER") other: String?): Boolean = true

    fun String?.asSql(): Boolean = true
    val Comparable<*>?.eq get() = true
    val Comparable<*>?.neq get() = true
    val Comparable<*>?.like get() = true
    val Comparable<*>?.notLike get() = true
    val Comparable<*>?.matchLeft get() = true
    val Comparable<*>?.matchRight get() = true
    val Comparable<*>?.matchBoth get() = true
    val Comparable<*>?.isNull get() = true
    val Comparable<*>?.notNull get() = true
    val Comparable<*>?.lt get() = true
    val Comparable<*>?.gt get() = true
    val Comparable<*>?.le get() = true
    val Comparable<*>?.ge get() = true
}