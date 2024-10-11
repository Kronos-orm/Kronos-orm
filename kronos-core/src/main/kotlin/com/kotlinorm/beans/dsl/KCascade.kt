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

package com.kotlinorm.beans.dsl

import com.kotlinorm.enums.CascadeDeleteAction
import com.kotlinorm.enums.CascadeDeleteAction.NO_ACTION
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.cascade.get
import com.kotlinorm.orm.cascade.set
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.starProjectedType

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
        inline fun <reified T : KPojo, reified R> manyToMany(relationOfThis: KProperty0<List<R>?>): ManyToMany<T, R> {
            return ManyToMany(relationOfThis, R::class, T::class)
        }

        // 自定义委托类
        class ManyToMany<T, R>(
            private var relationOfThis: KProperty0<List<R>?>,
            private val relationClass: KClass<*>,
            private val targetClass: KClass<*>
        ) : ReadWriteProperty<Any, List<T>> {

            private var targetOfRelation: KProperty<*>? = null

            private fun initKProperty() {
                if (targetOfRelation == null) {
                    targetOfRelation =
                        relationClass.memberProperties.find { it.returnType.classifier?.starProjectedType == targetClass.starProjectedType }!!
                }
            }

            @Suppress("UNCHECKED_CAST")
            override fun getValue(thisRef: Any, property: KProperty<*>): List<T> {
                initKProperty()
                return ((thisRef as KPojo)[relationOfThis] as List<R>? ?: emptyList()).map {
                    (it as KPojo)[targetOfRelation!!] as T
                }
            }

            override fun setValue(thisRef: Any, property: KProperty<*>, value: List<T>) {
                initKProperty()
                (thisRef as KPojo)[relationOfThis] = value.map {
                    relationClass.createInstance().apply {
                        (this@apply as KPojo)[targetOfRelation!!] = it
                    }
                }
            }
        }
    }
}