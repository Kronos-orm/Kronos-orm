package com.kotoframework.utils

import com.alibaba.fastjson2.into
import com.alibaba.fastjson2.toJSONString
import com.kotoframework.interfaces.KPojo

object Extensions {
    fun KPojo?.toMutableMap(vararg patch: Pair<String, Any?>): MutableMap<String, Any?> {
        return this.toJSONString().into<MutableMap<String, Any?>>().apply {
            patch.forEach { this[it.first] = it.second }
        }
    }

    /* It's an extension function of KPojo. It will return a map of the object. */
    fun KPojo?.toMap(vararg patch: Pair<String, Any?>): Map<String, Any?> {
        return this.toMutableMap(*patch)
    }
}