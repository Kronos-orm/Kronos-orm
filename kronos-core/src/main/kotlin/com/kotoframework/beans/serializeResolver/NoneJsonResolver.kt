package com.kotoframework.beans.serializeResolver

import com.kotoframework.interfaces.KronosSerializeResolver
import kotlin.reflect.KClass

class NoneSerializeResolver : KronosSerializeResolver {
    override fun <T> deserialize(serializedStr: String, kClass: KClass<*>): T {
        throw UnsupportedOperationException()
    }

    override fun serialize(obj: Any): String {
        throw UnsupportedOperationException()
    }
}