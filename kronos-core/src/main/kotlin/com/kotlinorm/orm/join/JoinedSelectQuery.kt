/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

@file:OptIn(com.kotlinorm.annotations.InternalKronosApi::class)

package com.kotlinorm.orm.join

import com.kotlinorm.beans.dsl.KSelectable
import com.kotlinorm.beans.dsl.KTableForCondition.Companion.afterFilter
import com.kotlinorm.beans.dsl.KTableForReference.Companion.afterReference
import com.kotlinorm.beans.dsl.KTableForSelect.Companion.afterSelect
import com.kotlinorm.beans.dsl.KTableForSort
import com.kotlinorm.beans.dsl.KTableForSort.Companion.afterSort
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.beans.task.KronosQueryTask
import com.kotlinorm.database.SqlManager.renderStatement
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.exceptions.EmptyFieldsException
import com.kotlinorm.functions.KronosFunctionExpressions.withQualifiedFieldArgs
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.cascade.CascadeJoinClause
import com.kotlinorm.orm.pagination.Cursor
import com.kotlinorm.orm.pagination.CursorPageField
import com.kotlinorm.orm.pagination.CursorPageQuery
import com.kotlinorm.orm.pagination.CursorSpec
import com.kotlinorm.orm.pagination.OffsetPageQuery
import com.kotlinorm.orm.pagination.checkedCursorFetchSize
import com.kotlinorm.orm.pagination.checkedPageOffset
import com.kotlinorm.orm.sql.SqlQueryPlan
import com.kotlinorm.orm.sql.toSqlParameterEq
import com.kotlinorm.syntax.limit.SqlLimit
import com.kotlinorm.syntax.statement.SqlLock
import com.kotlinorm.types.ToFilter
import com.kotlinorm.types.ToReference
import com.kotlinorm.types.ToSelect
import com.kotlinorm.types.ToSort
import com.kotlinorm.utils.DataSourceUtil.orDefault
import com.kotlinorm.utils.toLinkedSet
import kotlin.reflect.KType
import kotlin.reflect.typeOf

