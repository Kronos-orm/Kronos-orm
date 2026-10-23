/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.database.h2

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
import com.kotlinorm.syntax.expr.SqlType
import com.kotlinorm.syntax.order.SqlOrderingItem
import com.kotlinorm.syntax.statement.SqlColumnDefinition
import com.kotlinorm.syntax.statement.SqlDdlStatement
import com.kotlinorm.syntax.statement.SqlDmlStatement
import com.kotlinorm.syntax.statement.SqlPrimaryKeyMode
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.syntax.statement.SqlStatement
import com.kotlinorm.syntax.table.SqlTable
import com.kotlinorm.syntax.table.SqlTableAlias
import com.kotlinorm.syntax.token.SqlUnsafeToken

object H2Statements : DatabaseStatements() {
    override val defaultIndexType: String = "NORMAL"
    override val defaultIndexMethod: String = "BTREE"

    override fun sameColumnDefinition(expected: Field, current: Field): Boolean =
        getColumnType(expected.type, expected.length, expected.scale.normalizedH2Scale(expected.type)) ==
            getColumnType(current.type, current.length, current.scale.normalizedH2Scale(current.type)) &&
            expected.sameColumnAttributesAs(current)

    override fun databaseName(wrapper: KronosDataSourceWrapper): String =
        wrapper.url.removePrefix("jdbc:h2:").substringBefore(';').substringAfterLast('/')

    override fun lastInsertIdFallback(insert: SqlDmlStatement.Insert, generatedKey: Field): SqlQuery? = null

    override fun tableExists(): SqlQuery = SqlQuery.Select(
        select = listOf(SqlSelectItem.Expr(SqlExpr.UnsafeRaw("COUNT(*)"))),
        from = listOf(informationSchemaTable("TABLES")),
        where = schemaAndTableName("TABLE_SCHEMA", "TABLE_NAME")
    )

    override fun tableComment(): SqlQuery = SqlQuery.Select(
        select = listOf(SqlSelectItem.Expr(SqlExpr.UnsafeRaw("REMARKS"))),
        from = listOf(informationSchemaTable("TABLES")),
        where = schemaAndTableName("TABLE_SCHEMA", "TABLE_NAME")
    )

