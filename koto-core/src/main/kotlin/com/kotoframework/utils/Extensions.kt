package com.kotoframework.utils

import com.kotoframework.KotoApp.fieldNamingStrategy
import com.kotoframework.annotations.Column
import com.kotoframework.annotations.Table
import com.kotoframework.interfaces.KPojo
import java.beans.BeanInfo
import java.beans.Introspector
import java.beans.PropertyDescriptor
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.findAnnotation

object Extensions {
    @Suppress("UNCHECKED_CAST")
    fun <T> KClass<*>.javaInstance(): T {
        return java.newInstance() as T
    }

    /*
    * It's an extension function of KPojo. It will return a map of the object.
    * */
    fun KPojo?.toMutableMap(vararg patch: Pair<String, Any?>): MutableMap<String, Any?> {
        if (this == null) return mutableMapOf()
        val map: MutableMap<String, Any?> = HashMap()
        val beanInfo: BeanInfo = Introspector.getBeanInfo(this::class.java)
        val propertyDescriptors = beanInfo.propertyDescriptors
        for (i in propertyDescriptors.indices) {
            val descriptor: PropertyDescriptor = propertyDescriptors[i]
            val propertyName: String = descriptor.name
            if (propertyName != "class") {
                map[propertyName] = descriptor.readMethod.invoke(this)
            }
        }
        return map.apply { putAll(patch) }
    }

    /* It's an extension function of KPojo. It will return a map of the object. */
    fun KPojo?.toMap(vararg patch: Pair<String, Any?>): Map<String, Any?> {
        return this.toMutableMap(*patch)
    }

    inline fun <reified T : KPojo> KClass<out T>.tableName(): String {
        return this.findAnnotation<Table>()?.name ?: fieldNamingStrategy.k2db(this.simpleName!!)
    }

    inline fun <reified T : KPojo> T.tableName(): String {
        return this::class.tableName()
    }

    fun KProperty<*>.columnName(): String {
        return this.findAnnotation<Column>()?.name ?: fieldNamingStrategy.k2db(this.name)
    }

}