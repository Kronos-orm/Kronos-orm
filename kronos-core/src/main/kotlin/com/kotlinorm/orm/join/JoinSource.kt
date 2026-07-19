/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.orm.join

import com.kotlinorm.beans.dsl.KTableForCondition.Companion.afterFilter
import com.kotlinorm.beans.dsl.KTableForCondition
import com.kotlinorm.beans.dsl.SourceIdentityScope
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.exceptions.EmptyFieldsException
import com.kotlinorm.functions.KronosFunctionExpressions.withQualifiedFieldArgs
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.syntax.table.SqlJoinType
import com.kotlinorm.types.ToFilter
import com.kotlinorm.types.ToSelect
import kotlin.reflect.KType

sealed interface JoinResult

abstract class JoinSource<T1 : KPojo, Self : JoinSource<T1, Self>> @PublishedApi internal constructor(
    @PublishedApi internal val joinState: JoinSourceState<T1>
) : JoinResult {
    @PublishedApi
    internal abstract fun recreate(state: JoinSourceState<T1>): Self

    fun leftJoin(on: ToFilter<T1, Boolean?>): Self =
        recreate(joinState.append(SqlJoinType.Left, on))

    fun rightJoin(on: ToFilter<T1, Boolean?>): Self =
        recreate(joinState.append(SqlJoinType.Right, on))

    fun innerJoin(on: ToFilter<T1, Boolean?>): Self =
        recreate(joinState.append(SqlJoinType.Inner, on))

    fun fullJoin(on: ToFilter<T1, Boolean?>): Self =
        recreate(joinState.append(SqlJoinType.Full, on))

    fun crossJoin(): Self =
        recreate(joinState.append(SqlJoinType.Cross, null))

    fun select(fields: ToSelect<T1, Any?> = null): JoinedSelectQuery<T1, T1, T1> =
        createJoinedSelectQuery(
            projectionType = joinState.sourceType,
            nullableProjectionType = joinState.nullableSourceType,
            contextPojo = joinState.root,
            fields = fields
        )

    @PublishedApi
    internal fun <Selected : KPojo, Context : KPojo> createJoinedSelectQuery(
        projectionType: KType,
        nullableProjectionType: KType,
        contextPojo: Context,
        fields: ToSelect<T1, Any?>
    ): JoinedSelectQuery<T1, Selected, Context> =
        JoinedSelectQuery(
            state = joinState.requireComplete(),
            projectionType = projectionType,
            nullableProjectionType = nullableProjectionType,
            contextPojo = contextPojo,
            fields = fields
        )
}

@PublishedApi
internal class JoinSourceState<T1 : KPojo>(
    val root: T1,
    val sourceType: KType,
    val nullableSourceType: KType,
    val operands: List<FromSourceNode>,
    val current: FromSourceNode,
    val nextOperandIndex: Int
) {
    val leaves: List<FromSourceLeaf> = operands.flatMap(FromSourceNode::leaves)
    val sources: List<KPojo> = leaves.map(FromSourceLeaf::pojo)
    private val sourceIdentityFrame = SourceIdentityScope.frame(sources)

    fun append(joinType: SqlJoinType, on: ToFilter<T1, Boolean?>): JoinSourceState<T1> {
        check(nextOperandIndex < operands.size) {
            "JOIN has no remaining source for ${joinType.displayName()}Join()."
        }
        val condition = if (joinType == SqlJoinType.Cross) {
            null
        } else {
            captureCondition(on ?: throw EmptyFieldsException())
        }
        return JoinSourceState(
            root = root,
            sourceType = sourceType,
            nullableSourceType = nullableSourceType,
            operands = operands,
            current = FromSourceNode.Join(
                left = current,
                joinType = joinType,
                right = operands[nextOperandIndex],
                condition = condition
            ),
            nextOperandIndex = nextOperandIndex + 1
        )
    }

    fun requireComplete(): JoinSourceState<T1> {
        check(nextOperandIndex == operands.size) {
            "JOIN requires ${operands.size - 1} relation calls before select; " +
                "${operands.size - nextOperandIndex} source(s) remain."
        }
        return this
    }

    fun <T> withSourceScope(block: () -> T): T =
        SourceIdentityScope.withFrame(sourceIdentityFrame, block)

    fun qualifierFor(source: KPojo): String =
        sourceIdentityFrame.aliasForSource(source) ?: source.__tableName

    private fun captureCondition(
        on: KTableForCondition<T1>.(T1) -> Boolean?
    ): JoinConditionSnapshot = withSourceScope {
        lateinit var snapshot: JoinConditionSnapshot
        root.afterFilter {
            sourceValues = linkedMapOf<String, Any?>().also { values ->
                sources.forEach { values.putAll(it.toDataMap()) }
            }
            operationType = KOperationType.SELECT
            withQualifiedFieldArgs { on.invoke(this@afterFilter, root) }
            snapshot = JoinConditionSnapshot(
                expression = sqlExpr ?: throw EmptyFieldsException(),
                parameters = parameterValues.toMap(),
                qualifiers = sources.map { source ->
                    SourceQualifierSnapshot(source, qualifierFor(source))
                }
            )
        }
        snapshot
    }

    private fun SqlJoinType.displayName(): String = when (this) {
        SqlJoinType.Inner -> "inner"
        SqlJoinType.Left -> "left"
        SqlJoinType.Right -> "right"
        SqlJoinType.Full -> "full"
        SqlJoinType.Cross -> "cross"
        is SqlJoinType.UnsafeCustom -> "custom"
    }
}
