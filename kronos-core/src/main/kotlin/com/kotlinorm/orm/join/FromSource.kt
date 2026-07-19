/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.orm.join

import com.kotlinorm.beans.dsl.KSelectable
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.table.SqlJoinType

internal sealed interface FromSourceNode {
    val leaves: List<FromSourceLeaf>

    data class Leaf(
        val source: FromSourceLeaf
    ) : FromSourceNode {
        override val leaves: List<FromSourceLeaf> = listOf(source)
    }

    data class Join(
        val left: FromSourceNode,
        val joinType: SqlJoinType,
        val right: FromSourceNode,
        val condition: JoinConditionSnapshot?
    ) : FromSourceNode {
        override val leaves: List<FromSourceLeaf> = left.leaves + right.leaves
    }
}

internal data class FromSourceLeaf(
    val pojo: KPojo,
    val query: KSelectable<*>? = null
)

internal data class SourceQualifierSnapshot(
    val source: KPojo,
    val qualifier: String
)

internal data class JoinConditionSnapshot(
    val expression: SqlExpr,
    val parameters: Map<String, Any?>,
    val qualifiers: List<SourceQualifierSnapshot>
)

internal fun FromSourceNode.rootLeaf(): FromSourceLeaf =
    when (this) {
        is FromSourceNode.Leaf -> source
        is FromSourceNode.Join -> left.rootLeaf()
    }
