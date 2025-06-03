package com.kotlinorm.codegen

import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.enums.DBType
import com.kotlinorm.interfaces.KAtomicActionTask
import com.kotlinorm.interfaces.KAtomicQueryTask
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import org.apache.commons.dbcp2.BasicDataSource
import kotlin.reflect.KClass

open class SampleMysqlJdbcWrapper(val dataSource: BasicDataSource) : KronosDataSourceWrapper {
    override val url: String = dataSource.url
    override val userName: String = dataSource.userName
    override val dbType: DBType
        get() = DBType.Mysql

    override fun forList(task: KAtomicQueryTask): List<Map<String, Any>> {
        if (task.sql.startsWith("SELECT DISTINCT INDEX_NAME")) {
            return listOf(
                mapOf(
                    "tableName" to "tb_user",
                    "indexName" to "PRIMARY",
                    "columnName" to "id",
                    "nonUnique" to 0,
                    "indexType" to "BTREE"
                )
            )
        }
        return listOf(
            mapOf(
                "COLUMN_NAME" to "id",
                "DATA_TYPE" to "Int",
                "PRIMARY_KEY" to "PRI"
            ),
            mapOf(
                "COLUMN_NAME" to "username",
                "DATA_TYPE" to "Varchar"
            ),
            mapOf(
                "COLUMN_NAME" to "gender",
                "DATA_TYPE" to "Int"
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

    override fun forList(
        task: KAtomicQueryTask,
        kClass: KClass<*>,
        isKPojo: Boolean,
        superTypes: List<String>
    ): List<Any> {
        return listOf()
    }

    override fun forMap(task: KAtomicQueryTask): Map<String, Any>? {
        return null
    }

    override fun forObject(
        task: KAtomicQueryTask,
        kClass: KClass<*>,
        isKPojo: Boolean,
        superTypes: List<String>
    ): Any? {
        if (task.sql.startsWith("SELECT `TABLE_COMMENT`")) {
            return "Sample Table Comment"
        }
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