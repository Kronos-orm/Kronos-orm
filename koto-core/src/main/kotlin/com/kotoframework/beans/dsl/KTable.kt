package com.kotoframework.beans.dsl

import com.kotoframework.interfaces.KPojo

open class KTable<T : KPojo>(open val it: T) {
     val fields: MutableList<Field> = mutableListOf()
     val fieldParamMap: MutableMap<Field, Any?> = mutableMapOf()

    operator fun Any?.plus(other: Any?): Int = 1
    operator fun Any?.unaryPlus(): Int = 1

    fun Any?.alias(alias: String): String = alias

    fun addField(property: Field) {
        fields += property
    }

    fun setValue(property: Field, value: Any?) {
        addField(property) // Add the property to the field list
        fieldParamMap[property] = value
    }
}