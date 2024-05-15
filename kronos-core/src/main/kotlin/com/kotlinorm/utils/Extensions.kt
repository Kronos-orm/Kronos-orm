package com.kotlinorm.utils

import com.kotlinorm.beans.dsl.Criteria
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.enums.AND
import com.kotlinorm.enums.ConditionType
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

object Extensions {
    /* AN extension function of Map. It will return a KPojo of the map. */
    inline fun <reified K : KPojo> Map<String, Any?>.transformToKPojo(): K {
        return K::class.createInstance().fromMapValue(this)
    }

    /* AN extension function of Map. It will return a KPojo of the map. */
    fun Map<String, Any?>.transformToKPojo(kClass: KClass<*>): Any {
        return (kClass.createInstance() as KPojo).fromMapValue(this)
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