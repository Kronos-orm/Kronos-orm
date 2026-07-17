/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.database.postgres

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
import com.kotlinorm.syntax.statement.SqlPrimaryKeyMode
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.syntax.statement.SqlStatement
import com.kotlinorm.syntax.table.SqlTable
import com.kotlinorm.syntax.table.SqlTableAlias

object PostgresqlStatements : DatabaseStatements() {
    override val defaultIndexType: String = "NORMAL"
    override val defaultIndexMethod: String = "BTREE"

    override fun sameColumnDefinition(expected: Field, current: Field): Boolean =
        getColumnType(expected.type, expected.length, expected.scale) ==
            getColumnType(current.type, current.length, current.scale) &&
            expected.sameColumnAttributesAs(current)

    override fun databaseName(wrapper: KronosDataSourceWrapper): String =
        wrapper.url.substringBefore("?").substringAfterLast("/")

    private fun table(tableName: String) = SqlIdentifier.of("public", tableName)

    override fun tableExists(): SqlQuery = SqlQuery.Select(
        select = listOf(SqlSelectItem.Expr(SqlExpr.UnsafeRaw("COUNT(*)"))),
        from = listOf(SqlTable.Ident("information_schema.tables", identifier = SqlIdentifier.of("information_schema", "tables"))),
        where = SqlExpr.Binary(
            SqlExpr.Binary(SqlExpr.UnsafeRaw("table_schema"), SqlBinaryOperator.Equal, SqlExpr.Function(name = SqlIdentifier.of("current_schema"))),
            SqlBinaryOperator.And,
            SqlExpr.Binary(SqlExpr.UnsafeRaw("table_name"), SqlBinaryOperator.Equal, SqlExpr.Parameter(SqlParameter.Named("tableName")))
        )
    )

    override fun tableComment(): SqlQuery = SqlQuery.Select(
        select = listOf(SqlSelectItem.Expr(SqlExpr.UnsafeRaw("obj_description((current_schema() || '.' || :tableName)::regclass::oid)")))
    )

