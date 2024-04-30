package com.kotoframework.beans.dsw

import com.kotoframework.enums.DBType
import com.kotoframework.exceptions.NoDataSourceException
import com.kotoframework.i18n.Noun.noDataSourceMessage
import com.kotoframework.interfaces.KotoDataSourceWrapper
import kotlin.reflect.KClass

class NoneDataSourceWrapper : KotoDataSourceWrapper {
    override val url: String
        get() = throw NoDataSourceException(noDataSourceMessage)
    override val dbType: DBType
        get() = throw NoDataSourceException(noDataSourceMessage)

    override fun forList(sql: String, paramMap: Map<String, Any?>): List<Map<String, Any>> {
        throw NoDataSourceException(noDataSourceMessage)
    }

    override fun forList(sql: String, paramMap: Map<String, Any?>, kClass: KClass<*>): List<Any> {
        throw NoDataSourceException(noDataSourceMessage)
    }

    override fun forMap(sql: String, paramMap: Map<String, Any?>): Map<String, Any>? {
        throw NoDataSourceException(noDataSourceMessage)
    }

    override fun forObject(
        sql: String,
        paramMap: Map<String, Any?>,
        kClass: KClass<*>
    ): Any {
        throw NoDataSourceException(noDataSourceMessage)
    }

    override fun update(sql: String, paramMap: Map<String, Any?>): Int {
        throw NoDataSourceException(noDataSourceMessage)
    }

    override fun batchUpdate(sql: String, paramMaps: Array<Map<String, Any?>>): IntArray {
        throw NoDataSourceException(noDataSourceMessage)
    }
}