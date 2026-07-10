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

package com.kotlinorm.orm.join

import com.kotlinorm.beans.dsl.KJoinable
import com.kotlinorm.beans.dsl.KSelectable
import com.kotlinorm.beans.dsl.KTableForCondition.Companion.afterFilter
import com.kotlinorm.beans.dsl.KTableForReference.Companion.afterReference
import com.kotlinorm.beans.dsl.KTableForSelect
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
import com.kotlinorm.orm.sql.toSqlParameterEq
import com.kotlinorm.orm.sql.SqlQueryPlan
import com.kotlinorm.syntax.table.SqlJoinType
import com.kotlinorm.types.ToFilter
import com.kotlinorm.types.ToReference
import com.kotlinorm.types.ToSelect
import com.kotlinorm.types.ToSort
import com.kotlinorm.utils.DataSourceUtil.orDefault
import com.kotlinorm.utils.toLinkedSet
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * Select From
 *
 * Create a joint clause for the given pojos
 *
 * @param T1 the type of the first pojo
 *
 * @property t1 the instance of the first pojo
 */
@Suppress("UNCHECKED_CAST")
open class SelectFrom<T1 : KPojo, Selected : KPojo, Context : KPojo>(
    val t1: T1
) : KSelectable<Selected>(t1) {
    internal val context = SelectFromContext<T1, Selected, Context>(t1)
    private val planner = SelectFromPlanner(context)

    override val selectedType: KType
        get() = context.projectionType

    override val nullableSelectedType: KType
        get() = context.nullableProjectionType

    @PublishedApi
    internal fun initializeProjection(type: KType, nullableType: KType, receiverPojo: KPojo = t1) {
        context.projectionType = type
        context.nullableProjectionType = nullableType
        context.receiverPojo = receiverPojo
    }

    @PublishedApi
    internal fun registerDerivedJoin(tableName: String, query: KSelectable<*>, alias: String) {
        context.derivedJoinQueries[tableName] = query to alias
        context.derivedJoinAliasOverrides[tableName] = alias
    }

    fun on(on: ToFilter<T1, Boolean?>) {
        if (null == on) throw EmptyFieldsException()

        t1.afterFilter {
            sourceValues = context.paramMap
            operationType = KOperationType.SELECT
            on(t1)
            parameterValues.forEach { (name, value) -> context.paramMap[name] = value }
            context.listOfPojo.drop(1).forEach { (kClass, kPojo) ->
                context.joinables.add(
                    KJoinable(
                        kPojo.__tableName,
                        SqlJoinType.Left,
                        kClass,
                        kPojo,
                        condition = sqlExpr
                    )
                )
            }
        }
    }

    /**
     * Performs a left join operation between two tables.
     *
     * @param another The table to join with.
     * @param on The condition for the join.
     * @throws EmptyFieldsException If the `on` parameter is null.
     */
    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : KPojo> leftJoin(another: T, noinline on: ToFilter<T1, Boolean?>) {
        joinWith(another, SqlJoinType.Left, T::class as KClass<KPojo>, on)
    }

    /**
     * Performs a right join operation between two tables.
     *
     * @param another The table to join with.
     * @param on The condition for the join.
     * @throws EmptyFieldsException If the `on` parameter is null.
     */
    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : KPojo> rightJoin(another: T, noinline on: ToFilter<T1, Boolean?>) {
        joinWith(another, SqlJoinType.Right, T::class as KClass<KPojo>, on)
    }

    /**
     * Performs a cross join operation between two tables.
     *
     * @param another The table to join with.
     * @param on The condition for the join.
     * @throws EmptyFieldsException If the `on` parameter is null.
     */
    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : KPojo> crossJoin(another: T, noinline on: ToFilter<T1, Boolean?>) {
        joinWith(another, SqlJoinType.Cross, T::class as KClass<KPojo>, on)
    }

    /**
     * Performs an inner join operation between two tables.
     *
     * @param another The table to join with.
     * @param on The condition for the join.
     * @throws EmptyFieldsException If the `on` parameter is null.
     */
    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : KPojo> innerJoin(another: T, noinline on: ToFilter<T1, Boolean?>) {
        joinWith(another, SqlJoinType.Inner, T::class as KClass<KPojo>, on)
    }

    /**
     * Performs a full join operation between two tables.
     *
     * @param another The table to join with.
     * @param on The condition for the join.
     * @throws EmptyFieldsException If the `on` parameter is null.
     */
    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : KPojo> fullJoin(another: T, noinline on: ToFilter<T1, Boolean?>) {
        joinWith(another, SqlJoinType.Full, T::class as KClass<KPojo>, on)
    }

    @PublishedApi
    internal fun joinWith(
        another: KPojo,
        joinType: SqlJoinType,
        kClass: KClass<KPojo>,
        on: ToFilter<T1, Boolean?>
    ) {
        if (null == on) throw EmptyFieldsException()
        val tableName = another.__tableName
        t1.afterFilter {
            sourceValues = context.paramMap
            operationType = KOperationType.SELECT
            on(t1)
            parameterValues.forEach { (name, value) -> context.paramMap[name] = value }
            context.joinables.add(
                KJoinable(
                    tableName,
                    joinType,
                    kClass,
                    another,
                    condition = sqlExpr,
                    tableAliasOverrides = context.derivedJoinAliasOverrides[tableName]?.let { alias ->
                        mapOf(tableName to alias)
                    }.orEmpty()
                )
            )
        }
    }

    /**
     * Selects the specified fields from the table associated with the given KTableField.
     *
     * @param someFields The KTableField representing the fields to be selected.
     */
    @Suppress("UNCHECKED_CAST")
    fun select(someFields: ToSelect<T1, Any?>): SelectFrom<T1, Selected, Context> {
        if (null == someFields) return this

        t1.afterSelect {
            withQualifiedFieldArgs { someFields(t1) }
            if (fields.isEmpty() && selectItems.isEmpty()) {
                throw EmptyFieldsException()
            }
            context.registerSelectedFields(fields)
            projectionItems.forEach { projection ->
                if (projection is KTableForSelect.ProjectionItem.SelectItemValue ||
                    projection is KTableForSelect.ProjectionItem.ScalarSubqueryValue
                ) {
                    context.projectionItems += projection
                }
            }
        }
        return this
    }

    fun db(vararg databaseOfTables: Pair<KPojo, String>) {
        databaseOfTables.forEach {
            context.databaseOfTable[it.first.__tableName] = it.second
        }
    }

    fun cascade(enabled: Boolean) {
        context.cascadeEnabled = enabled
    }

    fun cascade(someFields: ToReference<T1, Any?>) {
        if (someFields == null) throw EmptyFieldsException()
        context.cascadeEnabled = true
        t1.afterReference {
            someFields(t1)
            if (fields.isEmpty()) throw EmptyFieldsException()
            context.cascadeAllowed = fields.toSet()
        }
    }

    /**
     * Orders the result set by the specified fields.
     *
     * @param someFields The fields to order the result set by.
     * @throws EmptyFieldsException If the `someFields` parameter is null.
     */
    fun orderBy(someFields: ToSort<Context, Any?>) {
        if (someFields == null) throw EmptyFieldsException()

        context.orderEnabled = true
        (context.receiverPojo as Context).afterSort {
            withQualifiedFieldArgs { someFields(context.receiverPojo as Context) }
            context.orderByItems = sortedItems.toList()
        }
    }

    /**
     * Sets the groupBy flag to true and checks if the `someFields` parameter is null.
     * If it is null, throws a EmptyFieldsException.
     *
     * @param someFields The fields to group the result set by.
     * @throws EmptyFieldsException If the `someFields` parameter is null.
     */
    fun groupBy(someFields: ToSelect<T1, Any?>) {
        context.groupEnabled = true
        // 检查 someFields 参数是否为空，如果为空则抛出异常
        if (null == someFields) throw EmptyFieldsException()
        t1.afterSelect {
            withQualifiedFieldArgs { someFields(t1) }
            if (fields.isEmpty()) {
                throw EmptyFieldsException()
            }
            // 设置分组字段
            context.groupByFields = fields.toLinkedSet()
        }
    }

    /**
     * Sets the distinctEnabled flag to true, indicating that the result set should be distinct.
     */
    fun distinct() {
        context.distinctEnabled = true
    }

    /**
     * Sets the limit flag to true and sets the limit capacity to the specified number.
     *
     * @param num the number of records to limit the result set to
     */
    fun limit(num: Int) {
        context.limitCapacity = num
    }

    /**
     * Sets the page information for the query, enabling pagination.
     *
     * @param pi the current page number, indicating which page of data to retrieve
     * @param ps the number of records per page, specifying the number of records to display per page
     */
    fun page(pi: Int, ps: Int) {
        context.pageEnabled = true
        context.pageSize = ps
        context.pageIndex = pi
    }

    /**
     * Executes the query logic defined in [someFields] and builds the query condition.
     *
     * @param someFields the fields to be queried
     * @throws EmptyFieldsException if [someFields] is null
     */
    fun by(someFields: ToSelect<T1, Any?>) {
        // 检查someFields是否为空，为空则抛出异常
        if (null == someFields) throw EmptyFieldsException()
        t1.afterSelect {
            // 执行someFields中定义的查询逻辑
            someFields(t1)
            if (fields.isEmpty()) {
                throw EmptyFieldsException()
            }
            context.andWhere(context.andAll(fields.map { field ->
                field.toSqlParameterEq(field.name, useTableAlias = true)
            }))
        }
    }

    /**
     * Sets the condition for the query based on the provided select condition.
     *
     * @param selectCondition the conditional field representing the query condition. Defaults to null.
     * If null, a condition is built to query all fields. Otherwise, the provided select condition is executed
     * and the resulting condition is set.
     */
    fun where(selectCondition: ToFilter<T1, Boolean?> = null) {
        if (selectCondition == null) {
            val rootValues = t1.toDataMap()
            val queryFields = context.allFields.filter { field ->
                rootValues[field.name] != null
            }
            val queryParameterNames = queryFields.mapTo(mutableSetOf()) { it.name }
            context.paramMap.keys.removeAll { it !in queryParameterNames }
            queryFields.forEach { field ->
                context.paramMap.putIfAbsent(field.name, rootValues[field.name])
            }
            context.andWhere(context.andAll(queryFields.map { field ->
                field.toSqlParameterEq(field.name, useTableAlias = true)
            }))
        } else {
            t1.afterFilter {
                sourceValues = context.paramMap
                operationType = context.operationType
                withQualifiedFieldArgs { selectCondition(t1) } // 执行用户提供的条件函数
                parameterValues.forEach { (name, value) -> context.paramMap[name] = value }
                context.andWhere(sqlExpr)
            }
        }
    }

    /**
     * Sets the condition for the HAVING clause based on the provided select condition.
     *
     * @param selectCondition the conditional field representing the HAVING condition. Defaults to null.
     * If null, a condition is built to query all fields. Otherwise, the provided select condition is executed
     * and the resulting condition is set.
     *
     * @throws EmptyFieldsException if the selectCondition parameter is null.
     */
    fun having(selectCondition: ToFilter<T1, Boolean?> = null) {
        // 检查是否提供了条件，未提供则抛出异常
        if (selectCondition == null) throw EmptyFieldsException()
        context.havingEnabled = true // 标记为HAVING条件
        t1.afterFilter {
            sourceValues = context.paramMap // 设置属性参数映射
            operationType = context.operationType
            withQualifiedFieldArgs { selectCondition(t1) } // 执行传入的条件函数
            parameterValues.forEach { (name, value) -> context.paramMap[name] = value }
            context.andHaving(sqlExpr)
        }
    }

    fun patch(vararg pairs: Pair<String, Any?>) {
        context.paramMap.putAll(pairs)
    }

    fun toMapList(wrapper: KronosDataSourceWrapper? = null): List<Map<String, Any?>> {
        return this.build(wrapper).toMapList(wrapper)
    }

    inline fun <reified T> toList(
        wrapper: KronosDataSourceWrapper? = null
    ): List<T> {
        return this.build(wrapper).toList(wrapper)
    }

    @JvmName("toProjectionList")
    @Suppress("UNCHECKED_CAST")
    fun toList(wrapper: KronosDataSourceWrapper? = null): List<Selected> {
        return this.build(wrapper).toList(wrapper, selectedType) as List<Selected>
    }

    fun toMap(wrapper: KronosDataSourceWrapper? = null): Map<String, Any?> {
        limit(1)
        return this.build(wrapper).toMap(wrapper)
    }

    fun toMapOrNull(wrapper: KronosDataSourceWrapper? = null): Map<String, Any?>? {
        limit(1)
        return this.build(wrapper).toMapOrNull(wrapper)
    }

    inline fun <reified T> first(
        wrapper: KronosDataSourceWrapper? = null
    ): T {
        limit(1)
        return this.build(wrapper).first(wrapper)
    }

    @JvmName("firstProjection")
    @Suppress("UNCHECKED_CAST")
    fun first(wrapper: KronosDataSourceWrapper? = null): Selected {
        limit(1)
        return this.build(wrapper).first(wrapper, selectedType) as Selected
    }

    inline fun <reified T> firstOrNull(wrapper: KronosDataSourceWrapper? = null): T? {
        limit(1)
        return this.build(wrapper).firstOrNull(wrapper)
    }

    @JvmName("firstProjectionOrNull")
    @Suppress("UNCHECKED_CAST")
    fun firstOrNull(wrapper: KronosDataSourceWrapper? = null): Selected? {
        limit(1)
        return this.build(wrapper).first(wrapper, nullableSelectedType, required = false) as Selected?
    }

    internal override fun toSqlQueryPlan(wrapper: KronosDataSourceWrapper?): SqlQueryPlan {
        return planner.plan(wrapper.orDefault())
    }

    override fun build(wrapper: KronosDataSourceWrapper?): KronosQueryTask {
        val dataSource = wrapper.orDefault()
        val plan = toSqlQueryPlan(dataSource)
        val renderedSql = renderStatement(dataSource, plan.query, plan.parameters, context.fieldsMap())

        return CascadeJoinClause.build(
            context.cascadeEnabled, context.cascadeAllowed, context.listOfPojo, KronosAtomicQueryTask(
                sql = renderedSql.sql,
                paramMap = renderedSql.parameters,
                operationType = KOperationType.SELECT,
                statement = plan.query,
                targetType = selectedType
            ), context.operationType, context.selectedFieldsByAlias, context.cascadeSelectedProps ?: mutableSetOf()
        )
    }

    internal override fun buildTotalCountTask(wrapper: KronosDataSourceWrapper?): KronosQueryTask {
        val dataSource = wrapper.orDefault()
        val plan = planner.planTotalCount(dataSource)
        val renderedSql = renderStatement(dataSource, plan.query, plan.parameters, context.fieldsMap())

        return KronosQueryTask(
            KronosAtomicQueryTask(
                sql = renderedSql.sql,
                paramMap = renderedSql.parameters,
                operationType = KOperationType.SELECT,
                statement = plan.query,
                targetType = typeOf<Int>()
            )
        )
    }
}
