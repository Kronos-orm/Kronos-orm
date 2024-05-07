package com.kotlinorm.beans.serializeResolver

import com.kotlinorm.interfaces.KronosSerializeResolver
import kotlin.reflect.KClass

class NoneSerializeResolver : KronosSerializeResolver {
    override fun <T> deserialize(serializedStr: String, kClass: KClass<*>): T {
        throw UnsupportedOperationException()
    }

    override fun serialize(obj: Any): String {
        throw UnsupportedOperationException()
    }
}