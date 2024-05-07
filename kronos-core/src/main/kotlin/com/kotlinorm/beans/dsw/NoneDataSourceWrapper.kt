package com.kotlinorm.beans.dsw

import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.KronosAtomicTask
import com.kotlinorm.enums.DBType
import com.kotlinorm.exceptions.NoDataSourceException
import com.kotlinorm.i18n.Noun.noDataSourceMessage
import com.kotlinorm.interfaces.KronosDataSourceWrapper
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