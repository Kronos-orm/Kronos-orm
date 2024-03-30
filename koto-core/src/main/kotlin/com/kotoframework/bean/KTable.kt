package com.kotoframework.bean

import com.kotoframework.interfaces.KPojo

open class KTable<T : KPojo>(open val it: T) {
    public var criteria: KCriteria? = null

    operator fun Any?.plus(other: Any?): Int = 1
    operator fun Any?.unaryPlus(): Int = 1
}