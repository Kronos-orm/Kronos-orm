package com.kotlinorm

import com.google.gson.Gson
import com.kotlinorm.interfaces.KronosSerializeProcessor
import kotlin.reflect.KType
import kotlin.reflect.jvm.jvmErasure

object GsonProcessor : KronosSerializeProcessor {
    override fun deserialize(serializedStr: String, kType: KType): Any {
        return Gson().fromJson(serializedStr, kType.jvmErasure.java)
    }

    override fun serialize(obj: Any, kType: KType): String {
        return Gson().toJson(obj)
    }
}
