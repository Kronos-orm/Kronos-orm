/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.syntax.statement

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

data class SqlUpdateSetPair(
    val column: String,
    val value: SqlExpr
) : SqlNode

sealed interface SqlDmlStatement : SqlStatement {
    data class Delete(
        val table: SqlTable.Ident,
        val where: SqlExpr? = null
    ) : SqlDmlStatement

    data class Insert(
        val table: SqlTable.Ident,
        val columns: List<String> = emptyList(),
        val mode: SqlInsertMode
    ) : SqlDmlStatement

    data class Update(
        val table: SqlTable.Ident,
        val setPairs: List<SqlUpdateSetPair>,
        val where: SqlExpr? = null
    ) : SqlDmlStatement {
        init {
            require(setPairs.isNotEmpty()) { "UPDATE requires at least one SET pair." }
        }
    }

    data class Truncate(
        val table: SqlTable.Ident
    ) : SqlDmlStatement

    data class Upsert(
        val table: SqlTable.Ident,
        val columns: List<String>,
        val values: List<SqlExpr>,
        val primaryKeys: List<String>,
        val updateColumns: List<String>
    ) : SqlDmlStatement {
        init {
            require(columns.isNotEmpty()) { "UPSERT requires at least one column." }
            require(values.isNotEmpty()) { "UPSERT requires at least one value." }
            require(primaryKeys.isNotEmpty()) { "UPSERT requires at least one primary key." }
        }
    }
}

