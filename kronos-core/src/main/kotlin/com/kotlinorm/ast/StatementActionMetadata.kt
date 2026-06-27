/**
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kotlinorm.ast

fun Statement.tableNameOrNull(): String? = when (this) {
    is InsertStatement -> table.tableNameOrNull()
    is UpdateStatement -> table.tableNameOrNull()
    is DeleteStatement -> table.tableNameOrNull()
    is DdlStatement.CreateTableStatement -> tableName
    is DdlStatement.AlterTableStatement -> tableName
    is DdlStatement.DropTableStatement -> tableName
    is DdlStatement.CreateIndexStatement -> tableName
    is DdlStatement.DropIndexStatement -> tableName
    is DdlStatement.TruncateTableStatement -> tableName
    is SelectStatement -> from?.tableNameOrNull()
    is UnionStatement -> null
}

fun Statement.whereOrNull(): Expression? = when (this) {
    is UpdateStatement -> where
    is DeleteStatement -> where
    is SelectStatement -> where
    else -> null
}

fun TableReference.tableNameOrNull(): String? = when (this) {
    is TableName -> table
    is TableReferenceImpl.SimpleTableReference -> tableName
    is JoinTable -> left.tableNameOrNull()
    is TableReferenceImpl.JoinedTableReference -> baseTable.tableNameOrNull()
    is SubqueryTable,
    is TableReferenceImpl.SubqueryTableReference -> null
}

fun Expression.renderSql(renderer: SqlRenderer, context: RenderContext): String =
    renderer.renderExpression(this, context)
