/**
 * Copyright 2022-2025 kronos-orm
 */
package com.kotlinorm.ast

/**
 * Lightweight builder helpers to construct nested SQL AST easily.
 * The goal is to make composing subqueries and complex statements straightforward
 * without committing to a full end-user DSL here (that remains in kronos-core DSL).
 */

// ---- Factory helpers ----

fun table(
    table: String,
    alias: String? = null,
    database: String? = null,
    schema: String? = null
): TableName = TableName(database = database, schema = schema, table = table, alias = alias)

fun subquery(query: SelectStatement, alias: String): SubquerySource = SubquerySource(query, alias)

fun join(
    left: TableSource,
    right: TableSource,
    type: JoinType = JoinType.inner,
    on: Expression? = null
): Join = Join(left, right, type, on)

fun col(column: String, tableAlias: String? = null): ColumnRef = ColumnRef(tableAlias = tableAlias, column = column)

fun <T : Any?> lit(value: T): Literal<T> = Literal(value)

fun param(name: String): NamedParam = NamedParam(name)

// ---- Expression ops (infix) ----

infix fun Expression.eq(other: Expression): Expression = BinaryOp(this, "=", other)

infix fun Expression.ne(other: Expression): Expression = BinaryOp(this, "<>", other)

infix fun Expression.gt(other: Expression): Expression = BinaryOp(this, ">", other)

infix fun Expression.gte(other: Expression): Expression = BinaryOp(this, ">=", other)

infix fun Expression.lt(other: Expression): Expression = BinaryOp(this, "<", other)

infix fun Expression.lte(other: Expression): Expression = BinaryOp(this, "<=", other)

infix fun Expression.and(other: Expression): Expression = BinaryOp(this, "AND", other)

infix fun Expression.or(other: Expression): Expression = BinaryOp(this, "OR", other)

operator fun Expression.plus(other: Expression): Expression = BinaryOp(this, "+", other)

operator fun Expression.minus(other: Expression): Expression = BinaryOp(this, "-", other)

operator fun Expression.times(other: Expression): Expression = BinaryOp(this, "*", other)

operator fun Expression.div(other: Expression): Expression = BinaryOp(this, "/", other)

infix fun Expression.like(other: Expression): Expression = BinaryOp(this, "LIKE", other)

fun not(expr: Expression): Expression = UnaryOp("NOT", expr)

fun between(expr: Expression, from: Expression, to: Expression, not: Boolean = false): Between =
    Between(expr, from, to, not)

fun inValues(expr: Expression, vararg values: Expression, not: Boolean = false): InOp =
    InOp(expr, InValues(values.toList()), not)

fun inSubquery(expr: Expression, sub: SelectStatement, not: Boolean = false): InOp =
    InOp(expr, InSubquery(sub), not)

fun exists(sub: SelectStatement, not: Boolean = false): Exists = Exists(sub, not)

fun func(name: String, vararg args: Expression, distinct: Boolean = false): FuncCall =
    FuncCall(name, args.toList(), distinct)

fun cast(expr: Expression, typeName: String): Cast = Cast(expr, typeName)

fun raw(sql: String): RawSqlExpr = RawSqlExpr(sql)

// ---- Select builder ----

class SelectBuilder internal constructor() {
    private val _with: MutableList<CommonTableExpression> = mutableListOf()
    private val _projections: MutableList<ProjectionItem> = mutableListOf()
    var from: TableSource? = null
    var where: Expression? = null
    private val _groupBy: MutableList<Expression> = mutableListOf()
    var having: Expression? = null
    private val _orderBy: MutableList<OrderItem> = mutableListOf()
    var distinct: Boolean = false
    var limit: Int? = null
    var offset: Int? = null

    fun with(name: String, query: SelectStatement, columns: List<String> = emptyList()) {
        _with += CommonTableExpression(name, query, columns)
    }

    fun project(expr: Expression, alias: String? = null) {
        _projections += ProjectionItem(expr, alias)
    }

    fun projectAll(vararg cols: ColumnRef) {
        cols.forEach { project(it) }
    }

    fun groupBy(vararg expr: Expression) {
        _groupBy += expr
    }

    fun orderBy(expr: Expression, direction: OrderDirection = OrderDirection.asc, nulls: NullsOrder = NullsOrder.defaultOrder) {
        _orderBy += OrderItem(expr, direction, nulls)
    }

    fun build(): SelectStatement = SelectStatement(
        with = _with.toMutableList(),
        projections = _projections.toMutableList(),
        from = from,
        where = where,
        groupBy = _groupBy.toMutableList(),
        having = having,
        orderBy = _orderBy.toMutableList(),
        distinct = distinct,
        limit = limit,
        offset = offset
    )
}

fun select(block: SelectBuilder.() -> Unit): SelectStatement = SelectBuilder().apply(block).build()

// ---- Convenience helpers to create ProjectionItem quickly ----

fun as_(expr: Expression, alias: String): ProjectionItem = ProjectionItem(expr, alias)
