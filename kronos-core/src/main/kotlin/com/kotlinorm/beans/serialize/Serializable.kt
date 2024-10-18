/**
 * Copyright 2022-2024 kronos-orm
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

package com.kotlinorm.beans.serialize

import com.kotlinorm.Kronos.serializeResolver
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.cascade.get
import com.kotlinorm.orm.cascade.set
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0


inline fun <reified T> serializable(toSerialize: KProperty0<String?>): Serializable<T?> {
    return Serializable(toSerialize, T::class)
}

// 自定义委托类
class Serializable<T>(
    private val toSerialize: KProperty0<String?>,
    private val targetKClass: KClass<*>
) : ReadWriteProperty<Any, T?> {
    @Suppress("UNCHECKED_CAST")
    override fun getValue(thisRef: Any, property: KProperty<*>): T? {
        if ((thisRef as KPojo)[toSerialize] == null) return null
        return serializeResolver.deserialize(thisRef[toSerialize] as String, targetKClass) as T?
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T?) {
        if (value == null) {
            (thisRef as KPojo)[toSerialize] = null
            return
        }
        (thisRef as KPojo)[toSerialize] = serializeResolver.serialize(value)
    }
}