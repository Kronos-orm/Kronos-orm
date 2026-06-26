package com.kotlinorm.wrappers

import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.TransactionScope
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.TransactionIsolation
import com.kotlinorm.interfaces.KAtomicActionTask
import com.kotlinorm.interfaces.KAtomicQueryTask
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import kotlin.reflect.KClass

open class SampleMysqlJdbcWrapper : KronosDataSourceWrapper {
    companion object{
        val sampleMysqlJdbcWrapper = SampleMysqlJdbcWrapper()
    }
    override var url: String = "jdbc:mysql://localhost:3306/kronos?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowMultiQueries=true&allowPublicKeyRetrieval=true&rewriteBatchedStatements=true"
    override val userName: String
        get() = "kronos"
    override val dbType: DBType
        get() = DBType.Mysql

    override fun forList(task: KAtomicQueryTask): List<Map<String, Any>> {
        return listOf(
            mapOf(
                "COLUMN_NAME" to "id",
                "DATA_TYPE" to "Int",
                "PRIMARY_KEY" to "YES"
            ),
            mapOf(
                "COLUMN_NAME" to "username",
                "DATA_TYPE" to "Varchar"
            ),
            mapOf(
                "COLUMN_NAME" to "gender",
                "Type" to "Int"
            ),
            mapOf(
                "COLUMN_NAME" to "create_time",
                "DATA_TYPE" to "Datetime"
            ),
            mapOf(
                "COLUMN_NAME" to "update_time",
                "DATA_TYPE" to "Datetime"
            ),
            mapOf(
                "COLUMN_NAME" to "deleted",
                "DATA_TYPE" to "Boolean"
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

    override fun transact(isolation: TransactionIsolation?, timeout: Int?, block: TransactionScope.() -> Any?): Any? {
        return null
    }
}