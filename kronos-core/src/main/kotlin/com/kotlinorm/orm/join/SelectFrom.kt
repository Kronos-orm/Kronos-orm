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

package com.kotlinorm.orm.join

import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.beans.dsl.*
import com.kotlinorm.beans.dsl.KTableForCondition.Companion.afterFilter
import com.kotlinorm.beans.dsl.KTableForSort.Companion.afterSort
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.beans.task.KronosQueryTask
import com.kotlinorm.database.SqlManager.getJoinSql
import com.kotlinorm.database.SqlManager.quote
import com.kotlinorm.enums.JoinType
import com.kotlinorm.enums.KColumnType.CUSTOM_CRITERIA_SQL
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.enums.QueryType
import com.kotlinorm.enums.SortType
import com.kotlinorm.exceptions.NeedFieldsException
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.beans.dsl.KTableForSelect.Companion.afterSelect
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.cascade.CascadeJoinClause
import com.kotlinorm.types.ToFilter
import com.kotlinorm.types.ToSelect
import com.kotlinorm.types.ToSort
import com.kotlinorm.utils.ConditionSqlBuilder
import com.kotlinorm.utils.ConditionSqlBuilder.buildConditionSqlWithParams
import com.kotlinorm.utils.DataSourceUtil.orDefault
import com.kotlinorm.utils.Extensions.asSql
import com.kotlinorm.utils.Extensions.eq
import com.kotlinorm.utils.Extensions.toCriteria
import com.kotlinorm.utils.logAndReturn
import com.kotlinorm.utils.setCommonStrategy
import com.kotlinorm.utils.toLinkedSet
import java.util.Stack
import kotlin.reflect.KProperty

/**
 * Select From
 *
 * Create a joint clause for the given pojos
 *
 * @param T1 the type of the first pojo
 *
 * @property t1 the instance of the first pojo
 */
open class SelectFrom<T1 : KPojo>(open val t1: T1) : KSelectable<T1>(t1) {
    open lateinit var tableName: String
    open lateinit var paramMap: MutableMap<String, Any?>
    open lateinit var logicDeleteStrategy: KronosCommonStrategy
    open lateinit var allFields: LinkedHashSet<Field>
    open lateinit var listOfPojo: MutableList<KPojo>
    private var condition: Criteria? = null
    private var lastCondition: Criteria? = null
    private var havingCondition: Criteria? = null
    override var selectFields: LinkedHashSet<Field> = linkedSetOf()
    override var selectFunctions: LinkedHashSet<KTableForFunction> = linkedSetOf()
    override var selectAll: Boolean = false
    private var selectFieldsWithNames: MutableMap<String, Field> = mutableMapOf()
    private var keyCounters: ConditionSqlBuilder.KeyCounter = ConditionSqlBuilder.KeyCounter()
    val joinables: MutableList<KJoinable> = mutableListOf()
    private var groupByFields: LinkedHashSet<Field> = linkedSetOf()
    private var orderByFields: LinkedHashSet<Pair<Field, SortType>> = linkedSetOf()
    private var distinctEnabled = false
    private var groupEnabled = false
    private var havingEnabled = false
    private var orderEnabled = false
    private var pageEnabled = false
    private var limitCapacity = 0
    private var cascadeEnabled = true
    private var cascadeAllowed: Array<out KProperty<*>> = arrayOf() // 级联查询的深度限制, 默认为不限制，即所有级联查询都会执行
    private var cascadeSelectedProps: Set<Field>? = null
    private var pi = 0
    private var ps = 0
    private val databaseOfTable: MutableMap<String, String> = mutableMapOf()
    internal var operationType = KOperationType.SELECT

