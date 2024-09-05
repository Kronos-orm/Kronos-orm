package com.kotlinorm.beans.serializeResolver

import com.kotlinorm.Kronos.serializeResolver
import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.orm.cascade.get
import com.kotlinorm.orm.cascade.set
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0


inline fun <reified T> serializable(toSerialize: KProperty0<String?>): Serializable<T?> {
    return Serializable(toSerialize, T::class)
}

// 自定义委托类
class Serializable<T>(
    private val toSerialize: KProperty0<String?>,
    private val targetKClass: KClass<*>
) : ReadWriteProperty<Any, T?> {
    @Suppress("UNCHECKED_CAST")
    override fun getValue(thisRef: Any, property: KProperty<*>): T? {
        if ((thisRef as KPojo)[toSerialize] == null) return null
        return serializeResolver.deserialize(thisRef[toSerialize] as String, targetKClass) as T?
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T?) {
        if (value == null) {
            (thisRef as KPojo)[toSerialize] = null
            return
        }
        (thisRef as KPojo)[toSerialize] = serializeResolver.serialize(value)
    }
}