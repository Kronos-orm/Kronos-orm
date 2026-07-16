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

package com.kotlinorm.orm.delete

import com.kotlinorm.beans.dsl.KTableForCondition.Companion.afterFilter
import com.kotlinorm.beans.dsl.KTableForReference.Companion.afterReference
import com.kotlinorm.beans.dsl.KTableForSelect.Companion.afterSelect
import com.kotlinorm.beans.task.KronosActionTask
import com.kotlinorm.beans.task.KronosAtomicActionTask
import com.kotlinorm.beans.task.KronosOperationResult
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.exceptions.EmptyFieldsException
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.cascade.CascadeDeleteClause
import com.kotlinorm.orm.sql.toSqlParameterEq
import com.kotlinorm.orm.statement.OrmContext
import com.kotlinorm.orm.statement.OrmDmlRenderer
import com.kotlinorm.orm.statement.ParameterSource
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlParameter
import com.kotlinorm.syntax.statement.SqlDmlStatement
import com.kotlinorm.types.ToFilter
import com.kotlinorm.types.ToReference
import com.kotlinorm.types.ToSelect
import com.kotlinorm.utils.DataSourceUtil.orDefault
import com.kotlinorm.utils.execute
import com.kotlinorm.utils.resolveRuntimeMetadata
import kotlin.reflect.KType

class DeleteClause<T : KPojo>(pojo: T, private val targetType: KType) {
    private val metadata = pojo.resolveRuntimeMetadata()
    internal val context = OrmContext(
        pojo = pojo,
        kClass = metadata.kClass,
        tableName = metadata.tableName,
        operationType = KOperationType.DELETE,
        fields = metadata.allColumns,
        allFields = metadata.allFields.toList(),
        fieldMap = metadata.fieldMap,
        updateTimeStrategy = metadata.updateTimeStrategy,
        logicDeleteStrategy = metadata.logicDeleteStrategy,
        optimisticLockStrategy = metadata.optimisticLockStrategy
    )
    private val planner = DeletePlanner(context)

    fun logic(enabled: Boolean = true): DeleteClause<T> {
        context.logicEnabled = enabled
        return this
    }

    /**
     * 根据指定的字段构建删除语句的条件部分。
     *
     * @param someFields KTableField类型，表示要用于删除条件的字段。不可为null。
     * @return DeleteClause类型，表示构建完成的删除语句实例。
     * @throws EmptyFieldsException 如果someFields为空或者最终没有有效的字段用于构建条件时抛出。
     */
    fun by(someFields: ToSelect<T, Any?>): DeleteClause<T> {
        // 检查传入的someFields是否为null，若为null则抛出异常
        if (someFields == null) throw EmptyFieldsException()
        context.pojo.afterSelect {
            someFields(it)
            // 若fields为空，则抛出异常，表示需要至少一个字段来构建删除条件
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

    fun cascade(enabled: Boolean): DeleteClause<T> {
        context.cascadeEnabled = enabled
        return this
    }

    fun cascade(someFields: ToReference<T, Any?>): DeleteClause<T> {
        if (someFields == null) throw EmptyFieldsException()
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

    /**
     * 构建删除语句的条件部分。
     *
     * 该函数允许用户指定一个删除条件，用于过滤需要被删除的数据。如果未指定条件，则默认删除所有匹配的数据。
     *
     * @param deleteCondition 一个函数，用于定义删除操作的条件。该函数接收一个 [ToFilter] 类型的参数，
     *                        并返回一个 [Boolean?] 类型的值，用于指示是否满足删除条件。如果为 null，则表示删除所有数据。
     * @return [DeleteClause] 类型的实例，用于链式调用其它删除操作。
     */
    fun where(deleteCondition: ToFilter<T, Boolean?> = null): DeleteClause<T> {
        if (deleteCondition == null) return this
        // 如果指定了删除条件，执行条件函数，并设置条件
        context.pojo.afterFilter filter@ { filterTable ->
            with(context) {
                this@filter.sourceValues = sourceValues.toMutableMap()
                this@filter.operationType = operationType
                deleteCondition(filterTable)
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

    fun patch(vararg pairs: Pair<String, Any?>): DeleteClause<T> {
        pairs.forEach { (name, value) ->
            val field = context.fields.firstOrNull { it.name == name || it.columnName == name }
            context.bind(name, value, field, ParameterSource.Patch)
        }
        return this
    }

    /**
     * 构建并返回一个KronosAtomicTask对象，用于执行数据库的原子操作。
     * 该方法根据设定的条件构建对应的UPDATE或DELETE SQL语句，并封装必要的参数与操作类型。
     *
     * @return [KronosAtomicActionTask] 一个包含SQL语句、参数映射以及操作类型的原子任务对象。
     */
    fun build(wrapper: KronosDataSourceWrapper? = null): KronosActionTask {
        val dataSource = wrapper.orDefault()
        val sqlStatement = planner.plan(dataSource)
        val where = when (sqlStatement) {
            is SqlDmlStatement.Delete -> sqlStatement.where
            is SqlDmlStatement.Update -> sqlStatement.where
            else -> null
        }
        val logic = sqlStatement is SqlDmlStatement.Update
        val rendered = OrmDmlRenderer.render(context, wrapper, sqlStatement)
        val (sql, paramMap) = rendered
        
        return CascadeDeleteClause.build(
            context.cascadeEnabled,
            context.cascadeAllowed,
            targetType,
            context.kClass,
            context.pojo,
            where,
            paramMap,
            logic,
            KronosAtomicActionTask(
                sql,
                paramMap,
                operationType = KOperationType.DELETE,
                statement = sqlStatement,
                listParameterOccurrences = rendered.listParameterOccurrences
            )
        )
    }

    /**
     * 执行Kronos操作的函数。
     *
     * @param wrapper 可选参数，KronosDataSourceWrapper的实例，用于提供数据源配置和上下文。
     *                如果为null，函数将使用默认配置执行操作。
     * @return 返回KronosOperationResult对象，包含操作的结果信息。
     */
    fun execute(wrapper: KronosDataSourceWrapper? = null): KronosOperationResult {
        // 构建并执行Kronos操作，根据提供的wrapper配置执行，如果没有提供则使用默认配置
        return build(wrapper).execute(wrapper)
    }
}
