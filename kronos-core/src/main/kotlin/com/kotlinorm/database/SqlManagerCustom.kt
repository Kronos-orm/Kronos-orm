package com.kotlinorm.database

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableIndex
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.interfaces.DatabasesSupport
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.database.TableColumnDiff
import com.kotlinorm.orm.database.TableIndexDiff

// Used to generate SQL that is independent of database type, including dialect differences.
object SqlManagerCustom {
    private val customDBTypeSupport: MutableMap<DBType, DatabasesSupport> = mutableMapOf()

    @Suppress("UNUSED")
    // Custom functions for generating SQL that is independent of database type, including dialect differences.
    fun registerDBTypeSupport(dbType: DBType, support: DatabasesSupport) {
        customDBTypeSupport[dbType] = support
    }

    fun tryGetDBNameFromUrlCustom(wrapper: KronosDataSourceWrapper): String? {
        return customDBTypeSupport[wrapper.dbType]?.getDBNameFromUrl(wrapper)
    }

    fun tryGetSqlColumnTypeCustom(dbType: DBType, type: KColumnType, length: Int): String? {
        return customDBTypeSupport[dbType]?.getColumnType(type, length)
    }

    fun tryGetColumnCreateSqlCustom(dbType: DBType, column: Field): String? {
        return customDBTypeSupport[dbType]?.getColumnCreateSql(dbType, column)
    }

    fun tryGetIndexCreateSqlCustom(dbType: DBType, tableName: String, index: KTableIndex): String? {
        return customDBTypeSupport[dbType]?.getIndexCreateSql(dbType, tableName, index)
    }

    fun tryGetTableCreateSqlListCustom(
        dbType: DBType, tableName: String, columns: List<Field>, indexes: List<KTableIndex>
    ): List<String>? {
        return customDBTypeSupport[dbType]?.getTableCreateSqlList(dbType, tableName, columns, indexes)
    }

    fun tryGetTableExistenceSqlCustom(dbType: DBType): String? {
        return customDBTypeSupport[dbType]?.getTableExistenceSql(dbType)
    }

    fun tryGetTableTruncateSqlCustom(dbType: DBType, tableName: String, restartIdentity: Boolean): String? {
        return customDBTypeSupport[dbType]?.getTableTruncateSql(dbType, tableName, restartIdentity)
    }

    fun tryGetTableDropSqlCustom(dbType: DBType, tableName: String): String? {
        return customDBTypeSupport[dbType]?.getTableDropSql(dbType, tableName)
    }

    fun tryGetTableColumnsCustom(dataSource: KronosDataSourceWrapper, tableName: String): List<Field>? {
        return customDBTypeSupport[dataSource.dbType]?.getTableColumns(dataSource, tableName)
    }

    fun tryGetTableIndexesCustom(
        dataSource: KronosDataSourceWrapper,
        tableName: String,
    ): List<KTableIndex>? {
        return customDBTypeSupport[dataSource.dbType]?.getTableIndexes(dataSource, tableName)
    }

    fun tryGetTableSyncSqlListCustom(
        dataSource: KronosDataSourceWrapper,
        tableName: String,
        columns: TableColumnDiff,
        indexes: TableIndexDiff,
    ): List<String>? {
        return customDBTypeSupport[dataSource.dbType]?.getTableSyncSqlList(dataSource, tableName, columns, indexes)
    }

    fun tryGetOnConflictSqlCustom(
        dataSource: KronosDataSourceWrapper,
        conflictResolver: ConflictResolver
    ): String? {
        return customDBTypeSupport[dataSource.dbType]?.getOnConflictSql(conflictResolver)
    }
}