/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.kotlinorm.database.oracle

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
import com.kotlinorm.enums.KColumnType.*
import com.kotlinorm.enums.PrimaryKeyType
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.syntax.SqlIdentifier
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlParameter
import com.kotlinorm.syntax.statement.SqlDdlStatement
import com.kotlinorm.syntax.statement.SqlDmlStatement
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.syntax.statement.SqlStatement
import com.kotlinorm.syntax.table.SqlTable
import com.kotlinorm.syntax.table.SqlTableAlias

object OracleStatements : DatabaseStatements() {
    override fun databaseName(wrapper: KronosDataSourceWrapper): String = wrapper.userName.uppercase()

    private fun table(tableName: String) = SqlIdentifier.of(tableName.uppercase())

    override fun tableExists(): SqlQuery = SqlQuery.Select(
        select = listOf(SqlSelectItem.Expr(SqlExpr.UnsafeRaw("COUNT(*)"))),
        from = listOf(SqlTable.Ident("all_tables")),
        where = SqlExpr.Binary(
            SqlExpr.Binary(SqlExpr.UnsafeRaw("owner"), SqlBinaryOperator.Equal, SqlExpr.Parameter(SqlParameter.Named("dbName"))),
            SqlBinaryOperator.And,
            SqlExpr.Binary(
                SqlExpr.UnsafeRaw("table_name"),
                SqlBinaryOperator.Equal,
                SqlExpr.Function(name = SqlIdentifier.of("UPPER"), args = listOf(SqlExpr.Parameter(SqlParameter.Named("tableName"))))
            )
        )
    )

    override fun tableComment(): SqlQuery = SqlQuery.Select(
        select = listOf(SqlSelectItem.Expr(SqlExpr.UnsafeRaw("comments"))),
        from = listOf(SqlTable.Ident("all_tab_comments")),
        where = SqlExpr.Binary(
            SqlExpr.Binary(SqlExpr.UnsafeRaw("owner"), SqlBinaryOperator.Equal, SqlExpr.Parameter(SqlParameter.Named("dbName"))),
            SqlBinaryOperator.And,
            SqlExpr.Binary(
                SqlExpr.UnsafeRaw("table_name"),
                SqlBinaryOperator.Equal,
                SqlExpr.Function(name = SqlIdentifier.of("UPPER"), args = listOf(SqlExpr.Parameter(SqlParameter.Named("tableName"))))
            )
        )
    )

