package com.kotlinorm.sql

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableIndex
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.interfaces.KronosDataSourceWrapper

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
}