package com.kotlinorm.orm.beans

import com.google.gson.Gson
import com.kotlinorm.interfaces.KronosSerializeResolver
import kotlin.reflect.KClass

object GsonResolver : KronosSerializeResolver {
    override fun deserialize(serializedStr: String, kClass: KClass<*>): Any {
        return Gson().fromJson(serializedStr, kClass.java)
    }

    override fun serialize(obj: Any): String {
        return Gson().toJson(obj)
    }
}