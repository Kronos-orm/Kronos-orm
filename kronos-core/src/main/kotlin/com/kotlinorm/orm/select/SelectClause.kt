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

package com.kotlinorm.orm.select

import com.kotlinorm.beans.dsl.KSelectable
import com.kotlinorm.beans.dsl.KTableForSelect
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
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.cascade.CascadeSelectClause
import com.kotlinorm.orm.pagination.Cursor
import com.kotlinorm.orm.pagination.CursorPageField
import com.kotlinorm.orm.pagination.CursorPageQuery
import com.kotlinorm.orm.pagination.OffsetPageQuery
import com.kotlinorm.orm.pagination.CursorSpec
import com.kotlinorm.orm.pagination.checkedCursorFetchSize
import com.kotlinorm.orm.pagination.checkedPageOffset
import com.kotlinorm.orm.sql.SqlQueryPlan
import com.kotlinorm.syntax.limit.SqlLimit
import com.kotlinorm.syntax.statement.SqlLock
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.types.ToFilter
import com.kotlinorm.types.ToReference
import com.kotlinorm.types.ToSelect
import com.kotlinorm.types.ToSort
import com.kotlinorm.utils.DataSourceUtil.orDefault
import com.kotlinorm.utils.toLinkedSet
import kotlin.reflect.KType
import kotlin.reflect.typeOf

