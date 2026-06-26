/**
 * Copyright 2022-2025 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * ```
 *     http://www.apache.org/licenses/LICENSE-2.0
 * ```
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.kotlinorm.database.sqlite

import com.kotlinorm.ast.AbstractSqlRenderer
import com.kotlinorm.ast.ColumnDefinition
import com.kotlinorm.ast.DdlStatement
import com.kotlinorm.ast.LimitClause
import com.kotlinorm.ast.RenderContext
import com.kotlinorm.ast.RenderedSql
import com.kotlinorm.ast.Statement
import com.kotlinorm.database.ConflictResolver
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.enums.PessimisticLock
import com.kotlinorm.enums.PrimaryKeyType

/**
 * SqliteSqlRenderer
 *
 * SQLite-specific SQL renderer implementation. Handles SQLite-specific syntax including:
 * - Double quotes for identifiers
 * - Standard LIMIT/OFFSET syntax (LIMIT count OFFSET offset)
 * - String literal escaping
 * - Date/time literal formatting
 * - INSERT OR REPLACE / ON CONFLICT ... DO UPDATE SET for conflict resolution
 * - AUTOINCREMENT for identity columns
 * - Dynamic typing (INTEGER, REAL, TEXT, BLOB, NUMERIC)
 *
 * @author OUSC
 */
class SqliteSqlRenderer : AbstractSqlRenderer() {
    override fun render(statement: Statement, context: RenderContext): RenderedSql {
        // Configure SQLite-specific quote characters (double quotes) and database type
        val sqliteContext = context.copy(
            quotes = Pair("\"", "\""),
            dbType = com.kotlinorm.enums.DBType.SQLite
        )
        return super.render(statement, sqliteContext)
    }

    override fun renderStringLiteral(value: String): String {
        // Escape single quotes by doubling them
        val escaped = value.replace("'", "''")
        return "'$escaped'"
    }

    override fun renderDateLiteral(value: String): String {
        // SQLite date format: 'YYYY-MM-DD'
        return "'$value'"
    }

    override fun renderTimeLiteral(value: String): String {
        // SQLite time format: 'HH:MM:SS'
        return "'$value'"
    }

    override fun renderTimestampLiteral(value: String): String {
        // SQLite timestamp format: 'YYYY-MM-DD HH:MM:SS' or 'YYYY-MM-DD HH:MM:SS.ffffff'
        return "'$value'"
    }

    override fun renderLock(lock: PessimisticLock): String {
        // SQLite doesn't support row-level locking, only database-level locking
        // Return empty string or throw exception
        throw UnsupportedOperationException("SQLite doesn't support row-level pessimistic locking")
    }

    override fun renderConflictResolver(
            resolver: ConflictResolver,
            context: RenderContext
    ): String {
        val (tableName, onFields, toUpdateFields, toInsertFields) = resolver
        val table = context.quote(tableName)
        val conflictTarget = onFields.joinToString(", ") { context.quote(it.columnName) }
        val insertColumns = toInsertFields.joinToString(", ") { context.quote(it.columnName) }
        val insertValues = toInsertFields.joinToString(", ") { ":${it.name}" }
        val updateClause =
                toUpdateFields.joinToString(", ") { field ->
                    val column = context.quote(field.columnName)
                    "$column = :${field.name}New"
                }
        return """
            INSERT OR REPLACE INTO $table ($insertColumns)
            VALUES ($insertValues)
            ON CONFLICT ($conflictTarget) DO UPDATE SET $updateClause
        """.trimIndent()
    }

    override fun renderLimitClause(limit: LimitClause, context: RenderContext): String {
        // SQLite uses standard SQL syntax: LIMIT count OFFSET offset
        return if (limit.offset != null && limit.offset!! > 0) {
            " LIMIT ${limit.limit} OFFSET ${limit.offset}"
        } else {
            " LIMIT ${limit.limit}"
        }
    }

    override fun renderCreateTable(
            create: DdlStatement.CreateTableStatement,
            context: RenderContext
    ): String {
        val tableName = context.quote(create.tableName)
        val columns =
                create.columns.joinToString(", ") { column: ColumnDefinition ->
                    renderColumnDefinition(column, context)
                }
        val indexes =
                create.indexes.joinToString(", ") { index: com.kotlinorm.beans.dsl.KTableIndex ->
                    renderIndexDefinition(index, create.tableName, context)
                }
        // SQLite doesn't support table comments directly
        val tableDefinition =
                listOfNotNull(columns, indexes.takeIf { it.isNotEmpty() }).joinToString(", ")
        return "CREATE TABLE IF NOT EXISTS $tableName ($tableDefinition)"
    }

