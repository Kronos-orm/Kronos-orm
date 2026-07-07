/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.kotlinorm.database.mssql

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableIndex
import com.kotlinorm.database.DatabaseCreateTable
import com.kotlinorm.database.DatabaseStatements
import com.kotlinorm.database.DatabaseSyncTable
import com.kotlinorm.database.asInt
import com.kotlinorm.database.cell
import com.kotlinorm.database.toColumnDefinition
import com.kotlinorm.database.toCreateIndexStatement
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.enums.PrimaryKeyType
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.syntax.SqlIdentifier
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.statement.SqlDdlStatement
import com.kotlinorm.syntax.statement.SqlDmlStatement
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.syntax.statement.SqlServerExtendedPropertyOperation
import com.kotlinorm.syntax.statement.SqlStatement
import com.kotlinorm.syntax.table.SqlTable
import com.kotlinorm.syntax.table.SqlTableAlias

object MssqlStatements : DatabaseStatements() {
    override fun databaseName(wrapper: KronosDataSourceWrapper): String =
        wrapper.url.substringAfter("//").substringBefore(";")

    private fun table(tableName: String) = SqlIdentifier.of("dbo", tableName)

    private fun metadataTable(schema: String, name: String, alias: String? = null): SqlTable.Ident =
        SqlTable.Ident(
            name = name,
            alias = alias?.let { SqlTableAlias(it) },
            identifier = SqlIdentifier.of(schema, name)
        )

    override fun tableExists(): SqlQuery = SqlQuery.Select(
        select = listOf(SqlSelectItem.Expr(SqlExpr.UnsafeRaw("COUNT(*)"))),
        from = listOf(
            metadataTable("sys", "tables", "t"),
            metadataTable("sys", "schemas", "s")
        ),
        where = SqlExpr.UnsafeRaw("t.schema_id = s.schema_id AND s.name = 'dbo' AND t.name = :tableName")
    )

    override fun tableComment(): SqlQuery = SqlQuery.Select(
        select = listOf(SqlSelectItem.Expr(SqlExpr.UnsafeRaw("CAST(ep.value AS NVARCHAR(MAX))"))),
        from = listOf(
            metadataTable("sys", "extended_properties", "ep"),
            metadataTable("sys", "tables", "t"),
            metadataTable("sys", "schemas", "s")
        ),
        where = SqlExpr.UnsafeRaw("ep.major_id = t.object_id AND t.schema_id = s.schema_id AND ep.minor_id = 0 AND ep.name = 'MS_Description' AND s.name = 'dbo' AND t.name = :tableName")
    )

