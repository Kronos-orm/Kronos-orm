package com.kotlinorm.interfaces

import kotlin.reflect.KClass

interface KronosSerializeResolver {
    fun <T> deserialize(serializedStr: String, kClass: KClass<*>): T
    fun serialize(obj: Any): String
}