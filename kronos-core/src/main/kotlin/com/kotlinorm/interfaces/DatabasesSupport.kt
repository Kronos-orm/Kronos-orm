package com.kotlinorm.interfaces

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableIndex
import com.kotlinorm.database.ConflictResolver
import com.kotlinorm.database.SqlManager.columnCreateDefSql
import com.kotlinorm.database.SqlManager.indexCreateDefSql
import com.kotlinorm.database.SqlManager.sqlColumnType
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.orm.database.TableColumnDiff
import com.kotlinorm.orm.database.TableIndexDiff
import com.kotlinorm.orm.select.SelectClauseInfo

interface DatabasesSupport {
    var quotes: Pair<String, String>

    fun String?.orEmpty(): String = this ?: ""

    fun quote(str: String): String = "${quotes.first}$str${quotes.second}"

    fun quote(field: Field, showTable: Boolean = false): String =
        "${if (showTable) quote(field.tableName) + "." else ""}${quote(field.columnName)}"

    fun equation(field: Field, showTable: Boolean = false): String = "${quote(field, showTable)} = :${field.name}"

    fun getColumnType(type: KColumnType, length: Int): String

    fun getKColumnType(type: String, length: Int = 0): KColumnType = KColumnType.fromString(type)

    fun getColumnCreateSql(dbType: DBType, column: Field): String =
        "${
            column.columnName
        }${
            " ${sqlColumnType(dbType, column.type, column.length)}"
        }${
            if (column.nullable) "" else " NOT NULL"
        }${
            if (column.primaryKey) " PRIMARY KEY" else ""
        }${
            if (column.identity) " AUTO_INCREMENT" else ""
        }${
            if (column.defaultValue != null) " DEFAULT ${column.defaultValue}" else ""
        }"

    fun getIndexCreateSql(dbType: DBType, tableName: String, index: KTableIndex) =
        "CREATE ${index.type} INDEX ${index.name} ON $tableName (${index.columns.joinToString(",")}) USING ${index.method}"

    fun getTableCreateSqlList(
        dbType: DBType,
        tableName: String,
        columns: List<Field>,
        indexes: List<KTableIndex>
    ): List<String> {
        val columnsSql = columns.joinToString(",") { columnCreateDefSql(dbType, it) }
        val indexesSql = indexes.map { indexCreateDefSql(dbType, tableName, it) }
        return listOf(
            "CREATE TABLE IF NOT EXISTS $tableName ($columnsSql)",
            *indexesSql.toTypedArray()
        )
    }

    fun getTableExistenceSql(
        dbType: DBType
    ): String

    fun getTableDropSql(
        dbType: DBType,
        tableName: String
    ) = "DROP TABLE IF EXISTS $tableName"

    fun getTableColumns(
        dataSource: KronosDataSourceWrapper,
        tableName: String,
    ): List<Field>

    fun getTableIndexes(
        dataSource: KronosDataSourceWrapper,
        tableName: String,
    ): List<KTableIndex>

    fun getTableSyncSqlList(
        dataSource: KronosDataSourceWrapper,
        tableName: String,
        columns: TableColumnDiff,
        indexes: TableIndexDiff,
    ): List<String>

    fun getOnConflictSql(conflictResolver: ConflictResolver): String

    fun getInsertSql(
        dataSource: KronosDataSourceWrapper,
        tableName: String,
        columns: List<Field>
    ): String

    fun getDeleteSql(
        dataSource: KronosDataSourceWrapper,
        tableName: String,
        whereClauseSql: String?
    ): String

    fun getUpdateSql(
        dataSource: KronosDataSourceWrapper,
        tableName: String,
        toUpdateFields: List<Field>,
        whereClauseSql: String?
    ): String

    fun getSelectSql(
        dataSource: KronosDataSourceWrapper,
        selectClause: SelectClauseInfo
    ): String
}