class SelectClause<Source : KPojo, Selected : KPojo, Context : KPojo>(
    override val pojo: Source,
    setSelectFields: ToSelect<Source, Any?> = null,
    projectionType: KType,
    nullableProjectionType: KType,
    contextPojo: Context = pojo as Context,
    sourceQuery: KSelectable<*>? = null,
    sourceAlias: String? = null
) : KSelectable<Selected>(pojo) {
    override val selectedType: KType = projectionType
    override val nullableSelectedType: KType = nullableProjectionType
    internal val context = SelectContext<Source, Selected, Context>(pojo, contextPojo, projectionType)
    private val planner = SelectPlanner(context)

    init {
        if (sourceQuery != null && sourceAlias != null) {
            with(context) {
                this.sourceQuery = sourceQuery
                sourceTableAlias = sourceAlias
                logicDeleteStrategy = null
                qualifySource(sourceAlias)
            }
        }
        if (setSelectFields != null) {
            context.withSourceScope {
                pojo.afterSelect(context.sourceBinding) {
                    setSelectFields(it)
                    if (fields.isEmpty() && selectItems.isEmpty()) {
                        throw EmptyFieldsException()
                    }
                    context.setProjectionItems(projectionItems.toList(), fields)
                }
            }
        }
    }

    override fun toSqlQueryPlan(wrapper: KronosDataSourceWrapper?): SqlQueryPlan =
        planner.plan(wrapper.orDefault())

    internal override fun buildTotalCountTask(wrapper: KronosDataSourceWrapper?): KronosQueryTask {
        val dataSource = wrapper.orDefault()
        val plan = planner.planTotalCount(dataSource)
        val renderedSql = renderStatement(dataSource, plan.query, plan.parameters, context.fieldMap)
        return KronosQueryTask(
            KronosAtomicQueryTask(
                sql = renderedSql.sql,
                paramMap = renderedSql.parameters,
                operationType = KOperationType.SELECT,
                statement = plan.query,
                targetType = typeOf<Int>(),
                listParameterOccurrences = renderedSql.listParameterOccurrences
            )
        )
    }

    internal override fun outputStableKeyCandidates(): List<List<String>> =
        context.outputStableKeyCandidates()

    fun single(): SelectClause<Source, Selected, Context> {
        context.limit = SqlLimit.limit(1)
        return this
    }

    @PublishedApi
    internal override fun prepareFirstResult() {
        limit(1)
    }

    fun limit(capacity: Int): SelectClause<Source, Selected, Context> {
        context.limit = if (capacity >= 0) SqlLimit.limit(capacity, context.limit?.offset?.numberLiteralInt()) else null
        return this
    }

    fun db(databaseName: String): SelectClause<Source, Selected, Context> {
        if (databaseName.isNotBlank()) {
            context.databaseName = databaseName
        }
        return this
    }

    fun orderBy(someFields: ToSort<Context, Any?>): SelectClause<Source, Selected, Context> {
        someFields ?: throw EmptyFieldsException()
        context.withSourceScope {
            context.receiverPojo.afterSort(context.sourceBinding) {
                someFields(it)
                context.orderByItems = if (sortedItems.isNotEmpty()) {
                    sortedItems.map { item ->
                        when (item) {
                            is KTableForSort.SortItem.FieldItem -> SelectOrderItem.FieldItem(
                                context.sourceBinding.bindField(item.field),
                                item.ordering,
                                item.expr
                            )
                            is KTableForSort.SortItem.ExpressionItem -> SelectOrderItem.ExprItem(
                                context.bindExpr(item.expression.qualifySourceAliasIfPresent(context.sourceTableAlias)),
                                item.ordering
                            )
                            is KTableForSort.SortItem.SelectableItem -> SelectOrderItem.SelectableItem(item.query, item.ordering)
                        }
                    }
                } else {
                    emptyList()
                }
            }
        }
        return this
    }

    fun groupBy(someFields: ToSelect<Source, Any?>): SelectClause<Source, Selected, Context> {
        someFields ?: throw EmptyFieldsException()
        context.withSourceScope {
            pojo.afterSelect(context.sourceBinding) {
                someFields(it)
                val items = projectionItems.mapNotNull { projection ->
                    when (projection) {
                        is KTableForSelect.ProjectionItem.FieldItem -> context.selectExpr(projection.field)
                        is KTableForSelect.ProjectionItem.SelectItemValue -> when (val item = projection.item) {
                            is SqlSelectItem.Expr -> context.bindExpr(
                                item.expr.qualifySourceAliasIfPresent(context.sourceTableAlias)
                            )
                            is SqlSelectItem.Asterisk -> null
                        }
                        is KTableForSelect.ProjectionItem.ScalarSubqueryValue -> null
                    }
                }
                if (items.isEmpty()) {
                    throw EmptyFieldsException()
                }
                context.groupByItems = items
            }
        }
        return this
    }

    fun distinct(): SelectClause<Source, Selected, Context> {
        context.distinct = true
        return this
    }

    private fun applyOffsetPage(offset: Int, pageSize: Int) {
        context.limit = SqlLimit.limit(pageSize, offset)
    }

    private fun prepareCursorPage(fetchSize: Int) {
        context.prepareCursorOrder()
        context.limit = SqlLimit.limit(fetchSize)
    }

    fun cascade(enabled: Boolean): SelectClause<Source, Selected, Context> {
        context.cascadeEnabled = enabled
        return this
    }

    fun cascade(someFields: ToReference<Source, Any?>): SelectClause<Source, Selected, Context> {
        someFields ?: throw EmptyFieldsException()
        context.cascadeEnabled = true
        context.withSourceScope {
            pojo.afterReference {
                someFields(it)
                if (fields.isEmpty()) {
                    throw EmptyFieldsException()
                }
                context.cascadeAllowed = fields.toSet()
            }
        }
        return this
    }

    fun by(someFields: ToSelect<Source, Any?>): SelectClause<Source, Selected, Context> {
        someFields ?: throw EmptyFieldsException()
        context.withSourceScope {
            pojo.afterSelect(context.sourceBinding) {
                someFields(it)
                if (fields.isEmpty()) {
                    throw EmptyFieldsException()
                }
                context.addFieldConditions(fields, context.sourceValues)
            }
        }
        return this
    }

    fun where(selectCondition: ToFilter<Source, Boolean?>? = null): SelectClause<Source, Selected, Context> {
        if (selectCondition == null) {
            context.addSourceValueConditions()
            return this
        }
        context.withSourceScope {
            pojo.afterFilter(context.sourceBinding) filter@ { filterTable ->
                with(context) {
                    this@filter.sourceValues = sourceValues
                    this@filter.operationType = operationType
                    selectCondition(filterTable)
                    andWhere(
                        this@filter.sqlExpr?.qualifySourceAliasIfPresent(sourceTableAlias),
                        this@filter.parameterValues
                    )
                }
            }
        }
        return this
    }

    fun having(selectCondition: ToFilter<Source, Boolean?>? = null): SelectClause<Source, Selected, Context> {
        selectCondition ?: throw EmptyFieldsException()
        context.withSourceScope {
            pojo.afterFilter(context.sourceBinding) filter@ { filterTable ->
                with(context) {
                    this@filter.sourceValues = sourceValues
                    this@filter.operationType = operationType
                    selectCondition(filterTable)
                    andHaving(
                        this@filter.sqlExpr?.qualifySourceAliasIfPresent(sourceTableAlias),
                        this@filter.parameterValues
                    )
                }
            }
        }
        return this
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
        query.prepareCursorPage(fetchSize)
        val fields = query.context.orderByItems.map { item ->
            require(item is SelectOrderItem.FieldItem) { "Cursor pagination requires field-based orderBy items." }
            val resultLabel = query.context.cursorValueLabel(item.field)
            CursorPageField(
                name = item.field.name,
                resultLabel = resultLabel,
                hidden = query.context.cursorOnlySelectFields.any { (_, label) -> label == resultLabel }
            )
        }
        return CursorPageQuery(query, pageSize, fields)
    }

    private fun paginationSnapshot(): SelectClause<Source, Selected, Context> =
        SelectClause<Source, Selected, Context>(
            pojo = pojo,
            projectionType = selectedType,
            nullableProjectionType = nullableSelectedType,
            contextPojo = context.receiverPojo
        ).also { snapshot -> snapshot.context.copyStateFrom(context) }

    fun patch(vararg pairs: Pair<String, Any?>): SelectClause<Source, Selected, Context> {
        context.sourceValues.putAll(pairs)
        context.patchValues.putAll(pairs)
        return this
    }

    fun lock(lock: SqlLock? = SqlLock.Update()): SelectClause<Source, Selected, Context> {
        context.lock = lock
        return this
    }

    override fun build(wrapper: KronosDataSourceWrapper?): KronosQueryTask {
        val dataSource = wrapper.orDefault()
        val plan = toSqlQueryPlan(dataSource)
        val renderedSql = renderStatement(dataSource, plan.query, plan.parameters, context.fieldMap)
        val finalSelectFields = if (context.selectAll) {
            (context.allFields + context.cascadeFields).toLinkedSet()
        } else {
            (context.selectedFields + context.cascadeFields).toLinkedSet()
        }
        val resultFieldsByLabel = if (context.selectAll) {
            context.allColumns.associateBy { it.name }
        } else {
            context.selectedFieldsByOutputName
        }
        return CascadeSelectClause.build(
            context.cascadeEnabled,
            context.cascadeAllowed,
            pojo,
            context.kClass,
            KronosAtomicQueryTask(
                sql = renderedSql.sql,
                paramMap = renderedSql.parameters,
                operationType = KOperationType.SELECT,
                statement = plan.query,
                targetType = context.projectionType,
                resultColumnTypes = resultColumnTypes(resultFieldsByLabel),
                listParameterOccurrences = renderedSql.listParameterOccurrences
            ),
            finalSelectFields,
            context.operationType,
            context.cascadeSelectedProps ?: mutableSetOf()
        )
    }

    fun toMapList(wrapper: KronosDataSourceWrapper? = null): List<Map<String, Any?>> {
        return build(wrapper).toMapList(wrapper)
    }

    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
    @kotlin.internal.LowPriorityInOverloadResolution
    inline fun <reified T> toList(
        wrapper: KronosDataSourceWrapper? = null
    ): List<T> {
        return build(wrapper).toList(wrapper)
    }

    @JvmName("toProjectionList")
    @Suppress("UNCHECKED_CAST")
    fun toList(wrapper: KronosDataSourceWrapper? = null): List<Selected> {
        return build(wrapper).toList(wrapper, selectedType) as List<Selected>
    }

    fun toMap(wrapper: KronosDataSourceWrapper? = null): Map<String, Any?> {
        limit(1)
        return build(wrapper).toMap(wrapper)
    }

    fun toMapOrNull(wrapper: KronosDataSourceWrapper? = null): Map<String, Any?>? {
        limit(1)
        return build(wrapper).toMapOrNull(wrapper)
    }

}
