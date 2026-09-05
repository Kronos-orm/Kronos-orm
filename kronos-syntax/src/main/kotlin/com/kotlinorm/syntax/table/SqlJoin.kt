/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.syntax.table

import com.kotlinorm.syntax.SqlNode
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.token.SqlUnsafeToken

sealed interface SqlJoinType : SqlNode {
    object Inner : SqlJoinType

    object Left : SqlJoinType

    object Right : SqlJoinType

    object Full : SqlJoinType

    object Cross : SqlJoinType

    data class UnsafeCustom(val tokens: List<SqlUnsafeToken>) : SqlJoinType
}

sealed interface SqlJoinCondition : SqlNode {
    data class On(val condition: SqlExpr) : SqlJoinCondition

    data class Using(val columnNames: List<String>) : SqlJoinCondition {
        init {
            require(columnNames.isNotEmpty()) { "USING requires at least one column." }
        }
    }
}

