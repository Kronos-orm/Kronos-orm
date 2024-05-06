package com.kotoframework.beans.dsw

import com.kotoframework.beans.task.KotoAtomicBatchTask
import com.kotoframework.beans.task.KotoAtomicTask
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

    override fun forList(task: KotoAtomicTask): List<Map<String, Any>> {
        throw NoDataSourceException(noDataSourceMessage)
    }

    override fun forList(task: KotoAtomicTask, kClass: KClass<*>): List<Any> {
        throw NoDataSourceException(noDataSourceMessage)
    }

    override fun forMap(task: KotoAtomicTask): Map<String, Any>? {
        throw NoDataSourceException(noDataSourceMessage)
    }

    override fun forObject(task: KotoAtomicTask, kClass: KClass<*>): Any? {
        throw NoDataSourceException(noDataSourceMessage)
    }

    override fun update(task: KotoAtomicTask): Int {
        throw NoDataSourceException(noDataSourceMessage)
    }

    override fun batchUpdate(task: KotoAtomicBatchTask): IntArray {
        throw NoDataSourceException(noDataSourceMessage)
    }
}