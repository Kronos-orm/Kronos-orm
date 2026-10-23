/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.orm.statement

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KSelectable
import com.kotlinorm.beans.dsl.KronosFunctionExpr
import com.kotlinorm.beans.dsl.SourceBinding
import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.beans.task.jdbcNullType
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.sql.toSqlExpr
import com.kotlinorm.syntax.SqlIdentifier
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlParameter
import com.kotlinorm.syntax.statement.SqlAssignmentTarget
import com.kotlinorm.syntax.statement.SqlUpdateSetPair
import com.kotlinorm.utils.fieldForParameter
import kotlin.reflect.KType

internal class OrmContext<T : KPojo>(
    val pojo: T,
    val kType: KType,
    val tableName: String,
    val declaredTableName: String = tableName,
    val operationType: KOperationType,
    val fields: List<Field>,
    val allFields: List<Field> = fields,
    val fieldMap: Map<String, Field>,
    val createTimeStrategy: KronosCommonStrategy? = null,
    val updateTimeStrategy: KronosCommonStrategy? = null,
    val logicDeleteStrategy: KronosCommonStrategy? = null,
    val optimisticLockStrategy: KronosCommonStrategy? = null,
    val sourceValues: Map<String, Any?> = pojo.toDataMap(),
    val parameterBindings: MutableMap<String, ParameterBinding> = linkedMapOf()
) {
    val sourceBinding = SourceBinding(
        tableName = tableName,
        dynamicTableNames = setOf(declaredTableName).filterTo(linkedSetOf()) {
            it.isNotBlank() && it != tableName
        },
        sourceColumnNames = fields.asSequence().filter { it.isColumn }.map { it.columnName }.toSet()
    )
    var cascadeEnabled: Boolean = true
    var cascadeAllowed: Set<Field>? = null
    var logicEnabled: Boolean? = null
    var restoreLogicDeleteOnUpdate: Boolean = false
    var where: SqlExpr? = null
    val setPairs: MutableList<SqlUpdateSetPair> = mutableListOf()

    fun bind(name: String, value: Any?, field: Field?, source: ParameterSource) {
        parameterBindings[name] = ParameterBinding(name, value, field, source)
    }

    fun parameterValues(vararg sources: ParameterSource): Map<String, Any?> {
        val allowed = sources.takeIf { it.isNotEmpty() }?.toSet()
        return parameterBindings.values
            .asSequence()
            .filter { allowed == null || it.source in allowed }
            .associate { it.name to it.value }
    }

    fun parameterFields(): Map<String, Field> = buildMap {
        putAll(fieldMap)
        parameterBindings.values.forEach { binding ->
            binding.field?.let { put(binding.name, it) }
        }
    }

    fun valueExpression(value: Any?, parameterName: String): SqlExpr =
        when (value) {
            is SqlExpr -> value
            is KronosFunctionExpr -> value.expr
            is Field -> value.toSqlExpr(false)
            is KSelectable<*> -> SqlExpr.Subquery(value.toSqlQuery())
            else -> SqlExpr.Parameter(SqlParameter.Named(parameterName))
        }

    fun bindValueIfNeeded(parameterName: String, value: Any?, field: Field, source: ParameterSource) {
        if (value !is SqlExpr && value !is KronosFunctionExpr && value !is Field && value !is KSelectable<*>) {
            bind(parameterName, value, field, source)
        }
    }

    fun addOrReplaceSetPair(pair: SqlUpdateSetPair) {
        val target = pair.target
        if (target is SqlAssignmentTarget.Column) {
            setPairs.removeAll { existing ->
                (existing.target as? SqlAssignmentTarget.Column)
                    ?.identifier
                    ?.canonical == target.identifier.canonical
            }
        }
        setPairs += pair
    }

    fun andWhere(expr: SqlExpr?) {
        if (expr == null) return
        where = where?.let { SqlExpr.Binary(it, SqlBinaryOperator.And, expr) } ?: expr
    }

    fun andWhereAll(expressions: Iterable<SqlExpr>) {
        val items = expressions.toList()
        if (items.isEmpty()) return
        andWhere(items.drop(1).fold(items.first()) { left, right ->
            SqlExpr.Binary(left, SqlBinaryOperator.And, right)
        })
    }

    fun set(field: Field, value: SqlExpr) {
        addOrReplaceSetPair(
            SqlUpdateSetPair(
                target = SqlAssignmentTarget.Column(SqlIdentifier.of(field.columnName)),
                value = value
            )
        )
    }

    fun removeSetPair(columnName: String) {
        val canonical = SqlIdentifier.of(columnName).canonical
        setPairs.removeAll { existing ->
            (existing.target as? SqlAssignmentTarget.Column)
                ?.identifier
                ?.canonical == canonical
        }
    }

    fun jdbcNullParameterTypeHints(parameterNames: Set<String>): Map<String, Int> =
        parameterBindings.values.mapNotNull { binding ->
            if (binding.name !in parameterNames || binding.value != null) {
                return@mapNotNull null
            }
            val field = binding.field ?: fieldMap.fieldForParameter(binding.name)
            field?.jdbcNullType()?.let { binding.name to it }
        }.toMap()
}

internal data class ParameterBinding(
    val name: String,
    val value: Any?,
    val field: Field?,
    val source: ParameterSource
)

internal enum class ParameterSource {
    Condition,
    Assignment,
    Strategy,
    Patch,
    Renderer
}
