package com.kotlinorm.utils

import com.kotlinorm.Kronos
import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.beans.dsl.Field


fun fieldK2db(str: String): String {
    return Kronos.fieldNamingStrategy.k2db(str)
}

fun fieldDb2k(str: String): String {
    return Kronos.fieldNamingStrategy.db2k(str)
}

fun tableK2db(str: String): String {
    return Kronos.fieldNamingStrategy.k2db(str)
}

fun tableDb2k(str: String): String {
    return Kronos.fieldNamingStrategy.db2k(str)
}

fun getCreateTimeStrategy(): KronosCommonStrategy {
    return Kronos.createTimeStrategy
}

fun getUpdateTimeStrategy(): KronosCommonStrategy {
    return Kronos.updateTimeStrategy
}

fun getLogicDeleteStrategy(): KronosCommonStrategy {
    return Kronos.logicDeleteStrategy
}

fun createPair(first: String, second: Any?) = first to second

fun createMutableMap(vararg pairs: Pair<String, Any?>) = mutableMapOf(*pairs)

fun createFieldList(vararg elements: Field) = mutableListOf(*elements)