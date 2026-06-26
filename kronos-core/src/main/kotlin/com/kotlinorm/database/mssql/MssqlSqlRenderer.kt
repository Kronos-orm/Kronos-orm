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
package com.kotlinorm.database.mssql

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
 * MssqlSqlRenderer
 *
 * SQL Server-specific SQL renderer implementation. Handles SQL Server-specific syntax including:
 * - Square brackets for identifiers [identifier]
 * - OFFSET/FETCH syntax (SQL Server 2012+)
 * - String literal escaping
 * - Date/time literal formatting
 * - IF EXISTS ... UPDATE ... ELSE INSERT for conflict resolution
 * - ROWLOCK hint for pessimistic locking
 * - [dbo] schema prefix for tables
 *
 * @author OUSC
 */
class MssqlSqlRenderer : AbstractSqlRenderer() {
    override fun render(statement: Statement, context: RenderContext): RenderedSql {
        // Configure SQL Server-specific quote characters (square brackets) and database type
        val mssqlContext = context.copy(
            quotes = Pair("[", "]"),
            dbType = com.kotlinorm.enums.DBType.Mssql
        )
        return super.render(statement, mssqlContext)
    }

    override fun renderStringLiteral(value: String): String {
        // Escape single quotes by doubling them
        val escaped = value.replace("'", "''")
        return "'$escaped'"
    }

    override fun renderDateLiteral(value: String): String {
        // SQL Server date format: 'YYYY-MM-DD'
        return "'$value'"
    }

    override fun renderTimeLiteral(value: String): String {
        // SQL Server time format: 'HH:MM:SS' or 'HH:MM:SS.ffffff'
        return "'$value'"
    }

    override fun renderTimestampLiteral(value: String): String {
        // SQL Server timestamp format: 'YYYY-MM-DD HH:MM:SS' or 'YYYY-MM-DD HH:MM:SS.ffffff'
        return "'$value'"
    }

    override fun renderLock(lock: PessimisticLock): String {
        return when (lock) {
            PessimisticLock.X -> "WITH (ROWLOCK, UPDLOCK)"
            PessimisticLock.S -> "WITH (ROWLOCK, HOLDLOCK)"
        }
    }

    override fun renderConflictResolver(
            resolver: ConflictResolver,
            context: RenderContext
    ): String {
        val (tableName, onFields, toUpdateFields, toInsertFields) = resolver
        val table = context.quote(tableName)
        val whereClause = onFields.joinToString(" AND ") { field ->
            val column = context.quote(field.columnName)
            "$column = :${field.name}"
        }
        val updateClause = toUpdateFields.joinToString(", ") { field ->
            val column = context.quote(field.columnName)
            "$column = :${field.name}New"
        }
        val insertColumns = toInsertFields.joinToString(", ") { context.quote(it.columnName) }
        val insertValues = toInsertFields.joinToString(", ") { ":${it.name}" }
        return """
            IF EXISTS (SELECT 1 FROM $table WHERE $whereClause)
                BEGIN 
                    UPDATE $table SET $updateClause WHERE $whereClause
                END
            ELSE 
                BEGIN
                    INSERT INTO $table ($insertColumns)
                    VALUES ($insertValues)
                END
        """.trimIndent()
    }

    override fun renderLimitClause(limit: LimitClause, context: RenderContext): String {
        // SQL Server 2012+ uses: OFFSET n ROWS FETCH NEXT m ROWS ONLY
        // Note: OFFSET requires ORDER BY clause
        return if (limit.offset != null && limit.offset!! > 0) {
            " OFFSET ${limit.offset} ROWS FETCH NEXT ${limit.limit} ROWS ONLY"
        } else {
            " FETCH NEXT ${limit.limit} ROWS ONLY"
        }
    }

