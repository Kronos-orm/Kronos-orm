package com.kotlinorm.database

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableIndex
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.database.TableColumnDiff
import com.kotlinorm.orm.database.TableIndexDiff

// Used to generate SQL that is independent of database type, including dialect differences.
object SqlManagerCustom {
    private var listOfDBNameFromUrlCustom: MutableList<(dataSource: KronosDataSourceWrapper) -> String?> =
        mutableListOf()
    private var listOfSqlColumnTypeCustom: MutableList<(dbType: DBType, columnType: KColumnType, length: Int) -> String?> =
        mutableListOf()
    private var listOfColumnCreateSqlCustom: MutableList<(dbType: DBType, column: Field) -> String?> = mutableListOf()
    private var listOfIndexCreateSqlCustom: MutableList<(dbType: DBType, tableName: String, index: KTableIndex) -> String?> =
        mutableListOf()
    private var listOfTableCreateSqlListCustom: MutableList<(dbType: DBType, tableName: String, columns: List<Field>, indexes: List<KTableIndex>) -> List<String>?> =
        mutableListOf()
    private var listOfTableDropSqlCustom: MutableList<(dbType: DBType, tableName: String) -> String?> = mutableListOf()
    private var listOfTableExistenceSqlCustom: MutableList<(dbType: DBType) -> String?> = mutableListOf()
    private var listOfTableColumnsCustom: MutableList<(dataSource: KronosDataSourceWrapper, tableName: String) -> List<Field>?> =
        mutableListOf()
    private var listOfTableIndexesCustom: MutableList<(dataSource: KronosDataSourceWrapper, tableName: String) -> List<KTableIndex>?> =
        mutableListOf()
    private var listOfTableSyncSqlCustom: MutableList<(dataSource: KronosDataSourceWrapper, tableName: String, columns: TableColumnDiff, indexes: TableIndexDiff) -> List<String>?> =
        mutableListOf()

    fun addDBNameFromUrlCustom(function: (dataSource: KronosDataSourceWrapper) -> String?) {
        listOfDBNameFromUrlCustom.add(function)
    }

    fun addSqlColumnTypeCustom(function: (dbType: DBType, columnType: KColumnType, length: Int) -> String?) {
        listOfSqlColumnTypeCustom.add(function)
    }

    fun addColumnCreateSqlCustom(function: (dbType: DBType, column: Field) -> String?) {
        listOfColumnCreateSqlCustom.add(function)
    }

    fun addIndexCreateSqlCustom(function: (dbType: DBType, tableName: String, index: KTableIndex) -> String?) {
        listOfIndexCreateSqlCustom.add(function)
    }

    fun addTableCreateSqlListCustom(function: (dbType: DBType, tableName: String, columns: List<Field>, indexes: List<KTableIndex>) -> List<String>) {
        listOfTableCreateSqlListCustom.add(function)
    }

    fun addTableExistenceSqlCustom(function: (dbType: DBType) -> String?) {
        listOfTableExistenceSqlCustom.add(function)
    }

    fun addTableDropSqlCustom(function: (dbType: DBType, tableName: String) -> String?) {
        listOfTableDropSqlCustom.add(function)
    }

    fun addTableColumnsCustom(function: (dataSource: KronosDataSourceWrapper, tableName: String) -> List<Field>?) {
        listOfTableColumnsCustom.add(function)
    }

    fun addTableIndexesCustom(function: (dataSource: KronosDataSourceWrapper, tableName: String) -> List<KTableIndex>?) {
        listOfTableIndexesCustom.add(function)
    }

    fun addTableSyncSqlCustom(function: (dataSource: KronosDataSourceWrapper, tableName: String, columns: TableColumnDiff, indexes: TableIndexDiff) -> List<String>?) {
        listOfTableSyncSqlCustom.add(function)
    }

    fun tryGetDBNameFromUrlCustom(wrapper: KronosDataSourceWrapper): String? {
        for (customFunction in listOfDBNameFromUrlCustom) {
            val result = customFunction(wrapper)
            if (result != null) {
                return result
            }
        }
        return null
    }

    fun tryGetSqlColumnTypeCustom(dbType: DBType, type: KColumnType, length: Int): String? {
        for (customFunction in listOfSqlColumnTypeCustom) {
            val result = customFunction(dbType, type, length)
            if (result != null) {
                return result
            }
        }
        return null
    }

    fun tryGetColumnCreateSqlCustom(dbType: DBType, column: Field): String? {
        for (customFunction in listOfColumnCreateSqlCustom) {
            val result = customFunction(dbType, column)
            if (result != null) {
                return result
            }
        }
        return null
    }

    fun tryGetIndexCreateSqlCustom(dbType: DBType, tableName: String, index: KTableIndex): String? {
        for (customFunction in listOfIndexCreateSqlCustom) {
            val result = customFunction(dbType, tableName, index)
            if (result != null) {
                return result
            }
        }
        return null
    }

    fun tryGetTableCreateSqlListCustom(
        dbType: DBType, tableName: String, columns: List<Field>, indexes: List<KTableIndex>
    ): List<String>? {
        for (customFunction in listOfTableCreateSqlListCustom) {
            val result = customFunction(dbType, tableName, columns, indexes)
            if (!result.isNullOrEmpty()) {
                return result
            }
        }
        return null
    }

    fun tryGetTableExistenceSqlCustom(dbType: DBType): String? {
        for (customFunction in listOfTableExistenceSqlCustom) {
            val result = customFunction(dbType)
            if (result != null) {
                return result
            }
        }
        return null
    }

    fun tryGetTableDropSqlCustom(dbType: DBType, tableName: String): String? {
        for (customFunction in listOfTableDropSqlCustom) {
            val result = customFunction(dbType, tableName)
            if (result != null) {
                return result
            }
        }
        return null
    }

    fun tryGetTableColumnsCustom(dataSource: KronosDataSourceWrapper, tableName: String): List<Field>? {
        for (customFunction in listOfTableColumnsCustom) {
            val result = customFunction(dataSource, tableName)
            if (!result.isNullOrEmpty()) {
                return result
            }
        }
        return null
    }

    fun tryGetTableIndexesCustom(
        dataSource: KronosDataSourceWrapper,
        tableName: String,
    ): List<KTableIndex>? {
        for (customFunction in listOfTableIndexesCustom) {
            val result = customFunction(dataSource, tableName)
            if (!result.isNullOrEmpty()) {
                return result
            }
        }
        return null
    }

    fun tryGetTableSyncSqlListCustom(
        dataSource: KronosDataSourceWrapper,
        tableName: String,
        columns: TableColumnDiff,
        indexes: TableIndexDiff,
    ): List<String>? {
        for (customFunction in listOfTableSyncSqlCustom) {
            val result = customFunction(dataSource, tableName, columns, indexes)
            if (!result.isNullOrEmpty()) {
                return result
            }
        }
        return null
    }
}