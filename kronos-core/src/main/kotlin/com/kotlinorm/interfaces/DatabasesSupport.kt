package com.kotlinorm.interfaces

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableIndex
import com.kotlinorm.database.ConflictResolver
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.orm.database.TableColumnDiff
import com.kotlinorm.orm.database.TableIndexDiff
import com.kotlinorm.orm.join.JoinClauseInfo
import com.kotlinorm.orm.select.SelectClauseInfo

interface DatabasesSupport {
    var quotes: Pair<String, String>

    fun getDBNameFromUrl(wrapper: KronosDataSourceWrapper): String

    fun String?.orEmpty(): String = this ?: ""

    fun quote(str: String): String = "${quotes.first}$str${quotes.second}"

    fun quote(field: Field, showTable: Boolean = false): String =
        "${if (showTable) quote(field.tableName) + "." else ""}${quote(field.columnName)}"

    fun equation(field: Field, showTable: Boolean = false): String = "${quote(field, showTable)} = :${field.name}"

    fun getColumnType(type: KColumnType, length: Int): String

    fun getKColumnType(type: String, length: Int = 0): KColumnType = KColumnType.fromString(type)

    fun getColumnCreateSql(dbType: DBType, column: Field): String

    fun getIndexCreateSql(dbType: DBType, tableName: String, index: KTableIndex): String

    fun getTableCreateSqlList(
        dbType: DBType,
        tableName: String,
        columns: List<Field>,
        indexes: List<KTableIndex>
    ): List<String>

    fun getTableExistenceSql(
        dbType: DBType
    ): String

    fun getTableTruncateSql(dbType: DBType, tableName: String, restartIdentity: Boolean): String
    fun getTableDropSql(
        dbType: DBType,
        tableName: String
    ): String

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
        whereClauseSql: String?,
        plusAssigns: MutableList<Pair<Field, String>>,
        minusAssigns: MutableList<Pair<Field, String>>
    ): String

    fun getSelectSql(
        dataSource: KronosDataSourceWrapper,
        selectClause: SelectClauseInfo
    ): String

    fun getJoinSql(
        dataSource: KronosDataSourceWrapper,
        joinClause: JoinClauseInfo
    ): String
}