    override fun tableColumns(tableName: String): SqlQuery = SqlQuery.Select(
        select = listOf(
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("C.COLUMN_NAME AS COLUMN_NAME")),
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("C.DATA_TYPE AS DATA_TYPE")),
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("C.CHARACTER_MAXIMUM_LENGTH AS LENGTH")),
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("C.NUMERIC_PRECISION AS PRECISION")),
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("COALESCE(C.NUMERIC_SCALE, C.DATETIME_PRECISION) AS SCALE")),
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("C.IS_NULLABLE = 'YES' AS IS_NULLABLE")),
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("C.COLUMN_DEFAULT AS COLUMN_DEFAULT")),
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("C.IS_IDENTITY = 'YES' AS IDENTITY")),
            SqlSelectItem.Expr(
                SqlExpr.UnsafeRaw(
                    "EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE K " +
                        "JOIN INFORMATION_SCHEMA.TABLE_CONSTRAINTS TC " +
                        "ON TC.CONSTRAINT_CATALOG = K.CONSTRAINT_CATALOG " +
                        "AND TC.CONSTRAINT_SCHEMA = K.CONSTRAINT_SCHEMA " +
                        "AND TC.CONSTRAINT_NAME = K.CONSTRAINT_NAME " +
                        "WHERE TC.TABLE_SCHEMA = C.TABLE_SCHEMA " +
                        "AND TC.TABLE_NAME = C.TABLE_NAME " +
                        "AND TC.CONSTRAINT_TYPE = 'PRIMARY KEY' " +
                        "AND K.COLUMN_NAME = C.COLUMN_NAME) AS PRIMARY_KEY"
                )
            ),
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("C.REMARKS AS COLUMN_COMMENT"))
        ),
        from = listOf(informationSchemaTable("COLUMNS", SqlTableAlias("C"))),
        where = schemaAndTableName("C.TABLE_SCHEMA", "C.TABLE_NAME"),
        orderBy = listOf(SqlOrderingItem(SqlExpr.UnsafeRaw("C.ORDINAL_POSITION")))
    )

    override fun tableIndexes(tableName: String): SqlQuery = SqlQuery.Select(
        select = listOf(
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("I.INDEX_NAME AS NAME")),
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("IC.COLUMN_NAME AS COLUMN_NAME")),
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("IC.ORDINAL_POSITION AS SEQ_IN_INDEX")),
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("IC.IS_UNIQUE AS IS_UNIQUE")),
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("I.INDEX_TYPE_NAME AS INDEX_TYPE"))
        ),
        from = listOf(
            informationSchemaTable("INDEXES", SqlTableAlias("I")),
            informationSchemaTable("INDEX_COLUMNS", SqlTableAlias("IC"))
        ),
        where = SqlExpr.Binary(
            SqlExpr.UnsafeRaw(
                "I.INDEX_CATALOG = IC.INDEX_CATALOG " +
                    "AND I.INDEX_SCHEMA = IC.INDEX_SCHEMA " +
                    "AND I.INDEX_NAME = IC.INDEX_NAME " +
                    "AND I.IS_GENERATED = FALSE " +
                    "AND I.TABLE_SCHEMA = CURRENT_SCHEMA()"
            ),
            SqlBinaryOperator.And,
            SqlExpr.Binary(
                SqlExpr.UnsafeRaw("I.TABLE_NAME"),
                SqlBinaryOperator.Equal,
                SqlExpr.Parameter(SqlParameter.Named("tableName"))
            )
        )
    )

    override fun mapColumns(tableName: String, rows: List<Map<String, Any>>): List<Field> =
        rows.map { row ->
            val dataType = row.cell("DATA_TYPE").toString()
            val length = if (dataType.isNumericH2Type()) row.cell("PRECISION").asInt() else row.cell("LENGTH").asInt()
            val scale = row.cell("SCALE").asInt()
            Field(
                columnName = row.cell("COLUMN_NAME").toString(),
                type = getKColumnType(dataType),
                length = length,
                scale = scale,
                tableName = tableName,
                nullable = row.cell("IS_NULLABLE") == true,
                primaryKey = when {
                    row.cell("IDENTITY") == true -> PrimaryKeyType.IDENTITY
                    row.cell("PRIMARY_KEY") == true -> PrimaryKeyType.DEFAULT
                    else -> PrimaryKeyType.NOT
                },
                defaultValue = row.cell("COLUMN_DEFAULT")?.toString(),
                kDoc = row.cell("COLUMN_COMMENT") as String?
            )
        }

    override fun mapIndexes(tableName: String, rows: List<Map<String, Any>>): List<KTableIndex> =
        rows.groupBy { it.cell("NAME").toString() }.mapNotNull { (indexName, columns) ->
            val first = columns.firstOrNull() ?: return@mapNotNull null
            KTableIndex(
                name = indexName,
                columns = columns.sortedBy { it.cell("SEQ_IN_INDEX").asInt() }
                    .map { it.cell("COLUMN_NAME").toString() }
                    .toTypedArray(),
                type = if (first.cell("IS_UNIQUE") == true) "UNIQUE" else "NORMAL",
                method = defaultIndexMethod
            )
        }

    override fun createTable(input: DatabaseCreateTable): List<SqlStatement> {
        val table = SqlIdentifier.of(input.tableName)
        return listOf(
            SqlDdlStatement.CreateTable(
                tableName = table,
                columns = input.columns.map { column -> column.h2ColumnDefinition() },
                ifNotExists = true
            )
        ) + input.indexes.map { it.toCreateIndexStatement(table) } +
            input.columns.mapNotNull { column ->
                column.kDoc?.takeIf { it.isNotEmpty() }?.let {
                    SqlDdlStatement.CommentOnColumn(table, SqlIdentifier.of(column.columnName), it)
                }
            } +
            listOfNotNull(input.tableComment?.takeIf { it.isNotEmpty() }?.let {
                SqlDdlStatement.CommentOnTable(table, it)
            })
    }

    override fun dropTable(tableName: String, ifExists: Boolean): List<SqlStatement> =
        listOf(SqlDdlStatement.DropTable(SqlIdentifier.of(tableName), ifExists))

    override fun truncateTable(tableName: String, restartIdentity: Boolean): List<SqlStatement> =
        listOf(SqlDmlStatement.Truncate(SqlTable.Ident(tableName), restartIdentity))

    override fun syncTable(input: DatabaseSyncTable): List<SqlStatement> {
        val table = SqlIdentifier.of(input.tableName)
        return buildList {
            if (input.originalTableComment.orEmpty() != input.tableComment.orEmpty()) {
                add(SqlDdlStatement.CommentOnTable(table, input.tableComment))
            }
            addAll(input.indexes.toDelete.map { SqlDdlStatement.DropIndex(SqlIdentifier.of(it.name), ifExists = true) })
            addAll(input.columns.toAdd.map { (column, _) ->
                SqlDdlStatement.AlterTable.AddColumn(table, column.h2ColumnDefinition())
            })
            addAll(input.columns.toModified.map { (column, _, _) ->
                SqlDdlStatement.AlterTable.ModifyColumn(table, column.h2ColumnDefinition())
            })
            addAll(input.columns.toDelete.map { SqlDdlStatement.AlterTable.DropColumn(table, SqlIdentifier.of(it.columnName)) })
            addAll(input.columns.toAdd.mapNotNull { (column, _) ->
                column.kDoc?.takeIf { it.isNotEmpty() }?.let {
                    SqlDdlStatement.CommentOnColumn(table, SqlIdentifier.of(column.columnName), it)
                }
            })
            addAll(input.columns.toModified.mapNotNull { (column, _, current) ->
                column.kDoc?.takeIf { it != current.kDoc }?.let {
                    SqlDdlStatement.CommentOnColumn(table, SqlIdentifier.of(column.columnName), it)
                }
            })
            addAll(input.indexes.toAdd.map { it.toCreateIndexStatement(table) })
        }
    }

    private fun informationSchemaTable(name: String, alias: SqlTableAlias? = null): SqlTable.Ident =
        SqlTable.Ident("INFORMATION_SCHEMA.$name", alias = alias, identifier = SqlIdentifier.of("INFORMATION_SCHEMA", name))

    private fun Field.h2ColumnDefinition(): SqlColumnDefinition =
        toColumnDefinition(::getColumnType).let { definition ->
            if (primaryKey == PrimaryKeyType.IDENTITY) {
                definition.copy(
                    type = SqlType.UnsafeCustom(
                        listOf(SqlUnsafeToken.Text("${getColumnType(type, length, scale)} GENERATED BY DEFAULT AS IDENTITY"))
                    ),
                    primaryKey = SqlPrimaryKeyMode.Primary
                )
            } else {
                definition
            }
        }

    private fun schemaAndTableName(schemaColumn: String, tableColumn: String): SqlExpr =
        SqlExpr.Binary(
            SqlExpr.Binary(
                SqlExpr.UnsafeRaw(schemaColumn),
                SqlBinaryOperator.Equal,
                SqlExpr.Function(name = SqlIdentifier.of("CURRENT_SCHEMA"))
            ),
            SqlBinaryOperator.And,
            SqlExpr.Binary(
                SqlExpr.UnsafeRaw(tableColumn),
                SqlBinaryOperator.Equal,
                SqlExpr.Parameter(SqlParameter.Named("tableName"))
            )
        )

    private fun String.isNumericH2Type(): Boolean = uppercase() in setOf(
        "TINYINT", "SMALLINT", "INTEGER", "BIGINT", "REAL", "DOUBLE PRECISION", "FLOAT", "DECIMAL", "NUMERIC"
    )

    private fun getColumnType(type: KColumnType, length: Int, scale: Int): String = when (type) {
        BIT -> "BOOLEAN"
        TINYINT -> "TINYINT"
        SMALLINT -> "SMALLINT"
        INT, MEDIUMINT, SERIAL, YEAR -> "INTEGER"
        BIGINT -> "BIGINT"
        REAL -> "REAL"
        FLOAT -> "DOUBLE PRECISION"
        DOUBLE -> "DOUBLE PRECISION"
        DECIMAL, NUMERIC -> decimalType("NUMERIC", length, scale)
        CHAR, NCHAR -> "CHAR(${length.takeIf { it > 0 } ?: 255})"
        VARCHAR, NVARCHAR -> "VARCHAR(${length.takeIf { it > 0 } ?: 255})"
        TEXT, MEDIUMTEXT, LONGTEXT, CLOB, NCLOB -> "CLOB"
        BINARY -> "BINARY(${length.takeIf { it > 0 } ?: 255})"
        VARBINARY -> "VARBINARY(${length.takeIf { it > 0 } ?: 255})"
        LONGVARBINARY, BLOB, MEDIUMBLOB, LONGBLOB -> "BLOB"
        DATE -> "DATE"
        TIME -> precisionType("TIME", scale)
        DATETIME, TIMESTAMP -> precisionType("TIMESTAMP", scale)
        JSON -> "JSON"
        ENUM, SET -> "VARCHAR(${length.takeIf { it > 0 } ?: 255})"
        UUID -> "UUID"
        XML -> "XML"
        GEOMETRY -> "GEOMETRY"
        POINT -> "POINT"
        LINESTRING -> "LINESTRING"
        else -> "VARCHAR(255)"
    }

    private fun decimalType(name: String, length: Int, scale: Int): String = when {
        length > 0 && scale > 0 -> "$name($length,$scale)"
        length > 0 -> "$name($length,0)"
        else -> name
    }

    private fun precisionType(name: String, scale: Int): String =
        if (scale > 0) "$name($scale)" else name

    private fun Int.normalizedH2Scale(type: KColumnType): Int =
        if (this == 6 && type in setOf(DATETIME, TIMESTAMP)) 0 else this

    private fun getKColumnType(type: String): KColumnType = when (type.uppercase()) {
        "BOOLEAN" -> BIT
        "CHARACTER" -> CHAR
        "CHARACTER VARYING", "VARCHAR_IGNORECASE" -> VARCHAR
        "INTEGER" -> INT
        "DOUBLE PRECISION" -> DOUBLE
        "BINARY VARYING" -> VARBINARY
        "BINARY LARGE OBJECT" -> BLOB
        "CHARACTER LARGE OBJECT" -> CLOB
        "TIME WITH TIME ZONE" -> TIME
        "TIMESTAMP WITH TIME ZONE" -> TIMESTAMP
        else -> KColumnType.fromString(type)
    }
}
