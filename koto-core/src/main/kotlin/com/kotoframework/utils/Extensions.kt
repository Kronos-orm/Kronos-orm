package com.kotoframework.utils

import com.kotoframework.interfaces.KPojo
import kotlin.reflect.full.memberProperties

object Extensions {
    fun KPojo.toMutableMap(vararg patch: Pair<String, Any?>): MutableMap<String, Any?> {
        return this::class.memberProperties.associate { it.name to it.getter.call(this)}.toMutableMap().apply {
            patch.forEach { this[it.first] = it.second }
        }
    }

    /* It's an extension function of KPojo. It will return a map of the object. */
    fun KPojo.toMap(vararg patch: Pair<String, Any?>): Map<String, Any?> {
        return this.toMutableMap(*patch)
    }
}