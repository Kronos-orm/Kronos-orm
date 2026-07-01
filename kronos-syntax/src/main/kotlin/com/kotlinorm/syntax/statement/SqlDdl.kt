/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.syntax.statement

import com.kotlinorm.syntax.SqlNode
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlType

sealed interface SqlDdlStatement : SqlStatement {
    data class CreateTable(
        val tableName: String,
        val columns: List<SqlColumnDefinition>,
        val indexes: List<SqlIndexDefinition> = emptyList(),
        val comment: String? = null,
        val ifNotExists: Boolean = false
    ) : SqlDdlStatement {
        init {
            require(columns.isNotEmpty()) { "CREATE TABLE requires at least one column." }
        }
    }

    data class CreateTableAsSelect(
        val tableName: String,
        val query: SqlQuery,
        val ifNotExists: Boolean = true
    ) : SqlDdlStatement

    sealed interface AlterTable : SqlDdlStatement {
        val tableName: String

        data class AddColumn(
            override val tableName: String,
            val column: SqlColumnDefinition
        ) : AlterTable

        data class DropColumn(
            override val tableName: String,
            val columnName: String
        ) : AlterTable

        data class ModifyColumn(
            override val tableName: String,
            val column: SqlColumnDefinition
        ) : AlterTable
    }

    data class DropTable(
        val tableName: String,
        val ifExists: Boolean = false
    ) : SqlDdlStatement

    data class CreateIndex(
        val indexName: String,
        val tableName: String,
        val columns: List<String>,
        val unique: Boolean = false
    ) : SqlDdlStatement {
        init {
            require(columns.isNotEmpty()) { "CREATE INDEX requires at least one column." }
        }
    }

    data class DropIndex(
        val indexName: String,
        val tableName: String
    ) : SqlDdlStatement

}

data class SqlColumnDefinition(
    val name: String,
    val type: SqlType,
    val nullable: Boolean = true,
    val primaryKey: SqlPrimaryKeyMode = SqlPrimaryKeyMode.NotPrimary,
    val defaultValue: SqlExpr? = null
) : SqlNode

data class SqlIndexDefinition(
    val name: String,
    val columns: List<String>,
    val unique: Boolean = false,
    val method: String? = null
) : SqlNode {
    init {
        require(columns.isNotEmpty()) { "Index definition requires at least one column." }
    }
}

enum class SqlPrimaryKeyMode {
    NotPrimary,
    Primary,
    Identity,
    Uuid,
    Snowflake
}