    override fun renderCreateTable(
            create: DdlStatement.CreateTableStatement,
            context: RenderContext
    ): String {
        val tableName = "[dbo].${context.quote(create.tableName)}"
        val columns = create.columns.joinToString(", ") { column: ColumnDefinition ->
            renderColumnDefinition(column, context)
        }
        val indexes = create.indexes.joinToString(", ") { index: com.kotlinorm.beans.dsl.KTableIndex ->
            renderIndexDefinition(index, create.tableName, context)
        }
        val comment = create.comment?.let { " EXEC sp_addextendedproperty 'MS_Description', '${it.replace("'", "''")}', 'SCHEMA', 'dbo', 'TABLE', '${create.tableName}'" } ?: ""
        val tableDefinition = listOfNotNull(columns, indexes.takeIf { it.isNotEmpty() })
                .joinToString(", ")
        return "IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[${create.tableName}]') AND type in (N'U')) BEGIN CREATE TABLE $tableName ($tableDefinition); END;$comment"
    }

    override fun renderAlterTable(
            alter: DdlStatement.AlterTableStatement,
            context: RenderContext
    ): String {
        val tableName = "[dbo].${context.quote(alter.tableName)}"
        return when (alter) {
            is DdlStatement.AlterTableStatement.AddColumnStatement -> {
                val column = renderColumnDefinition(alter.column, context)
                "ALTER TABLE $tableName ADD $column"
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
        val tableName = "[dbo].${context.quote(drop.tableName)}"
        val ifExists = if (drop.ifExists) {
            "IF EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[${drop.tableName}]') AND type in (N'U')) BEGIN DROP TABLE $tableName END"
        } else {
            "DROP TABLE $tableName"
        }
        return ifExists
    }

    override fun renderCreateIndex(
            create: DdlStatement.CreateIndexStatement,
            context: RenderContext
    ): String {
        val indexName = context.quote(create.indexName)
        val tableName = "[dbo].${context.quote(create.tableName)}"
        val columns = create.columns.joinToString(", ") { context.quote(it) }
        val unique = if (create.unique) "UNIQUE " else ""
        return "CREATE ${unique}INDEX $indexName ON $tableName ($columns)"
    }

    override fun renderDropIndex(
            drop: DdlStatement.DropIndexStatement,
            context: RenderContext
    ): String {
        val indexName = context.quote(drop.indexName)
        val tableName = "[dbo].${context.quote(drop.tableName)}"
        return "DROP INDEX $indexName ON $tableName"
    }

    override fun renderTruncateTable(
            truncate: DdlStatement.TruncateTableStatement,
            context: RenderContext
    ): String {
        val tableName = "[dbo].${context.quote(truncate.tableName)}"
        // SQL Server doesn't support RESTART IDENTITY
        return "TRUNCATE TABLE $tableName"
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
        val identity = if (column.primaryKey == PrimaryKeyType.IDENTITY) {
            " IDENTITY(1,1)"
        } else {
            ""
        }
        val defaultValue = column.defaultValue?.let { " DEFAULT ${renderExpression(it, context)}" } ?: ""
        return "$name $type$identity$nullable$primaryKey$defaultValue"
    }

    private fun renderColumnType(column: ColumnDefinition): String {
        // Simplified version - in practice, use MssqlSupport.getColumnType
        val typeStr = when (column.type) {
            KColumnType.INT -> "INT"
            KColumnType.BIGINT -> "BIGINT"
            KColumnType.VARCHAR -> {
                val length = column.length.takeIf { it > 0 } ?: 255
                if (length > 8000) "VARCHAR(MAX)" else "VARCHAR($length)"
            }
            KColumnType.NVARCHAR -> {
                val length = column.length.takeIf { it > 0 } ?: 255
                if (length > 4000) "NVARCHAR(MAX)" else "NVARCHAR($length)"
            }
            KColumnType.TEXT -> "TEXT"
            KColumnType.DATETIME -> "DATETIME"
            KColumnType.TIMESTAMP -> "TIMESTAMP"
            KColumnType.DATE -> "DATE"
            KColumnType.BIT -> "BIT"
            KColumnType.BLOB -> "VARBINARY(MAX)"
            else -> "NVARCHAR(255)" // Default fallback
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