    override fun tableColumns(tableName: String): SqlQuery = SqlQuery.Select(
        select = listOf(
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("c.COLUMN_NAME")),
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("c.DATA_TYPE")),
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("CASE WHEN c.DATA_TYPE IN ('char', 'nchar', 'varchar', 'nvarchar') THEN c.CHARACTER_MAXIMUM_LENGTH ELSE NULL END AS CHARACTER_MAXIMUM_LENGTH")),
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("CASE WHEN c.DATA_TYPE IN ('decimal', 'numeric') THEN c.NUMERIC_PRECISION ELSE NULL END AS NUMERIC_PRECISION")),
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("c.IS_NULLABLE")),
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("c.COLUMN_DEFAULT")),
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("CASE WHEN EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.CONSTRAINT_COLUMN_USAGE ccu INNER JOIN INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc ON ccu.Constraint_Name = tc.Constraint_Name AND tc.Constraint_Type = 'PRIMARY KEY' WHERE ccu.COLUMN_NAME = c.COLUMN_NAME AND ccu.TABLE_NAME = c.TABLE_NAME) THEN 'YES' ELSE 'NO' END AS PRIMARY_KEY")),
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("CASE WHEN EXISTS (SELECT 1 FROM sysobjects a INNER JOIN syscolumns b ON a.id = b.id WHERE columnproperty(a.id, b.name, 'isIdentity') = 1 AND objectproperty(a.id, 'isTable') = 1 AND a.name = :tableName AND b.name = c.COLUMN_NAME) THEN 'YES' ELSE 'NO' END AS AUTOINCREAMENT")),
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("CAST((SELECT ep.value FROM sys.extended_properties ep WHERE ep.major_id = OBJECT_ID(:tableName) AND ep.minor_id = c.ORDINAL_POSITION AND ep.name = 'MS_Description') AS NVARCHAR(MAX)) AS COLUMN_COMMENT"))
        ),
        from = listOf(metadataTable("INFORMATION_SCHEMA", "COLUMNS", "c")),
        where = SqlExpr.UnsafeRaw("c.TABLE_CATALOG = DB_NAME() AND c.TABLE_NAME = :tableName")
    )

    override fun tableIndexes(tableName: String): SqlQuery = SqlQuery.Select(
        select = listOf(
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("i.name AS name")),
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("c.name AS columnName")),
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("ic.key_ordinal AS seqInIndex")),
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("i.is_unique AS isUnique")),
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("i.type_desc AS indexType"))
        ),
        from = listOf(
            metadataTable("sys", "indexes", "i"),
            metadataTable("sys", "index_columns", "ic"),
            metadataTable("sys", "columns", "c"),
            metadataTable("sys", "tables", "t"),
            metadataTable("sys", "schemas", "s")
        ),
        where = SqlExpr.UnsafeRaw(
            "i.object_id = ic.object_id AND i.index_id = ic.index_id " +
                "AND c.object_id = ic.object_id AND c.column_id = ic.column_id " +
                "AND t.object_id = i.object_id AND s.schema_id = t.schema_id " +
                "AND s.name = 'dbo' AND t.name = :tableName " +
                "AND i.is_primary_key = 0 AND i.name IS NOT NULL"
        )
    )

    override fun mapColumns(tableName: String, rows: List<Map<String, Any>>): List<Field> =
        rows.map {
            val length = it.cell("CHARACTER_MAXIMUM_LENGTH").asInt()
            val scale = it.cell("NUMERIC_PRECISION").asInt()
            Field(
                columnName = it.cell("COLUMN_NAME").toString(),
                type = getKColumnType(it.cell("DATA_TYPE").toString(), length, scale),
                length = length,
                scale = scale,
                tableName = tableName,
                nullable = it.cell("IS_NULLABLE") == "YES",
                primaryKey = when {
                    it.cell("PRIMARY_KEY") == "NO" -> PrimaryKeyType.NOT
                    it.cell("AUTOINCREAMENT") == "YES" -> PrimaryKeyType.IDENTITY
                    else -> PrimaryKeyType.DEFAULT
                },
                defaultValue = removeOuterParentheses(it.cell("COLUMN_DEFAULT") as String?),
                kDoc = it.cell("COLUMN_COMMENT") as String?
            )
        }

    override fun mapIndexes(tableName: String, rows: List<Map<String, Any>>): List<KTableIndex> =
        rows.groupBy { it.cell("name").toString() }.mapNotNull { (indexName, columns) ->
            val first = columns.firstOrNull() ?: return@mapNotNull null
            KTableIndex(
                name = indexName,
                columns = columns.sortedBy { it.cell("seqInIndex").asInt() }
                    .map { it.cell("columnName").toString() }
                    .toTypedArray(),
                type = first.cell("indexType")?.toString().orEmpty(),
                method = if (first.cell("isUnique").asInt() == 1) "UNIQUE" else ""
            )
        }

    override fun createTable(input: DatabaseCreateTable): List<SqlStatement> {
        val table = table(input.tableName)
        return listOf(
            SqlDdlStatement.CreateTable(
                tableName = table,
                columns = input.columns.map { it.toColumnDefinition(::getColumnType) },
                ifNotExists = true
            )
        ) + input.indexes.map { it.toCreateIndexStatement(table) } +
            input.columns.filter { !it.kDoc.isNullOrEmpty() }.map {
                SqlDdlStatement.SqlServerExtendedPropertyComment(
                    tableName = table,
                    columnName = SqlIdentifier.of(it.columnName),
                    comment = it.kDoc,
                    operation = SqlServerExtendedPropertyOperation.Add
                )
            } +
            listOfNotNull(input.tableComment?.takeIf { it.isNotEmpty() }?.let {
                SqlDdlStatement.SqlServerExtendedPropertyComment(
                    tableName = table,
                    comment = it,
                    operation = SqlServerExtendedPropertyOperation.Add
                )
            })
    }

    override fun dropTable(tableName: String, ifExists: Boolean): List<SqlStatement> =
        listOf(SqlDdlStatement.DropTable(table(tableName), ifExists))

    override fun truncateTable(tableName: String, restartIdentity: Boolean): List<SqlStatement> =
        listOf(SqlDmlStatement.Truncate(SqlTable.Ident(tableName, identifier = table(tableName)), restartIdentity = false))

    override fun syncTable(input: DatabaseSyncTable): List<SqlStatement> {
        val table = table(input.tableName)
        return buildList {
            if (input.originalTableComment.orEmpty() != input.tableComment.orEmpty()) {
                add(
                    SqlDdlStatement.SqlServerExtendedPropertyComment(
                        tableName = table,
                        comment = input.tableComment,
                        operation = commentOperation(input.originalTableComment, input.tableComment)
                    )
                )
            }
            addAll(input.indexes.toDelete.map { SqlDdlStatement.DropIndex(SqlIdentifier.of(it.name), table) })
            input.columns.toDelete.forEach {
                add(SqlDdlStatement.SqlServerDropDefaultConstraint(table, SqlIdentifier.of(it.columnName)))
                add(SqlDdlStatement.AlterTable.DropColumn(table, SqlIdentifier.of(it.columnName)))
            }
            input.columns.toAdd.forEach {
                add(SqlDdlStatement.AlterTable.AddColumn(table, it.first.toColumnDefinition(::getColumnType)))
            }
            input.columns.toModified.forEach { (column, _, current) ->
                add(SqlDdlStatement.SqlServerDropDefaultConstraint(table, SqlIdentifier.of(column.columnName)))
                add(SqlDdlStatement.AlterTable.ModifyColumn(table, column.toColumnDefinition(::getColumnType)))
                if (column.kDoc.orEmpty() != current.kDoc.orEmpty()) {
                    add(
                        SqlDdlStatement.SqlServerExtendedPropertyComment(
                            tableName = table,
                            columnName = SqlIdentifier.of(column.columnName),
                            comment = column.kDoc,
                            operation = commentOperation(current.kDoc, column.kDoc)
                        )
                    )
                }
            }
            addAll(input.indexes.toAdd.map { it.toCreateIndexStatement(table) })
        }
    }

    private fun commentOperation(original: String?, next: String?): SqlServerExtendedPropertyOperation =
        when {
            next == null -> SqlServerExtendedPropertyOperation.Drop
            original == null -> SqlServerExtendedPropertyOperation.Add
            else -> SqlServerExtendedPropertyOperation.Update
        }

    private fun removeOuterParentheses(input: String?): String? {
        var result = input ?: return null
        while (result.length > 1 && result.first() == '(' && result.last() == ')') {
            result = result.substring(1, result.length - 1)
        }
        return result
    }

    private fun getColumnType(type: KColumnType, length: Int, scale: Int): String = when (type) {
        KColumnType.BIT -> "BIT"
        KColumnType.TINYINT -> "TINYINT"
        KColumnType.SMALLINT -> "SMALLINT"
        KColumnType.INT, KColumnType.MEDIUMINT, KColumnType.SERIAL, KColumnType.YEAR -> "INT"
        KColumnType.BIGINT -> "BIGINT"
        KColumnType.REAL -> "REAL"
        KColumnType.FLOAT -> if (length > 0) "FLOAT($length)" else "FLOAT"
        KColumnType.DOUBLE -> "FLOAT(53)"
        KColumnType.DECIMAL -> when {
            length > 0 && scale > 0 -> "DECIMAL($length,$scale)"
            length > 0 -> "DECIMAL($length,0)"
            else -> "DECIMAL(18,0)"
        }
        KColumnType.NUMERIC -> when {
            length > 0 && scale > 0 -> "NUMERIC($length,$scale)"
            length > 0 -> "NUMERIC($length,0)"
            else -> "NUMERIC(18,0)"
        }
        KColumnType.CHAR -> "CHAR(${length.takeIf { it > 0 } ?: 255})"
        KColumnType.VARCHAR -> "VARCHAR(${if (length <= 0) 255 else if (length > 8000) "MAX" else length})"
        KColumnType.NCHAR -> "NVARCHAR(${length.takeIf { it > 0 } ?: 255})"
        KColumnType.NVARCHAR -> "NVARCHAR(${if (length <= 0) 255 else if (length > 4000) "MAX" else length})"
        KColumnType.BINARY -> "BINARY(${length.takeIf { it > 0 } ?: 255})"
        KColumnType.VARBINARY -> "VARBINARY(${length.takeIf { it > 0 } ?: 255})"
        KColumnType.LONGVARBINARY,
        KColumnType.BLOB,
        KColumnType.MEDIUMBLOB,
        KColumnType.LONGBLOB -> "VARBINARY(MAX)"
        KColumnType.TEXT,
        KColumnType.MEDIUMTEXT,
        KColumnType.LONGTEXT,
        KColumnType.CLOB -> "TEXT"
        KColumnType.DATE -> "DATE"
        KColumnType.TIME -> if (scale > 0) "TIME($scale)" else "TIME"
        KColumnType.DATETIME -> if (scale > 0) "DATETIME2($scale)" else "DATETIME"
        KColumnType.TIMESTAMP -> "TIMESTAMP"
        KColumnType.JSON -> "JSON"
        KColumnType.ENUM -> "NVARCHAR(255)"
        KColumnType.NCLOB -> "NTEXT"
        KColumnType.UUID -> "CHAR(36)"
        KColumnType.SET -> "NVARCHAR(255)"
        KColumnType.GEOMETRY,
        KColumnType.POINT,
        KColumnType.LINESTRING -> "GEOMETRY"
        KColumnType.XML -> "XML"
        else -> "NVARCHAR(255)"
    }

    private fun getKColumnType(type: String, length: Int, scale: Int): KColumnType =
        KColumnType.fromString(type)
}
