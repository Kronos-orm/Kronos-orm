package com.kotlinorm.orm.utils

import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.KronosAtomicTask
import com.kotlinorm.enums.DBType
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import kotlin.reflect.KClass

object TestWrapper : KronosDataSourceWrapper {
    override val url: String
        get() = "test"
    override val dbType: DBType
        get() = DBType.Mysql

    override fun forList(task: KronosAtomicTask): List<Map<String, Any>> {
        return listOf()
    }

    override fun forList(task: KronosAtomicTask, kClass: KClass<*>): List<Any> {
        return listOf()
    }

    override fun forMap(task: KronosAtomicTask): Map<String, Any>? {
        return null
    }

    override fun forObject(task: KronosAtomicTask, kClass: KClass<*>): Any? {
        return null
    }

    override fun update(task: KronosAtomicTask): Int {
        return 1
    }

    override fun batchUpdate(task: KronosAtomicBatchTask): IntArray {
        return intArrayOf(1)
    }
}