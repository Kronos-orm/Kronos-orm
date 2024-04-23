package com.kotoframework.beans.dsl

import com.kotoframework.interfaces.KPojo
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

open class KTable<T : KPojo>(open val it: T) {
    public val criteria: Criteria? = null
    public val fields: MutableList<String> = mutableListOf()
    public val fieldParamMap: MutableMap<String, Any?> = mutableMapOf()

    operator fun Any?.plus(other: Any?): Int = 1
    operator fun Any?.unaryPlus(): Int = 1

    fun Any?.alias(alias: String): String = alias

    fun addField(property: Any){
        when(property) {
            is KProperty<*> -> {
                fields += property.columnName()
            }
            is String -> fields += property
            else -> throw IllegalArgumentException("Unknown property type: $property")
        }
    }

    fun setValue(property: Any, value: Any?) {
        addField(property)
        when(property) {
            is KProperty<*> -> {
                fieldParamMap[property.columnName()] = value
            }
            is String -> fieldParamMap[property] = value
            else -> throw IllegalArgumentException("Unknown property type: $property")
        }
    }

    fun <K> KProperty1<T, K>.set(value: K?) {
        addField(this)
        fieldParamMap[columnName()] = value
    }

    internal fun KProperty<*>.columnName(): String {
        return this.name
    }

    fun KPojo.getKProperty(columnName: String): KProperty<*> {
        return this::class.java.kotlin.memberProperties
            .first { it.name == columnName }
    }
}