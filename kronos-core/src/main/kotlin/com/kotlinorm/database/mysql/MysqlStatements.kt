/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.database.mysql

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
import com.kotlinorm.syntax.order.SqlOrderingItem
import com.kotlinorm.syntax.statement.SqlColumnPosition
import com.kotlinorm.syntax.statement.SqlDdlStatement
import com.kotlinorm.syntax.statement.SqlDmlStatement
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.syntax.statement.SqlStatement
import com.kotlinorm.syntax.table.SqlTable
import com.kotlinorm.syntax.table.SqlTableAlias
import com.kotlinorm.utils.extractNumberInParentheses

object MysqlStatements : DatabaseStatements() {
    override val supportsColumnReordering: Boolean = true
    override val defaultIndexType: String = "NORMAL"
    override val defaultIndexMethod: String = "BTREE"

    override fun sameColumnDefinition(expected: Field, current: Field): Boolean =
        getColumnType(expected.type, expected.length, expected.scale) ==
            getColumnType(current.type, current.length, current.scale) &&
            expected.sameColumnAttributesAs(current)

    override fun databaseName(wrapper: KronosDataSourceWrapper): String =
        wrapper.url.substringBefore("?").substringAfter("//").substringAfter("/")

    override fun tableExists(): SqlQuery = SqlQuery.Select(
        select = listOf(SqlSelectItem.Expr(SqlExpr.UnsafeRaw("COUNT(*)"))),
        from = listOf(SqlTable.Ident("INFORMATION_SCHEMA.TABLES", identifier = SqlIdentifier.of("INFORMATION_SCHEMA", "TABLES"))),
        where = SqlExpr.Binary(
            SqlExpr.Binary(SqlExpr.UnsafeRaw("TABLE_SCHEMA"), SqlBinaryOperator.Equal, SqlExpr.Function(name = SqlIdentifier.of("DATABASE"))),
            SqlBinaryOperator.And,
            SqlExpr.Binary(SqlExpr.UnsafeRaw("TABLE_NAME"), SqlBinaryOperator.Equal, SqlExpr.Parameter(SqlParameter.Named("tableName")))
        )
    )

    override fun tableComment(): SqlQuery = SqlQuery.Select(
        select = listOf(SqlSelectItem.Expr(SqlExpr.UnsafeRaw("TABLE_COMMENT"))),
        from = listOf(SqlTable.Ident("INFORMATION_SCHEMA.TABLES", identifier = SqlIdentifier.of("INFORMATION_SCHEMA", "TABLES"))),
        where = SqlExpr.Binary(
            SqlExpr.Binary(SqlExpr.UnsafeRaw("TABLE_SCHEMA"), SqlBinaryOperator.Equal, SqlExpr.Function(name = SqlIdentifier.of("DATABASE"))),
            SqlBinaryOperator.And,
            SqlExpr.Binary(SqlExpr.UnsafeRaw("TABLE_NAME"), SqlBinaryOperator.Equal, SqlExpr.Parameter(SqlParameter.Named("tableName")))
        )
    )

