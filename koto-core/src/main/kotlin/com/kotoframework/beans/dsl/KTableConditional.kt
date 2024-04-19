package com.kotoframework.beans.dsl

import com.kotoframework.annotations.UnsafeCriteria
import com.kotoframework.interfaces.KPojo
import java.util.*

open class KTableConditional<T : KPojo>(override val it: T): KTable<T>(it) {
    operator fun <T> Collection<T>?.contains(
        other: @Suppress(
            "INVISIBLE_MEMBER", "INVISIBLE_REFERENCE"
        ) @kotlin.internal.NoInfer T?
    ): Boolean = true

    operator fun Number?.compareTo(other: Number?): Int = 1
    operator fun Date?.compareTo(other: Date?): Int = 1

    @UnsafeCriteria("It's not safe to compare String with other Type.")
    operator fun <T> String?.compareTo(other: T?): Int = 1

    infix fun Comparable<*>?.like(other: String?): Boolean = true
    infix fun Comparable<*>?.notLike(other: String?): Boolean = true
    infix fun Comparable<*>?.between(other: ClosedRange<*>?): Boolean = true
    infix fun Comparable<*>?.notBetween(other: ClosedRange<*>?): Boolean = true
    infix fun Comparable<*>?.matchLeft(other: String?): Boolean = true
    infix fun Comparable<*>?.matchRight(other: String?): Boolean = true
    infix fun Comparable<*>?.matchBoth(other: String?): Boolean = true

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