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

package com.kotlinorm.beans.dsl

import com.kotlinorm.enums.CascadeDeleteAction
import com.kotlinorm.enums.CascadeDeleteAction.NO_ACTION
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.utils.createInstance
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0

class KCascade(
    val properties: Array<String> = arrayOf(),
    val targetProperties: Array<String> = arrayOf(),
    val onDelete: CascadeDeleteAction = NO_ACTION,
    val defaultValue: Array<String> = arrayOf(),
    val usage: Array<KOperationType> = arrayOf(
        KOperationType.INSERT,
        KOperationType.UPDATE,
        KOperationType.DELETE,
        KOperationType.SELECT,
        KOperationType.UPSERT
    )
) {
    companion object {
        inline fun <reified T : KPojo, reified R : KPojo> manyToMany(relationOfThis: KProperty0<List<R>?>): ManyToMany<T, R> {
            return ManyToMany(relationOfThis.name, R::class, T::class)
        }

        // 自定义委托类
        class ManyToMany<T, R>(
            private var relationOfThis: String, private val relationClass: KClass<*>, private val targetClass: KClass<*>
        ) : ReadWriteProperty<Any, List<T>> {

            private var targetOfRelationName: String? = null

            @Suppress("UNCHECKED_CAST")
            private fun initKProperty() {
                if (targetOfRelationName == null) {
                    val relation = (relationClass as KClass<KPojo>).createInstance()
                    targetOfRelationName = relation.kronosColumns().find { it.kClass == targetClass }?.name
                }
            }

            @Suppress("UNCHECKED_CAST")
            override fun getValue(thisRef: Any, property: KProperty<*>): List<T> {
                initKProperty()
                return ((thisRef as KPojo)[relationOfThis] as List<R>? ?: emptyList()).map {
                    (it as KPojo)[targetOfRelationName!!] as T
                }
            }

            @Suppress("UNCHECKED_CAST")
            override fun setValue(thisRef: Any, property: KProperty<*>, value: List<T>) {
                initKProperty()
                (thisRef as KPojo)[relationOfThis] = value.map {
                    (relationClass as KClass<KPojo>).createInstance().apply {
                        this@apply[targetOfRelationName!!] = it
                    }
                }
            }
        }
    }
}