    fun on(on: ToFilter<T1, Boolean?>) {
        if (null == on) throw NeedFieldsException()

        val criteriaMap = mutableMapOf<String, MutableList<Criteria>>()
        val constMap = mutableMapOf<String, MutableList<Criteria>>()
        val repeatlist = mutableListOf<Triple<Criteria, String, String>>()

        t1.afterFilter {
            criteriaParamMap = paramMap
            on(t1)
            criteria

            val stack = Stack<Criteria>()
            var cur = criteria
            var prev = criteria
            while (null != cur || !stack.isEmpty()) {
                while (null != cur) {
                    stack.push(cur)
                    cur = if (cur.children.size > 0) cur.children[0] else null
                }
                val top = stack.peek()
                if (top.children.size <= 1 || top.children[1] == prev) {
                    prev = top
                    val topTableName = top.tableName
                    if (!topTableName.isNullOrEmpty()) {

                        val fieldTableName = top.field.tableName
                        val valueTableName = if (top.value is Field) (top.value as Field).tableName else null

                        if (null == valueTableName)
                            setInMap(top, fieldTableName, constMap)
                        else if (valueTableName == tableName || (fieldTableName != tableName && !criteriaMap.contains(
                                fieldTableName
                            ) && criteriaMap.contains(valueTableName))
                        ) //value侧为主表或目前field侧无条件而value侧有条件，直接将条件放入field侧
                            setInMap(top, fieldTableName, criteriaMap)
                        else if (fieldTableName == tableName || (valueTableName != tableName && criteriaMap.contains(
                                fieldTableName
                            ))
                        ) //field侧为主表或目前value侧无条件而field侧有条件或两侧都有条件，可直接将条件放入vaule侧
                            setInMap(top, valueTableName, criteriaMap)
                        else { // 条件两侧均未出现过，将条件放入两侧，后期再根据两端条件数量删除一侧
                            setInMap(top, valueTableName, criteriaMap)
                            setInMap(top, fieldTableName, criteriaMap)
                            repeatlist.add(Triple(top, fieldTableName, valueTableName))
                        }

                    }

                    stack.pop()
                } else cur = top.children[1]
            }

            repeatlist.forEach {
                val (repeatCriteria, fieldTableName, valueTableName) = it
                if (null != criteriaMap[fieldTableName] && criteriaMap[fieldTableName]!!.size == 1)
                    removeInMap(repeatCriteria, valueTableName, criteriaMap)
                else removeInMap(repeatCriteria, fieldTableName, criteriaMap)
            }

            criteriaMap.putAll(constMap)
            criteriaMap.keys.forEach { key ->
                joinables.add(
                    KJoinable(
                        key,
                        JoinType.LEFT_JOIN,
                        criteriaMap[key]!!.toCriteria(),
                        listOfPojo.find { it.kronosTableName() == key }!!.kronosLogicDelete()
                    )
                )
            }
        }
    }

    private fun setInMap(
        criteria: Criteria,
        criteriaTableName: String,
        map: MutableMap<String, MutableList<Criteria>>
    ) {
        val criteriaList = map.getOrDefault(criteriaTableName, mutableListOf())
        criteriaList.add(criteria)
        map[criteriaTableName] = criteriaList
    }

    private fun removeInMap(
        criteria: Criteria,
        criteriaTableName: String,
        map: MutableMap<String, MutableList<Criteria>>
    ) {
        val criteriaList = map[criteriaTableName]!!
        criteriaList.remove(criteria)
        map[criteriaTableName] = criteriaList
    }

    /**
     * Performs a left join operation between two tables.
     *
     * @param another The table to join with.
     * @param on The condition for the join.
     * @throws NeedFieldsException If the `on` parameter is null.
     */
    inline fun <reified T : KPojo> leftJoin(another: T, noinline on: ToFilter<T1, Boolean?>) {
        if (null == on) throw NeedFieldsException()
        val tableName = another.kronosTableName()
        t1.afterFilter {
            criteriaParamMap = paramMap
            on(t1)
            joinables.add(KJoinable(tableName, JoinType.LEFT_JOIN, criteria, another.kronosLogicDelete()))
        }
    }

    /**
     * Performs a right join operation between two tables.
     *
     * @param another The table to join with.
     * @param on The condition for the join.
     * @throws NeedFieldsException If the `on` parameter is null.
     */
    inline fun <reified T : KPojo> rightJoin(another: T, noinline on: ToFilter<T1, Boolean?>) {
        if (null == on) throw NeedFieldsException()
        val tableName = another.kronosTableName()
        t1.afterFilter {
            criteriaParamMap = paramMap
            on(t1)
            joinables.add(KJoinable(tableName, JoinType.RIGHT_JOIN, criteria, another.kronosLogicDelete()))
        }
    }

    /**
     * Performs a cross join operation between two tables.
     *
     * @param another The table to join with.
     * @param on The condition for the join.
     * @throws NeedFieldsException If the `on` parameter is null.
     */
    inline fun <reified T : KPojo> crossJoin(another: T, noinline on: ToFilter<T1, Boolean?>) {
        if (null == on) throw NeedFieldsException()
        val tableName = another.kronosTableName()
        t1.afterFilter {
            criteriaParamMap = paramMap
            on(t1)
            joinables.add(KJoinable(tableName, JoinType.CROSS_JOIN, criteria, another.kronosLogicDelete()))
        }
    }