    override fun tableColumns(tableName: String): SqlQuery = SqlQuery.Select(
        select = listOf(
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("c.COLUMN_NAME")),
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("c.DATA_TYPE")),
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("c.CHARACTER_MAXIMUM_LENGTH AS LENGTH")),
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("c.NUMERIC_SCALE AS SCALE")),
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("c.COLUMN_TYPE")),
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("c.IS_NULLABLE")),
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("c.COLUMN_DEFAULT")),
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("c.COLUMN_COMMENT")),
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("CASE WHEN c.EXTRA = 'auto_increment' THEN 'YES' ELSE 'NO' END AS IDENTITY")),
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("CASE WHEN c.COLUMN_KEY = 'PRI' THEN 'YES' ELSE 'NO' END AS PRIMARY_KEY"))
        ),
        from = listOf(
            SqlTable.Ident(
                "INFORMATION_SCHEMA.COLUMNS",
                alias = SqlTableAlias("c"),
                identifier = SqlIdentifier.of("INFORMATION_SCHEMA", "COLUMNS")
            )
        ),
        where = SqlExpr.Binary(
            SqlExpr.Binary(SqlExpr.UnsafeRaw("c.TABLE_SCHEMA"), SqlBinaryOperator.Equal, SqlExpr.Function(name = SqlIdentifier.of("DATABASE"))),
            SqlBinaryOperator.And,
            SqlExpr.Binary(SqlExpr.UnsafeRaw("c.TABLE_NAME"), SqlBinaryOperator.Equal, SqlExpr.Parameter(SqlParameter.Named("tableName")))
        ),
        orderBy = listOf(SqlOrderingItem(SqlExpr.UnsafeRaw("ORDINAL_POSITION")))
    )

    override fun tableIndexes(tableName: String): SqlQuery = SqlQuery.Select(
        select = listOf(
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("DISTINCT INDEX_NAME AS indexName")),
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("COLUMN_NAME AS columnName")),
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("SEQ_IN_INDEX AS seqInIndex")),
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("NON_UNIQUE AS nonUnique")),
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("INDEX_TYPE AS indexType"))
        ),
        from = listOf(SqlTable.Ident("INFORMATION_SCHEMA.STATISTICS", identifier = SqlIdentifier.of("INFORMATION_SCHEMA", "STATISTICS"))),
        where = SqlExpr.UnsafeRaw("TABLE_SCHEMA = DATABASE() AND TABLE_NAME = :tableName AND INDEX_NAME != 'PRIMARY'")
    )

    override fun mapColumns(tableName: String, rows: List<Map<String, Any>>): List<Field> =
        rows.map {
            val explicitType = it.cell("COLUMN_TYPE")?.toString()?.let(::extractNumberInParentheses)
            val length = it.cell("LENGTH").asInt().takeIf { len -> len != 0 } ?: explicitType?.first ?: 0
            val scale = it.cell("SCALE").asInt().takeIf { value -> value != 0 } ?: explicitType?.second ?: 0
            Field(
                columnName = it.cell("COLUMN_NAME").toString(),
                type = getKColumnType(it.cell("DATA_TYPE").toString(), length, scale),
                length = length,
                scale = scale,
                tableName = tableName,
                nullable = it.cell("IS_NULLABLE") == "YES",
                primaryKey = when {
                    it.cell("PRIMARY_KEY") == "NO" -> PrimaryKeyType.NOT
                    it.cell("IDENTITY") == "YES" -> PrimaryKeyType.IDENTITY
                    else -> PrimaryKeyType.DEFAULT
                },
                defaultValue = normalizeDefaultValue(it.cell("COLUMN_DEFAULT") as String?, it.cell("DATA_TYPE")?.toString()),
                kDoc = it.cell("COLUMN_COMMENT") as String?
            )
        }

    override fun mapIndexes(tableName: String, rows: List<Map<String, Any>>): List<KTableIndex> =
        rows.groupBy { it.cell("indexName").toString() }.mapNotNull { (indexName, columns) ->
            val first = columns.firstOrNull() ?: return@mapNotNull null
            val method = first.cell("indexType").toString()
            val type = when {
                first.cell("indexType") == "FULLTEXT" -> "FULLTEXT"
                first.cell("indexType") == "SPATIAL" -> "SPATIAL"
                first.cell("nonUnique").asInt() == 0 -> "UNIQUE"
                else -> "NORMAL"
            }
            KTableIndex(
                name = indexName,
                columns = columns.sortedBy { it.cell("seqInIndex").asInt() }
                    .map { it.cell("columnName").toString() }
                    .toTypedArray(),
                type = type,
                method = method
            )
        }

    override fun createTable(input: DatabaseCreateTable): List<SqlStatement> {
        val table = SqlIdentifier.of(input.tableName)
        return listOf(
            SqlDdlStatement.CreateTable(
                tableName = table,
                columns = input.columns.map { it.toColumnDefinition(::getColumnType) },
                comment = input.tableComment,
                ifNotExists = true
            )
        ) + input.indexes.map { it.toCreateIndexStatement(table) }
    }

    override fun dropTable(tableName: String, ifExists: Boolean): List<SqlStatement> =
        listOf(SqlDdlStatement.DropTable(SqlIdentifier.of(tableName), ifExists))

    override fun truncateTable(tableName: String, restartIdentity: Boolean): List<SqlStatement> =
        listOf(SqlDmlStatement.Truncate(SqlTable.Ident(tableName), restartIdentity = false))

    override fun syncTable(input: DatabaseSyncTable): List<SqlStatement> {
        val table = SqlIdentifier.of(input.tableName)
        return buildList {
            if (input.originalTableComment.orEmpty() != input.tableComment.orEmpty()) {
                add(SqlDdlStatement.AlterTable.SetTableComment(table, input.tableComment.orEmpty()))
            }
            addAll(input.indexes.toDelete.map { SqlDdlStatement.DropIndex(SqlIdentifier.of(it.name), table) })
            addAll(input.columns.toAdd.map { (column, previous) ->
                SqlDdlStatement.AlterTable.AddColumn(
                    tableName = table,
                    column = column.toColumnDefinition(::getColumnType),
                    position = previous?.let { SqlColumnPosition.After(SqlIdentifier.of(it.columnName)) } ?: SqlColumnPosition.First
                )
            })
            addAll(input.columns.toModified.map { (column, previous, _) ->
                SqlDdlStatement.AlterTable.ModifyColumn(
                    tableName = table,
                    column = column.toColumnDefinition(::getColumnType),
                    position = previous?.let { SqlColumnPosition.After(SqlIdentifier.of(it.columnName)) } ?: SqlColumnPosition.First
                )
            })
            addAll(input.columns.toDelete.map { SqlDdlStatement.AlterTable.DropColumn(table, SqlIdentifier.of(it.columnName)) })
            addAll(input.indexes.toAdd.map { it.toCreateIndexStatement(table) })
        }
    }

    private fun getColumnType(type: KColumnType, length: Int, scale: Int): String = when (type) {
        BIT -> "TINYINT(1)"
        TINYINT -> if (length > 0) "TINYINT($length)" else "TINYINT(4)"
        SMALLINT -> if (length > 0) "SMALLINT($length)" else "SMALLINT(6)"
        INT, SERIAL -> if (length > 0) "INT($length)" else "INT(11)"
        MEDIUMINT -> if (length > 0) "MEDIUMINT($length)" else "MEDIUMINT(9)"
        BIGINT -> if (length > 0) "BIGINT($length)" else "BIGINT(20)"
        FLOAT -> if (length > 0 && scale > 0) "FLOAT($length,$scale)" else "FLOAT"
        DOUBLE -> if (length > 0 && scale > 0) "DOUBLE($length,$scale)" else "DOUBLE"
        DECIMAL -> when {
            length > 0 && scale > 0 -> "DECIMAL($length,$scale)"
            length > 0 -> "DECIMAL($length,0)"
            else -> "DECIMAL(10,0)"
        }
        NUMERIC -> when {
            length > 0 && scale > 0 -> "NUMERIC($length,$scale)"
            length > 0 -> "NUMERIC($length,0)"
            else -> "NUMERIC(10,0)"
        }
        REAL -> "REAL"
        CHAR, NCHAR -> if (length > 0) "CHAR($length)" else "CHAR(255)"
        VARCHAR, NVARCHAR -> if (length > 0) "VARCHAR($length)" else "VARCHAR(255)"
        TEXT, XML -> "TEXT"
        MEDIUMTEXT -> "MEDIUMTEXT"
        LONGTEXT -> "LONGTEXT"
        DATE -> "DATE"
        TIME -> "TIME"
        DATETIME -> "DATETIME"
        TIMESTAMP -> "TIMESTAMP"
        BINARY -> "BINARY(${length.takeIf { it > 0 } ?: 255})"
        VARBINARY -> "VARBINARY(${length.takeIf { it > 0 } ?: 255})"
        LONGVARBINARY, LONGBLOB -> "LONGBLOB"
        BLOB -> "BLOB"
        MEDIUMBLOB -> "MEDIUMBLOB"
        CLOB -> "CLOB"
        JSON -> "JSON"
        ENUM -> "ENUM"
        NCLOB -> "NCLOB"
        UUID -> "CHAR(36)"
        YEAR -> "YEAR"
        SET -> "SET"
        GEOMETRY -> "GEOMETRY"
        POINT -> "POINT"
        LINESTRING -> "LINESTRING"
        else -> "VARCHAR(255)"
    }

    private fun getKColumnType(type: String, length: Int, scale: Int): KColumnType {
        if (type.lowercase() in listOf("int", "smallint", "tinyint", "bigint") && length == 1) {
            return BIT
        }
        return KColumnType.fromString(type)
    }

    private fun normalizeDefaultValue(defaultValue: String?, dataType: String?): String? {
        if (defaultValue == null) return null
        return if (dataType?.contains("char", ignoreCase = true) == true || dataType.equals("text", ignoreCase = true)) {
            "'${defaultValue.trim('\'')}'"
        } else {
            defaultValue
        }
    }
}
