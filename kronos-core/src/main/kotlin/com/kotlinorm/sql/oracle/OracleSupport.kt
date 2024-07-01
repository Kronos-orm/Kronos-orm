package com.kotlinorm.sql.oracle

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableIndex
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.interfaces.DatabasesSupport
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.database.TableColumnDiff
import com.kotlinorm.orm.database.TableIndexDiff
import com.kotlinorm.sql.SqlManager.getDBNameFrom

object OracleSupport : DatabasesSupport {
    override fun getColumnType(type: KColumnType, length: Int): String {
        return when (type) {
            KColumnType.BIT -> "NUMBER(1)"
            KColumnType.TINYINT -> "NUMBER"
            KColumnType.SMALLINT -> "NUMBER(5)"
            KColumnType.INT -> "NUMBER"
            KColumnType.MEDIUMINT -> "NUMBER(7)"
            KColumnType.BIGINT -> "NUMBER(19)"
            KColumnType.REAL -> "REAL"
            KColumnType.FLOAT -> "FLOAT"
            KColumnType.DOUBLE -> "DOUBLE"
            KColumnType.DECIMAL -> "DECIMAL"
            KColumnType.NUMERIC -> "NUMERIC"
            KColumnType.CHAR -> "CHAR(${length.takeIf { it > 0 } ?: 255})"
            KColumnType.VARCHAR -> "VARCHAR(${length.takeIf { it > 0 } ?: 255})"
            KColumnType.TEXT -> "CLOB"
            KColumnType.MEDIUMTEXT -> "CLOB"
            KColumnType.LONGTEXT -> "CLOB"
            KColumnType.DATE -> "DATE"
            KColumnType.TIME -> "DATE"
            KColumnType.DATETIME -> "DATE"
            KColumnType.TIMESTAMP -> "TIMESTAMP"
            KColumnType.BINARY -> "BLOB"
            KColumnType.VARBINARY -> "BLOB"
            KColumnType.LONGVARBINARY -> "BLOB"
            KColumnType.BLOB -> "BLOB"
            KColumnType.MEDIUMBLOB -> "BLOB"
            KColumnType.LONGBLOB -> "LONGBLOB"
            KColumnType.CLOB -> "CLOB"
            KColumnType.JSON -> "JSON"
            KColumnType.ENUM -> "ENUM"
            KColumnType.NVARCHAR -> "NVARCHAR(${length.takeIf { it > 0 } ?: 255})"
            KColumnType.NCHAR -> "NCHAR(${length.takeIf { it > 0 } ?: 255})"
            KColumnType.NCLOB -> "NCLOB"
            KColumnType.UUID -> "CHAR(36)"
            KColumnType.SERIAL -> "NUMBER"
            KColumnType.YEAR -> "NUMBER"
            KColumnType.SET -> "SET"
            KColumnType.GEOMETRY -> "GEOMETRY"
            KColumnType.POINT -> "POINT"
            KColumnType.LINESTRING -> "LINESTRING"
            KColumnType.XML -> "XML"
            else -> "VARCHAR(255)"
        }
    }

