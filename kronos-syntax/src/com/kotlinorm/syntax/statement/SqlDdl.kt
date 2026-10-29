/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.syntax.statement

import com.kotlinorm.syntax.SqlIdentifier
import com.kotlinorm.syntax.SqlNode
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlType

sealed interface SqlDdlStatement : SqlStatement {
    data class CreateTable(
        val tableName: SqlIdentifier,
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
        val tableName: SqlIdentifier,
        val query: SqlQuery,
        val ifNotExists: Boolean = true
    ) : SqlDdlStatement

    sealed interface AlterTable : SqlDdlStatement {
        val tableName: SqlIdentifier

        data class AddColumn(
            override val tableName: SqlIdentifier,
            val column: SqlColumnDefinition,
            val position: SqlColumnPosition? = null
        ) : AlterTable

        data class DropColumn(
            override val tableName: SqlIdentifier,
            val columnName: SqlIdentifier
        ) : AlterTable

        data class ModifyColumn(
            override val tableName: SqlIdentifier,
            val column: SqlColumnDefinition,
            val position: SqlColumnPosition? = null
        ) : AlterTable

        data class RenameColumn(
            override val tableName: SqlIdentifier,
            val oldName: SqlIdentifier,
            val newName: SqlIdentifier
        ) : AlterTable

        data class RenameTable(
            override val tableName: SqlIdentifier,
            val newName: SqlIdentifier
        ) : AlterTable

        data class AlterColumnDefault(
            override val tableName: SqlIdentifier,
            val columnName: SqlIdentifier,
            val defaultValue: SqlExpr?
        ) : AlterTable

        data class AlterColumnNullable(
            override val tableName: SqlIdentifier,
            val columnName: SqlIdentifier,
            val nullable: Boolean
        ) : AlterTable

        data class SetTableComment(
            override val tableName: SqlIdentifier,
            val comment: String?
        ) : AlterTable
    }

    data class DropTable(
        val tableName: SqlIdentifier,
        val ifExists: Boolean = false
    ) : SqlDdlStatement

    data class CreateIndex(
        val indexName: SqlIdentifier,
        val tableName: SqlIdentifier,
        val columns: List<SqlIdentifier>,
        val unique: Boolean = false,
        val method: String? = null,
        val type: String? = null,
        val concurrently: Boolean = false,
        val ifNotExists: Boolean = false
    ) : SqlDdlStatement {
        init {
            require(columns.isNotEmpty()) { "CREATE INDEX requires at least one column." }
        }
    }

    data class DropIndex(
        val indexName: SqlIdentifier,
        val tableName: SqlIdentifier? = null,
        val ifExists: Boolean = false,
        val concurrently: Boolean = false
    ) : SqlDdlStatement

    data class AddConstraint(
        val tableName: SqlIdentifier,
        val constraint: SqlTableConstraint
    ) : SqlDdlStatement

    data class DropConstraint(
        val tableName: SqlIdentifier,
        val constraintName: SqlIdentifier
    ) : SqlDdlStatement

    data class CommentOnTable(
        val tableName: SqlIdentifier,
        val comment: String?
    ) : SqlDdlStatement

    data class CommentOnColumn(
        val tableName: SqlIdentifier,
        val columnName: SqlIdentifier,
        val comment: String?
    ) : SqlDdlStatement

    data class Vacuum(
        val schemaName: SqlIdentifier? = null,
        val into: SqlExpr? = null
    ) : SqlDdlStatement

    data class SqlServerExtendedPropertyComment(
        val tableName: SqlIdentifier,
        val columnName: SqlIdentifier? = null,
        val comment: String?,
        val schemaName: SqlIdentifier? = null,
        val propertyName: String = "MS_Description",
        val operation: SqlServerExtendedPropertyOperation = SqlServerExtendedPropertyOperation.Add
    ) : SqlDdlStatement {
        init {
            require(propertyName.isNotBlank()) { "SQL Server extended property name must not be blank." }
        }
    }

    data class SqlServerDropDefaultConstraint(
        val tableName: SqlIdentifier,
        val columnName: SqlIdentifier,
        val schemaName: SqlIdentifier? = null
    ) : SqlDdlStatement

}

enum class SqlServerExtendedPropertyOperation {
    Add,
    Update,
    Drop
}

sealed interface SqlColumnPosition : SqlNode {
    object First : SqlColumnPosition

    data class After(val columnName: SqlIdentifier) : SqlColumnPosition
}

data class SqlColumnDefinition(
    val name: SqlIdentifier,
    val type: SqlType,
    val nullable: Boolean = true,
    val primaryKey: SqlPrimaryKeyMode = SqlPrimaryKeyMode.NotPrimary,
    val defaultValue: SqlExpr? = null
) : SqlNode

data class SqlIndexDefinition(
    val name: SqlIdentifier,
    val columns: List<SqlIdentifier>,
    val unique: Boolean = false,
    val method: String? = null
) : SqlNode {
    init {
        require(columns.isNotEmpty()) { "Index definition requires at least one column." }
    }
}

sealed interface SqlTableConstraint : SqlNode {
    val name: SqlIdentifier?

    data class PrimaryKey(
        override val name: SqlIdentifier? = null,
        val columns: List<SqlIdentifier>
    ) : SqlTableConstraint {
        init {
            require(columns.isNotEmpty()) { "PRIMARY KEY constraint requires at least one column." }
        }
    }

    data class Unique(
        override val name: SqlIdentifier? = null,
        val columns: List<SqlIdentifier>
    ) : SqlTableConstraint {
        init {
            require(columns.isNotEmpty()) { "UNIQUE constraint requires at least one column." }
        }
    }

    data class Check(
        override val name: SqlIdentifier? = null,
        val condition: SqlExpr
    ) : SqlTableConstraint

    data class ForeignKey(
        override val name: SqlIdentifier? = null,
        val columns: List<SqlIdentifier>,
        val referencedTable: SqlIdentifier,
        val referencedColumns: List<SqlIdentifier>
    ) : SqlTableConstraint {
        init {
            require(columns.isNotEmpty()) { "FOREIGN KEY constraint requires at least one column." }
            require(referencedColumns.isNotEmpty()) { "FOREIGN KEY constraint requires at least one referenced column." }
        }
    }
}

enum class SqlPrimaryKeyMode {
    NotPrimary,
    Primary,
    Identity,
    Uuid,
    Snowflake
}
