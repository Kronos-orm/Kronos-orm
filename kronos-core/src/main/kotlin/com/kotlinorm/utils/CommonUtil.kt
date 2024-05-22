package com.kotlinorm.utils

import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.utils.DateTimeUtil.currentDateTime
import com.kotlinorm.utils.KotlinClassMapper.toKClass

/**
 *@program: kronos-orm
 *@author: Jieyao Lu
 *@description:
 *@create: 2024/5/7 16:01
 **/

fun setCommonStrategy(
    strategy: KronosCommonStrategy,
    timeStrategy: Boolean = false,
    deleted: Boolean = false,
    callBack: (field: Field, value: Any?) -> Unit
) {
    if (strategy.enabled) {
        if (timeStrategy) {
            val format = (strategy.config ?: "yyyy-MM-dd HH:mm:ss").toString()
            callBack(strategy.field, currentDateTime(format))
        } else {
            callBack(strategy.field, 1.takeIf { deleted } ?: 0)
        }
    }
}

fun <T> Collection<T>.toLinkedSet(): LinkedHashSet<T> = linkedSetOf<T>().apply { addAll(this@toLinkedSet) }

@Suppress("UNUSED")
fun getSafeValue(
    kotlinType: String,
    superTypes: List<String>,
    map: Map<String, Any?>,
    key: String,
    useSerializeResolver: Boolean
): Any? {
    val kClass = kotlinType.toKClass()
    //TODO:
    // 1.类型转换 Any->String,Long->Int, Short->Int, Int->Short, Int->Boolean...
    // 2.日期转换 String->Date, Long->Date, String-> LocalDateTime, Long->LocalDateTime
    // 3.将String使用serialize resolver转为指定类型
    return map[key]
}