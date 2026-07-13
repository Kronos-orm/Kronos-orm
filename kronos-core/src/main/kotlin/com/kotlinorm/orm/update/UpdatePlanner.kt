/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.orm.update

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
import com.kotlinorm.syntax.table.SqlTable
import com.kotlinorm.utils.databaseBooleanLiteral
import com.kotlinorm.utils.execute
import com.kotlinorm.utils.toDatabaseBooleanValue

internal class UpdatePlanner<T : KPojo>(
    private val context: OrmContext<T>
) {
    private val createTimeStrategy = context.createTimeStrategy
    private val updateTimeStrategy = context.updateTimeStrategy
    private val logicDeleteStrategy = context.logicDeleteStrategy
    private val optimisticStrategy = context.optimisticLockStrategy

    fun plan(dataSource: KronosDataSourceWrapper): SqlDmlStatement.Update {
        ensureDefaultAssignments()
        applyStrategies(dataSource)
        return SqlDmlStatement.Update(
            table = SqlTable.Ident(
                name = context.tableName,
                identifier = SqlIdentifier.of(context.tableName)
            ),
            setPairs = context.setPairs,
            where = context.where
        )
    }

    private fun ensureDefaultAssignments() {
        if (context.setPairs.isNotEmpty()) return
        context.allFields.filter { it.isColumn }.forEach { field ->
            val parameterName = "${field.name}New"
            context.bind(parameterName, context.sourceValues[field.name], field, ParameterSource.Assignment)
            context.set(field, SqlExpr.Parameter(SqlParameter.Named(parameterName)))
        }
    }

    private fun applyStrategies(dataSource: KronosDataSourceWrapper) {
        createTimeStrategy?.field?.let { field ->
            context.removeSetPair(field.columnName)
            context.parameterBindings.remove("${field.name}New")
        }

        updateTimeStrategy?.execute(true) { field, value ->
            val parameterName = "${field.name}New"
            context.bind(parameterName, value, field, ParameterSource.Strategy)
            context.set(field, SqlExpr.Parameter(SqlParameter.Named(parameterName)))
        }

        if (context.logicEnabled != false) {
            logicDeleteStrategy?.execute(defaultValue = false) { field, _ ->
                context.removeSetPair(field.columnName)
                context.parameterBindings.remove("${field.name}New")
                context.andWhere(
                    SqlExpr.Binary(
                        field.toSqlExpr(false),
                        SqlBinaryOperator.Equal,
                        databaseBooleanLiteral(dataSource, field, false)
                    )
                )
            }
        } else if (context.restoreLogicDeleteOnUpdate) {
            logicDeleteStrategy?.execute(defaultValue = false) { field, _ ->
                val parameterName = "${field.name}New"
                context.bind(parameterName, toDatabaseBooleanValue(dataSource, field, false), field, ParameterSource.Strategy)
                context.set(field, SqlExpr.Parameter(SqlParameter.Named(parameterName)))
            }
        }

        optimisticStrategy?.execute { field, _ ->
            val targetName = SqlIdentifier.of(field.columnName).canonical
            if (context.setPairs.any { (it.target as? SqlAssignmentTarget.Column)?.identifier?.canonical == targetName }) {
                throw IllegalArgumentException("The version field cannot be updated manually.")
            }
            val parameterName = "${field.name}2PlusNew"
            context.bind(parameterName, 1, field, ParameterSource.Strategy)
            context.set(
                field,
                SqlExpr.Binary(
                    field.toSqlExpr(false),
                    SqlBinaryOperator.Plus,
                    SqlExpr.Parameter(SqlParameter.Named(parameterName))
                )
            )
        }
    }
}
