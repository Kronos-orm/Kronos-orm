package com.kotlinorm.orm.utils

import com.google.gson.Gson
import com.kotlinorm.interfaces.KronosSerializeResolver
import kotlin.reflect.KClass


object GsonResolver : KronosSerializeResolver {
    override fun <T> deserialize(serializedStr: String, kClass: KClass<*>): T {
        return Gson().fromJson<T>(serializedStr, kClass.java)
    }

    override fun deserializeObj(serializedStr: String, kClass: KClass<*>): Any {
        return Gson().fromJson(serializedStr, kClass.java)
    }

    override fun serialize(obj: Any): String {
        return Gson().toJson(obj)
    }
}