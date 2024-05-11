package com.kotlinorm.utils

import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.utils.DateTimeUtil.currentDateTime

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

fun <T> Collection<T>.toLinkedSet(): LinkedHashSet<T> = linkedSetOf<T>().apply { this.addAll(this@toLinkedSet) }