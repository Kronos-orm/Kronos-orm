package com.kotlinorm.utils.lruCache

import com.kotlinorm.beans.task.KronosAtomicTask
import com.kotlinorm.enums.DBType
import com.kotlinorm.exceptions.UnsupportedDatabaseTypeException
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper

object TableCache {
    val lruLimit = 20
    internal val lruCache = LRUCacheImplementation(lruLimit)

    private val KronosDataSourceWrapper.dbName
        get() = getDBNameFromUrl(url)

    private fun KronosDataSourceWrapper.getDBNameFromUrl(url: String): String {
        return when (dbType) {
            DBType.Mysql -> url.split("?").first().split("//")[1]
            DBType.SQLite -> url.split("//").last()
            DBType.Oracle -> url.split("@").last()
            DBType.Mssql -> url.split("//").last().split(";").first()
            DBType.Postgres -> url.split("//").last().split("/").first()
            else -> throw UnsupportedDatabaseTypeException()
        }
    }

    private fun tableMetaKey(dataSource: KronosDataSourceWrapper, tableName: String): String {
        return try {
            "${dataSource.dbName}_$tableName"
        } catch (npe: NullPointerException) {
            tableName
        }
    }


    internal fun getTable(wrapper: KronosDataSourceWrapper, tableName: String, kPojo: KPojo): TableObject {
        val key = tableMetaKey(wrapper, tableName)
        if (lruCache[key] == null) {
            try {
                val list =
                    wrapper.forList(
                        KronosAtomicTask(
                            when (wrapper.dbType) {
                                DBType.Mysql -> "show full fields from $tableName"
                                DBType.SQLite -> "PRAGMA table_info($tableName)"
                                DBType.Oracle -> """
                                SELECT 
                                    cols.column_name Field,
                                    cols.data_type Type,
                                    CASE
                                        WHEN cons.constraint_type = 'P' THEN 'YES'
                                        ELSE 'NO'
                                    END AS PrimaryKey
                                FROM all_tab_columns cols
                                LEFT JOIN all_cons_columns cons_cols ON cols.table_name = cons_cols.table_name AND cols.column_name = cons_cols.column_name
                                LEFT JOIN all_constraints cons ON cons_cols.constraint_name = cons.constraint_name AND cons_cols.table_name = cons.table_name
                                WHERE cols.table_name = '${tableName.uppercase()}';
                            """.trimIndent()

                                DBType.Mssql -> """
                                SELECT 
                                    COLUMN_NAME as Field,
                                    DATA_TYPE as Type,
                                    CASE WHEN COLUMN_NAME IN (
                                        SELECT COLUMN_NAME
                                        FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
                                        WHERE OBJECTPROPERTY(OBJECT_ID(CONSTRAINT_SCHEMA + '.' + CONSTRAINT_NAME), 'IsPrimaryKey') = 1
                                        AND TABLE_NAME = '$tableName'}'
                                    ) THEN '1' ELSE '0' END AS PrimaryKey
                                FROM INFORMATION_SCHEMA.COLUMNS
                                WHERE TABLE_NAME = '$tableName';
                            """.trimIndent()

                                DBType.Postgres -> """
                                SELECT 
                                    cols.column_name Field,
                                    cols.data_type Type,
                                    CASE WHEN cons.constraint_type = 'PRIMARY KEY' THEN '1' ELSE '0' END AS PrimaryKey
                                FROM 
                                    information_schema.columns cols
                                LEFT JOIN 
                                    information_schema.key_column_usage kcu 
                                    ON cols.table_name = kcu.table_name AND cols.column_name = kcu.column_name
                                LEFT JOIN 
                                    information_schema.table_constraints cons 
                                    ON kcu.constraint_name = cons.constraint_name
                                WHERE 
                                    cols.table_name = '$tableName';
                            """.trimIndent()

                                else -> throw UnsupportedDatabaseTypeException()
                            }
                        )
                    )
                val columns = list.map {
                    TableColumn(
                        name = (it["Field"] ?: it["name"]).toString(),
                        type = (it["Type"] ?: it["type"]).toString(),
                        primaryKey = when (wrapper.dbType) {
                            DBType.Mysql -> it["Key"]?.toString() == "PRI"
                            DBType.SQLite -> it["pk"]?.toString() == "1"
                            DBType.Oracle -> it["PrimaryKey"]?.toString() == "1"
                            DBType.Mssql -> it["PrimaryKey"]?.toString() == "1"
                            DBType.Postgres -> it["PrimaryKey"]?.toString() == "1"
                            else -> throw UnsupportedDatabaseTypeException()
                        }
                    )
                }
                lruCache[key] = TableObject(
                    columns,
                    tableName
                )
            } catch (e: Exception) {
                throw e
            }
        }
        return lruCache[key]!!
    }
}