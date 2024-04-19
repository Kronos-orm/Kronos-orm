package com.kotoframework.beans.dsl

import com.kotoframework.interfaces.KPojo

open class KTable<T : KPojo>(open val it: T) {
    public var criteria: Criteria? = null
    public var fields: List<String> = listOf()
    public var map: MutableMap<String, Any?> = mutableMapOf()

    operator fun Any?.plus(other: Any?): Int = 1
    operator fun Any?.unaryPlus(): Int = 1
}