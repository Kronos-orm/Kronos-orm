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
    inline fun <reified K : KPojo> Map<String, Any?>.mapperTo(): K {
        return K::class.createInstance().fromMapData(this)
    }

    fun Map<String, Any?>.mapperTo(kClass: KClass<KPojo>): Any {
        return kClass.createInstance().fromMapData(this)
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