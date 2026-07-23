/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.database.sqlite

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableIndex
import com.kotlinorm.database.DatabaseCreateTable
import com.kotlinorm.database.DatabaseStatements
import com.kotlinorm.database.DatabaseSyncTable
import com.kotlinorm.database.asInt
import com.kotlinorm.database.cell
import com.kotlinorm.database.sameColumnAttributesAs
import com.kotlinorm.database.toColumnDefinition
import com.kotlinorm.database.toCreateIndexStatement
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.enums.KColumnType.*
import com.kotlinorm.enums.PrimaryKeyType
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.syntax.SqlIdentifier
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlParameter
import com.kotlinorm.syntax.statement.SqlDdlStatement
import com.kotlinorm.syntax.statement.SqlDmlStatement
import com.kotlinorm.syntax.statement.SqlInsertMode
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.syntax.statement.SqlStatement
import com.kotlinorm.syntax.table.SqlTable
import com.kotlinorm.syntax.table.SqlTableAlias
import com.kotlinorm.utils.extractNumberInParentheses

object SqliteStatements : DatabaseStatements() {
    override val defaultIndexType: String = "NORMAL"
    override val defaultIndexMethod: String = ""

    override fun sameColumnDefinition(expected: Field, current: Field): Boolean =
        getColumnType(expected.type, expected.length, expected.scale) ==
            getColumnType(current.type, current.length, current.scale) &&
            expected.sameColumnAttributesAs(current, compareComment = false)

    override fun databaseName(wrapper: KronosDataSourceWrapper): String =
        wrapper.url.substringAfter("//")

    override fun lastInsertIdFallback(insert: SqlDmlStatement.Insert, generatedKey: Field): SqlQuery =
        SqlQuery.Select(
            select = listOf(SqlSelectItem.Expr(SqlExpr.Function(SqlIdentifier.of("last_insert_rowid"))))
        )

    override fun tableExists(): SqlQuery = SqlQuery.Select(
        select = listOf(SqlSelectItem.Expr(SqlExpr.UnsafeRaw("COUNT(*)"))),
        from = listOf(SqlTable.Ident("sqlite_master")),
        where = SqlExpr.Binary(
            SqlExpr.Binary(SqlExpr.UnsafeRaw("type"), SqlBinaryOperator.Equal, SqlExpr.StringLiteral("table")),
            SqlBinaryOperator.And,
            SqlExpr.Binary(SqlExpr.UnsafeRaw("name"), SqlBinaryOperator.Equal, SqlExpr.Parameter(SqlParameter.Named("tableName")))
        )
    )