    override fun tableColumns(tableName: String): SqlQuery = SqlQuery.Select(
        select = listOf(
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("cols.column_name AS COLUMN_NAME")),
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("cols.data_type AS DATA_TYPE")),
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("cols.data_length AS LENGTH")),
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("cols.data_precision AS PRECISION")),
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("cols.data_scale AS SCALE")),
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("cols.nullable AS IS_NULLABLE")),
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("cols.data_default AS COLUMN_DEFAULT")),
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("CASE WHEN EXISTS (SELECT 1 FROM all_cons_columns cons_cols JOIN all_constraints cons ON cons.owner = cons_cols.owner AND cons.constraint_name = cons_cols.constraint_name AND cons.table_name = cons_cols.table_name WHERE cons.constraint_type = 'P' AND cons_cols.owner = cols.owner AND cons_cols.table_name = cols.table_name AND cons_cols.column_name = cols.column_name) THEN '1' ELSE '0' END AS PRIMARY_KEY")),
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("(SELECT comments FROM all_col_comments cc WHERE cc.owner = cols.owner AND cc.table_name = cols.table_name AND cc.column_name = cols.column_name) AS COLUMN_COMMENT"))
        ),
        from = listOf(SqlTable.Ident("all_tab_columns", alias = SqlTableAlias("cols"))),
        where = SqlExpr.Binary(
            SqlExpr.Binary(
                SqlExpr.UnsafeRaw("cols.table_name"),
                SqlBinaryOperator.Equal,
                SqlExpr.Function(name = SqlIdentifier.of("UPPER"), args = listOf(SqlExpr.Parameter(SqlParameter.Named("tableName"))))
            ),
            SqlBinaryOperator.And,
            SqlExpr.Binary(SqlExpr.UnsafeRaw("cols.owner"), SqlBinaryOperator.Equal, SqlExpr.Parameter(SqlParameter.Named("dbName")))
        )
    )

    override fun tableIndexes(tableName: String): SqlQuery = SqlQuery.Select(
        select = listOf(
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("i.INDEX_NAME AS NAME")),
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("ic.COLUMN_NAME AS COLUMN_NAME")),
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("ic.COLUMN_POSITION AS SEQ_IN_INDEX")),
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("i.UNIQUENESS AS UNIQUENESS")),
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("i.INDEX_TYPE AS INDEX_TYPE"))
        ),
        from = listOf(
            SqlTable.Ident("ALL_INDEXES", alias = SqlTableAlias("i")),
            SqlTable.Ident("ALL_IND_COLUMNS", alias = SqlTableAlias("ic"))
        ),
        where = SqlExpr.UnsafeRaw(
            "i.OWNER = ic.INDEX_OWNER AND i.INDEX_NAME = ic.INDEX_NAME " +
                "AND i.TABLE_NAME = UPPER(:tableName) AND i.OWNER = :dbName " +
                "AND i.INDEX_NAME NOT LIKE UPPER('SYS_%')"
        )
    )

    override fun mapColumns(tableName: String, rows: List<Map<String, Any>>): List<Field> =
        rows.map {
            val dataType = it.cell("DATA_TYPE").toString()
            val precision = it.cell("PRECISION").asInt()
            val scale = it.cell("SCALE").asInt()
            val length = if (dataType.equals("NUMBER", ignoreCase = true) && precision > 0) {
                precision
            } else {
                it.cell("LENGTH").asInt()
            }
            val defaultValue = it.cell("COLUMN_DEFAULT")?.toString()
            Field(
                columnName = it.cell("COLUMN_NAME").toString().lowercase(),
                type = getKColumnType(dataType, length, scale),
                length = length,
                scale = scale,
                tableName = tableName.uppercase(),
                nullable = it.cell("IS_NULLABLE") == "Y",
                primaryKey = when {
                    it.cell("PRIMARY_KEY") == "0" -> PrimaryKeyType.NOT
                    defaultValue?.endsWith(".nextval") == true -> PrimaryKeyType.IDENTITY
                    else -> PrimaryKeyType.DEFAULT
                },
                defaultValue = defaultValue?.takeUnless { value -> value.endsWith(".nextval") },
                kDoc = it.cell("COLUMN_COMMENT") as String?
            )
        }

    override fun mapIndexes(tableName: String, rows: List<Map<String, Any>>): List<KTableIndex> =
        rows.groupBy { it.cell("NAME").toString() }.mapNotNull { (indexName, columns) ->
            val first = columns.firstOrNull() ?: return@mapNotNull null
            KTableIndex(
                name = indexName,
                columns = columns.sortedBy { it.cell("SEQ_IN_INDEX").asInt() }
                    .map { it.cell("COLUMN_NAME").toString().lowercase() }
                    .toTypedArray(),
                type = if (first.cell("UNIQUENESS") == "UNIQUE") "UNIQUE" else "NORMAL",
                method = first.cell("INDEX_TYPE")?.toString()?.takeUnless { it.equals("NORMAL", ignoreCase = true) }.orEmpty()
            )
        }

    override fun createTable(input: DatabaseCreateTable): List<SqlStatement> {
        val table = table(input.tableName)
        return listOf(
            SqlDdlStatement.CreateTable(
                tableName = table,
                columns = input.columns.map { it.oracleColumnDefinition() }
            )
        ) + input.indexes.map { it.toCreateIndexStatement(table) } +
            input.columns.filter { !it.kDoc.isNullOrEmpty() }.map {
                SqlDdlStatement.CommentOnColumn(table, SqlIdentifier.of(it.columnName.uppercase()), it.kDoc)
            } +
            listOfNotNull(input.tableComment?.takeIf { it.isNotEmpty() }?.let {
                SqlDdlStatement.CommentOnTable(table, it)
            })
    }

    override fun dropTable(tableName: String, ifExists: Boolean): List<SqlStatement> =
        listOf(SqlDdlStatement.DropTable(table(tableName), ifExists = false))

    override fun truncateTable(tableName: String, restartIdentity: Boolean): List<SqlStatement> =
        listOf(SqlDmlStatement.Truncate(SqlTable.Ident(tableName.uppercase(), identifier = table(tableName))))

    override fun syncTable(input: DatabaseSyncTable): List<SqlStatement> {
        val table = table(input.tableName)
        return buildList {
            if (input.originalTableComment.orEmpty() != input.tableComment.orEmpty()) {
                add(SqlDdlStatement.CommentOnTable(table, input.tableComment.orEmpty()))
            }
            addAll(input.indexes.toDelete.map { SqlDdlStatement.DropIndex(SqlIdentifier.of(it.name)) })
            addAll(input.columns.toDelete.map { SqlDdlStatement.AlterTable.DropColumn(table, SqlIdentifier.of(it.columnName.uppercase())) })
            addAll(input.columns.toModified.map { (column, _, current) ->
                SqlDdlStatement.AlterTable.ModifyColumn(table, column.oracleColumnDefinition())
            })
            addAll(input.columns.toAdd.map { SqlDdlStatement.AlterTable.AddColumn(table, it.first.oracleColumnDefinition()) })
            input.columns.toModified.forEach { (column, _, current) ->
                if (column.kDoc.orEmpty() != current.kDoc.orEmpty()) {
                    add(SqlDdlStatement.CommentOnColumn(table, SqlIdentifier.of(column.columnName.uppercase()), column.kDoc))
                }
            }
            addAll(input.indexes.toAdd.map { it.toCreateIndexStatement(table) })
        }
    }

    private fun Field.oracleColumnDefinition() =
        toColumnDefinition(::getColumnType).copy(name = SqlIdentifier.of(columnName.uppercase()))

    private fun getColumnType(type: KColumnType, length: Int, scale: Int): String = when (type) {
        BIT -> "NUMBER(1)"
        TINYINT -> "NUMBER(3)"
        SMALLINT -> "NUMBER(5)"
        MEDIUMINT -> "NUMBER(7)"
        INT -> "NUMBER(${length.takeIf { it > 0 } ?: 10})"
        BIGINT -> "NUMBER(19)"
        SERIAL -> "NUMBER"
        REAL -> "BINARY_FLOAT"
        FLOAT -> if (length > 0) "FLOAT($length)" else "BINARY_DOUBLE"
        DOUBLE -> "BINARY_DOUBLE"
        DECIMAL -> when {
            length > 0 && scale > 0 -> "NUMBER($length,$scale)"
            length > 0 -> "NUMBER($length,0)"
            else -> "NUMBER(10,0)"
        }
        NUMERIC -> when {
            length > 0 && scale > 0 -> "NUMERIC($length,$scale)"
            else -> "NUMERIC(10,0)"
        }
        CHAR -> "CHAR(${length.takeIf { it > 0 } ?: 255})"
        VARCHAR -> "VARCHAR2(${length.takeIf { it > 0 } ?: 255})"
        NVARCHAR -> "NVARCHAR2(${length.takeIf { it > 0 } ?: 255})"
        NCHAR -> "NCHAR(${length.takeIf { it > 0 } ?: 255})"
        TEXT, MEDIUMTEXT, LONGTEXT, CLOB -> "CLOB"
        NCLOB -> "NCLOB"
        BINARY, VARBINARY -> "RAW(${length.takeIf { it > 0 } ?: 2000})"
        BLOB, MEDIUMBLOB, LONGBLOB, LONGVARBINARY -> "BLOB"
        DATE -> "DATE"
        TIME -> "TIMESTAMP(0)"
        DATETIME -> "TIMESTAMP(6)"
        TIMESTAMP -> "TIMESTAMP(${scale.coerceIn(0, 9)})"
        JSON -> "JSON"
        XML -> "XMLType"
        UUID -> "CHAR(36)"
        ENUM -> "VARCHAR2(255)"
        SET -> "VARCHAR2(1000)"
        GEOMETRY, POINT, LINESTRING -> "SDO_GEOMETRY"
        YEAR -> "NUMBER(4)"
        else -> "VARCHAR2(255)"
    }

    private fun getKColumnType(type: String, length: Int, scale: Int): KColumnType = when (type.uppercase()) {
        "NUMBER" -> when (length) {
            1 -> BIT
            3 -> TINYINT
            5 -> SMALLINT
            7 -> MEDIUMINT
            19 -> BIGINT
            else -> INT
        }
        "VARCHAR2" -> VARCHAR
        else -> KColumnType.fromString(type)
    }
}
