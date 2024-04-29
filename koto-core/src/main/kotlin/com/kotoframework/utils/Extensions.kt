package com.kotoframework.utils

import com.kotoframework.interfaces.KPojo
import java.beans.BeanInfo
import java.beans.Introspector
import java.beans.PropertyDescriptor
import java.util.HashMap
import kotlin.reflect.KClass
import kotlin.reflect.javaType

object Extensions {
    /* It's an extension function of KPojo. It will return a map of the object. */
    fun KPojo?.toMap(vararg pairs: Pair<String, Any?>): Map<String, Any?> {
        return this.toMutableMap(*pairs)
    }

    /* It's an extension function of KPojo. It will return a KPojo of the object. */
    inline fun <reified T : KPojo> KPojo.toKPojo(vararg pairs: Pair<String, Any?>): T {
        return this.toMap().toMutableMap().apply { putAll(pairs) }.toKPojo()
    }

    /* AN extension function of Map. It will return a KPojo of the map. */
    inline fun <reified T> Map<String, *>.toKPojo(): T {
        return this.toKPojo(T::class) as T
    }

    /* AN extension function of Map. It will return a KPojo of the map. */
    @Suppress("UNCHECKED_CAST")
    fun <T> Map<String, *>.toKPojo(clazz: Class<*>): T {
        return this.toKPojo(clazz.kotlin) as T
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun Map<String, *>.toKPojo(clazz: KClass<*>): Any {
        val constructor = clazz.constructors.first()
        return try {
            val booleanNames =
                constructor.parameters.filter { it.type.javaType.typeName == "java.lang.Boolean" }.map { it.name }
            constructor.callBy(constructor.parameters.associateWith {
                if (booleanNames.contains(it.name)) {
                    if (this[it.name] is Int)
                        (this[it.name] == 1)
                    else
                        (this[it.name] != null)
                } else {
                    this[it.name] ?: this[fieldDb2k(it.name!!)]
                }
            })
        } catch (e: IllegalArgumentException) {
            // compare the argument type of constructor and the given value, print which argument is mismatched
            val mismatchedArgument = constructor.parameters.first {
                if (this[it.name] == null) {
                    !it.isOptional
                } else {
                    it.type.javaType.typeName != this[it.name]!!.javaClass.typeName
                }
            }
            if (this[mismatchedArgument.name] == null) {
                throw IllegalArgumentException("The argument ${clazz.simpleName}.${mismatchedArgument.name} is null, but it's not optional.")
            } else {
                throw IllegalArgumentException("The argument ${clazz.simpleName}.${mismatchedArgument.name} is ${this[mismatchedArgument.name]!!.javaClass.typeName} but expected ${mismatchedArgument.type.javaType.typeName}.")
            }
        }
    }

    /* It's an extension function of KPojo. It will return a map of the object. */
    fun KPojo?.toMutableMap(vararg pairs: Pair<String, Any?>): MutableMap<String, Any?> {
        if (this == null) return mutableMapOf()
        val map: MutableMap<String, Any?> = HashMap()
        val beanInfo: BeanInfo = Introspector.getBeanInfo(this::class.java)
        val propertyDescriptors = beanInfo.propertyDescriptors
        for (i in propertyDescriptors.indices) {
            val descriptor = propertyDescriptors[i]
            val propertyName = descriptor.name
            if (propertyName != "class") {
                map[propertyName] = descriptor.readMethod.invoke(this)
            }
        }
        return map.apply { putAll(pairs) }
    }
}