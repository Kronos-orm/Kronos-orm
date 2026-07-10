package com.kotlinorm.codegen

import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.TransactionScope
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.TransactionIsolation
import com.kotlinorm.interfaces.KAtomicActionTask
import com.kotlinorm.interfaces.KAtomicQueryTask
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import org.apache.commons.dbcp2.BasicDataSource

open class SampleMysqlJdbcWrapper(val dataSource: BasicDataSource) : KronosDataSourceWrapper {
    override val url: String = dataSource.url
    override val userName: String = dataSource.userName
    override val dbType: DBType
        get() = DBType.Mysql

    override fun toList(task: KAtomicQueryTask): List<Any?> {
        if (task.sql.startsWith("SELECT DISTINCT INDEX_NAME")) {
            return [
                mapOf(
                    "tableName" to "tb_user",
                    "indexName" to "PRIMARY",
                    "columnName" to "id",
                    "nonUnique" to 0,
                    "indexType" to "BTREE"
                )
            ]
        }
        return [
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
        ]
    }

    override fun first(task: KAtomicQueryTask): Any? {
        val normalizedSql = task.sql.replace("`", "").uppercase()
        if (normalizedSql.startsWith("SELECT TABLE_COMMENT") && normalizedSql.contains("INFORMATION_SCHEMA.TABLES")) {
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

    override fun transact(isolation: TransactionIsolation?, timeout: Int?, block: TransactionScope.() -> Any?): Any? {
        return null
    }
}
