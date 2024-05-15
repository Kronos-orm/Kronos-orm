package com.kotlinorm.orm.utils

import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.KronosAtomicTask
import com.kotlinorm.enums.DBType
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import kotlin.reflect.KClass

object TestWrapper : KronosDataSourceWrapper {
    override val url: String
        get() = "jdbc:mysql://localhost:3306/eqm?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowMultiQueries=true&allowPublicKeyRetrieval=true&rewriteBatchedStatements=true"
    override val dbType: DBType
        get() = DBType.Mysql

    override fun forList(task: KronosAtomicTask): List<Map<String, Any>> {
        return listOf(
            mapOf(
                "Field" to "id",
                "Type" to "Int",
                "Key" to "PRI"
            ),
            mapOf(
                "Field" to "username",
                "Type" to "Varchar"
            ),
            mapOf(
                "Field" to "gender",
                "Type" to "Int"
            ),
            mapOf(
                "Field" to "create_time",
                "Type" to "Datetime"
            ),
            mapOf(
                "Field" to "update_time",
                "Type" to "Datetime"
            ),
            mapOf(
                "Field" to "deleted",
                "Type" to "Boolean"
            )
        )
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