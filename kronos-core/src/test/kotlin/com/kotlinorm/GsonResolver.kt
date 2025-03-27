package com.kotlinorm

import com.google.gson.Gson
import com.kotlinorm.interfaces.KronosSerializeProcessor
import kotlin.reflect.KClass

object GsonProcessor : KronosSerializeProcessor {
    override fun deserialize(serializedStr: String, kClass: KClass<*>): Any {
        return Gson().fromJson(serializedStr, kClass.java)
    }

    override fun serialize(obj: Any): String {
        return Gson().toJson(obj)
    }
}