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
import com.kotlinorm.syntax.table.SqlTable

sealed interface SqlStatement : SqlNode

sealed interface SqlInsertMode : SqlNode {
    data class Values(val values: List<List<SqlExpr>>) : SqlInsertMode {
        init {
            require(values.isNotEmpty()) { "INSERT VALUES requires at least one row." }
            require(values.all { it.isNotEmpty() }) { "INSERT VALUES rows must not be empty." }
        }
    }

    data class Subquery(val query: SqlQuery) : SqlInsertMode
}

data class SqlReturning(
    val items: List<SqlSelectItem>
) : SqlNode {
    init {
        require(items.isNotEmpty()) { "RETURNING requires at least one item." }
    }
}

sealed interface SqlAssignmentTarget : SqlNode {
    data class Column(
        val identifier: SqlIdentifier,
        val qualifier: SqlIdentifier? = null
    ) : SqlAssignmentTarget
}

data class SqlUpdateSetPair(
    val target: SqlAssignmentTarget,
    val value: SqlExpr
) : SqlNode

data class SqlConflictTarget(
    val columns: List<SqlIdentifier> = emptyList(),
    val constraintName: SqlIdentifier? = null,
    val where: SqlExpr? = null
) : SqlNode

sealed interface SqlUpsertAction : SqlNode {
    object DoNothing : SqlUpsertAction

    data class Update(
        val setPairs: List<SqlUpdateSetPair>,
        val where: SqlExpr? = null
    ) : SqlUpsertAction
}

sealed interface SqlDmlStatement : SqlStatement {
    data class Delete(
        val table: SqlTable.Ident,
        val where: SqlExpr? = null,
        val returning: SqlReturning? = null
    ) : SqlDmlStatement

    data class Insert(
        val table: SqlTable.Ident,
        val columns: List<SqlIdentifier> = emptyList(),
        val mode: SqlInsertMode,
        val returning: SqlReturning? = null
    ) : SqlDmlStatement

    data class Update(
        val table: SqlTable.Ident,
        val setPairs: List<SqlUpdateSetPair>,
        val where: SqlExpr? = null,
        val returning: SqlReturning? = null
    ) : SqlDmlStatement {
        init {
            require(setPairs.isNotEmpty()) { "UPDATE requires at least one SET pair." }
        }
    }

    data class Truncate(
        val table: SqlTable.Ident,
        val restartIdentity: Boolean = false
    ) : SqlDmlStatement

    data class Upsert(
        val table: SqlTable.Ident,
        val columns: List<SqlIdentifier>,
        val values: List<SqlExpr>,
        val primaryKeys: List<SqlIdentifier>,
        val conflictTarget: SqlConflictTarget = SqlConflictTarget(columns = primaryKeys),
        val action: SqlUpsertAction = SqlUpsertAction.Update(
            columns.filter { column -> column !in primaryKeys }
                .map { column ->
                    SqlUpdateSetPair(
                        SqlAssignmentTarget.Column(column),
                        SqlExpr.ExcludedColumn(column)
                    )
                }
        ),
        val returning: SqlReturning? = null
    ) : SqlDmlStatement {
        init {
            require(columns.isNotEmpty()) { "UPSERT requires at least one column." }
            require(values.isNotEmpty()) { "UPSERT requires at least one value." }
        }
    }
}
