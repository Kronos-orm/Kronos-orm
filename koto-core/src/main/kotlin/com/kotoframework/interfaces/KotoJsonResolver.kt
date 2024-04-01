package com.kotoframework.interfaces

import kotlin.reflect.KClass

interface KotoJsonResolver {
    fun <T> parseJSON(json: String, kClass: KClass<*>): T
    fun toJSONString(obj: Any): String
}