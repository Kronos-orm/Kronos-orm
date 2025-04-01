/**
 * Copyright 2022-2024 kronos-orm
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

import com.kotlinorm.beans.dsl.Criteria
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KSelectable
import com.kotlinorm.beans.dsl.KTableForCondition.Companion.afterFilter
import com.kotlinorm.beans.dsl.KTableForReference.Companion.afterReference
import com.kotlinorm.beans.dsl.KTableForSelect.Companion.afterSelect
import com.kotlinorm.beans.dsl.KTableForSort.Companion.afterSort
import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.beans.task.KronosQueryTask
import com.kotlinorm.database.SqlManager.getSelectSql
import com.kotlinorm.database.SqlManager.quoted
import com.kotlinorm.enums.KColumnType.CUSTOM_CRITERIA_SQL
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.enums.PessimisticLock
import com.kotlinorm.enums.QueryType.QueryList
import com.kotlinorm.enums.QueryType.QueryOne
import com.kotlinorm.enums.QueryType.QueryOneOrNull
import com.kotlinorm.enums.SortType
import com.kotlinorm.exceptions.NeedFieldsException
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.cascade.CascadeSelectClause
import com.kotlinorm.orm.pagination.PagedClause
import com.kotlinorm.types.ToFilter
import com.kotlinorm.types.ToReference
import com.kotlinorm.types.ToSelect
import com.kotlinorm.types.ToSort
import com.kotlinorm.utils.ConditionSqlBuilder.buildConditionSqlWithParams
import com.kotlinorm.utils.DataSourceUtil.orDefault
import com.kotlinorm.utils.Extensions.asSql
import com.kotlinorm.utils.Extensions.eq
import com.kotlinorm.utils.Extensions.toCriteria
import com.kotlinorm.utils.logAndReturn
import com.kotlinorm.utils.setCommonStrategy
import com.kotlinorm.utils.toLinkedSet

class SelectClause<T : KPojo>(
    override val pojo: T, setSelectFields: ToSelect<T, Any?> = null
) : KSelectable<T>(pojo) {
    private var tableName = pojo.kronosTableName()
    internal var paramMap = pojo.toDataMap()
    private var logicDeleteStrategy = pojo.kronosLogicDelete()
    private var allFields = pojo.kronosColumns().toLinkedSet()
    internal var condition: Criteria? = null
    private var havingCondition: Criteria? = null
    override var selectFields: LinkedHashSet<Field> = linkedSetOf()
    private var groupByFields: LinkedHashSet<Field> = linkedSetOf()
    private var orderByFields: LinkedHashSet<Pair<Field, SortType>> = linkedSetOf()
    override var limitCapacity = 0
    private var distinctEnabled = false
    override var pageEnabled = false
    private var groupEnabled = false
    private var havingEnabled = false
    private var orderEnabled = false
    private var cascadeEnabled = true

    /**
     * 级联查询允许的字段，若为空则表示所有字段均可级联查询，优先级高于[com.kotlinorm.annotations.Ignore[com.kotlinorm.enums.IgnoreAction.CASCADE_SELECT]]
     * */
    internal var cascadeAllowed: Set<Field>? = null
    internal var cascadeSelectedProps: Set<Field>? = null
    private var lock: PessimisticLock? = null
    override var selectAll = true
    private var ps = 0
    private var pi = 0
    private var databaseName: String? = null
    internal var operationType = KOperationType.SELECT // 级联操作类型，默认为SELECT

    /**
     * 初始化函数：用于在对象初始化时配置选择字段。
     * 该函数不接受参数，也不返回任何值。
     * 它首先检查setSelectFields是否为非空，如果是，则调用pojo.tableRun块，
     * 在该块内调用setSelectFields方法来设置选择的字段，并将当前字段集合转换为链接集合后赋值给selectFields属性。
     */
    init {
        if (setSelectFields != null) {
            pojo.afterSelect {
                setSelectFields(it) // 设置选择的字段
                if (fields.isEmpty()) {
                    throw NeedFieldsException()
                }
                selectFields = fields.toLinkedSet() // 将字段集合转换为不可变的链接集合并赋值给selectFields
                if (selectFields.isNotEmpty()) {
                    selectAll = false
                }
            }
        }
    }

    fun single(): SelectClause<T> {
        limitCapacity = 1
        return this
    }

    fun limit(capacity: Int): SelectClause<T> {
        limitCapacity = capacity
        return this
    }

    fun db(databaseName: String): SelectClause<T> {
        if (databaseName.isNotBlank()) this.databaseName = databaseName
        return this
    }

    /**
     * 根据指定的字段对当前对象进行排序。
     *
     * @param someFields 可排序字段的集合，这里的字段类型为 [ToSort]，单位为 [Unit]。
     *                   该参数指定了排序时所依据的字段。
     * @return 返回 [SelectClause] 对象，允许链式调用。
     */
    fun orderBy(someFields: ToSort<T, Any?>): SelectClause<T> {
        if (someFields == null) throw NeedFieldsException()

        orderEnabled = true
        pojo.afterSort {
            someFields(it)// 在这里对排序操作进行封装，为后续的链式调用提供支持。
            orderByFields = sortedFields.toLinkedSet()
        }
        return this // 返回当前对象，允许继续进行其他查询操作。
    }


    /**
     * 根据指定的字段对数据进行分组。
     *
     * @param someFields 要用于分组的字段，类型为 KTableField<T, Unit>。该字段不能为空。
     * @return 返回 SelectClause<T> 实例，允许链式调用。
     * @throws NeedFieldsException 如果 someFields 为空，则抛出此异常。
     */
    fun groupBy(someFields: ToSelect<T, Any?>): SelectClause<T> {
        groupEnabled = true
        // 检查 someFields 参数是否为空，如果为空则抛出异常
        if (someFields == null) throw NeedFieldsException()
        pojo.afterSelect {
            someFields(it)
            if (fields.isEmpty()) {
                throw NeedFieldsException()
            }
            // 设置分组字段
            groupByFields = fields.toLinkedSet()
        }
        return this
    }


    /**
     * 将当前选择语句设置为Distinct模式，即去除结果中的重复项。
     *
     * @return [SelectClause<T>] 返回当前选择语句实例，允许链式调用。
     */
    fun distinct(): SelectClause<T> {
        distinctEnabled = true // 标记为Distinct，去除结果中的重复项
        return this
    }


    /**
     * 设置分页信息，用于查询语句的分页操作。
     *
     * @param pi 当前页码，表示需要获取哪一页的数据。
     * @param ps 每页的记录数，指定每页显示的数据量。
     * @return 返回 SelectClause<T> 实例，支持链式调用。
     */
    fun page(pi: Int, ps: Int): SelectClause<T> {
        pageEnabled = true
        this.ps = ps
        this.pi = pi
        return this
    }

    fun cascade(enabled: Boolean): SelectClause<T> {
        cascadeEnabled = enabled
        return this
    }

    fun cascade(someFields: ToReference<T, Any?>): SelectClause<T> {
        if(someFields == null) throw NeedFieldsException()
        cascadeEnabled = true
        pojo.afterReference {
            someFields(it)
            if (fields.isEmpty()) {
                throw NeedFieldsException()
            }
            cascadeAllowed = fields.toSet()
        }
        return this
    }

    /**
     * 根据指定的字段构建查询条件，并返回SelectClause实例。
     *
     * @param someFields KTableField类型，表示要用来构建查询条件的字段。
     *                   不能为空，否则会抛出NeedFieldsException异常。
     * @return 返回当前SelectClause实例，允许链式调用。
     */
    fun by(someFields: ToSelect<T, Any?>): SelectClause<T> {
        // 检查someFields是否为空，为空则抛出异常
        if (someFields == null) throw NeedFieldsException()
        pojo.afterSelect { t ->
            // 执行someFields中定义的查询逻辑
            someFields(t)
            if (fields.isEmpty()) {
                throw NeedFieldsException()
            }
            // 构建查询条件，将字段名映射到参数值，并转换为查询条件对象
            condition = fields.map { it.eq(paramMap[it.name]) }.toCriteria()
        }
        return this // 返回当前SelectClause实例，允许链式调用
    }


    /**
     * 根据提供的选择条件构建查询条件。
     *
     * @param selectCondition 一个函数，用于定义条件查询。该函数接收一个 [ToFilter] 类型的参数，
     *                        并返回一个 [Boolean]? 类型的值，用于指定条件是否成立。如果为 null，则表示选择所有字段。
     * @return [SelectClause] 的实例，代表了一个查询的选择子句。
     */
    fun where(selectCondition: ToFilter<T, Boolean?> = null): SelectClause<T> {
        if (selectCondition == null) return this
        pojo.afterFilter {
            criteriaParamMap = paramMap
            selectCondition(it) // 执行用户提供的条件函数
            condition = criteria // 设置查询条件
        }
        return this
    }

    /**
     * 设置HAVING条件的函数，用于在查询中添加基于聚合结果的条件限制。
     *
     * @param selectCondition 一个KTableConditionalField类型的函数参数，表示筛选的条件。该条件是一个函数，
     *                        它接收当前的参数映射表和执行条件，并设置HAVING子句的条件。
     * @return 返回SelectClause类型的实例，允许链式调用。
     * @throws NeedFieldsException 如果selectCondition为null，则抛出此异常，表示需要提供条件字段。
     */
    fun having(selectCondition: ToFilter<T, Boolean?> = null): SelectClause<T> {
        havingEnabled = true // 标记为HAVING条件
        // 检查是否提供了条件，未提供则抛出异常
        if (selectCondition == null) throw NeedFieldsException()
        pojo.afterFilter {
            criteriaParamMap = paramMap // 设置属性参数映射
            selectCondition(it) // 执行传入的条件函数
            havingCondition = criteria // 设置HAVING条件
        }
        return this // 允许链式调用
    }

    fun withTotal(): PagedClause<T, SelectClause<T>> {
        return PagedClause(this)
    }

    fun patch(vararg pairs: Pair<String, Any?>): SelectClause<T> {
        paramMap.putAll(pairs)
        return this
    }

    fun lock(lock: PessimisticLock? = PessimisticLock.X): SelectClause<T> {
        this.lock = lock
        return this
    }

    private var buildCondition: Criteria? = null

    /**
     * 构建一个KronosAtomicTask对象。
     *
     * 该方法主要用于根据提供的KronosDataSourceWrapper（如果存在）和其他参数构建一个用于执行数据库操作的KronosAtomicTask对象。
     * 这包括构建SQL查询语句及其参数映射，配置逻辑删除策略，并根据不同的标志（如分页、去重、分组等）调整查询语句的构造。
     *
     * @param wrapper 可选的KronosDataSourceWrapper对象，用于提供数据库表信息等。
     * @return 构建好的KronosAtomicTask对象，包含了完整的SQL查询语句和对应的参数映射。
     */
    override fun build(wrapper: KronosDataSourceWrapper?): KronosQueryTask {
        buildCondition = condition
        // 初始化所有字段集合
        allFields = pojo.kronosColumns().toLinkedSet()

        if (selectAll) {
            selectFields += allFields.filter { it.isColumn }
        }

        val columns = allFields.filter { it.isColumn }
        // 如果条件为空，则根据paramMap构建查询条件
        if (buildCondition == null) {
            buildCondition = paramMap.keys.filter {
                paramMap[it] != null
            }.mapNotNull { propName ->
                columns.find { it.name == propName }?.eq(paramMap[propName])
            }.toCriteria()
        }

        // 设置逻辑删除的条件
        if (logicDeleteStrategy.enabled) setCommonStrategy(logicDeleteStrategy, allFields) { _, value ->
            buildCondition = listOfNotNull(
                buildCondition, "${logicDeleteStrategy.field.quoted(wrapper.orDefault())} = $value".asSql()
            ).toCriteria()
        }

        val paramMap = mutableMapOf<String, Any?>()
        // 构建查询条件SQL
        val sql = getSelectSql(wrapper.orDefault(), toSelectClauseInfo(wrapper) {
            paramMap.putAll(it)
        })

        // 返回构建好的KronosAtomicTask对象
        return CascadeSelectClause.build(
            cascadeEnabled, cascadeAllowed, pojo, KronosAtomicQueryTask(
                sql, paramMap, operationType = KOperationType.SELECT
            ), if (selectAll) allFields else selectFields,
            operationType, cascadeSelectedProps ?: mutableSetOf()
        )
    }

    /**
     * 执行Kronos操作的函数。
     *
     * @param wrapper 可选参数，KronosDataSourceWrapper的实例，用于提供数据源配置和上下文。
     *                如果为null，函数将使用默认配置执行操作。
     * @return 返回KronosOperationResult对象，包含操作的结果信息。
     */
    fun query(wrapper: KronosDataSourceWrapper? = null): List<Map<String, Any>> {
        return this.build().query(wrapper)
    }

    inline fun <reified T> queryList(wrapper: KronosDataSourceWrapper? = null, isKPojo: Boolean = false, superTypes: List<String> = listOf()): List<T> {
        return this.build().queryList(wrapper, isKPojo, superTypes)
    }

    @JvmName("queryForList")
    @Suppress("UNCHECKED_CAST")
    fun queryList(wrapper: KronosDataSourceWrapper? = null): List<T> {
        with(this.build()) {
            beforeQuery?.invoke(this)
            val result = atomicTask.logAndReturn(
                wrapper.orDefault().forList(atomicTask, pojo::class, true, listOf()) as List<T>, QueryList
            )
            afterQuery?.invoke(result, QueryList, wrapper.orDefault())
            return result
        }
    }


    fun queryMap(wrapper: KronosDataSourceWrapper? = null): Map<String, Any> {
        return this.build().queryMap(wrapper)
    }

    fun queryMapOrNull(wrapper: KronosDataSourceWrapper? = null): Map<String, Any>? {
        return this.build().queryMapOrNull(wrapper)
    }

    inline fun <reified T> queryOne(wrapper: KronosDataSourceWrapper? = null, isKPojo: Boolean = false, superTypes: List<String> = listOf()): T {
        return this.build().queryOne(wrapper, isKPojo, superTypes)
    }

    @JvmName("queryForObject")
    @Suppress("UNCHECKED_CAST")
    fun queryOne(wrapper: KronosDataSourceWrapper? = null): T {
        with(this.build()) {
            beforeQuery?.invoke(this)
            val result = atomicTask.logAndReturn(
                (wrapper.orDefault().forObject(atomicTask, pojo::class, true, listOf())
                    ?: throw NullPointerException("No such record")) as T, QueryOne
            )
            afterQuery?.invoke(result, QueryOne, wrapper.orDefault())
            return result
        }
    }

    inline fun <reified T> queryOneOrNull(wrapper: KronosDataSourceWrapper? = null, isKPojo: Boolean = false, superTypes: List<String> = listOf()): T? {
        return this.build().queryOneOrNull(wrapper, isKPojo, superTypes)
    }

    @JvmName("queryForObjectOrNull")
    @Suppress("UNCHECKED_CAST")
    fun queryOneOrNull(wrapper: KronosDataSourceWrapper? = null): T? {
        with(build()) {
            beforeQuery?.invoke(this)
            val result = atomicTask.logAndReturn(
                wrapper.orDefault().forObject(atomicTask, pojo::class, true, listOf()) as T?, QueryOneOrNull
            )
            afterQuery?.invoke(result, QueryOneOrNull, wrapper.orDefault())
            return result
        }
    }

    companion object {

        fun <T : KPojo> Iterable<SelectClause<T>>.by(someFields: ToSelect<T, Any?>): List<SelectClause<T>> {
            return map { it.by(someFields) }
        }

        fun <T : KPojo> Iterable<SelectClause<T>>.cascade(
            enabled: Boolean
        ): List<SelectClause<T>> {
            return map { it.cascade(enabled) }
        }

        fun <T : KPojo> Iterable<SelectClause<T>>.cascade(
            someFields: ToReference<T, Any?>
        ): List<SelectClause<T>> {
            return map { it.cascade(someFields) }
        }

        /**
         * Applies the `where` operation to each update clause in the list based on the provided update condition.
         *
         * @param selectCondition the condition for the update clause. Defaults to null.
         * @return a list of UpdateClause objects with the updated condition
         */
        fun <T : KPojo> Iterable<SelectClause<T>>.where(selectCondition: ToFilter<T, Boolean?> = null): List<SelectClause<T>> {
            return map { it.where(selectCondition) }
        }

        /**
         * Builds a KronosAtomicBatchTask from a list of UpdateClause objects.
         *
         * @param T The type of KPojo objects in the list.
         * @return A KronosAtomicBatchTask object with the SQL and parameter map array from the UpdateClause objects.
         */
        fun <T : KPojo> Iterable<SelectClause<T>>.build(): KronosAtomicBatchTask {
            val tasks = this.map { it.build() }
            return KronosAtomicBatchTask(
                sql = tasks.first().component1(),
                paramMapArr = tasks.map { it.component2() }.toTypedArray(),
                operationType = KOperationType.SELECT
            )
        }

        fun <T : KPojo> Iterable<SelectClause<T>>.query(wrapper: KronosDataSourceWrapper? = null): List<List<Map<String, Any>>> {
            return map { it.query(wrapper) }
        }

        inline fun <reified T : KPojo> Iterable<SelectClause<T>>.queryList(wrapper: KronosDataSourceWrapper? = null): List<List<T>> {
            return map { it.queryList<T>(wrapper) }
        }

        fun <T : KPojo> Iterable<SelectClause<T>>.queryMap(wrapper: KronosDataSourceWrapper? = null): List<Map<String, Any>> {
            return map { it.queryMap(wrapper) }
        }

        fun <T : KPojo> Iterable<SelectClause<T>>.queryMapOrNull(wrapper: KronosDataSourceWrapper? = null): List<Map<String, Any>?> {
            return map { it.queryMapOrNull(wrapper) }
        }

        inline fun <reified T : KPojo> Iterable<SelectClause<T>>.queryOne(wrapper: KronosDataSourceWrapper? = null): List<T> {
            return map { it.queryOne(wrapper) }
        }

        inline fun <reified T : KPojo> Iterable<SelectClause<T>>.queryOneOrNull(wrapper: KronosDataSourceWrapper? = null): List<T?> {
            return map { it.queryOneOrNull(wrapper) }
        }
    }

    private fun toSelectClauseInfo(
        wrapper: KronosDataSourceWrapper? = null, updateMap: (map: MutableMap<String, Any?>) -> Unit
    ): SelectClauseInfo {
        // 构建带有参数的查询条件SQL
        val (whereClauseSql, mapOfWhere) = buildConditionSqlWithParams(KOperationType.SELECT, wrapper, buildCondition).toWhereClause()
        val groupByClauseSql =
            if (groupEnabled && groupByFields.isNotEmpty()) " GROUP BY " + (groupByFields.joinToString(", ") {
                it.quoted(wrapper.orDefault())
            }) else null
        val orderByClauseSql =
            if (orderEnabled && orderByFields.isNotEmpty()) " ORDER BY " + orderByFields.joinToString(", ") {
                if (it.first.type == CUSTOM_CRITERIA_SQL) it.first.toString() else it.first.quoted(wrapper.orDefault()) + " " + it.second
            } else null

        val (havingClauseSql, mapOfHaving) = if (havingEnabled) buildConditionSqlWithParams(
            KOperationType.SELECT,
            wrapper, havingCondition
        ).toHavingClause() else null to mutableMapOf()
        updateMap(mapOfWhere)
        updateMap(mapOfHaving)
        return SelectClauseInfo(
            databaseName,
            tableName,
            selectFields.toList(),
            distinctEnabled,
            pageEnabled,
            pi,
            ps,
            limitCapacity,
            lock,
            whereClauseSql,
            groupByClauseSql,
            orderByClauseSql,
            havingClauseSql
        )
    }
}