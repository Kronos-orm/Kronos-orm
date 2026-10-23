/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.database

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableIndex
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.ddl.TableColumnDiff
import com.kotlinorm.orm.ddl.TableIndexDiff
import com.kotlinorm.syntax.statement.SqlDmlStatement
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlStatement

data class DatabaseCreateTable(
    val tableName: String,
    val tableComment: String?,
    val columns: List<Field>,
    val indexes: List<KTableIndex>
)

data class DatabaseSyncTable(
    val tableName: String,
    val originalTableComment: String?,
    val tableComment: String?,
    val expectedColumns: List<Field>,
    val currentColumns: List<Field>,
    val columns: TableColumnDiff,
    val expectedIndexes: List<KTableIndex>,
    val currentIndexes: List<KTableIndex>,
    val indexes: TableIndexDiff
)

abstract class DatabaseStatements {
    internal open val supportsColumnReordering: Boolean = false
    internal open val defaultIndexType: String = "NORMAL"
    internal open val defaultIndexMethod: String = ""

    internal open fun canonicalColumnName(name: String): String = name

    internal abstract fun sameColumnDefinition(expected: Field, current: Field): Boolean

    internal open fun sameIndexDefinition(expected: KTableIndex, current: KTableIndex): Boolean =
        normalizeIndexDefinition(expected) == normalizeIndexDefinition(current)

    private fun normalizeIndexDefinition(index: KTableIndex): KTableIndex {
        val unique = index.type.equals("UNIQUE", ignoreCase = true) ||
            index.method.equals("UNIQUE", ignoreCase = true)
        val type = if (unique) "UNIQUE" else index.type.ifBlank { defaultIndexType }.uppercase()
        val method = index.method.takeUnless {
            it.isBlank() ||
                it.equals("UNIQUE", ignoreCase = true) ||
                it.equals(index.type, ignoreCase = true)
        }?.uppercase() ?: defaultIndexMethod.uppercase()

        return KTableIndex(
            name = index.name.lowercase(),
            columns = index.columns.map(::canonicalColumnName).toTypedArray(),
            type = type,
            method = method
        )
    }

    abstract fun databaseName(wrapper: KronosDataSourceWrapper): String
    abstract fun tableExists(): SqlQuery
    open fun tableComment(): SqlQuery? = null
    abstract fun tableColumns(tableName: String): SqlQuery
    abstract fun tableIndexes(tableName: String): SqlQuery
    abstract fun mapColumns(tableName: String, rows: List<Map<String, Any>>): List<Field>
    abstract fun mapIndexes(tableName: String, rows: List<Map<String, Any>>): List<KTableIndex>
    open fun lastInsertIdFallback(insert: SqlDmlStatement.Insert, generatedKey: Field): SqlQuery? = null
    abstract fun createTable(input: DatabaseCreateTable): List<SqlStatement>
    abstract fun dropTable(tableName: String, ifExists: Boolean): List<SqlStatement>
    abstract fun truncateTable(tableName: String, restartIdentity: Boolean): List<SqlStatement>
    abstract fun syncTable(input: DatabaseSyncTable): List<SqlStatement>
}
