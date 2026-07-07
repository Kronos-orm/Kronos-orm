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
import com.kotlinorm.cache.fieldsMapCache
import com.kotlinorm.database.SqlManager.renderStatement
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.enums.QueryType.QueryList
import com.kotlinorm.enums.QueryType.QueryOne
import com.kotlinorm.enums.QueryType.QueryOneOrNull
import com.kotlinorm.exceptions.EmptyFieldsException
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.cascade.CascadeSelectClause
import com.kotlinorm.orm.pagination.PagedClause
import com.kotlinorm.orm.sql.SqlQueryPlan
import com.kotlinorm.syntax.limit.SqlLimit
import com.kotlinorm.syntax.statement.SqlLock
import com.kotlinorm.syntax.statement.SqlSelectItem
import com.kotlinorm.types.ToFilter
import com.kotlinorm.types.ToReference
import com.kotlinorm.types.ToSelect
import com.kotlinorm.types.ToSort
import com.kotlinorm.utils.DataSourceUtil.orDefault
import com.kotlinorm.utils.logAndReturn
import com.kotlinorm.utils.toLinkedSet
import kotlin.reflect.KClass

class SelectClause<Source : KPojo, Selected : KPojo, Context : KPojo>(
    override val pojo: Source,
    setSelectFields: ToSelect<Source, Any?> = null,
    projectionClass: KClass<Selected>,
    contextPojo: Context = pojo as Context,
    sourceQuery: KSelectable<*>? = null,
    sourceAlias: String? = null
) : KSelectable<Selected>(pojo, projectionClass) {
    internal val context = SelectContext(pojo, contextPojo, projectionClass)
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
            pojo.afterSelect {
                setSelectFields(it)
                if (fields.isEmpty() && selectItems.isEmpty()) {
                    throw EmptyFieldsException()
                }
                context.setProjectionItems(projectionItems.toList(), fields)
            }
        }
    }

    override fun toSqlQueryPlan(wrapper: KronosDataSourceWrapper?): SqlQueryPlan =
        planner.plan(wrapper.orDefault())

    internal override fun buildTotalCountTask(wrapper: KronosDataSourceWrapper?): KronosQueryTask {
        val dataSource = wrapper.orDefault()
        val plan = planner.planTotalCount(dataSource)
        val renderedSql = renderStatement(dataSource, plan.query, plan.parameters, fieldsMapCache[context.kClass]!!)
        return KronosQueryTask(
            KronosAtomicQueryTask(
                sql = renderedSql.sql,
                paramMap = renderedSql.parameters,
                operationType = KOperationType.SELECT,
                statement = plan.query
            )
        )
    }

    fun single(): SelectClause<Source, Selected, Context> {
        context.limit = SqlLimit.limit(1)
        return this
    }

    fun limit(capacity: Int): SelectClause<Source, Selected, Context> {
        context.limit = if (capacity > 0) SqlLimit.limit(capacity, context.limit?.offset?.numberLiteralInt()) else null
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
        context.receiverPojo.afterSort {
            someFields(it)
            context.orderByItems = if (sortedItems.isNotEmpty()) {
                sortedItems.map { item ->
                    when (item) {
                        is KTableForSort.SortItem.FieldItem -> SelectOrderItem.FieldItem(item.field, item.ordering)
                        is KTableForSort.SortItem.ExpressionItem -> SelectOrderItem.ExprItem(
                            item.expression.qualifySourceAliasIfPresent(context.sourceTableAlias),
                            item.ordering
                        )
                        is KTableForSort.SortItem.SelectableItem -> SelectOrderItem.SelectableItem(item.query, item.ordering)
                    }
                }
            } else {
                emptyList()
            }
        }
        return this
    }

    fun groupBy(someFields: ToSelect<Source, Any?>): SelectClause<Source, Selected, Context> {
        someFields ?: throw EmptyFieldsException()
        pojo.afterSelect {
            someFields(it)
            val items = projectionItems.mapNotNull { projection ->
                when (projection) {
                    is KTableForSelect.ProjectionItem.FieldItem -> context.selectExpr(projection.field)
                    is KTableForSelect.ProjectionItem.SelectItemValue -> when (val item = projection.item) {
                        is SqlSelectItem.Expr -> item.expr.qualifySourceAliasIfPresent(context.sourceTableAlias)
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
        return this
    }

    fun distinct(): SelectClause<Source, Selected, Context> {
        context.distinct = true
        return this
    }

    fun page(pi: Int, ps: Int): SelectClause<Source, Selected, Context> {
        context.limit = SqlLimit.limit(ps, if (pi > 0) (pi - 1) * ps else 0)
        return this
    }

    fun cascade(enabled: Boolean): SelectClause<Source, Selected, Context> {
        context.cascadeEnabled = enabled
        return this
    }

    fun cascade(someFields: ToReference<Source, Any?>): SelectClause<Source, Selected, Context> {
        someFields ?: throw EmptyFieldsException()
        context.cascadeEnabled = true
        pojo.afterReference {
            someFields(it)
            if (fields.isEmpty()) {
                throw EmptyFieldsException()
            }
            context.cascadeAllowed = fields.toSet()
        }
        return this
    }

    fun by(someFields: ToSelect<Source, Any?>): SelectClause<Source, Selected, Context> {
        someFields ?: throw EmptyFieldsException()
        pojo.afterSelect {
            someFields(it)
            if (fields.isEmpty()) {
                throw EmptyFieldsException()
            }
            context.addFieldConditions(fields, context.sourceValues)
        }
        return this
    }

    fun where(selectCondition: ToFilter<Source, Boolean?> = null): SelectClause<Source, Selected, Context> {
        if (selectCondition == null) {
            return this
        }
        pojo.afterFilter filter@ { filterTable ->
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
        return this
    }

    fun having(selectCondition: ToFilter<Source, Boolean?> = null): SelectClause<Source, Selected, Context> {
        selectCondition ?: throw EmptyFieldsException()
        pojo.afterFilter filter@ { filterTable ->
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
        return this
    }

    fun withTotal(): PagedClause<Source, Selected, SelectClause<Source, Selected, Context>> {
        return PagedClause(this)
    }

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
        val renderedSql = renderStatement(dataSource, plan.query, plan.parameters, fieldsMapCache[context.kClass]!!)
        val finalSelectFields = if (context.selectAll) {
            (context.allFields + context.cascadeFields).toLinkedSet()
        } else {
            (context.selectedFields + context.cascadeFields).toLinkedSet()
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
                statement = plan.query
            ),
            finalSelectFields,
            context.operationType,
            context.cascadeSelectedProps ?: mutableSetOf()
        )
    }

    fun query(wrapper: KronosDataSourceWrapper? = null): List<Map<String, Any>> {
        return build(wrapper).query(wrapper)
    }

    inline fun <reified T> queryList(
        wrapper: KronosDataSourceWrapper? = null,
        isKPojo: Boolean = false,
        superTypes: List<String> = []
    ): List<T> {
        return build(wrapper).queryList(wrapper, isKPojo, superTypes)
    }

    @JvmName("queryForList")
    @Suppress("UNCHECKED_CAST")
    fun queryList(wrapper: KronosDataSourceWrapper? = null): List<Selected> {
        with(build(wrapper)) {
            beforeQuery?.invoke(this)
            val result = atomicTask.logAndReturn(
                wrapper.orDefault().forList(atomicTask, context.projectionClass, true, []) as List<Selected>,
                QueryList
            )
            afterQuery?.invoke(result, QueryList, wrapper.orDefault())
            return result
        }
    }

    fun queryMap(wrapper: KronosDataSourceWrapper? = null): Map<String, Any> {
        limit(1)
        return build(wrapper).queryMap(wrapper)
    }

    fun queryMapOrNull(wrapper: KronosDataSourceWrapper? = null): Map<String, Any>? {
        limit(1)
        return build(wrapper).queryMapOrNull(wrapper)
    }

    inline fun <reified T> queryOne(
        wrapper: KronosDataSourceWrapper? = null,
        isKPojo: Boolean = false,
        superTypes: List<String> = []
    ): T {
        limit(1)
        return build(wrapper).queryOne(wrapper, isKPojo, superTypes)
    }

    @JvmName("queryForObject")
    @Suppress("UNCHECKED_CAST")
    fun queryOne(wrapper: KronosDataSourceWrapper? = null): Selected {
        limit(1)
        with(build(wrapper)) {
            beforeQuery?.invoke(this)
            val result = atomicTask.logAndReturn(
                (wrapper.orDefault().forObject(atomicTask, context.projectionClass, true, [])
                    ?: throw NullPointerException("No such record")) as Selected,
                QueryOne
            )
            afterQuery?.invoke(result, QueryOne, wrapper.orDefault())
            return result
        }
    }

    inline fun <reified T> queryOneOrNull(
        wrapper: KronosDataSourceWrapper? = null,
        isKPojo: Boolean = false,
        superTypes: List<String> = []
    ): T? {
        limit(1)
        return build(wrapper).queryOneOrNull(wrapper, isKPojo, superTypes)
    }

    @JvmName("queryForObjectOrNull")
    @Suppress("UNCHECKED_CAST")
    fun queryOneOrNull(wrapper: KronosDataSourceWrapper? = null): Selected? {
        limit(1)
        with(build(wrapper)) {
            beforeQuery?.invoke(this)
            val result = atomicTask.logAndReturn(
                wrapper.orDefault().forObject(atomicTask, context.projectionClass, true, []) as Selected?,
                QueryOneOrNull
            )
            afterQuery?.invoke(result, QueryOneOrNull, wrapper.orDefault())
            return result
        }
    }

}
