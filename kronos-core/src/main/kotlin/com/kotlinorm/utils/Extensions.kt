/**
 * Copyright 2022-2025 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kotlinorm.utils

import com.kotlinorm.Kronos
import com.kotlinorm.interfaces.KPojo
import kotlin.reflect.KType
import kotlin.reflect.typeOf

object Extensions {

    fun Map<String, Any?>.safeMapperTo(type: KType): Any {
        return Kronos.createKPojo(type).safeFromMapData(this)
    }

    fun Map<String, Any?>.mapperTo(type: KType): Any {
        return Kronos.createKPojo(type).fromMapData(this)
    }

    inline fun <reified K : KPojo> Map<String, Any?>.safeMapperTo(): K {
        return safeMapperTo(typeOf<K>()) as K
    }

    inline fun <reified K : KPojo> Map<String, Any?>.mapperTo(): K {
        return mapperTo(typeOf<K>()) as K
    }

    fun KPojo.safeMapperTo(type: KType): Any {
        return Kronos.createKPojo(type).safeFromMapData(toDataMap())
    }

    fun KPojo.mapperTo(type: KType): Any {
        return Kronos.createKPojo(type).fromMapData(toDataMap())
    }

    inline fun <reified K : KPojo> KPojo.safeMapperTo(): K {
        return safeMapperTo(typeOf<K>()) as K
    }

    inline fun <reified K : KPojo> KPojo.mapperTo(): K {
        return mapperTo(typeOf<K>()) as K
    }

    fun KPojo.patchTo(type: KType, vararg data: Pair<String, Any?>): KPojo {
        return this.toDataMap().apply {
            data.forEach { (k, v) ->
                try { this[k] = v } catch (_: NoSuchElementException) {}
            }
        }.mapperTo(type) as KPojo
    }

    internal fun Any?.isEmptyArrayOrCollection(): Boolean {
        return when (this) {
            is Iterable<*> -> this.spliterator().exactSizeIfKnown == 0L
            is Array<*> -> this.isEmpty()
            is IntArray -> this.isEmpty()
            is LongArray -> this.isEmpty()
            is ShortArray -> this.isEmpty()
            is FloatArray -> this.isEmpty()
            is DoubleArray -> this.isEmpty()
            is BooleanArray -> this.isEmpty()
            is ByteArray -> this.isEmpty()
            else -> false
        }
    }
}