    /**
     * Performs an inner join operation between two tables.
     *
     * @param another The table to join with.
     * @param on The condition for the join.
     * @throws NeedFieldsException If the `on` parameter is null.
     */
    inline fun <reified T : KPojo> innerJoin(another: T, noinline on: ToFilter<T1, Boolean?>) {
        if (null == on) throw NeedFieldsException()
        val tableName = another.kronosTableName()
        t1.afterFilter {
            criteriaParamMap = paramMap
            on(t1)
            joinables.add(KJoinable(tableName, JoinType.INNER_JOIN, criteria, another.kronosLogicDelete()))
        }
    }

    /**
     * Performs a full join operation between two tables.
     *
     * @param another The table to join with.
     * @param on The condition for the join.
     * @throws NeedFieldsException If the `on` parameter is null.
     */
    inline fun <reified T : KPojo> fullJoin(another: T, noinline on: ToFilter<T1, Boolean?>) {
        if (null == on) throw NeedFieldsException()
        val tableName = another.kronosTableName()
        t1.afterFilter {
            criteriaParamMap = paramMap
            on(t1)
            joinables.add(KJoinable(tableName, JoinType.FULL_JOIN, criteria, another.kronosLogicDelete()))
        }
    }

    /**
     * Selects the specified fields from the table associated with the given KTableField.
     *
     * @param someFields The KTableField representing the fields to be selected.
     */
    @Suppress("UNCHECKED_CAST")
    fun select(someFields: ToSelect<T1, Any?>) {
        if (null == someFields) return

        pojo.afterSelect {
            someFields(t1)
            selectFields += fields
            fields.forEach { field ->
                val safeKey = ConditionSqlBuilder.getSafeKey(
                    field.name,
                    keyCounters,
                    selectFieldsWithNames as MutableMap<String, Any?>,
                    field
                )
                selectFieldsWithNames[safeKey] = field
            }
        }
    }

    fun db(vararg databaseOfTables: Pair<KPojo, String>) {
        databaseOfTables.forEach {
            databaseOfTable[it.first.kronosTableName()] = it.second
        }
    }

    fun cascade(vararg props: KProperty<*>, enabled: Boolean = true) {
        cascadeEnabled = enabled
        cascadeAllowed = props
    }

    /**
     * Orders the result set by the specified fields.
     *
     * @param someFields The fields to order the result set by.
     * @throws NeedFieldsException If the `someFields` parameter is null.
     */
    fun orderBy(someFields: ToSort<T1, Any?>) {
        if (someFields == null) throw NeedFieldsException()

        orderEnabled = true
        pojo.afterSort {
            someFields(t1)// 在这里对排序操作进行封装，为后续的链式调用提供支持。
            orderByFields = sortedFields.toLinkedSet()
        }
    }

    /**
     * Sets the groupBy flag to true and checks if the `someFields` parameter is null.
     * If it is null, throws a NeedFieldsException.
     *
     * @param someFields The fields to group the result set by.
     * @throws NeedFieldsException If the `someFields` parameter is null.
     */
    fun groupBy(someFields: ToSelect<T1, Any?>) {
        groupEnabled = true
        // 检查 someFields 参数是否为空，如果为空则抛出异常
        if (null == someFields) throw NeedFieldsException()
        pojo.afterSelect {
            someFields(t1)
            // 设置分组字段
            groupByFields = fields.toLinkedSet()
        }
    }

    /**
     * Sets the distinctEnabled flag to true, indicating that the result set should be distinct.
     */
    fun distinct() {
        this.distinctEnabled = true
    }

    /**
     * Sets the limit flag to true and sets the limit capacity to the specified number.
     *
     * @param num the number of records to limit the result set to
     */
    fun limit(num: Int) {
        this.limitCapacity = num
    }

    /**
     * Sets the page information for the query, enabling pagination.
     *
     * @param pi the current page number, indicating which page of data to retrieve
     * @param ps the number of records per page, specifying the number of records to display per page
     */
    fun page(pi: Int, ps: Int) {
        this.pageEnabled = true
        this.ps = ps
        this.pi = pi
    }

