package com.kotoframework.interfaces

import kotlin.reflect.KClass

interface KotoSerializeResolver {
    fun <T> deserialize(serializedStr: String, kClass: KClass<*>): T
    fun serialize(obj: Any): String
}