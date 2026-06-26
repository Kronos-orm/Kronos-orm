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
package com.kotlinorm.database.mysql

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
 * MysqlSqlRenderer
 *
 * MySQL-specific SQL renderer implementation. Handles MySQL-specific syntax including:
 * - Backtick quotes for identifiers
 * - LIMIT syntax (supports both LIMIT offset, count and LIMIT count OFFSET offset)
 * - String literal escaping
 * - Date/time literal formatting
 * - ON DUPLICATE KEY UPDATE for conflict resolution
 *
 * @author OUSC
 */
class MysqlSqlRenderer : AbstractSqlRenderer() {
    override fun render(statement: Statement, context: RenderContext): RenderedSql {
        // Configure MySQL-specific quote characters (backticks) and database type
        val mysqlContext = context.copy(
            quotes = Pair("`", "`"),
            dbType = com.kotlinorm.enums.DBType.Mysql
        )
        return super.render(statement, mysqlContext)
    }

    override fun renderStringLiteral(value: String): String {
        // Escape single quotes by doubling them
        val escaped = value.replace("'", "''")
        return "'$escaped'"
    }

    override fun renderDateLiteral(value: String): String {
        // MySQL date format: 'YYYY-MM-DD'
        return "'$value'"
    }

    override fun renderTimeLiteral(value: String): String {
        // MySQL time format: 'HH:MM:SS'
        return "'$value'"
    }

    override fun renderTimestampLiteral(value: String): String {
        // MySQL timestamp format: 'YYYY-MM-DD HH:MM:SS'
        return "'$value'"
    }

    override fun renderLock(lock: PessimisticLock): String {
        return when (lock) {
            PessimisticLock.X -> "FOR UPDATE"
            PessimisticLock.S -> "LOCK IN SHARE MODE"
        }
    }

    override fun renderConflictResolver(
            resolver: ConflictResolver,
            context: RenderContext
    ): String {
        val (tableName, onFields, _, toInsertFields) = resolver
        val updateClause =
                onFields.joinToString(", ") { field ->
                    val column = context.quote(field.columnName)
                    "$column = VALUES($column)"
                }
        return " ON DUPLICATE KEY UPDATE $updateClause"
    }

    override fun renderLimitClause(limit: LimitClause, context: RenderContext): String {
        // MySQL supports both formats:
        // 1. LIMIT offset, count (MySQL traditional)
        // 2. LIMIT count OFFSET offset (SQL standard, MySQL 8.0+)
        // We'll use the SQL standard format for consistency
        return if (limit.offset != null) {
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
                create.columns.joinToString(", ") { column ->
                    renderColumnDefinition(column, context)
                }
        val indexes =
                create.indexes.joinToString(", ") { index ->
                    renderIndexDefinition(index, create.tableName, context)
                }
        val comment = create.comment?.let { " COMMENT = '${it.replace("'", "''")}'" } ?: ""
        val tableDefinition =
                listOfNotNull(columns, indexes.takeIf { it.isNotEmpty() }).joinToString(", ")
        return "CREATE TABLE IF NOT EXISTS $tableName ($tableDefinition)$comment"
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
                val columnName = context.quote(alter.columnName)
                "ALTER TABLE $tableName DROP COLUMN $columnName"
            }
            is DdlStatement.AlterTableStatement.ModifyColumnStatement -> {
                val column = renderColumnDefinition(alter.column, context)
                "ALTER TABLE $tableName MODIFY COLUMN $column"
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
        return "CREATE ${unique}INDEX $indexName ON $tableName ($columns)"
    }

    override fun renderDropIndex(
            drop: DdlStatement.DropIndexStatement,
            context: RenderContext
    ): String {
        val indexName = context.quote(drop.indexName)
        val tableName = context.quote(drop.tableName)
        return "DROP INDEX $indexName ON $tableName"
    }

    override fun renderTruncateTable(
            truncate: DdlStatement.TruncateTableStatement,
            context: RenderContext
    ): String {
        val tableName = context.quote(truncate.tableName)
        // MySQL doesn't support RESTART IDENTITY, but we include it for completeness
        return "TRUNCATE TABLE $tableName"
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
                    " AUTO_INCREMENT"
                } else {
                    ""
                }
        val defaultValue =
                column.defaultValue?.let { " DEFAULT ${renderExpression(it, context)}" } ?: ""
        return "$name $type$nullable$primaryKey$autoIncrement$defaultValue"
    }

    private fun renderColumnType(column: ColumnDefinition): String {
        // This is a simplified version - in practice, you might want to use
        // MysqlSupport.getColumnType
        val typeStr =
                when (column.type) {
                    KColumnType.INT -> "INT"
                    KColumnType.BIGINT -> "BIGINT"
                    KColumnType.VARCHAR -> "VARCHAR(${column.length.takeIf { it > 0 } ?: 255})"
                    KColumnType.TEXT -> "TEXT"
                    KColumnType.DATETIME -> "DATETIME"
                    KColumnType.TIMESTAMP -> "TIMESTAMP"
                    else -> "VARCHAR(255)" // Default fallback
                }
        return typeStr
    }

    private fun renderIndexDefinition(
            index: com.kotlinorm.beans.dsl.KTableIndex,
            tableName: String,
            context: RenderContext
    ): String {
        val indexName = context.quote(index.name)
        val columns = index.columns.joinToString(", ") { context.quote(it) }
        val type =
                when (index.type) {
                    "UNIQUE" -> "UNIQUE "
                    "FULLTEXT" -> "FULLTEXT "
                    "SPATIAL" -> "SPATIAL "
                    else -> ""
                }
        val method = if (index.method.isNotEmpty()) " USING ${index.method}" else ""
        return "${type}INDEX $indexName ($columns)$method"
    }
}
