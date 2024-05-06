package com.kotoframework.beans.dsw

import com.kotoframework.beans.task.KronosAtomicBatchTask
import com.kotoframework.beans.task.KronosAtomicTask
import com.kotoframework.enums.DBType
import com.kotoframework.exceptions.NoDataSourceException
import com.kotoframework.i18n.Noun.noDataSourceMessage
import com.kotoframework.interfaces.KronosDataSourceWrapper
import kotlin.reflect.KClass

class NoneDataSourceWrapper : KronosDataSourceWrapper {
    override val url: String
        get() = throw NoDataSourceException(noDataSourceMessage)
    override val dbType: DBType
        get() = throw NoDataSourceException(noDataSourceMessage)

    override fun forList(task: KronosAtomicTask): List<Map<String, Any>> {
        throw NoDataSourceException(noDataSourceMessage)
    }

    override fun forList(task: KronosAtomicTask, kClass: KClass<*>): List<Any> {
        throw NoDataSourceException(noDataSourceMessage)
    }

    override fun forMap(task: KronosAtomicTask): Map<String, Any>? {
        throw NoDataSourceException(noDataSourceMessage)
    }

    override fun forObject(task: KronosAtomicTask, kClass: KClass<*>): Any? {
        throw NoDataSourceException(noDataSourceMessage)
    }

    override fun update(task: KronosAtomicTask): Int {
        throw NoDataSourceException(noDataSourceMessage)
    }

    override fun batchUpdate(task: KronosAtomicBatchTask): IntArray {
        throw NoDataSourceException(noDataSourceMessage)
    }
}