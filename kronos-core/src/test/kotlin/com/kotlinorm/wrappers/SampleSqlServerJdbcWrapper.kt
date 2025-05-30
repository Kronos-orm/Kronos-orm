package com.kotlinorm.wrappers

import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.enums.DBType
import com.kotlinorm.interfaces.KAtomicActionTask
import com.kotlinorm.interfaces.KAtomicQueryTask
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import kotlin.reflect.KClass

object SampleSqlServerJdbcWrapper : KronosDataSourceWrapper {
    override val url: String
        get() = "jdbc:sqlserver://localhost:1433;databaseName=test;encrypt=true;trustServerCertificate=true"
    override val userName: String
        get() = "sa"
    override val dbType: DBType
        get() = DBType.Mssql

    override fun forList(task: KAtomicQueryTask): List<Map<String, Any>> {
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

    override fun forList(task: KAtomicQueryTask, kClass: KClass<*>, isKPojo: Boolean, superTypes: List<String>): List<Any> {
        return listOf()
    }

    override fun forMap(task: KAtomicQueryTask): Map<String, Any>? {
        return null
    }

    override fun forObject(task: KAtomicQueryTask, kClass: KClass<*>, isKPojo: Boolean, superTypes: List<String>): Any? {
        return null
    }

    override fun update(task: KAtomicActionTask): Int {
        return 1
    }

    override fun batchUpdate(task: KronosAtomicBatchTask): IntArray {
        return intArrayOf(1)
    }

    override fun transact(block: () -> Any?): Any? {
        return null
    }
}