    /**
     * Executes the query logic defined in [someFields] and builds the query condition.
     *
     * @param someFields the fields to be queried
     * @throws NeedFieldsException if [someFields] is null
     */
    fun by(someFields: ToSelect<T1, Any?>) {
        // 检查someFields是否为空，为空则抛出异常
        if (null == someFields) throw NeedFieldsException()
        pojo.afterSelect {
            // 执行someFields中定义的查询逻辑
            someFields(t1)
            // 构建查询条件，将字段名映射到参数值，并转换为查询条件对象
            havingCondition = fields.map { it.eq(paramMap[it.name]) }.toCriteria()
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
            // 当没有提供选择条件时，构建一个查询所有字段的条件
            condition = paramMap.keys.map { propName ->
                allFields.first { it.name == propName }.eq(paramMap[propName])
            }.toCriteria()
        } else {
            pojo.afterFilter {
                criteriaParamMap = paramMap
                selectCondition(t1) // 执行用户提供的条件函数
                condition = criteria // 设置查询条件
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
     * @throws NeedFieldsException if the selectCondition parameter is null.
     */
    fun having(selectCondition: ToFilter<T1, Boolean?> = null) {
        // 检查是否提供了条件，未提供则抛出异常
        if (selectCondition == null) throw NeedFieldsException()
        havingEnabled = true // 标记为HAVING条件
        pojo.afterFilter {
            criteriaParamMap = paramMap // 设置属性参数映射
            selectCondition(t1) // 执行传入的条件函数
            havingCondition = criteria // 设置HAVING条件
        }
    }

    fun patch(vararg pairs: Pair<String, Any?>) {
        paramMap.putAll(pairs)
    }

    /**
     * Queries the data source using the provided data source wrapper and returns a list of maps representing the results.
     *
     * @param wrapper the data source wrapper to use for the query. Defaults to null. If null, the default data source wrapper is used.
     * @return a list of maps representing the results of the query.
     */
    fun query(wrapper: KronosDataSourceWrapper? = null): List<Map<String, Any>> {
        return this.build().query(wrapper)
    }

    inline fun <reified T> queryList(wrapper: KronosDataSourceWrapper? = null): List<T> {
        return this.build().queryList(wrapper)
    }

    @JvmName("queryForList")
    @Suppress("UNCHECKED_CAST")
    fun queryList(wrapper: KronosDataSourceWrapper? = null): List<T1> {
        with(this.build()) {
            beforeQuery?.invoke(this)
            val result = atomicTask.logAndReturn(
                wrapper.orDefault().forList(atomicTask, pojo::class) as List<T1>,
                QueryType.QueryList
            )
            afterQuery?.invoke(result, QueryType.QueryList, wrapper.orDefault())
            return result
        }
    }

    fun queryMap(wrapper: KronosDataSourceWrapper? = null): Map<String, Any> {
        return this.build().queryMap(wrapper)
    }

    fun queryMapOrNull(wrapper: KronosDataSourceWrapper? = null): Map<String, Any>? {
        return this.build().queryMapOrNull(wrapper)
    }

    inline fun <reified T> queryOne(wrapper: KronosDataSourceWrapper? = null): T {
        return this.build().queryOne(wrapper)
    }

    @JvmName("queryForObject")
    @Suppress("UNCHECKED_CAST")
    fun queryOne(wrapper: KronosDataSourceWrapper? = null): T1 {
        with(this.build()) {
            beforeQuery?.invoke(this)
            val result = atomicTask.logAndReturn(
                (wrapper.orDefault().forObject(atomicTask, pojo::class)
                    ?: throw NullPointerException("No such record")) as T1,
                QueryType.QueryOne
            )
            afterQuery?.invoke(result, QueryType.QueryOne, wrapper.orDefault())
            return result
        }
    }

    inline fun <reified T> queryOneOrNull(wrapper: KronosDataSourceWrapper? = null): T? {
        return this.build().queryOneOrNull(wrapper)
    }

    @JvmName("queryForObjectOrNull")
    @Suppress("UNCHECKED_CAST")
    fun queryOneOrNull(wrapper: KronosDataSourceWrapper? = null): T1? {
        with(this.build()) {
            beforeQuery?.invoke(this)
            val result = atomicTask.logAndReturn(
                wrapper.orDefault().forObject(atomicTask, pojo::class) as T1?,
                QueryType.QueryOneOrNull
            )
            afterQuery?.invoke(result, QueryType.QueryOneOrNull, wrapper.orDefault())
            return result
        }
    }

    /**
     * Builds and returns a KronosAtomicQueryTask object based on the provided data source wrapper.
     *
     * @param wrapper the data source wrapper to use for the query. Defaults to null. If null, the default data source wrapper is used.
     * @return a KronosAtomicQueryTask object representing the query.
     */
    override fun build(wrapper: KronosDataSourceWrapper?): KronosQueryTask {
        var buildCondition = condition

        // 初始化所有字段集合
        allFields = pojo.kronosColumns().filter { it.isColumn }.toLinkedSet()

        if (selectFields.isEmpty()) {
            selectFields += allFields
        }

        // 如果条件为空，则根据paramMap构建查询条件
        if (buildCondition == null) {
            buildCondition = paramMap.keys.filter {
                paramMap[it] != null
            }.mapNotNull { propName ->
                allFields.firstOrNull { it.name == propName }?.eq(paramMap[propName])
            }.toCriteria()
        }

        // 设置逻辑删除的条件
        if (logicDeleteStrategy.enabled) setCommonStrategy(logicDeleteStrategy) { _, value ->
            buildCondition = listOfNotNull(
                buildCondition,
                "${quote(wrapper.orDefault(), logicDeleteStrategy.field, true, databaseOfTable)} = $value".asSql()
            ).toCriteria()
        }

        // 如果存在额外的最后条件，则将其添加到查询条件中
        if (lastCondition != null) {
            buildCondition = listOfNotNull(
                buildCondition, lastCondition
            ).toCriteria()
        }

        val paramMapNew = mutableMapOf<String, Any?>()
        val sql = getJoinSql(wrapper.orDefault(), toJoinClauseInfo(wrapper, buildCondition) {
            paramMapNew.putAll(it.filter { entry -> null != entry.value })
        })

        // 返回构建好的KronosAtomicTask对象
        return CascadeJoinClause.build(
            cascadeEnabled, cascadeAllowed, listOfPojo, KronosAtomicQueryTask(
                sql, paramMapNew, operationType = KOperationType.SELECT
            ), operationType, selectFieldsWithNames, cascadeSelectedProps ?: mutableSetOf()
        )
    }

    private fun toJoinClauseInfo(
        wrapper: KronosDataSourceWrapper? = null,
        buildCondition: Criteria?,
        updateMap: (map: MutableMap<String, Any?>) -> Unit
    ): JoinClauseInfo {
        val (whereClauseSql, mapOfWhere) = buildConditionSqlWithParams(
            KOperationType.SELECT,
            wrapper,
            buildCondition,
            showTable = true,
            databaseOfTable = databaseOfTable
        ).toWhereClause()

        val joinSql = " " + joinables.joinToString(" ") {
            var joinCondition = it.condition
            if (it.logicDeleteStrategy.enabled) setCommonStrategy(it.logicDeleteStrategy) { _, value ->
                joinCondition = listOfNotNull(
                    joinCondition,
                    "${
                        quote(
                            wrapper.orDefault(),
                            it.logicDeleteStrategy.field,
                            true,
                            databaseOfTable
                        )
                    } = $value".asSql()
                ).toCriteria()
            }

            val (onSql, mapOfOn) = buildConditionSqlWithParams(
                KOperationType.SELECT,
                wrapper,
                joinCondition,
                paramMap,
                showTable = true,
                databaseOfTable = databaseOfTable
            )
                .toOnClause()
            updateMap(mapOfOn)

            it.joinType.value + " " + quote(wrapper.orDefault(), it.tableName, true, map = databaseOfTable) + onSql
        }

        val groupByClauseSql =
            if (groupEnabled && groupByFields.isNotEmpty()) " GROUP BY " + (groupByFields.joinToString(", ") {
                quote(wrapper.orDefault(), it, true, databaseOfTable)
            }) else null
        val orderByClauseSql =
            if (orderEnabled && orderByFields.isNotEmpty()) " ORDER BY " + orderByFields.joinToString(", ") {
                if (it.first.type == CUSTOM_CRITERIA_SQL) it.first.toString() else quote(
                    wrapper.orDefault(),
                    it.first,
                    true,
                    databaseOfTable
                ) + " " + it.second
            } else null
        val (havingClauseSql, mapOfHaving) = if (havingEnabled) buildConditionSqlWithParams(
            KOperationType.SELECT, wrapper, havingCondition, showTable = true, databaseOfTable = databaseOfTable
        ).toHavingClause() else null to mutableMapOf()
        updateMap(mapOfWhere)
        updateMap(mapOfHaving)
        return JoinClauseInfo(
            tableName,
            selectFieldsWithNames.toList(),
            distinctEnabled,
            pageEnabled,
            pi,
            ps,
            limitCapacity,
            databaseOfTable,
            whereClauseSql,
            groupByClauseSql,
            orderByClauseSql,
            havingClauseSql,
            joinSql
        )
    }
}