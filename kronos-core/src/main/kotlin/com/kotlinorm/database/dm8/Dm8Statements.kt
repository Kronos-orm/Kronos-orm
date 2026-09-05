/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.database.dm8

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableIndex
import com.kotlinorm.database.DatabaseCreateTable
import com.kotlinorm.database.DatabaseStatements
import com.kotlinorm.database.DatabaseSyncTable
import com.kotlinorm.database.oracle.OracleStatements
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.enums.PrimaryKeyType
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.syntax.SqlIdentifier
import com.kotlinorm.syntax.expr.SqlType
import com.kotlinorm.syntax.statement.SqlColumnDefinition
import com.kotlinorm.syntax.statement.SqlDdlStatement
import com.kotlinorm.syntax.statement.SqlPrimaryKeyMode
import com.kotlinorm.syntax.statement.SqlQuery
import com.kotlinorm.syntax.statement.SqlStatement
import com.kotlinorm.syntax.token.SqlUnsafeToken

/** DM8 uses Oracle-compatible query and metadata SQL, with native identity-column DDL. */
object Dm8Statements : DatabaseStatements() {
    override val defaultIndexType: String = OracleStatements.defaultIndexType
    override val defaultIndexMethod: String = OracleStatements.defaultIndexMethod

    override fun canonicalColumnName(name: String): String = OracleStatements.canonicalColumnName(name)

    override fun sameColumnDefinition(expected: Field, current: Field): Boolean =
        expected.matchesDm8IdentityColumn(current) || OracleStatements.sameColumnDefinition(expected, current)

    override fun databaseName(wrapper: KronosDataSourceWrapper): String = OracleStatements.databaseName(wrapper)

    override fun tableExists(): SqlQuery = OracleStatements.tableExists()

    override fun tableComment(): SqlQuery = OracleStatements.tableComment()

    override fun tableColumns(tableName: String): SqlQuery = OracleStatements.tableColumns(tableName)

    override fun tableIndexes(tableName: String): SqlQuery {
        val oracleQuery = OracleStatements.tableIndexes(tableName) as SqlQuery.Select
        val oracleWhere = oracleQuery.where as? com.kotlinorm.syntax.expr.SqlExpr.UnsafeRaw
            ?: return oracleQuery
        return oracleQuery.copy(
            select = listOf(
                com.kotlinorm.syntax.statement.SqlSelectItem.Expr(
                    com.kotlinorm.syntax.expr.SqlExpr.UnsafeRaw(
                        "COALESCE((SELECT c.CONSTRAINT_NAME FROM ALL_CONSTRAINTS c " +
                            "WHERE c.OWNER = i.OWNER AND c.TABLE_NAME = i.TABLE_NAME " +
                            "AND c.INDEX_NAME = i.INDEX_NAME AND c.CONSTRAINT_TYPE = 'U'), " +
                            "i.INDEX_NAME) AS NAME"
                    )
                )
            ) + oracleQuery.select.drop(1),
            where = com.kotlinorm.syntax.expr.SqlExpr.UnsafeRaw(
                "${oracleWhere.sql} AND NOT EXISTS (" +
                    "SELECT 1 FROM ALL_CONSTRAINTS c " +
                    "WHERE c.OWNER = i.OWNER AND c.TABLE_NAME = i.TABLE_NAME " +
                    "AND c.INDEX_NAME = i.INDEX_NAME AND c.CONSTRAINT_TYPE = 'P'" +
                    ") AND (i.GENERATED = 'N' OR EXISTS (" +
                    "SELECT 1 FROM ALL_CONSTRAINTS c " +
                    "WHERE c.OWNER = i.OWNER AND c.TABLE_NAME = i.TABLE_NAME " +
                    "AND c.INDEX_NAME = i.INDEX_NAME AND c.CONSTRAINT_TYPE = 'U'))"
            )
        )
    }

    override fun mapColumns(tableName: String, rows: List<Map<String, Any>>): List<Field> =
        OracleStatements.mapColumns(tableName, rows)

    override fun mapIndexes(tableName: String, rows: List<Map<String, Any>>): List<KTableIndex> =
        OracleStatements.mapIndexes(tableName, rows)

    override fun createTable(input: DatabaseCreateTable): List<SqlStatement> {
        val fieldsByName = input.columns.associateBy { it.columnName.uppercase() }
        return OracleStatements.createTable(input).map { statement ->
            if (statement is SqlDdlStatement.CreateTable) {
                statement.copy(columns = statement.columns.map { column -> column.withDm8Identity(fieldsByName[column.name.canonical]) })
            } else {
                statement
            }
        }
    }

    override fun dropTable(tableName: String, ifExists: Boolean): List<SqlStatement> =
        listOf(SqlDdlStatement.DropTable(SqlIdentifier.of(tableName.uppercase()), ifExists = ifExists))

    override fun truncateTable(tableName: String, restartIdentity: Boolean): List<SqlStatement> =
        OracleStatements.truncateTable(tableName, restartIdentity)

    override fun syncTable(input: DatabaseSyncTable): List<SqlStatement> {
        val fieldsByName = input.expectedColumns.associateBy { it.columnName.uppercase() }
        return OracleStatements.syncTable(input).map { statement ->
            when (statement) {
                is SqlDdlStatement.AlterTable.AddColumn ->
                    statement.copy(column = statement.column.withDm8Identity(fieldsByName[statement.column.name.canonical]))

                is SqlDdlStatement.AlterTable.ModifyColumn ->
                    statement.copy(column = statement.column.withDm8Identity(fieldsByName[statement.column.name.canonical]))

                else -> statement
            }
        }
    }

    private fun Field.matchesDm8IdentityColumn(current: Field): Boolean =
        primaryKey == PrimaryKeyType.IDENTITY &&
            current.primaryKey == PrimaryKeyType.DEFAULT &&
            dm8IdentityBaseType() == current.type &&
            nullable == current.nullable &&
            defaultValue.orEmpty() == current.defaultValue.orEmpty() &&
            kDoc.orEmpty() == current.kDoc.orEmpty()

    private fun Field.dm8IdentityBaseType(): KColumnType =
        if (type == KColumnType.BIGINT) KColumnType.BIGINT else KColumnType.INT

    private fun SqlColumnDefinition.withDm8Identity(field: Field?): SqlColumnDefinition {
        if (field?.primaryKey != PrimaryKeyType.IDENTITY) return this
        val typeName = if (field.type == KColumnType.BIGINT) "BIGINT" else "INT"
        return copy(
            type = SqlType.UnsafeCustom(listOf(SqlUnsafeToken.Text("$typeName IDENTITY(1,1)"))),
            primaryKey = SqlPrimaryKeyMode.Primary
        )
    }
}
