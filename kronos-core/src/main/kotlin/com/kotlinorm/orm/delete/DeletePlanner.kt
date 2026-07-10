/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.orm.delete

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.cache.kPojoLogicDeleteCache
import com.kotlinorm.cache.kPojoOptimisticLockCache
import com.kotlinorm.cache.kPojoUpdateTimeCache
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.sql.toSqlExpr
import com.kotlinorm.orm.statement.OrmContext
import com.kotlinorm.orm.statement.ParameterSource
import com.kotlinorm.syntax.SqlIdentifier
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlParameter
import com.kotlinorm.syntax.statement.SqlAssignmentTarget
import com.kotlinorm.syntax.statement.SqlDmlStatement
import com.kotlinorm.syntax.statement.SqlUpdateSetPair
import com.kotlinorm.syntax.table.SqlTable
import com.kotlinorm.utils.databaseBooleanLiteral
import com.kotlinorm.utils.execute

internal class DeletePlanner<T : KPojo>(
    private val context: OrmContext<T>
) {
    private val logicDeleteStrategy by lazy(LazyThreadSafetyMode.NONE) { kPojoLogicDeleteCache[context.kClass] }
    private val updateTimeStrategy by lazy(LazyThreadSafetyMode.NONE) { kPojoUpdateTimeCache[context.kClass] }
    private val optimisticStrategy by lazy(LazyThreadSafetyMode.NONE) { kPojoOptimisticLockCache[context.kClass] }

    fun usesLogicDelete(): Boolean =
        context.logicEnabled ?: (logicDeleteStrategy?.enabled ?: false)

    fun plan(dataSource: KronosDataSourceWrapper): SqlDmlStatement =
        if (usesLogicDelete()) {
            buildLogicDeleteUpdate(dataSource)
        } else {
            SqlDmlStatement.Delete(
                table = table(),
                where = context.where
            )
        }

    private fun table(): SqlTable.Ident =
        SqlTable.Ident(
            name = context.tableName,
            identifier = SqlIdentifier.of(context.tableName)
        )

    private fun buildLogicDeleteUpdate(dataSource: KronosDataSourceWrapper): SqlDmlStatement.Update {
        val updateSetPairs = mutableListOf<SqlUpdateSetPair>()

        fun addSetPair(field: Field, value: SqlExpr) {
            updateSetPairs += SqlUpdateSetPair(
                SqlAssignmentTarget.Column(SqlIdentifier.of(field.columnName)),
                value
            )
        }

        fun bindStrategyField(field: Field, value: Any?) {
            val parameterName = "${field.name}New"
            context.bind(parameterName, value, field, ParameterSource.Strategy)
            addSetPair(field, SqlExpr.Parameter(SqlParameter.Named(parameterName)))
        }

        updateTimeStrategy?.execute(true) { field, value -> bindStrategyField(field, value) }
        logicDeleteStrategy?.execute(defaultValue = true) { field, _ ->
            bindStrategyField(field, true)
        }

        optimisticStrategy?.execute { field, _ ->
            val targetName = SqlIdentifier.of(field.columnName).canonical
            if (updateSetPairs.any { (it.target as? SqlAssignmentTarget.Column)?.identifier?.canonical == targetName }) {
                throw IllegalArgumentException("The version field cannot be updated manually.")
            }
            val parameterName = "${field.name}2PlusNew"
            context.bind(parameterName, 1, field, ParameterSource.Strategy)
            addSetPair(
                field,
                SqlExpr.Binary(
                    field.toSqlExpr(false),
                    SqlBinaryOperator.Plus,
                    SqlExpr.Parameter(SqlParameter.Named(parameterName))
                )
            )
        }

        var where = context.where
        logicDeleteStrategy?.execute(defaultValue = false) { field, _ ->
            val logicDeleteExpression = SqlExpr.Binary(
                field.toSqlExpr(false),
                SqlBinaryOperator.Equal,
                databaseBooleanLiteral(dataSource, field, false)
            )
            where = where?.let { SqlExpr.Binary(it, SqlBinaryOperator.And, logicDeleteExpression) }
                ?: logicDeleteExpression
        }

        return SqlDmlStatement.Update(
            table = table(),
            setPairs = updateSetPairs,
            where = where
        )
    }
}
