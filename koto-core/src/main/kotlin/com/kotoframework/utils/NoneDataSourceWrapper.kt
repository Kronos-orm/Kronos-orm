package com.kotoframework.utils

import com.kotoframework.enums.DBType
import com.kotoframework.exceptions.NoDataSourceException
import com.kotoframework.interfaces.KotoDataSourceWrapper
import kotlin.reflect.KClass

class NoneDataSourceWrapper : KotoDataSourceWrapper {
    override val url: String
        get() = throw NoDataSourceException("No DataSource wrapper setting found!")
    override val dbType: DBType
        get() = throw NoDataSourceException("No DataSource wrapper setting found!")

    override fun forList(sql: String, paramMap: Map<String, Any?>): List<Map<String, Any>> {
        throw NoDataSourceException("No DataSource wrapper setting found!")
    }

    override fun forList(sql: String, paramMap: Map<String, Any?>, kClass: KClass<*>): List<Any> {
        throw NoDataSourceException("No DataSource wrapper setting found!")
    }

    override fun forMap(sql: String, paramMap: Map<String, Any?>): Map<String, Any>? {
        throw NoDataSourceException("No DataSource wrapper setting found!")
    }

    override fun forObject(
        sql: String,
        paramMap: Map<String, Any?>,
        javaClass: Class<*>
    ): Any {
        throw NoDataSourceException("No DataSource wrapper setting found!")
    }

    override fun update(sql: String, paramMap: Map<String, Any?>): Int {
        throw NoDataSourceException("No DataSource wrapper setting found!")
    }

    override fun batchUpdate(sql: String, paramMaps: Array<Map<String, Any?>>): IntArray {
        throw NoDataSourceException("No DataSource wrapper setting found!")
    }
}