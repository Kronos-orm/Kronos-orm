package com.kotoframework.interfaces

import com.kotoframework.enums.DBType
import kotlin.reflect.KClass

interface KotoJdbcWrapper {
    val url: String
    val dbType: DBType

    fun forList(sql: String, paramMap: Map<String, Any?> = mapOf()): List<Map<String, Any>>

    fun forList(
        sql: String,
        paramMap: Map<String, Any?>,
        kClass: KClass<*>
    ): List<Any>

    fun forMap(sql: String, paramMap: Map<String, Any?> = mapOf()): Map<String, Any>?

    fun <T> forObject(sql: String, paramMap: Map<String, Any?> = mapOf(), clazz: Class<T>): T?

    fun forObject(
        sql: String,
        paramMap: Map<String, Any?>,
        withoutErrorPrintln: Boolean = false,
        kClass: KClass<*>
    ): Any

    fun forObjectOrNull(
        sql: String,
        paramMap: Map<String, Any?>,
        kClass: KClass<*>
    ): Any?

    fun update(sql: String, paramMap: Map<String, Any?> = mapOf()): Int

    fun batchUpdate(sql: String, paramMaps: Array<Map<String, Any?>> = arrayOf()): IntArray
}