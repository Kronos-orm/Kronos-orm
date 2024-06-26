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

package com.kotlinorm.utils

import com.kotlinorm.beans.dsl.Criteria
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.enums.AND
import com.kotlinorm.enums.ConditionType
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

object Extensions {

    fun Map<String, Any?>.safeMapperTo(kClass: KClass<KPojo>): Any {
        return kClass.createInstance().safeFromMapData(this)
    }

    fun Map<String, Any?>.mapperTo(kClass: KClass<KPojo>): Any {
        return kClass.createInstance().fromMapData(this)
    }

    inline fun <reified K : KPojo> Map<String, Any?>.safeMapperTo(): K {
        return K::class.createInstance().safeFromMapData(this)
    }

    inline fun <reified K : KPojo> Map<String, Any?>.mapperTo(): K {
        return K::class.createInstance().fromMapData(this)
    }

    fun KPojo.safeMapperTo(kClass: KClass<KPojo>): Any {
        return kClass.createInstance().fromMapData(toDataMap())
    }

    fun KPojo.mapperTo(kClass: KClass<KPojo>): Any {
        return kClass.createInstance().fromMapData(toDataMap())
    }

    inline fun <reified K : KPojo> KPojo.safeMapperTo(): K {
        return K::class.createInstance().fromMapData(toDataMap())
    }

    inline fun <reified K : KPojo> KPojo.mapperTo(): K {
        return K::class.createInstance().fromMapData(toDataMap())
    }

    internal fun List<Criteria>.toCriteria(): Criteria {
        return Criteria(type = AND, children = this.toMutableList())
    }

    internal infix fun Field.eq(value: Any?): Criteria {
        return Criteria(this, ConditionType.EQUAL, false, value)
    }

    internal fun String.asSql(): Criteria {
        return Criteria(type = ConditionType.SQL, value = this)
    }

    /* It's an extension function of String. It will return a string with redundant spaces removed. */
    internal fun String.rmRedundantBlk(): String {
        return this.replace("\\s+".toRegex(), " ").trim()
    }
}