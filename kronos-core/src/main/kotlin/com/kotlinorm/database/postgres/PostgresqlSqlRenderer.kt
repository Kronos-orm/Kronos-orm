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
package com.kotlinorm.database.postgres

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
 * PostgresqlSqlRenderer
 *
 * PostgreSQL-specific SQL renderer implementation. Handles PostgreSQL-specific syntax including:
 * - Double quotes for identifiers
 * - Standard LIMIT/OFFSET syntax (LIMIT count OFFSET offset)
 * - String literal escaping
 * - Date/time literal formatting with timezone support
 * - ON CONFLICT ... DO UPDATE SET for conflict resolution
 * - ILIKE for case-insensitive pattern matching
 *
 * @author OUSC
 */
class PostgresqlSqlRenderer : AbstractSqlRenderer() {
    override fun render(statement: Statement, context: RenderContext): RenderedSql {
        // Configure PostgreSQL-specific quote characters (double quotes) and database type
        val postgresContext = context.copy(
            quotes = Pair("\"", "\""),
            dbType = com.kotlinorm.enums.DBType.Postgres
        )
        return super.render(statement, postgresContext)
    }

    override fun renderStringLiteral(value: String): String {
        // Escape single quotes by doubling them
        val escaped = value.replace("'", "''")
        return "'$escaped'"
    }

    override fun renderDateLiteral(value: String): String {
        // PostgreSQL date format: 'YYYY-MM-DD'
        return "'$value'"
    }

    override fun renderTimeLiteral(value: String): String {
        // PostgreSQL time format: 'HH:MM:SS' or 'HH:MM:SS.ffffff'
        return "'$value'"
    }

    override fun renderTimestampLiteral(value: String): String {
        // PostgreSQL timestamp format: 'YYYY-MM-DD HH:MM:SS' or 'YYYY-MM-DD HH:MM:SS.ffffff'
        // Can include timezone: 'YYYY-MM-DD HH:MM:SS+HH:MM'
        return "'$value'"
    }

    override fun renderLock(lock: PessimisticLock): String {
        return when (lock) {
            PessimisticLock.X -> "FOR UPDATE"
            PessimisticLock.S -> "FOR SHARE"
        }
    }

    override fun renderConflictResolver(
            resolver: ConflictResolver,
            context: RenderContext
    ): String {
        val (tableName, onFields, toUpdateFields, _) = resolver
        val conflictTarget = onFields.joinToString(", ") { context.quote(it.columnName) }
        val updateClause = toUpdateFields.joinToString(", ") { field ->
            val column = context.quote(field.columnName)
            "$column = EXCLUDED.$column"
        }
        return " ON CONFLICT ($conflictTarget) DO UPDATE SET $updateClause"
    }

    override fun renderLimitClause(limit: LimitClause, context: RenderContext): String {
        // PostgreSQL uses standard SQL syntax: LIMIT count OFFSET offset
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
        val columns = create.columns.joinToString(", ") { column: ColumnDefinition ->
            renderColumnDefinition(column, context)
        }
        val indexes = create.indexes.joinToString(", ") { index: com.kotlinorm.beans.dsl.KTableIndex ->
            renderIndexDefinition(index, create.tableName, context)
        }
        val comment = create.comment?.let { " COMMENT ON TABLE $tableName IS '${it.replace("'", "''")}'" } ?: ""
        val tableDefinition = listOfNotNull(columns, indexes.takeIf { it.isNotEmpty() })
                .joinToString(", ")
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
                "ALTER TABLE $tableName ALTER COLUMN $column"
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
        val restartIdentity = if (truncate.restartIdentity) " RESTART IDENTITY" else ""
        return "TRUNCATE TABLE $tableName$restartIdentity"
    }

    // Helper methods

    private fun renderColumnDefinition(
            column: ColumnDefinition,
            context: RenderContext
    ): String {
        val name = context.quote(column.name)
        val type = renderColumnType(column)
        val nullable = if (column.nullable) "" else " NOT NULL"
        val primaryKey = if (column.primaryKey != PrimaryKeyType.NOT) {
            " PRIMARY KEY"
        } else {
            ""
        }
        val defaultValue = column.defaultValue?.let { " DEFAULT ${renderExpression(it, context)}" } ?: ""
        // PostgreSQL uses SERIAL/BIGSERIAL for auto-increment, handled in type
        return "$name $type$nullable$primaryKey$defaultValue"
    }

    private fun renderColumnType(column: ColumnDefinition): String {
        // Simplified version - in practice, use PostgresqlSupport.getColumnType
        val typeStr = when (column.type) {
            KColumnType.INT -> if (column.primaryKey == PrimaryKeyType.IDENTITY) "SERIAL" else "INTEGER"
            KColumnType.BIGINT -> if (column.primaryKey == PrimaryKeyType.IDENTITY) "BIGSERIAL" else "BIGINT"
            KColumnType.VARCHAR -> "VARCHAR(${column.length.takeIf { it > 0 } ?: 255})"
            KColumnType.TEXT -> "TEXT"
            KColumnType.DATETIME -> "TIMESTAMP"
            KColumnType.TIMESTAMP -> "TIMESTAMP"
            KColumnType.BIT -> "BOOLEAN"
            KColumnType.UUID -> "UUID"
            KColumnType.JSON -> "JSONB"
            else -> "TEXT" // Default fallback
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
        val type = when (index.type) {
            "UNIQUE" -> "UNIQUE "
            else -> ""
        }
        val method = if (index.method.isNotEmpty()) " USING ${index.method}" else ""
        return "${type}INDEX $indexName ($columns)$method"
    }
}

