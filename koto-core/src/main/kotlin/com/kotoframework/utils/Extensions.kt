package com.kotoframework.utils

import com.kotoframework.beans.serializeResolver.MoshiResolver
import com.kotoframework.interfaces.KPojo

object Extensions {
    private val resolver = MoshiResolver()
    @Suppress("UNCHECKED_CAST")
    fun KPojo?.toMutableMap(vararg patch: Pair<String, Any?>): MutableMap<String, Any?> {
        return (resolver.moshi.adapter(this!!.javaClass).toJsonValue(this) as MutableMap<String, Any?>).apply {
            patch.forEach { this[it.first] = it.second }
        }
    }

    /* It's an extension function of KPojo. It will return a map of the object. */
    fun KPojo?.toMap(vararg patch: Pair<String, Any?>): Map<String, Any?> {
        return this.toMutableMap(*patch)
    }
}