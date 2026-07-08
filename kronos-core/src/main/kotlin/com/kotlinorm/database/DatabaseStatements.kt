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
    val columns: TableColumnDiff,
    val indexes: TableIndexDiff
)

abstract class DatabaseStatements {
    abstract fun databaseName(wrapper: KronosDataSourceWrapper): String
    abstract fun tableExists(): SqlQuery
    open fun tableComment(): SqlQuery? = null
    abstract fun tableColumns(tableName: String): SqlQuery
    abstract fun tableIndexes(tableName: String): SqlQuery
    abstract fun mapColumns(tableName: String, rows: List<Map<String, Any>>): List<Field>
    abstract fun mapIndexes(tableName: String, rows: List<Map<String, Any>>): List<KTableIndex>
    abstract fun createTable(input: DatabaseCreateTable): List<SqlStatement>
    abstract fun dropTable(tableName: String, ifExists: Boolean): List<SqlStatement>
    abstract fun truncateTable(tableName: String, restartIdentity: Boolean): List<SqlStatement>
    abstract fun syncTable(input: DatabaseSyncTable): List<SqlStatement>
}
