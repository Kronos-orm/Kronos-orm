package com.kotoframework.beans.serializeResolver

import com.kotoframework.interfaces.KotoSerializeResolver
import com.squareup.moshi.Moshi
import kotlin.reflect.KClass
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class MoshiResolver : KotoSerializeResolver {
    val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    @Suppress("UNCHECKED_CAST")
    override fun <T> deserialize(serializedStr: String, kClass: KClass<*>): T {
        return moshi.adapter(kClass.java).fromJson(serializedStr) as T
    }

    override fun serialize(obj: Any): String {
        return moshi.adapter(obj.javaClass).toJson(obj)
    }
}