    override fun tableColumns(tableName: String): SqlQuery = SqlQuery.Select(
        select = listOf(
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("p.name")),
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("p.type")),
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("p.\"notnull\"")),
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("p.dflt_value")),
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("p.pk")),
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("m.sql AS table_sql"))
        ),
        from = listOf(
            SqlTable.Func(name = "pragma_table_info", args = listOf(SqlExpr.Parameter(SqlParameter.Named("tableName"))), alias = SqlTableAlias("p")),
            SqlTable.Ident("sqlite_master", alias = SqlTableAlias("m"))
        ),
        where = SqlExpr.Binary(
            SqlExpr.Binary(SqlExpr.UnsafeRaw("m.type"), SqlBinaryOperator.Equal, SqlExpr.StringLiteral("table")),
            SqlBinaryOperator.And,
            SqlExpr.Binary(SqlExpr.UnsafeRaw("m.tbl_name"), SqlBinaryOperator.Equal, SqlExpr.Parameter(SqlParameter.Named("tableName")))
        )
    )

    override fun tableIndexes(tableName: String): SqlQuery = SqlQuery.Select(
        select = listOf(
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("name")),
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("sql"))
        ),
        from = listOf(SqlTable.Ident("sqlite_master")),
        where = SqlExpr.UnsafeRaw("type = 'index' AND tbl_name = :tableName AND sql IS NOT NULL")
    )

    override fun mapColumns(tableName: String, rows: List<Map<String, Any>>): List<Field> =
        rows.map {
            val (length, scale) = extractNumberInParentheses(it.cell("type").toString())
            val columnName = it.cell("name").toString()
            val identity = it.cell("pk").asInt() == 1 &&
                Regex("""("?\w+"?)\s+INTEGER\s+NOT\s+NULL\s+PRIMARY\s+KEY\s+AUTOINCREMENT""", RegexOption.IGNORE_CASE)
                    .find(it.cell("table_sql")?.toString().orEmpty())
                    ?.groupValues
                    ?.getOrNull(1)
                    ?.trim('"') == columnName
            Field(
                columnName = columnName,
                type = getKColumnType(it.cell("type").toString().substringBefore("("), length, scale),
                length = length,
                scale = scale,
                tableName = tableName,
                nullable = it.cell("notnull").asInt() == 0,
                primaryKey = when {
                    it.cell("pk").asInt() == 0 -> PrimaryKeyType.NOT
                    identity -> PrimaryKeyType.IDENTITY
                    else -> PrimaryKeyType.DEFAULT
                },
                defaultValue = it.cell("dflt_value") as String?
            )
        }

    override fun mapIndexes(tableName: String, rows: List<Map<String, Any>>): List<KTableIndex> =
        rows.map {
            val sql = it.cell("sql")?.toString().orEmpty()
            KTableIndex(
                name = it.cell("name").toString(),
                columns = sqliteIndexColumns(sql),
                type = if (sql.startsWith("CREATE UNIQUE INDEX", ignoreCase = true)) "UNIQUE" else "NORMAL",
                method = ""
            )
        }

    private fun sqliteIndexColumns(sql: String): Array<String> =
        sql.substringAfterLast("(", "")
            .substringBeforeLast(")", "")
            .split(',')
            .map { spec ->
                Regex("\\s+COLLATE\\s+", RegexOption.IGNORE_CASE)
                    .split(spec.trim(), limit = 2)
                    .first()
                    .trim('`', '"', '[', ']', ' ')
            }
            .filter { it.isNotBlank() }
            .toTypedArray()

    override fun createTable(input: DatabaseCreateTable): List<SqlStatement> {
        val table = SqlIdentifier.of(input.tableName)
        return listOf(
            SqlDdlStatement.CreateTable(
                tableName = table,
                columns = input.columns.map { it.toColumnDefinition(::getColumnType) },
                ifNotExists = true
            )
        ) + input.indexes.map { it.toCreateIndexStatement(table, ifNotExists = true) }
    }

    override fun dropTable(tableName: String, ifExists: Boolean): List<SqlStatement> =
        listOf(SqlDdlStatement.DropTable(SqlIdentifier.of(tableName), ifExists))

    override fun truncateTable(tableName: String, restartIdentity: Boolean): List<SqlStatement> =
        listOf(SqlDmlStatement.Delete(SqlTable.Ident(tableName)))

    override fun syncTable(input: DatabaseSyncTable): List<SqlStatement> {
        val table = SqlIdentifier.of(input.tableName)
        val rebuildRequired = input.columns.toModified.isNotEmpty() || input.columns.toDelete.isNotEmpty()
        if (rebuildRequired) {
            return rebuildTable(input)
        }
        return buildList {
            addAll(input.indexes.toDelete.map { SqlDdlStatement.DropIndex(SqlIdentifier.of(it.name), ifExists = true) })
            addAll(input.columns.toAdd.map {
                SqlDdlStatement.AlterTable.AddColumn(table, it.first.toColumnDefinition(::getColumnType))
            })
            addAll(input.indexes.toAdd.map { it.toCreateIndexStatement(table) })
        }
    }

    private fun rebuildTable(input: DatabaseSyncTable): List<SqlStatement> {
        val originalTable = SqlIdentifier.of(input.tableName)
        val temporaryTableName = "${input.tableName}__kronos_tmp"
        val temporaryTable = SqlIdentifier.of(temporaryTableName)
        val currentColumnNames = input.currentColumns.map { it.columnName.lowercase() }.toSet()
        val copiedColumns = input.expectedColumns
            .filter { it.columnName.lowercase() in currentColumnNames }
            .map { SqlIdentifier.of(it.columnName) }

        return buildList {
            addAll(input.currentIndexes.map { SqlDdlStatement.DropIndex(SqlIdentifier.of(it.name), ifExists = true) })
            add(
                SqlDdlStatement.CreateTable(
                    tableName = temporaryTable,
                    columns = input.expectedColumns.map { it.toColumnDefinition(::getColumnType) }
                )
            )
            if (copiedColumns.isNotEmpty()) {
                add(
                    SqlDmlStatement.Insert(
                        table = SqlTable.Ident(temporaryTableName),
                        columns = copiedColumns,
                        mode = SqlInsertMode.Subquery(
                            SqlQuery.Select(
                                select = copiedColumns.map { SqlSelectItem.Expr(SqlExpr.Column(columnName = it.canonical)) },
                                from = listOf(SqlTable.Ident(input.tableName))
                            )
                        )
                    )
                )
            }
            add(SqlDdlStatement.DropTable(originalTable, ifExists = true))
            add(SqlDdlStatement.AlterTable.RenameTable(temporaryTable, originalTable))
            addAll(input.expectedIndexes.map { it.toCreateIndexStatement(originalTable) })
        }
    }

    private fun getColumnType(type: KColumnType, length: Int, scale: Int): String = when (type) {
        BIT, TINYINT, SMALLINT, INT, MEDIUMINT, BIGINT, SERIAL, YEAR, SET -> "INTEGER"
        REAL, FLOAT, DOUBLE -> "REAL"
        DECIMAL, NUMERIC -> "NUMERIC"
        CHAR,
        VARCHAR,
        TEXT,
        MEDIUMTEXT,
        LONGTEXT,
        DATE,
        TIME,
        DATETIME,
        TIMESTAMP,
        CLOB,
        JSON,
        ENUM,
        NVARCHAR,
        NCHAR,
        NCLOB,
        UUID,
        GEOMETRY,
        POINT,
        LINESTRING,
        XML -> "TEXT"
        BINARY, VARBINARY, LONGVARBINARY, BLOB, MEDIUMBLOB, LONGBLOB -> "BLOB"
        else -> "NOT"
    }

    private fun getKColumnType(type: String, length: Int, scale: Int): KColumnType = when (type.uppercase()) {
        "INTEGER" -> INT
        else -> KColumnType.fromString(type)
    }
}
