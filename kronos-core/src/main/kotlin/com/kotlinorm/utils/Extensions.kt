package com.kotlinorm.utils

import com.kotlinorm.beans.dsl.Criteria
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.enums.AND
import com.kotlinorm.enums.ConditionType
import kotlin.reflect.KClass
import kotlin.reflect.javaType

object Extensions {
    /* AN extension function of Map. It will return a KPojo of the map. */
    inline fun <reified K : KPojo> Map<String, Any?>.transformToKPojo(): K {
        return transformToKPojo(K::class) as K
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun Map<String, *>.transformToKPojo(clazz: KClass<*>): Any {
        val constructor = clazz.constructors.first()
        return try {
            val booleanNames =
                constructor.parameters.filter { it.name == "boolean" }.map { it.name }
            constructor.callBy(constructor.parameters.associateWith {
                if (booleanNames.contains(it.name)) {
                    (this[it.name] is Int && this[it.name] == 1) || this[it.name] != null
                } else {
                    this[it.name] ?: this[fieldDb2k(it.name!!)]
                }
            })
        } catch (e: IllegalArgumentException) {
            // compare the argument type of constructor and the given value, print which argument is mismatched
            val mismatchedArgument = constructor.parameters.first {
                if (this[it.name] == null) {
                    !it.isOptional
                } else {
                    it.type.javaType.typeName != this[it.name]!!.javaClass.typeName
                }
            }
            if (this[mismatchedArgument.name] == null) {
                throw IllegalArgumentException("The argument ${clazz.simpleName}.${mismatchedArgument.name} is null, but it's not optional.").apply {
                    addSuppressed(e)
                }
            } else {
                throw IllegalArgumentException("The argument ${clazz.simpleName}.${mismatchedArgument.name} is ${this[mismatchedArgument.name]!!.javaClass.typeName} but expected ${mismatchedArgument.type.javaType.typeName}.").apply {
                    addSuppressed(e)
                }
            }
        }
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