package com.kotoframework.beans.serializeResolver

import com.google.gson.Gson
import com.kotoframework.interfaces.KotoSerializeResolver
import kotlin.reflect.KClass

class GsonResolver : KotoSerializeResolver {
    private val gson = Gson()

    @Suppress("UNCHECKED_CAST")
    override fun <T> deserialize(serializedStr: String, kClass: KClass<*>): T {
        return gson.fromJson(serializedStr, kClass.java) as T
    }

    override fun serialize(obj: Any): String {
        return gson.toJson(obj)
    }
}