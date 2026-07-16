/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.beans.dsl

import com.kotlinorm.syntax.SqlIdentifier
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlInRightOperand
import com.kotlinorm.syntax.expr.SqlWindow
import com.kotlinorm.syntax.statement.SqlSelectItem

/**
 * Runtime source rules for turning DSL [Field] references into SQL column expressions.
 */
data class SourceBinding(
    val tableName: String? = null,
    val alias: String? = null,
    val dynamicTableNames: Set<String> = emptySet(),
    val sourceColumnNames: Set<String> = emptySet(),
    val derived: Boolean = false,
    val derivedOutputNamesByColumn: Map<String, String> = emptyMap()
) {
    fun bindField(field: Field): Field =
        if (field.tableName in dynamicTableNames && field.columnName in sourceColumnNames) {
            field.copy(tableName = tableName ?: field.tableName)
        } else {
            field
        }

    fun columnName(field: Field): String =
        if (derived && alias != null) {
            derivedOutputNamesByColumn[field.columnName] ?: field.name.ifBlank { field.columnName }
        } else {
            field.columnName
        }

    fun column(field: Field, fallbackTableName: String? = field.tableName): SqlExpr.Column {
        val boundField = bindField(field)
        val sourceName = alias ?: boundField.tableName.ifBlank { tableName ?: fallbackTableName.orEmpty() }
        return SqlExpr.Column(
            tableName = sourceName.takeIf { it.isNotBlank() },
            columnName = columnName(boundField)
        )
    }

    fun projectionColumn(field: Field): SqlExpr.Column {
        val boundField = bindField(field)
        return if (derived && alias != null) {
            SqlExpr.Column(tableName = alias, columnName = columnName(boundField))
        } else {
            SqlExpr.Column(columnName = boundField.columnName)
        }
    }

    internal fun bindProjectionItem(item: KTableForSelect.ProjectionItem): KTableForSelect.ProjectionItem =
        when (item) {
            is KTableForSelect.ProjectionItem.FieldItem -> item.copy(
                field = bindField(item.field),
                expr = item.expr?.let(::bindColumn)
            )
            is KTableForSelect.ProjectionItem.SelectItemValue -> item.copy(item = bindSelectItem(item.item))
            is KTableForSelect.ProjectionItem.ScalarSubqueryValue -> item
        }

    fun bindSelectItem(item: SqlSelectItem): SqlSelectItem =
        when (item) {
            is SqlSelectItem.Asterisk -> item
            is SqlSelectItem.Expr -> item.copy(
                expr = bindExpr(item.expr),
                metadata = item.metadata?.let { it.copy(expression = bindExpr(it.expression)) }
            )
        }

    fun bindExpr(expr: SqlExpr): SqlExpr =
        when (expr) {
            is SqlExpr.Column -> bindColumn(expr)
            is SqlExpr.Unary -> expr.copy(expr = bindExpr(expr.expr))
            is SqlExpr.Binary -> expr.copy(left = bindExpr(expr.left), right = bindExpr(expr.right))
            is SqlExpr.Tuple -> expr.copy(items = expr.items.map(::bindExpr))
            is SqlExpr.Array -> expr.copy(items = expr.items.map(::bindExpr))
            is SqlExpr.In -> expr.copy(
                expr = bindExpr(expr.expr),
                `in` = when (val operand = expr.`in`) {
                    is SqlInRightOperand.Values -> operand.copy(items = operand.items.map(::bindExpr))
                    is SqlInRightOperand.Subquery -> operand
                }
            )
            is SqlExpr.Between -> expr.copy(
                expr = bindExpr(expr.expr),
                start = bindExpr(expr.start),
                end = bindExpr(expr.end)
            )
            is SqlExpr.Like -> expr.copy(
                expr = bindExpr(expr.expr),
                pattern = bindExpr(expr.pattern),
                escape = expr.escape?.let(::bindExpr)
            )
            is SqlExpr.Function -> expr.copy(
                args = expr.args.map(::bindExpr),
                orderBy = expr.orderBy.map { it.copy(expr = bindExpr(it.expr)) },
                withinGroup = expr.withinGroup.map { it.copy(expr = bindExpr(it.expr)) },
                filter = expr.filter?.let(::bindExpr)
            )
            is SqlExpr.Window -> expr.copy(expr = bindExpr(expr.expr), window = bindWindow(expr.window))
            is SqlExpr.Case -> expr.copy(
                branches = expr.branches.map { it.copy(`when` = bindExpr(it.`when`), then = bindExpr(it.then)) },
                default = expr.default?.let(::bindExpr)
            )
            is SqlExpr.SimpleCase -> expr.copy(
                expr = bindExpr(expr.expr),
                branches = expr.branches.map { it.copy(`when` = bindExpr(it.`when`), then = bindExpr(it.then)) },
                default = expr.default?.let(::bindExpr)
            )
            is SqlExpr.Coalesce -> expr.copy(items = expr.items.map(::bindExpr))
            is SqlExpr.NullIf -> expr.copy(expr = bindExpr(expr.expr), test = bindExpr(expr.test))
            is SqlExpr.Cast -> expr.copy(expr = bindExpr(expr.expr))
            is SqlExpr.QuantifiedComparisonPredicate -> expr.copy(expr = bindExpr(expr.expr))
            else -> expr
        }

    fun bindWindow(window: SqlWindow): SqlWindow =
        window.copy(
            partitionBy = window.partitionBy.map(::bindExpr),
            orderBy = window.orderBy.map { it.copy(expr = bindExpr(it.expr)) }
        )

    private fun bindColumn(column: SqlExpr.Column): SqlExpr.Column {
        if (alias != null && tableName != null && column.tableName == tableName) {
            return column.copy(tableName = alias, qualifier = SqlIdentifier.of(alias))
        }
        if (column.tableName in dynamicTableNames && column.columnName in sourceColumnNames) {
            val boundTableName = tableName ?: return column
            return column.copy(tableName = boundTableName, qualifier = SqlIdentifier.of(boundTableName))
        }
        if (derived && alias != null && column.tableName == alias) {
            val outputName = derivedOutputNamesByColumn[column.columnName]
            if (outputName != null && outputName != column.columnName) {
                return column.copy(columnName = outputName, identifier = SqlIdentifier.of(outputName))
            }
        }
        return column
    }
}

fun Field.toSourceColumn(sourceBinding: SourceBinding?, tableName: String? = this.tableName): SqlExpr.Column =
    sourceBinding?.column(this, tableName)
        ?: SqlExpr.Column(tableName = tableName?.takeIf { it.isNotBlank() }, columnName = columnName)
