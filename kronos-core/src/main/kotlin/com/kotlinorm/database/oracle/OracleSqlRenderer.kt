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
package com.kotlinorm.database.oracle

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
 * OracleSqlRenderer
 *
 * Oracle-specific SQL renderer implementation. Handles Oracle-specific syntax including:
 * - Double quotes for identifiers (typically uppercase)
 * - FETCH FIRST/OFFSET syntax (Oracle 12c+)
 * - String literal escaping
 * - Date/time literal formatting
 * - PL/SQL exception handling for conflict resolution
 * - FOR UPDATE(NOWAIT) for pessimistic locking
 *
 * @author OUSC
 */
class OracleSqlRenderer : AbstractSqlRenderer() {
    override fun render(statement: Statement, context: RenderContext): RenderedSql {
        // Configure Oracle-specific quote characters (double quotes) and database type
        val oracleContext = context.copy(
            quotes = Pair("\"", "\""),
            dbType = com.kotlinorm.enums.DBType.Oracle
        )
        return super.render(statement, oracleContext)
    }

    override fun renderStringLiteral(value: String): String {
        // Escape single quotes by doubling them
        val escaped = value.replace("'", "''")
        return "'$escaped'"
    }

    override fun renderDateLiteral(value: String): String {
        // Oracle date format: DATE 'YYYY-MM-DD' or TO_DATE('YYYY-MM-DD', 'YYYY-MM-DD')
        return "DATE '$value'"
    }

    override fun renderTimeLiteral(value: String): String {
        // Oracle time format: TIMESTAMP 'YYYY-MM-DD HH:MM:SS'
        return "TIMESTAMP '$value'"
    }

    override fun renderTimestampLiteral(value: String): String {
        // Oracle timestamp format: TIMESTAMP 'YYYY-MM-DD HH:MM:SS.ffffff'
        return "TIMESTAMP '$value'"
    }

    override fun renderLock(lock: PessimisticLock): String {
        return when (lock) {
            PessimisticLock.X -> "FOR UPDATE(NOWAIT)"
            PessimisticLock.S -> "LOCK IN SHARE MODE"
        }
    }

    override fun renderConflictResolver(
            resolver: ConflictResolver,
            context: RenderContext
    ): String {
        val (tableName, onFields, toUpdateFields, toInsertFields) = resolver
        val insertColumns =
                toInsertFields.joinToString(", ") { context.quote(it.columnName.uppercase()) }
        val insertValues = toInsertFields.joinToString(", ") { ":${it.name}" }
        val updateClause =
                toUpdateFields.joinToString(", ") { field ->
                    val column = context.quote(field.columnName.uppercase())
                    "$column = :${field.name}New"
                }
        val whereClause =
                onFields.joinToString(" AND ") { field ->
                    val column = context.quote(field.columnName.uppercase())
                    "$column = :${field.name}"
                }
        val table = context.quote(tableName.uppercase())
        return """
            BEGIN
                INSERT INTO $table ($insertColumns)
                VALUES ($insertValues)
            EXCEPTION
                WHEN DUP_VAL_ON_INDEX THEN
                    UPDATE $table
                    SET $updateClause
                    WHERE $whereClause;
            END;
        """.trimIndent()
    }

    override fun renderLimitClause(limit: LimitClause, context: RenderContext): String {
        // Oracle 12c+ uses: FETCH FIRST n ROWS ONLY or OFFSET n ROWS FETCH NEXT m ROWS ONLY
        return if (limit.offset != null && limit.offset!! > 0) {
            " OFFSET ${limit.offset} ROWS FETCH NEXT ${limit.limit} ROWS ONLY"
        } else {
            " FETCH FIRST ${limit.limit} ROWS ONLY"
        }
    }

