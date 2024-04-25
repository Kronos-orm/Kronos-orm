package com.kotoframework.beans.serializeResolver

import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.toJSONString
import com.kotoframework.interfaces.KotoSerializeResolver
import kotlin.reflect.KClass

class FastJson2Resolver : KotoSerializeResolver {
    @Suppress("UNCHECKED_CAST")
    override fun <T> deserialize(serializedStr: String, kClass: KClass<*>): T {
        return JSON.parseObject(serializedStr, kClass.java) as T
    }

    override fun serialize(obj: Any): String {
        return obj.toJSONString()
    }
}