    override fun renderAlterTable(
            alter: DdlStatement.AlterTableStatement,
            context: RenderContext
    ): String {
        val tableName = context.quote(alter.tableName)
        return when (alter) {
            is DdlStatement.AlterTableStatement.AddColumnStatement -> {
                val column = renderColumnDefinition(alter.column, context)
                "ALTER TABLE $tableName ADD COLUMN $column"
            }
            is DdlStatement.AlterTableStatement.DropColumnStatement -> {
                // SQLite has limited ALTER TABLE support - DROP COLUMN requires table recreation
                // For now, we'll use the standard syntax (SQLite 3.35.0+)
                val columnName = context.quote(alter.columnName)
                "ALTER TABLE $tableName DROP COLUMN $columnName"
            }
            is DdlStatement.AlterTableStatement.ModifyColumnStatement -> {
                // SQLite doesn't support MODIFY COLUMN directly
                // This would require table recreation in practice
                val column = renderColumnDefinition(alter.column, context)
                throw UnsupportedOperationException(
                        "SQLite doesn't support MODIFY COLUMN. Table recreation required."
                )
            }
            else -> throw IllegalArgumentException("Unsupported ALTER TABLE statement type")
        }
    }

    override fun renderDropTable(
            drop: DdlStatement.DropTableStatement,
            context: RenderContext
    ): String {
        val tableName = context.quote(drop.tableName)
        val ifExists = if (drop.ifExists) "IF EXISTS " else ""
        return "DROP TABLE $ifExists$tableName"
    }

    override fun renderCreateIndex(
            create: DdlStatement.CreateIndexStatement,
            context: RenderContext
    ): String {
        val indexName = context.quote(create.indexName)
        val tableName = context.quote(create.tableName)
        val columns = create.columns.joinToString(", ") { context.quote(it) }
        val unique = if (create.unique) "UNIQUE " else ""
        return "CREATE ${unique}INDEX IF NOT EXISTS $indexName ON $tableName ($columns)"
    }

    override fun renderDropIndex(
            drop: DdlStatement.DropIndexStatement,
            context: RenderContext
    ): String {
        val indexName = context.quote(drop.indexName)
        val tableName = context.quote(drop.tableName)
        return "DROP INDEX IF EXISTS $indexName"
    }

    override fun renderTruncateTable(
            truncate: DdlStatement.TruncateTableStatement,
            context: RenderContext
    ): String {
        val tableName = context.quote(truncate.tableName)
        // SQLite doesn't support TRUNCATE, use DELETE FROM instead
        // RESTART IDENTITY is not supported
        return "DELETE FROM $tableName"
    }

    // Helper methods

    private fun renderColumnDefinition(column: ColumnDefinition, context: RenderContext): String {
        val name = context.quote(column.name)
        val type = renderColumnType(column)
        val nullable = if (column.nullable) "" else " NOT NULL"
        val primaryKey =
                if (column.primaryKey != PrimaryKeyType.NOT) {
                    " PRIMARY KEY"
                } else {
                    ""
                }
        val autoIncrement =
                if (column.primaryKey == PrimaryKeyType.IDENTITY) {
                    " AUTOINCREMENT"
                } else {
                    ""
                }
        val defaultValue =
                column.defaultValue?.let { " DEFAULT ${renderExpression(it, context)}" } ?: ""
        return "$name $type$nullable$primaryKey$autoIncrement$defaultValue"
    }

    private fun renderColumnType(column: ColumnDefinition): String {
        // SQLite uses dynamic typing but supports: INTEGER, REAL, TEXT, BLOB, NUMERIC
        val typeStr =
                when (column.type) {
                    KColumnType.INT,
                    KColumnType.BIGINT,
                    KColumnType.SMALLINT,
                    KColumnType.TINYINT,
                    KColumnType.MEDIUMINT,
                    KColumnType.BIT -> "INTEGER"
                    KColumnType.REAL, KColumnType.FLOAT, KColumnType.DOUBLE -> "REAL"
                    KColumnType.DECIMAL, KColumnType.NUMERIC -> "NUMERIC"
                    KColumnType.BLOB,
                    KColumnType.BINARY,
                    KColumnType.VARBINARY,
                    KColumnType.LONGVARBINARY -> "BLOB"
                    else -> "TEXT" // Default to TEXT for strings, dates, etc.
                }
        return typeStr
    }

    private fun renderIndexDefinition(
            index: com.kotlinorm.beans.dsl.KTableIndex,
            tableName: String,
            context: RenderContext
    ): String {
        val indexName = context.quote(index.name)
        val columns =
                index.columns.joinToString(", ") { col ->
                    val quoted = context.quote(col)
                    if (index.method.isNotEmpty()) {
                        "$quoted COLLATE ${index.method}"
                    } else {
                        quoted
                    }
                }
        val type =
                when (index.type) {
                    "UNIQUE" -> "UNIQUE "
                    else -> ""
                }
        return "${type}INDEX $indexName ($columns)"
    }
}
