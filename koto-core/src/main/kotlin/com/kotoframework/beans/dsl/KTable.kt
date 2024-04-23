package com.kotoframework.beans.dsl

import com.kotoframework.interfaces.KPojo
import com.kotoframework.types.Field
import com.kotoframework.utils.Extensions.columnName
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

open class KTable<T : KPojo>(open val it: T) {
    public val fields: MutableList<String> = mutableListOf()
    public val fieldParamMap: MutableMap<String, Any?> = mutableMapOf()

    operator fun Any?.plus(other: Any?): Int = 1
    operator fun Any?.unaryPlus(): Int = 1

    fun Any?.alias(alias: String): String = alias

    fun addField(property: Field) {
        when(property) {
            is KProperty<*> -> {
                fields += property.columnName()
            }
            is String -> fields += property
            else -> throw IllegalArgumentException("Unknown property type: $property")
        }
    }

    /**
     * Sets the value for the specified property in the fieldParamMap.
     *
     * @param property The property for which the value is being set.
     * @param value The value to be set for the property.
     * @throws IllegalArgumentException if the property type is unknown.
     */
    fun setValue(property: Field, value: Any?) {
        addField(property) // Add the property to the field list
        when(property) {
            is KProperty<*> -> {
                fieldParamMap[property.columnName()] = value // Set value for KProperty
            }
            is String -> fieldParamMap[property] = value // Set value for String property
            else -> throw IllegalArgumentException("Unknown property type: $property") // Throw exception for unknown property type
        }
    }

    fun <K> KProperty1<T, K>.set(value: K?) {
        addField(this)
        fieldParamMap[columnName()] = value
    }

    fun KPojo.getKProperty(columnName: String): KProperty<*> {
        return this::class.java.kotlin.memberProperties
            .first { it.name == columnName }
    }
}