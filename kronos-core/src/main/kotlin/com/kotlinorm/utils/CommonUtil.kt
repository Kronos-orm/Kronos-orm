package com.kotlinorm.utils

import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.enums.KOperationType
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 *@program: kronos-orm
 *@author: Jieyao Lu
 *@description:
 *@create: 2024/5/7 16:01
 **/
object CommonUtil {

    fun setAuditStrategy(
        strategy: KronosCommonStrategy,
        fields: MutableSet<String>,
        paramMap: MutableMap<String, Any?>,
        timeStrategy: Boolean,
        deleted: Boolean = true,
    ) {
        if (strategy.enabled) {

            if (timeStrategy) {
                val format = (strategy.config ?: "yyyy-MM-dd HH:mm:ss").toString()
                fields.add(strategy.field.columnName)
                paramMap[strategy.field.name] = DateTimeFormatter.ofPattern(format).format(LocalDateTime.now())
            } else {
                fields.add(strategy.field.columnName)
                paramMap[strategy.field.name] = 1.takeIf { deleted } ?: 0
            }
        }
    }

}