    override fun getColumnCreateSql(dbType: DBType, column: Field): String {
        return "${
            column.columnName
        }${
            " ${getColumnType(column.type, column.length)}"
        }${
            if (column.nullable) "" else " NOT NULL"
        }${
            if (column.primaryKey) " PRIMARY KEY" else ""
        }${
            if (column.identity) " GENERATED ALWAYS AS IDENTITY" else ""
        }${
            if (column.defaultValue != null) " DEFAULT ${column.defaultValue}" else ""
        }"
    }

    override fun getIndexCreateSql(dbType: DBType, tableName: String, index: KTableIndex) =
        "CREATE ${index.type.uppercase()} INDEX ${index.name} ON $tableName (${
            index.columns.joinToString(
                ", "
            )
        })"

    override fun getTableCreateSqlList(
        dbType: DBType,
        tableName: String,
        columns: List<Field>,
        indexes: List<KTableIndex>
    ): List<String> {
        val columnsSql = columns.joinToString(",") { getColumnCreateSql(dbType, it) }
        val indexesSql = indexes.map { getIndexCreateSql(dbType, tableName, it) }
        return listOf(
            "CREATE TABLE $tableName ($columnsSql)",
            *indexesSql.toTypedArray()
        )
    }

    override fun getTableExistenceSql(dbType: DBType) = "SELECT count(1) FROM user_tables WHERE table_name = :tableName"
    override fun getTableDropSql(dbType: DBType, tableName: String) =
        "DROP TABLE ['$tableName'] STATE CASCADE CONSTRAINTS"

    override fun getTableColumns(dataSource: KronosDataSourceWrapper, tableName: String): List<Field> {
        return dataSource.forList(
            KronosAtomicQueryTask(
                """
                WITH RankedColumns AS (
                    SELECT 
                        cols.column_name AS COLUMN_NAME,
                        cols.data_type AS DATE_TYPE,
                        CAST(cols.data_length AS integer) AS LENGTH,
                        cols.nullable AS IS_NULLABLE,
                        cols.data_default AS COLUMN_DEFAULT,
                        CASE WHEN cons.constraint_type = 'P' THEN '1' ELSE '0' END AS PRIMARY_KEY,
                        ROW_NUMBER() OVER (PARTITION BY cols.column_name ORDER BY CASE WHEN cons.constraint_type = 'P' THEN 0 ELSE 1 END, cons.constraint_type) AS rn
                    FROM 
                        all_tab_columns cols
                    LEFT JOIN 
                        all_cons_columns cons_cols 
                        ON cols.owner = cons_cols.owner AND cols.table_name = cons_cols.table_name AND cols.column_name = cons_cols.column_name
                    LEFT JOIN 
                        all_constraints cons 
                        ON cols.owner = cons.owner AND cons_cols.constraint_name = cons.constraint_name AND cons_cols.table_name = cons.table_name
                    WHERE 
                        cols.table_name = :tableName AND cols.OWNER = :dbName
                )
                SELECT 
                    COLUMN_NAME, DATE_TYPE, LENGTH, IS_NULLABLE, COLUMN_DEFAULT, PRIMARY_KEY
                FROM 
                    RankedColumns
                WHERE 
                    rn = 1
            """.trimIndent(),
                mapOf("tableName" to tableName.uppercase(), "dbName" to getDBNameFrom(dataSource).uppercase())
            )
        ).map {
            Field(
                columnName = it["COLUMN_NAME"].toString(),
                type = KColumnType.fromString(it["DATA_TYPE"].toString()),
                length = it["LENGTH"] as Int? ?: 0,
                tableName = tableName.uppercase(),
                nullable = it["IS_NULLABLE"] == "Y",
                primaryKey = it["PRIMARY_KEY"] == 1,
                defaultValue = it["COLUMN_DEFAULT"] as String?
            )
        }
    }

    override fun getTableIndexes(
        dataSource: KronosDataSourceWrapper,
        tableName: String
    ): List<KTableIndex> {
        return dataSource.forList(
            KronosAtomicQueryTask(
                """
                SELECT DISTINCT i.INDEX_NAME AS NAME
                FROM ALL_INDEXES i
                JOIN ALL_IND_COLUMNS ic ON i.INDEX_NAME = ic.INDEX_NAME
                WHERE i.TABLE_NAME = UPPER(:tableName) 
                AND i.OWNER = :dbName
                AND i.INDEX_NAME NOT LIKE UPPER('SYS_%')
                """,
                mapOf(
                    "tableName" to tableName.uppercase(),
                    "dbName" to getDBNameFrom(dataSource).uppercase()
                )
            )
        ).map {
            KTableIndex(it["NAME"] as String, arrayOf(), "", "")
        }
    }

    override fun getTableSyncSqlList(
        dataSource: KronosDataSourceWrapper,
        tableName: String,
        columns: TableColumnDiff,
        indexes: TableIndexDiff
    ): List<String> {
        val dbType = dataSource.dbType
        val dbName = getDBNameFrom(dataSource)
        return indexes.toDelete.map {
            "DROP INDEX \"$dbName\".\"${it.name}\""
        } + columns.toDelete.map {
            "ALTER TABLE $tableName DROP COLUMN \"${it.columnName}\""
        } + columns.toModified.map {
            "ALTER TABLE tableName modify(${getColumnCreateSql(dbType, it)})}"
        } + columns.toAdd.map {
            "ALTER TABLE $tableName ADD ${getColumnCreateSql(dbType, it)}"
        } + indexes.toAdd.map {
            "CREATE  ${it.type} INDEX ${it.name} ON \"$dbName\".\"$tableName\" (${
                it.columns.joinToString(",") { col ->
                    "\"${col.uppercase()}\""
                }
            })"
        }
    }
}