package com.kotoframework.utils

import com.google.gson.Gson
import com.kotoframework.interfaces.KPojo

object Extensions {
    private val gson = Gson()
    fun KPojo?.toMutableMap(vararg patch: Pair<String, Any?>): MutableMap<String, Any?> {
        return gson.fromJson<MutableMap<String, Any?>>(gson.toJson(this), MutableMap::class.java).apply {
            patch.forEach { this[it.first] = it.second }
        }
    }

    /* It's an extension function of KPojo. It will return a map of the object. */
    fun KPojo?.toMap(vararg patch: Pair<String, Any?>): Map<String, Any?> {
        return this.toMutableMap(*patch)
    }
}