    override fun tableColumns(tableName: String): SqlQuery = SqlQuery.Select(
        select = listOf(
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("c.column_name AS COLUMN_NAME")),
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("col_description((current_schema() || '.' || c.table_name)::regclass::oid, c.ordinal_position) AS COLUMN_COMMENT")),
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("CASE WHEN c.data_type IN ('character varying', 'varchar') THEN 'VARCHAR' WHEN c.data_type IN ('integer', 'int') THEN 'INT' WHEN c.data_type IN ('bigint') THEN 'BIGINT' WHEN c.data_type IN ('smallint') THEN 'TINYINT' WHEN c.data_type IN ('decimal', 'numeric') THEN 'DECIMAL' WHEN c.data_type IN ('double precision', 'real') THEN 'DOUBLE' WHEN c.data_type IN ('boolean') THEN 'BOOLEAN' WHEN c.data_type LIKE 'timestamp%' THEN 'TIMESTAMP' WHEN c.data_type LIKE 'date' THEN 'DATE' ELSE c.data_type END AS DATA_TYPE")),
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("c.character_maximum_length AS LENGTH")),
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("c.numeric_precision AS SCALE")),
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("c.is_nullable = 'YES' AS IS_NULLABLE")),
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("c.column_default AS COLUMN_DEFAULT")),
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("EXISTS (SELECT 1 FROM information_schema.key_column_usage kcu INNER JOIN information_schema.table_constraints tc ON kcu.constraint_name = tc.constraint_name AND kcu.constraint_schema = tc.constraint_schema WHERE tc.constraint_type = 'PRIMARY KEY' AND kcu.table_schema = c.table_schema AND kcu.table_name = c.table_name AND kcu.column_name = c.column_name) OR (c.column_name = 'id' AND c.data_type LIKE 'serial%') AS PRIMARY_KEY"))
        ),
        from = listOf(
            SqlTable.Ident(
                "information_schema.columns",
                alias = SqlTableAlias("c"),
                identifier = SqlIdentifier.of("information_schema", "columns")
            )
        ),
        where = SqlExpr.Binary(
            SqlExpr.Binary(SqlExpr.UnsafeRaw("c.table_schema"), SqlBinaryOperator.Equal, SqlExpr.Function(name = SqlIdentifier.of("current_schema"))),
            SqlBinaryOperator.And,
            SqlExpr.Binary(SqlExpr.UnsafeRaw("c.table_name"), SqlBinaryOperator.Equal, SqlExpr.Parameter(SqlParameter.Named("tableName")))
        )
    )

    override fun tableIndexes(tableName: String): SqlQuery = SqlQuery.Select(
        select = listOf(
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("indexname AS name")),
            SqlSelectItem.Expr(SqlExpr.UnsafeRaw("indexdef AS indexDef"))
        ),
        from = listOf(SqlTable.Ident("pg_indexes")),
        where = SqlExpr.UnsafeRaw("tablename = :tableName AND schemaname = current_schema() AND indexname NOT LIKE CONCAT(tablename, '_pkey')")
    )

    override fun mapColumns(tableName: String, rows: List<Map<String, Any>>): List<Field> =
        rows.map {
            val length = it.cell("LENGTH").asInt()
            val scale = it.cell("SCALE").asInt()
            val defaultValue = normalizeDefaultValue(it.cell("COLUMN_DEFAULT")?.toString())
            Field(
                columnName = it.cell("COLUMN_NAME").toString(),
                type = getKColumnType(it.cell("DATA_TYPE").toString(), length, scale),
                length = length,
                scale = scale,
                tableName = tableName,
                nullable = it.cell("IS_NULLABLE") == true,
                primaryKey = when {
                    it.cell("PRIMARY_KEY") == false -> PrimaryKeyType.NOT
                    defaultValue?.startsWith("nextval(") == true -> PrimaryKeyType.IDENTITY
                    else -> PrimaryKeyType.DEFAULT
                },
                defaultValue = defaultValue?.takeUnless { value -> value.startsWith("nextval(") },
                kDoc = it.cell("COLUMN_COMMENT") as String?
            )
        }

    override fun mapIndexes(tableName: String, rows: List<Map<String, Any>>): List<KTableIndex> =
        rows.map { row ->
            val indexDef = row.cell("indexDef")?.toString().orEmpty()
            KTableIndex(
                name = row.cell("name").toString(),
                columns = indexDef.substringAfterLast("(", "")
                    .substringBeforeLast(")", "")
                    .split(',')
                    .map { it.trim().trim('"') }
                    .filter { it.isNotBlank() }
                    .toTypedArray(),
                type = if (indexDef.contains(" UNIQUE INDEX ", ignoreCase = true)) "UNIQUE" else "NORMAL",
                method = indexDef.substringAfter(" USING ", "")
                    .substringBefore(" ", "")
                    .takeIf { it.isNotBlank() }
                    .orEmpty()
            )
        }

    override fun createTable(input: DatabaseCreateTable): List<SqlStatement> {
        val table = table(input.tableName)
        return listOf(
            SqlDdlStatement.CreateTable(
                tableName = table,
                columns = input.columns.map { it.postgresColumnDefinition() },
                ifNotExists = true
            )
        ) + input.indexes.map { it.toCreateIndexStatement(table) } +
            input.columns.filter { !it.kDoc.isNullOrEmpty() }.map {
                SqlDdlStatement.CommentOnColumn(table, SqlIdentifier.of(it.columnName), it.kDoc)
            } +
            listOfNotNull(input.tableComment?.takeIf { it.isNotEmpty() }?.let {
                SqlDdlStatement.CommentOnTable(table, it)
            })
    }

    override fun dropTable(tableName: String, ifExists: Boolean): List<SqlStatement> =
        listOf(SqlDdlStatement.DropTable(table(tableName), ifExists))

    override fun truncateTable(tableName: String, restartIdentity: Boolean): List<SqlStatement> =
        listOf(SqlDmlStatement.Truncate(SqlTable.Ident(tableName, identifier = table(tableName)), restartIdentity))

    override fun syncTable(input: DatabaseSyncTable): List<SqlStatement> {
        val table = table(input.tableName)
        return buildList {
            if (input.originalTableComment.orEmpty() != input.tableComment.orEmpty()) {
                add(SqlDdlStatement.CommentOnTable(table, input.tableComment.orEmpty()))
            }
            addAll(input.indexes.toDelete.map { SqlDdlStatement.DropIndex(SqlIdentifier.of("public", it.name)) })
            addAll(input.columns.toAdd.map { SqlDdlStatement.AlterTable.AddColumn(table, it.first.postgresColumnDefinition()) })
            input.columns.toModified.forEach { (column, _, current) ->
                add(SqlDdlStatement.AlterTable.ModifyColumn(table, column.postgresColumnDefinition()))
                if (column.defaultValue.orEmpty() != current.defaultValue.orEmpty()) {
                    add(SqlDdlStatement.AlterTable.AlterColumnDefault(table, SqlIdentifier.of(column.columnName), column.defaultValue?.let(SqlExpr::UnsafeRaw)))
                }
                if (column.nullable != current.nullable) {
                    add(SqlDdlStatement.AlterTable.AlterColumnNullable(table, SqlIdentifier.of(column.columnName), column.nullable))
                }
                if (column.kDoc.orEmpty() != current.kDoc.orEmpty()) {
                    add(SqlDdlStatement.CommentOnColumn(table, SqlIdentifier.of(column.columnName), column.kDoc))
                }
            }
            addAll(input.columns.toDelete.map { SqlDdlStatement.AlterTable.DropColumn(table, SqlIdentifier.of(it.columnName)) })
            addAll(input.indexes.toAdd.map { it.toCreateIndexStatement(table) })
        }
    }

    private fun Field.postgresColumnDefinition() =
        toColumnDefinition(::getColumnType).let { definition ->
            if (primaryKey == PrimaryKeyType.IDENTITY) {
                definition.copy(
                    type = com.kotlinorm.syntax.expr.SqlType.UnsafeCustom(
                        listOf(com.kotlinorm.syntax.token.SqlUnsafeToken.Text(if (type == BIGINT) "BIGSERIAL" else "SERIAL"))
                    ),
                    primaryKey = SqlPrimaryKeyMode.Primary
                )
            } else {
                definition
            }
        }

    private fun normalizeDefaultValue(defaultValue: String?): String? =
        defaultValue?.replace(Regex("""::[\w\s]+"""), "")

    private fun getColumnType(type: KColumnType, length: Int, scale: Int): String = when (type) {
        BIT -> "BOOLEAN"
        TINYINT, SMALLINT -> "SMALLINT"
        INT, MEDIUMINT -> "INTEGER"
        BIGINT -> "BIGINT"
        SERIAL -> "SERIAL"
        YEAR -> "INTEGER"
        REAL -> "REAL"
        FLOAT -> if (length > 0) "FLOAT($length)" else "DOUBLE PRECISION"
        DOUBLE -> "DOUBLE PRECISION"
        DECIMAL -> when {
            length > 0 && scale > 0 -> "DECIMAL($length,$scale)"
            length > 0 -> "DECIMAL($length,0)"
            else -> "DECIMAL"
        }
        NUMERIC -> when {
            length > 0 && scale > 0 -> "NUMERIC($length,$scale)"
            length > 0 -> "NUMERIC($length,0)"
            else -> "NUMERIC"
        }
        CHAR, NCHAR -> "CHAR(${length.takeIf { it > 0 } ?: 255})"
        VARCHAR, NVARCHAR -> if (length > 0 && length <= 10485760) "VARCHAR($length)" else "TEXT"
        TEXT, CLOB, MEDIUMTEXT, LONGTEXT -> "TEXT"
        BINARY, VARBINARY, LONGVARBINARY, BLOB, MEDIUMBLOB, LONGBLOB -> "BYTEA"
        DATE -> "DATE"
        TIME -> "TIME(${scale.coerceIn(0, 6)})"
        DATETIME, TIMESTAMP -> "TIMESTAMP(${scale.coerceIn(0, 6)})"
        JSON -> "JSONB"
        XML -> "XML"
        UUID -> "UUID"
        ENUM -> if (length > 0) "VARCHAR($length)" else "VARCHAR(255)"
        SET -> "TEXT"
        GEOMETRY -> "GEOMETRY"
        POINT -> "POINT"
        LINESTRING -> "LINESTRING"
        else -> "TEXT"
    }

    private fun getKColumnType(type: String, length: Int, scale: Int): KColumnType = when (type.uppercase()) {
        "INTEGER" -> INT
        "BYTEA" -> BLOB
        "BOOLEAN" -> BIT
        else -> KColumnType.fromString(type)
    }
}