    override fun renderCreateTable(
            create: DdlStatement.CreateTableStatement,
            context: RenderContext
    ): String {
        val tableName = context.quote(create.tableName.uppercase())
        val columns =
                create.columns.joinToString(", ") { column: ColumnDefinition ->
                    renderColumnDefinition(column, context)
                }
        val indexes =
                create.indexes.joinToString(", ") { index: com.kotlinorm.beans.dsl.KTableIndex ->
                    renderIndexDefinition(index, create.tableName, context)
                }
        val comment =
                create.comment?.let { " COMMENT ON TABLE $tableName IS '${it.replace("'", "''")}'" }
                        ?: ""
        val tableDefinition =
                listOfNotNull(columns, indexes.takeIf { it.isNotEmpty() }).joinToString(", ")
        return "CREATE TABLE $tableName ($tableDefinition)$comment"
    }

    override fun renderAlterTable(
            alter: DdlStatement.AlterTableStatement,
            context: RenderContext
    ): String {
        val tableName = context.quote(alter.tableName.uppercase())
        return when (alter) {
            is DdlStatement.AlterTableStatement.AddColumnStatement -> {
                val column = renderColumnDefinition(alter.column, context)
                "ALTER TABLE $tableName ADD $column"
            }
            is DdlStatement.AlterTableStatement.DropColumnStatement -> {
                val columnName = context.quote(alter.columnName.uppercase())
                "ALTER TABLE $tableName DROP COLUMN $columnName"
            }
            is DdlStatement.AlterTableStatement.ModifyColumnStatement -> {
                val column = renderColumnDefinition(alter.column, context)
                "ALTER TABLE $tableName MODIFY $column"
            }
            else -> throw IllegalArgumentException("Unsupported ALTER TABLE statement type")
        }
    }

    override fun renderDropTable(
            drop: DdlStatement.DropTableStatement,
            context: RenderContext
    ): String {
        val tableName = context.quote(drop.tableName.uppercase())
        val ifExists =
                if (drop.ifExists) "" else "" // Oracle doesn't support IF EXISTS in DROP TABLE
        return "DROP TABLE $tableName"
    }

    override fun renderCreateIndex(
            create: DdlStatement.CreateIndexStatement,
            context: RenderContext
    ): String {
        val indexName = context.quote(create.indexName.uppercase())
        val tableName = context.quote(create.tableName.uppercase())
        val columns = create.columns.joinToString(", ") { context.quote(it.uppercase()) }
        val unique = if (create.unique) "UNIQUE " else ""
        return "CREATE ${unique}INDEX $indexName ON $tableName ($columns)"
    }

    override fun renderDropIndex(
            drop: DdlStatement.DropIndexStatement,
            context: RenderContext
    ): String {
        val indexName = context.quote(drop.indexName.uppercase())
        val tableName = context.quote(drop.tableName.uppercase())
        return "DROP INDEX $indexName"
    }

    override fun renderTruncateTable(
            truncate: DdlStatement.TruncateTableStatement,
            context: RenderContext
    ): String {
        val tableName = context.quote(truncate.tableName.uppercase())
        // Oracle doesn't support RESTART IDENTITY, but supports CASCADE/REUSE STORAGE
        return "TRUNCATE TABLE $tableName"
    }

    // Helper methods

    private fun renderColumnDefinition(column: ColumnDefinition, context: RenderContext): String {
        val name = context.quote(column.name.uppercase())
        val type = renderColumnType(column)
        val nullable = if (column.nullable) "" else " NOT NULL"
        val primaryKey =
                if (column.primaryKey != PrimaryKeyType.NOT) {
                    " PRIMARY KEY"
                } else {
                    ""
                }
        val identity =
                if (column.primaryKey == PrimaryKeyType.IDENTITY) {
                    " GENERATED ALWAYS AS IDENTITY"
                } else {
                    ""
                }
        val defaultValue =
                column.defaultValue?.let { " DEFAULT ${renderExpression(it, context)}" } ?: ""
        return "$name $type$identity$nullable$primaryKey$defaultValue"
    }

    private fun renderColumnType(column: ColumnDefinition): String {
        // Simplified version - in practice, use OracleSupport.getColumnType
        val typeStr =
                when (column.type) {
                    KColumnType.INT -> "NUMBER(${column.length.takeIf { it > 0 } ?: 10})"
                    KColumnType.BIGINT -> "NUMBER(19)"
                    KColumnType.VARCHAR -> "VARCHAR2(${column.length.takeIf { it > 0 } ?: 255})"
                    KColumnType.TEXT -> "CLOB"
                    KColumnType.DATETIME -> "TIMESTAMP(6)"
                    KColumnType.TIMESTAMP -> "TIMESTAMP(${column.scale.coerceIn(0, 9)})"
                    KColumnType.DATE -> "DATE"
                    KColumnType.BLOB -> "BLOB"
                    KColumnType.JSON -> "JSON"
                    else -> "VARCHAR2(255)" // Default fallback
                }
        return typeStr
    }

    private fun renderIndexDefinition(
            index: com.kotlinorm.beans.dsl.KTableIndex,
            tableName: String,
            context: RenderContext
    ): String {
        val indexName = context.quote(index.name.uppercase())
        val columns = index.columns.joinToString(", ") { context.quote(it.uppercase()) }
        val type =
                when (index.type) {
                    "UNIQUE" -> "UNIQUE "
                    else -> ""
                }
        val method = if (index.method.isNotEmpty()) " USING ${index.method}" else ""
        return "${type}INDEX $indexName ($columns)$method"
    }
}