class JoinedSelectQuery<Source : KPojo, Selected : KPojo, Context : KPojo> @PublishedApi internal constructor(
    @PublishedApi internal val state: JoinSourceState<Source>,
    projectionType: KType,
    nullableProjectionType: KType,
    contextPojo: Context,
    fields: ToSelect<Source, Any?> = null
) : KSelectable<Selected>(state.root), JoinResult {
    override val pojo: Source = state.root
    override val selectedType: KType = projectionType
    override val nullableSelectedType: KType = nullableProjectionType
    internal val context = JoinedSelectContext<Source, Selected, Context>(
        state,
        contextPojo,
        projectionType,
        nullableProjectionType
    )
    private val planner = JoinedSelectPlanner(context)

    init {
        if (fields != null) {
            context.withSourceScope {
                pojo.afterSelect {
                    withQualifiedFieldArgs { fields.invoke(this@afterSelect, pojo) }
                    if (this.fields.isEmpty() && selectItems.isEmpty()) throw EmptyFieldsException()
                    context.registerProjectionItems(projectionItems.toList(), this.fields)
                }
            }
        }
    }

    override fun toSqlQueryPlan(wrapper: KronosDataSourceWrapper?): SqlQueryPlan =
        planner.plan(wrapper.orDefault())

    internal override fun buildTotalCountTask(wrapper: KronosDataSourceWrapper?): KronosQueryTask {
        val dataSource = wrapper.orDefault()
        val plan = planner.planTotalCount(dataSource)
        val rendered = renderStatement(
            dataSource,
            plan.query,
            plan.parameters,
            context.fieldsMap() + plan.parameterFields
        )
        return KronosQueryTask(
            KronosAtomicQueryTask(
                sql = rendered.sql,
                paramMap = rendered.parameters,
                operationType = KOperationType.SELECT,
                statement = plan.query,
                targetType = typeOf<Int>(),
                listParameterOccurrences = rendered.listParameterOccurrences
            )
        )
    }

    fun single(): JoinedSelectQuery<Source, Selected, Context> = limit(1)

    @PublishedApi
    internal override fun prepareFirstResult() {
        limit(1)
    }

    fun limit(capacity: Int): JoinedSelectQuery<Source, Selected, Context> {
        context.limit = if (capacity >= 0) SqlLimit.limit(capacity) else null
        return this
    }

    fun db(vararg databaseOfSources: Pair<KPojo, String>): JoinedSelectQuery<Source, Selected, Context> {
        databaseOfSources.forEach { (source, database) -> context.setDatabase(source, database) }
        return this
    }

    fun orderBy(fields: ToSort<Context, Any?>): JoinedSelectQuery<Source, Selected, Context> {
        fields ?: throw EmptyFieldsException()
        context.withSourceScope {
            context.receiverPojo.afterSort {
                withQualifiedFieldArgs { fields.invoke(this@afterSort, context.receiverPojo) }
                context.orderByItems = sortedItems.toList()
            }
        }
        return this
    }

    fun groupBy(fields: ToSelect<Source, Any?>): JoinedSelectQuery<Source, Selected, Context> {
        fields ?: throw EmptyFieldsException()
        context.withSourceScope {
            pojo.afterSelect {
                withQualifiedFieldArgs { fields.invoke(this@afterSelect, pojo) }
                if (this.fields.isEmpty()) throw EmptyFieldsException()
                context.groupByFields = this.fields.toLinkedSet()
            }
        }
        return this
    }

    fun distinct(): JoinedSelectQuery<Source, Selected, Context> {
        context.distinct = true
        return this
    }

    private fun applyOffsetPage(offset: Int, pageSize: Int) {
        context.limit = SqlLimit.limit(pageSize, offset)
    }

    fun page(pageIndex: Int, pageSize: Int): OffsetPageQuery<Selected> {
        require(pageIndex > 0) { "Page index must be greater than zero." }
        require(pageSize > 0) { "Page size must be greater than zero." }
        val offset = checkedPageOffset(pageIndex, pageSize)
        val query = paginationSnapshot()
        query.applyOffsetPage(offset, pageSize)
        return OffsetPageQuery(query, pageIndex, pageSize)
    }

    fun cursor(pageSize: Int, after: Cursor? = null): CursorPageQuery<Selected> {
        require(pageSize > 0) { "Page size must be greater than zero." }
        val fetchSize = checkedCursorFetchSize(pageSize)
        val query = paginationSnapshot()
        query.context.cursorSpec = CursorSpec(after, pageSize)
        query.context.prepareCursorOrder()
        query.context.limit = SqlLimit.limit(fetchSize)
        val fields = query.context.orderByItems.map { item ->
            require(item is KTableForSort.SortItem.FieldItem) {
                "Cursor pagination requires field-based orderBy items."
            }
            val resultLabel = query.context.cursorValueLabel(item.field)
            CursorPageField(
                name = item.field.name,
                key = query.context.cursorKey(item.field),
                resultLabel = resultLabel,
                hidden = query.context.cursorOnlySelectFields.any { (_, label) -> label == resultLabel }
            )
        }
        return CursorPageQuery(query, pageSize, fields)
    }

    private fun paginationSnapshot(): JoinedSelectQuery<Source, Selected, Context> =
        JoinedSelectQuery<Source, Selected, Context>(
            state = state,
            projectionType = selectedType,
            nullableProjectionType = nullableSelectedType,
            contextPojo = context.receiverPojo
        ).also { snapshot -> snapshot.context.copyStateFrom(context) }

    fun cascade(enabled: Boolean): JoinedSelectQuery<Source, Selected, Context> {
        context.cascadeEnabled = enabled
        return this
    }

    fun cascade(fields: ToReference<Source, Any?>): JoinedSelectQuery<Source, Selected, Context> {
        fields ?: throw EmptyFieldsException()
        context.cascadeEnabled = true
        context.withSourceScope {
            pojo.afterReference {
                fields.invoke(this@afterReference, pojo)
                if (this.fields.isEmpty()) throw EmptyFieldsException()
                context.cascadeAllowed = this.fields.toSet()
            }
        }
        return this
    }

    fun by(fields: ToSelect<Source, Any?>): JoinedSelectQuery<Source, Selected, Context> {
        fields ?: throw EmptyFieldsException()
        context.withSourceScope {
            pojo.afterSelect {
                fields.invoke(this@afterSelect, pojo)
                if (this.fields.isEmpty()) throw EmptyFieldsException()
                val expressions = this.fields.map { field ->
                    val parameterName = context.bindParameter(field.name, context.sourceValue(field), field)
                    field.toSqlParameterEq(parameterName, useTableAlias = true)
                }
                context.andWhere(context.andAll(expressions))
            }
        }
        return this
    }

    fun where(condition: ToFilter<Source, Boolean?>? = null): JoinedSelectQuery<Source, Selected, Context> {
        if (condition == null) return this
        context.withSourceScope {
            pojo.afterFilter {
                sourceValues = context.sourceValues
                operationType = context.operationType
                withQualifiedFieldArgs { condition.invoke(this@afterFilter, pojo) }
                context.andWhere(sqlExpr, parameterValues, parameterFields)
            }
        }
        return this
    }

    fun having(condition: ToFilter<Source, Boolean?>? = null): JoinedSelectQuery<Source, Selected, Context> {
        condition ?: throw EmptyFieldsException()
        context.withSourceScope {
            pojo.afterFilter {
                sourceValues = context.sourceValues
                operationType = context.operationType
                withQualifiedFieldArgs { condition.invoke(this@afterFilter, pojo) }
                context.andHaving(sqlExpr, parameterValues, parameterFields)
            }
        }
        return this
    }

    fun patch(vararg pairs: Pair<String, Any?>): JoinedSelectQuery<Source, Selected, Context> {
        context.sourceValues.putAll(pairs)
        context.patchValues.putAll(pairs)
        return this
    }

    fun lock(lock: SqlLock? = SqlLock.Update()): JoinedSelectQuery<Source, Selected, Context> {
        context.lock = lock
        return this
    }

    override fun build(wrapper: KronosDataSourceWrapper?): KronosQueryTask {
        val dataSource = wrapper.orDefault()
        val plan = toSqlQueryPlan(dataSource)
        val rendered = renderStatement(
            dataSource,
            plan.query,
            plan.parameters,
            context.fieldsMap() + plan.parameterFields
        )
        val resultFieldsByLabel = if (context.selectAll) {
            context.allFields.associateBy { it.name }
        } else {
            context.selectedFieldsByOutputName
        }
        return CascadeJoinClause.build(
            context.cascadeEnabled,
            context.cascadeAllowed,
            context.listOfPojo,
            KronosAtomicQueryTask(
                sql = rendered.sql,
                paramMap = rendered.parameters,
                operationType = KOperationType.SELECT,
                statement = plan.query,
                targetType = selectedType,
                resultColumns = resultColumns(resultFieldsByLabel),
                listParameterOccurrences = rendered.listParameterOccurrences
            ),
            context.operationType,
            resultFieldsByLabel.toMutableMap(),
            context.cascadeSelectedProps ?: emptySet()
        )
    }

    fun toMapList(wrapper: KronosDataSourceWrapper? = null): List<Map<String, Any?>> =
        build(wrapper).toMapList(wrapper)

    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
    @kotlin.internal.LowPriorityInOverloadResolution
    inline fun <reified T> toList(wrapper: KronosDataSourceWrapper? = null): List<T> =
        build(wrapper).toList(wrapper)

    @JvmName("toProjectionList")
    @Suppress("UNCHECKED_CAST")
    fun toList(wrapper: KronosDataSourceWrapper? = null): List<Selected> =
        build(wrapper).toList(wrapper, selectedType) as List<Selected>

    fun toMap(wrapper: KronosDataSourceWrapper? = null): Map<String, Any?> {
        limit(1)
        return build(wrapper).toMap(wrapper)
    }

    fun toMapOrNull(wrapper: KronosDataSourceWrapper? = null): Map<String, Any?>? {
        limit(1)
        return build(wrapper).toMapOrNull(wrapper)
    }

}
