/**
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kotlinorm.orm.update

import com.kotlinorm.beans.dsl.KTableForCondition.Companion.afterFilter
import com.kotlinorm.beans.dsl.KTableForReference.Companion.afterReference
import com.kotlinorm.beans.dsl.KTableForSelect.Companion.afterSelect
import com.kotlinorm.beans.dsl.KTableForSet.Companion.afterSet
import com.kotlinorm.beans.dsl.KSelectable
import com.kotlinorm.beans.task.JdbcParameterTypeHints
import com.kotlinorm.beans.task.KronosActionTask
import com.kotlinorm.beans.task.KronosAtomicActionTask
import com.kotlinorm.beans.task.KronosOperationResult
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.exceptions.EmptyFieldsException
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.cascade.CascadeUpdateClause
import com.kotlinorm.orm.sql.materializeSqlQuery
import com.kotlinorm.orm.sql.toSqlExpr
import com.kotlinorm.orm.sql.toSqlParameterEq
import com.kotlinorm.orm.statement.OrmContext
import com.kotlinorm.orm.statement.OrmDmlRenderer
import com.kotlinorm.orm.statement.ParameterSource
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlParameter
import com.kotlinorm.syntax.statement.SqlAssignmentTarget
import com.kotlinorm.types.ToFilter
import com.kotlinorm.types.ToReference
import com.kotlinorm.types.ToSelect
import com.kotlinorm.types.ToSet
import com.kotlinorm.utils.DataSourceUtil.orDefault
import com.kotlinorm.utils.execute
import com.kotlinorm.utils.resolveRuntimeMetadata
import com.kotlinorm.utils.toLinkedSet
import kotlin.reflect.KType

class UpdateClause<T : KPojo>(
    pojo: T,
    private val targetType: KType,
    setUpdateFields: ToSelect<T, Any?> = null
) {
    private val metadata = pojo.resolveRuntimeMetadata()
    internal val context = OrmContext(
        pojo = pojo,
        kClass = metadata.kClass,
        tableName = metadata.tableName,
        declaredTableName = metadata.allColumns.firstOrNull { it.tableName.isNotBlank() }?.tableName
            ?: metadata.tableName,
        operationType = KOperationType.UPDATE,
        fields = metadata.allColumns,
        allFields = metadata.allFields.toList(),
        fieldMap = metadata.fieldMap,
        createTimeStrategy = metadata.createTimeStrategy,
        updateTimeStrategy = metadata.updateTimeStrategy,
        logicDeleteStrategy = metadata.logicDeleteStrategy,
        optimisticLockStrategy = metadata.optimisticLockStrategy
    )
    private val planner = UpdatePlanner(context)
    private val assignmentParameterCounter = mutableMapOf<String, Int>()

    init {
        if (setUpdateFields != null) {
            context.pojo.afterSelect { selectTable ->
                setUpdateFields(selectTable)
                fields.forEach { field ->
                    val parameterName = "${field.name}New"
                    context.bind(parameterName, context.sourceValues[field.name], field, ParameterSource.Assignment)
                    context.set(field, SqlExpr.Parameter(SqlParameter.Named(parameterName)))
                }
            }
        }
    }

    fun set(newValue: ToSet<T, Unit>): UpdateClause<T> {
        newValue ?: throw EmptyFieldsException()
        context.pojo.afterSet {
            newValue(it)
            val plusAssignMap = plusAssignFields.toMap()
            val minusAssignMap = minusAssignFields.toMap()
            fields.toList().forEach { field ->
                when {
                    plusAssignMap.containsKey(field) -> {
                        val parameterName = "${field.name}2PlusNew"
                        context.bind(parameterName, plusAssignMap[field], field, ParameterSource.Assignment)
                        context.set(
                            field,
                            SqlExpr.Binary(
                                field.toSqlExpr(false),
                                SqlBinaryOperator.Plus,
                                SqlExpr.Parameter(SqlParameter.Named(parameterName))
                            )
                        )
                    }

                    minusAssignMap.containsKey(field) -> {
                        val parameterName = "${field.name}2MinusNew"
                        context.bind(parameterName, minusAssignMap[field], field, ParameterSource.Assignment)
                        context.set(
                            field,
                            SqlExpr.Binary(
                                field.toSqlExpr(false),
                                SqlBinaryOperator.Minus,
                                SqlExpr.Parameter(SqlParameter.Named(parameterName))
                            )
                        )
                    }

                    else -> {
                        val parameterName = "${field.name}New"
                        val value = fieldParamMap[field]
                        context.set(field, assignmentValueExpression(value, parameterName, ParameterSource.Assignment))
                        context.bindValueIfNeeded(parameterName, value, field, ParameterSource.Assignment)
                    }
                }
            }
        }
        return this
    }

    fun cascade(enabled: Boolean): UpdateClause<T> {
        context.cascadeEnabled = enabled
        return this
    }

    fun cascade(someFields: ToReference<T, Any?>): UpdateClause<T> {
        someFields ?: throw EmptyFieldsException()
        context.cascadeEnabled = true
        context.pojo.afterReference {
            someFields(it)
            if (fields.isEmpty()) {
                throw EmptyFieldsException()
            }
            context.cascadeAllowed = fields.toSet()
        }
        return this
    }

    fun by(someFields: ToSelect<T, Any?>): UpdateClause<T> {
        someFields ?: throw EmptyFieldsException()
        context.pojo.afterSelect {
            someFields(it)
            if (fields.isEmpty()) {
                throw EmptyFieldsException()
            }
            context.andWhereAll(fields.map { field ->
                context.bind(field.name, context.sourceValues[field.name], field, ParameterSource.Condition)
                field.toSqlParameterEq(field.name)
            })
        }
        return this
    }

    fun where(updateCondition: ToFilter<T, Boolean?>? = null): UpdateClause<T> {
        if (updateCondition == null) return this
        context.pojo.afterFilter(context.sourceBinding) filter@ { filterTable ->
            with(context) {
                this@filter.sourceValues = sourceValues.toMutableMap()
                this@filter.operationType = operationType
                updateCondition(filterTable)
                this@filter.sqlExpr?.let { expr ->
                    context.andWhere(expr)
                    this@filter.parameterValues.forEach { (name, value) ->
                        bind(name, value, null, ParameterSource.Condition)
                    }
                }
            }
        }
        return this
    }

    fun patch(vararg pairs: Pair<String, Any?>): UpdateClause<T> {
        pairs.forEach { (fieldName, value) ->
            val field = context.fields.firstOrNull { it.name == fieldName || it.columnName == fieldName } ?: return@forEach
            val parameterName = "${field.name}New"
            context.set(field, assignmentValueExpression(value, parameterName, ParameterSource.Patch))
            context.bindValueIfNeeded(parameterName, value, field, ParameterSource.Patch)
        }
        return this
    }

    private fun assignmentValueExpression(value: Any?, parameterName: String, source: ParameterSource): SqlExpr {
        if (value !is KSelectable<*>) return context.valueExpression(value, parameterName)

        val parameters = linkedMapOf<String, Any?>()
        parameters.putAll(context.parameterValues())
        val existingNames = parameters.keys.toSet()
        val query = value.materializeSqlQuery(parameters, assignmentParameterCounter)
        parameters.forEach { (name, parameterValue) ->
            if (name !in existingNames) {
                context.bind(name, parameterValue, null, source)
            }
        }
        return SqlExpr.Subquery(query)
    }

    fun build(wrapper: KronosDataSourceWrapper? = null): KronosActionTask {
        val dataSource = wrapper.orDefault()
        val sqlStatement = planner.plan(dataSource)
        val rendered = OrmDmlRenderer.render(context, wrapper, sqlStatement)
        val (sql, paramMap, jdbcTypeHints) = rendered
        val toUpdateFields = context.setPairs.mapNotNull { pair ->
            val column = (pair.target as? SqlAssignmentTarget.Column)?.identifier?.last ?: return@mapNotNull null
            context.fields.find { it.columnName == column }
        }.toLinkedSet()
        val rootTask = KronosAtomicActionTask(
            sql,
            paramMap,
            operationType = KOperationType.UPDATE,
            statement = sqlStatement,
            stash = JdbcParameterTypeHints.stashFor(jdbcTypeHints),
            listParameterOccurrences = rendered.listParameterOccurrences
        )

        return CascadeUpdateClause.build(
            context.cascadeEnabled,
            context.cascadeAllowed,
            context.pojo,
            targetType,
            context.kClass,
            paramMap,
            toUpdateFields,
            context.where,
            rootTask
        )
    }

    fun execute(wrapper: KronosDataSourceWrapper? = null): KronosOperationResult {
        return build(wrapper).execute(wrapper)
    }
}
