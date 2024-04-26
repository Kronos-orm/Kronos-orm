package com.kotoframework.beans.dsl

import com.kotoframework.annotations.Column
import com.kotoframework.interfaces.KPojo
import com.kotoframework.utils.fieldK2db
import kotlin.reflect.KProperty
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties

open class KTable<T : KPojo>(open val it: T) {
     val fields: MutableList<Field> = mutableListOf()
     val fieldParamMap: MutableMap<Field, Any?> = mutableMapOf()

    operator fun Any?.plus(other: Any?): Int = 1
    operator fun Any?.unaryPlus(): Int = 1

    @JvmName("KPropertySetValue")
    fun KProperty<*>.setValue(value: Any?) = setValue(this.toField(), value)

    @JvmName("FieldSetValue")
    fun Field.setValue(value: Any?) = setValue(this, value)

    fun Any?.alias(alias: String): String = alias

    fun addField(property: Field) {
        fields += property
    }

    fun setValue(property: Field, value: Any?) {
        addField(property) // Add the property to the field list
        fieldParamMap[property] = value
    }

    fun KProperty<*>.toField(): Field{
        return Field(this.findAnnotation<Column>()?.name ?: fieldK2db(this.name), this.name)
    }
}