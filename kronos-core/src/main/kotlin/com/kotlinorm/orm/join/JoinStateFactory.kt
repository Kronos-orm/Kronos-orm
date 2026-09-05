/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.orm.join

import com.kotlinorm.beans.dsl.KSelectable
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.types.ToSelect
import com.kotlinorm.utils.createKPojo
import kotlin.reflect.KType
import kotlin.reflect.full.withNullability
import kotlin.reflect.typeOf

@PublishedApi
internal inline fun <T1 : KPojo, reified R : KPojo, reified C : KPojo> JoinSource<T1, *>.selectGeneratedProjection(
    noinline fields: ToSelect<T1, Any?> = null
): JoinedSelectQuery<T1, R, C> {
    val projectionType = typeOf<R>()
    return createJoinedSelectQuery(
        projectionType = projectionType,
        nullableProjectionType = projectionType.withNullability(true),
        contextPojo = createKPojo<C>(),
        fields = fields
    )
}

@PublishedApi
internal inline fun <reified T1 : KPojo> tableJoinState(
    root: T1,
    vararg right: KPojo
): JoinSourceState<T1> = tableJoinState(root, typeOf<T1>(), typeOf<T1?>(), right.asList())

@PublishedApi
internal inline fun <reified T1 : KPojo, reified T2 : KPojo> selectableJoinState(
    left: T1,
    right: KSelectable<T2>
): Pair<JoinSourceState<T1>, T2> =
    tableSelectableJoinState(left, typeOf<T1>(), typeOf<T1?>(), right)

@PublishedApi
internal inline fun <reified T1 : KPojo, reified T2 : KPojo> selectableLeftJoinState(
    left: KSelectable<T1>,
    right: T2
): Triple<JoinSourceState<T1>, T1, T2> = selectableTableJoinState(left, right)

@PublishedApi
internal inline fun <reified T1 : KPojo> prependJoinState(
    root: T1,
    nested: JoinSourceState<*>
): JoinSourceState<T1> = tableRawJoinState(root, typeOf<T1>(), typeOf<T1?>(), nested)

@PublishedApi
@Suppress("UNCHECKED_CAST")
internal inline fun <reified T : KPojo> JoinSourceState<*>.sourceAt(index: Int): T =
    sources[index] as T

@PublishedApi
internal fun <T1 : KPojo> appendJoinState(
    nested: JoinSourceState<T1>,
    right: KPojo
): JoinSourceState<T1> = rawTableJoinState(nested, right)

private data class JoinOperand(
    val node: FromSourceNode,
    val rows: List<KPojo>
)

private fun tableOperand(source: KPojo): JoinOperand =
    JoinOperand(FromSourceNode.Leaf(FromSourceLeaf(source)), listOf(source))

@Suppress("UNCHECKED_CAST")
private fun <T : KPojo> selectableOperand(query: KSelectable<T>): JoinOperand {
    val row = createKPojo(query.selectedType) as T
    return JoinOperand(FromSourceNode.Leaf(FromSourceLeaf(row, query)), listOf(row))
}

private fun rawOperand(state: JoinSourceState<*>): JoinOperand {
    val complete = state.requireComplete()
    return JoinOperand(complete.current, complete.sources)
}

private fun <T1 : KPojo> joinState(
    root: T1,
    sourceType: KType,
    nullableSourceType: KType,
    operands: List<JoinOperand>
): JoinSourceState<T1> {
    require(operands.size >= 2) { "JOIN requires at least two operands." }
    val nodes = operands.map(JoinOperand::node)
    return JoinSourceState(
        root = root,
        sourceType = sourceType,
        nullableSourceType = nullableSourceType,
        operands = nodes,
        current = nodes.first(),
        nextOperandIndex = 1
    )
}

@PublishedApi
internal fun <T1 : KPojo> tableJoinState(
    root: T1,
    sourceType: KType,
    nullableSourceType: KType,
    right: List<KPojo>
): JoinSourceState<T1> = joinState(
    root,
    sourceType,
    nullableSourceType,
    listOf(tableOperand(root)) + right.map(::tableOperand)
)

@PublishedApi
@Suppress("UNCHECKED_CAST")
internal fun <T1 : KPojo, T2 : KPojo> tableSelectableJoinState(
    left: T1,
    sourceType: KType,
    nullableSourceType: KType,
    right: KSelectable<T2>
): Pair<JoinSourceState<T1>, T2> {
    val rightOperand = selectableOperand(right)
    return joinState(left, sourceType, nullableSourceType, listOf(tableOperand(left), rightOperand)) to
        rightOperand.rows.single() as T2
}

@PublishedApi
@Suppress("UNCHECKED_CAST")
internal fun <T1 : KPojo, T2 : KPojo> selectableTableJoinState(
    left: KSelectable<T1>,
    right: T2
): Triple<JoinSourceState<T1>, T1, T2> {
    val leftOperand = selectableOperand(left)
    val root = leftOperand.rows.single() as T1
    return Triple(
        joinState(root, left.selectedType, left.nullableSelectedType, listOf(leftOperand, tableOperand(right))),
        root,
        right
    )
}

@PublishedApi
@Suppress("UNCHECKED_CAST")
internal fun <T1 : KPojo, T2 : KPojo> selectableSelectableJoinState(
    left: KSelectable<T1>,
    right: KSelectable<T2>
): JoinSourceState<T1> {
    val leftOperand = selectableOperand(left)
    val root = leftOperand.rows.single() as T1
    return joinState(root, left.selectedType, left.nullableSelectedType, listOf(leftOperand, selectableOperand(right)))
}

@PublishedApi
internal fun <T1 : KPojo> tableRawJoinState(
    root: T1,
    sourceType: KType,
    nullableSourceType: KType,
    right: JoinSourceState<*>
): JoinSourceState<T1> =
    joinState(root, sourceType, nullableSourceType, listOf(tableOperand(root), rawOperand(right)))

@PublishedApi
@Suppress("UNCHECKED_CAST")
internal fun <T1 : KPojo> selectableRawJoinState(
    left: KSelectable<T1>,
    right: JoinSourceState<*>
): JoinSourceState<T1> {
    val leftOperand = selectableOperand(left)
    val root = leftOperand.rows.single() as T1
    return joinState(root, left.selectedType, left.nullableSelectedType, listOf(leftOperand, rawOperand(right)))
}

@PublishedApi
internal fun <T1 : KPojo> rawTableJoinState(
    left: JoinSourceState<T1>,
    right: KPojo
): JoinSourceState<T1> = joinState(
    left.root,
    left.sourceType,
    left.nullableSourceType,
    listOf(rawOperand(left), tableOperand(right))
)

@PublishedApi
internal fun <T1 : KPojo, T2 : KPojo> rawSelectableJoinState(
    left: JoinSourceState<T1>,
    right: KSelectable<T2>
): JoinSourceState<T1> = joinState(
    left.root,
    left.sourceType,
    left.nullableSourceType,
    listOf(rawOperand(left), selectableOperand(right))
)

@PublishedApi
internal fun <T1 : KPojo> rawRawJoinState(
    left: JoinSourceState<T1>,
    right: JoinSourceState<*>
): JoinSourceState<T1> = joinState(
    left.root,
    left.sourceType,
    left.nullableSourceType,
    listOf(rawOperand(left), rawOperand(right))
)
