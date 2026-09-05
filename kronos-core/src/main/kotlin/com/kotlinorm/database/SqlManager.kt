/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.database

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.database.mssql.MssqlStatements
import com.kotlinorm.database.dm8.Dm8Statements
import com.kotlinorm.database.h2.H2Statements
import com.kotlinorm.database.mysql.MysqlStatements
import com.kotlinorm.database.oracle.OracleStatements
import com.kotlinorm.database.postgres.PostgresqlStatements
import com.kotlinorm.database.sqlite.SqliteStatements
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.DBType.DM8
import com.kotlinorm.enums.DBType.H2
import com.kotlinorm.enums.DBType.Mssql
import com.kotlinorm.enums.DBType.Mysql
import com.kotlinorm.enums.DBType.Oracle
import com.kotlinorm.enums.DBType.Postgres
import com.kotlinorm.enums.DBType.SQLite
import com.kotlinorm.exceptions.UnsupportedDatabaseTypeException
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.syntax.render.RenderedSql
import com.kotlinorm.syntax.render.SqlDialect
import com.kotlinorm.syntax.statement.SqlStatement

object SqlManager {
    private val dialects: MutableMap<DBType, SqlDialect> = mutableMapOf(
        Mysql to SqlDialect.MySql,
        Postgres to SqlDialect.PostgreSql,
        SQLite to SqlDialect.SQLite,
        H2 to SqlDialect.H2,
        Mssql to SqlDialect.SqlServer,
        Oracle to SqlDialect.Oracle,
        DM8 to SqlDialect.Oracle
    )

    private val statements: MutableMap<DBType, DatabaseStatements> = mutableMapOf(
        Mysql to MysqlStatements,
        Postgres to PostgresqlStatements,
        SQLite to SqliteStatements,
        H2 to H2Statements,
        Mssql to MssqlStatements,
        Oracle to OracleStatements,
        DM8 to Dm8Statements
    )

    fun registerDialect(dbType: DBType, dialect: SqlDialect) {
        dialects[dbType] = dialect
    }

    fun registerStatements(dbType: DBType, definitions: DatabaseStatements) {
        statements[dbType] = definitions
    }

    fun registerDatabase(dbType: DBType, dialect: SqlDialect, definitions: DatabaseStatements) {
        registerDialect(dbType, dialect)
        registerStatements(dbType, definitions)
    }

    fun dialectOf(dbType: DBType): SqlDialect =
        dialects[dbType] ?: throw UnsupportedDatabaseTypeException(dbType)

    fun statementsOf(dbType: DBType): DatabaseStatements =
        statements[dbType] ?: throw UnsupportedDatabaseTypeException(dbType)

    fun renderStatement(
        dataSource: KronosDataSourceWrapper,
        statement: SqlStatement,
        parameterValues: Map<String, Any?> = emptyMap(),
        fieldsMap: Map<String, Field> = emptyMap()
    ): RenderedSql = statement.renderForCore(dataSource, parameterValues, fieldsMap)
}
