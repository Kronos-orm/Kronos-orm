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
import com.kotlinorm.beans.dsl.KTable.Companion.tableRun
import com.kotlinorm.beans.dsl.KTableConditional.Companion.conditionalRun
import com.kotlinorm.beans.dsl.KTableSortable.Companion.sortableRun
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.JoinType
import com.kotlinorm.enums.KColumnType.CUSTOM_CRITERIA_SQL
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.enums.SortType
import com.kotlinorm.exceptions.NeedFieldsException
import com.kotlinorm.exceptions.UnsupportedDatabaseTypeException
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.types.KTableConditionalField
import com.kotlinorm.types.KTableField
import com.kotlinorm.types.KTableSortableField
import com.kotlinorm.utils.ConditionSqlBuilder
import com.kotlinorm.utils.DataSourceUtil.orDefault
import com.kotlinorm.utils.Extensions.asSql
import com.kotlinorm.utils.Extensions.eq
import com.kotlinorm.utils.Extensions.toCriteria
import com.kotlinorm.utils.query
import com.kotlinorm.utils.setCommonStrategy
import com.kotlinorm.utils.toLinkedSet

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
    private var condition: Criteria? = null
    private var lastCondition: Criteria? = null
    private var havingCondition: Criteria? = null
    override var selectFields: LinkedHashSet<Field> = linkedSetOf()
    private var selectFieldsWithNames: MutableMap<String, Field> = mutableMapOf()
    private var keyCounters: ConditionSqlBuilder.KeyCounter = ConditionSqlBuilder.KeyCounter()
    val joinables: MutableList<KJoinable> = mutableListOf()
    private var groupByFields: LinkedHashSet<Field> = linkedSetOf()
    private var orderByFields: LinkedHashSet<Pair<Field, SortType>> = linkedSetOf()
    private var isDistinct = false
    private var isGroup = false
    private var isHaving = false
    private var isOrder = false
    private var isLimit = false
    private var isPage = false
    private var limitCapacity = 0
    private var pi = 0
    private var ps = 0

    /**
     * Performs a left join operation between two tables.
     *
     * @param another The table to join with.
     * @param on The condition for the join.
     * @throws NeedFieldsException If the `on` parameter is null.
     */
    inline fun <reified T : KPojo> leftJoin(another: T, noinline on: KTableConditionalField<T1, Boolean?>) {
        if (null == on) throw NeedFieldsException()
        val tableName = another.kronosTableName()
        t1.conditionalRun {
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
    inline fun <reified T : KPojo> rightJoin(another: T, noinline on: KTableConditionalField<T1, Boolean?>) {
        if (null == on) throw NeedFieldsException()
        val tableName = another.kronosTableName()
        t1.conditionalRun {
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
    inline fun <reified T : KPojo> crossJoin(another: T, noinline on: KTableConditionalField<T1, Boolean?>) {
        if (null == on) throw NeedFieldsException()
        val tableName = another.kronosTableName()
        t1.conditionalRun {
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
    inline fun <reified T : KPojo> innerJoin(another: T, noinline on: KTableConditionalField<T1, Boolean?>) {
        if (null == on) throw NeedFieldsException()
        val tableName = another.kronosTableName()
        t1.conditionalRun {
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
    inline fun <reified T : KPojo> fullJoin(another: T, noinline on: KTableConditionalField<T1, Boolean?>) {
        if (null == on) throw NeedFieldsException()
        val tableName = another.kronosTableName()
        t1.conditionalRun {
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
    fun select(someFields: KTableField<T1, Any?>) {
        if (null == someFields) return

        pojo.tableRun {
            someFields(t1)
            selectFields += fields
            fields.forEach { field ->
                val safeKey = ConditionSqlBuilder.getSafeKey(
                    field.columnName,
                    keyCounters,
                    selectFieldsWithNames as MutableMap<String, Any?>,
                    field
                )
                selectFieldsWithNames[safeKey] = field
            }
        }
    }

    /**
     * Orders the result set by the specified fields.
     *
     * @param someFields The fields to order the result set by.
     * @throws NeedFieldsException If the `someFields` parameter is null.
     */
    fun orderBy(someFields: KTableSortableField<T1, Any?>) {
        if (someFields == null) throw NeedFieldsException()

        isOrder = true
        pojo.sortableRun {
            someFields(t1)// 在这里对排序操作进行封装，为后续的链式调用提供支持。
            orderByFields = sortFields.toLinkedSet()
        }
    }

    /**
     * Sets the groupBy flag to true and checks if the `someFields` parameter is null.
     * If it is null, throws a NeedFieldsException.
     *
     * @param someFields The fields to group the result set by.
     * @throws NeedFieldsException If the `someFields` parameter is null.
     */
    fun groupBy(someFields: KTableField<T1, Any?>) {
        isGroup = true
        // 检查 someFields 参数是否为空，如果为空则抛出异常
        if (null == someFields) throw NeedFieldsException()
        pojo.tableRun {
            someFields(t1)
            // 设置分组字段
            groupByFields = fields.toLinkedSet()
        }
    }

    /**
     * Sets the isDistinct flag to true, indicating that the result set should be distinct.
     */
    fun distinct() {
        this.isDistinct = true
    }

    /**
     * Sets the limit flag to true and sets the limit capacity to the specified number.
     *
     * @param num the number of records to limit the result set to
     */
    fun limit(num: Int) {
        this.isLimit = true
        this.limitCapacity = num
    }

    /**
     * Sets the page information for the query, enabling pagination.
     *
     * @param pi the current page number, indicating which page of data to retrieve
     * @param ps the number of records per page, specifying the number of records to display per page
     */
    fun page(pi: Int, ps: Int) {
        this.isPage = true
        this.ps = ps
        this.pi = pi
    }

    /**
     * Executes the query logic defined in [someFields] and builds the query condition.
     *
     * @param someFields the fields to be queried
     * @throws NeedFieldsException if [someFields] is null
     */
    fun by(someFields: KTableField<T1, Any?>) {
        // 检查someFields是否为空，为空则抛出异常
        if (null == someFields) throw NeedFieldsException()
        pojo.tableRun {
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
    fun where(selectCondition: KTableConditionalField<T1, Boolean?> = null) {
        if (selectCondition == null) {
            // 当没有提供选择条件时，构建一个查询所有字段的条件
            condition = paramMap.keys.map { propName ->
                allFields.first { it.name == propName }.eq(paramMap[propName])
            }.toCriteria()
        } else {
            pojo.conditionalRun {
                propParamMap = paramMap
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
    fun having(selectCondition: KTableConditionalField<T1, Boolean?> = null) {
        // 检查是否提供了条件，未提供则抛出异常
        if (selectCondition == null) throw NeedFieldsException()
        isHaving = true // 标记为HAVING条件
        pojo.conditionalRun {
            propParamMap = paramMap // 设置属性参数映射
            selectCondition(t1) // 执行传入的条件函数
            havingCondition = criteria // 设置HAVING条件
        }
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

    /**
     * Builds and returns a KronosAtomicQueryTask object based on the provided data source wrapper.
     *
     * @param wrapper the data source wrapper to use for the query. Defaults to null. If null, the default data source wrapper is used.
     * @return a KronosAtomicQueryTask object representing the query.
     */
    override fun build(wrapper: KronosDataSourceWrapper?): KronosAtomicQueryTask {
        var buildCondition = condition

        // 初始化所有字段集合
        allFields = pojo.kronosColumns().toLinkedSet()

        if (selectFields.isEmpty()) {
            selectFields += allFields
        }

        // 如果条件为空，则根据paramMap构建查询条件
        if (buildCondition == null) {
            buildCondition = paramMap.keys.filter {
                paramMap[it] != null
            }.map { propName ->
                allFields.first { it.name == propName }.eq(paramMap[propName])
            }.toCriteria()
        }

        // 设置逻辑删除的条件
        if (logicDeleteStrategy.enabled) setCommonStrategy(logicDeleteStrategy) { _, value ->
            buildCondition = listOfNotNull(
                buildCondition, "${logicDeleteStrategy.field.quoted(true)} = $value".asSql()
            ).toCriteria()
        }

        // 如果存在额外的最后条件，则将其添加到查询条件中
        if (lastCondition != null) {
            buildCondition = listOfNotNull(
                buildCondition, lastCondition
            ).toCriteria()
        }

        // 构建带有参数的查询条件SQL
        val (whereClauseSql, paramMap) = ConditionSqlBuilder.buildConditionSqlWithParams(
            buildCondition,
            mutableMapOf(),
            showTable = true
        )
            .toWhereClause()

        val joinClauseSql = joinables.joinToString(" ") {
            var joinCondition = it.condition
            if (it.logicDeleteStrategy.enabled) setCommonStrategy(it.logicDeleteStrategy) { _, value ->
                joinCondition = listOfNotNull(
                    joinCondition, "${it.logicDeleteStrategy.field.quoted(true)} = $value".asSql()
                ).toCriteria()
            }
            listOfNotNull(
                it.joinType.value,
                "`${it.tableName}`",
                ConditionSqlBuilder.buildConditionSqlWithParams(joinCondition, paramMap, showTable = true)
                    .toOnClause().first
            ).joinToString(" ")
        }

        // 检查并设置是否使用去重（DISTINCT）
        val selectKeyword = if (isDistinct) "SELECT DISTINCT" else "SELECT"

        //检查是否设置排序
        val orderByKeywords = if (isOrder && orderByFields.isNotEmpty()) "ORDER BY " +
                orderByFields.joinToString(", ") {
                    if (it.first.type == "string") it.first.toString() else it.first.quoted(true) +
                            " " + it.second
                } else null

        // 检查并设置是否分组
        val groupByKeyword = if (isGroup) "GROUP BY " + (groupByFields.takeIf { it.isNotEmpty() }
            ?.joinToString(", ") { it.quoted(true) }) else null

        // 检查并设置是否使用HAVING条件
        val havingKeyword = if (isHaving) "HAVING " + (havingCondition.let { it ->
            it?.children?.joinToString(" AND ") { it?.field?.equation().toString() }
        }) else null

        // 如果分页，则将分页参数添加到SQL中
        var limitedPrefix: String? = null
        var limitedSuffix: String? = null
        if (isPage) when (wrapper.orDefault().dbType) {
            DBType.Mysql, DBType.SQLite, DBType.Postgres -> limitedSuffix =
                "LIMIT $ps" + " OFFSET " + "${ps * (pi - 1)}"

            DBType.Oracle -> {
                limitedPrefix = "SELECT * FROM ("
                selectFields += Field("rownum", "R")
                limitedSuffix = ") WHERE R BETWEEN ${ps * (pi - 1) + 1} AND ${ps * pi}"
            }

            DBType.Mssql -> {
                limitedSuffix = "OFFSET ${ps * (pi - 1)} ROWS FETCH NEXT ${ps * pi} ROWS ONLY"
            }

            else -> throw UnsupportedDatabaseTypeException()
        }

        //检查并设置是否使用LIMIT条件
        if (limitCapacity > 0) when (wrapper.orDefault().dbType) {

            DBType.Mysql, DBType.SQLite, DBType.Postgres -> limitedSuffix = "LIMIT $limitCapacity"
            DBType.Oracle -> {
                limitedPrefix = "SELECT * FROM ("
                selectFields += Field("rownum", "R")
                limitedSuffix = ") WHERE R <= $limitCapacity"
            }

            DBType.Mssql -> {
                limitedSuffix = "OFFSET 0 ROWS FETCH NEXT $limitCapacity ROWS ONLY"
            }

            else -> throw UnsupportedDatabaseTypeException()
        }

        // 组装最终的SQL语句
        val sql = listOfNotNull(
            limitedPrefix,
            selectKeyword,
            selectFields.joinToString(", ") { field ->
                field.let { item ->
                    if (item.type == CUSTOM_CRITERIA_SQL) field.toString()
                    else "${item.quoted(true)} AS `${selectFieldsWithNames.filterKeys { selectFieldsWithNames[it] == item }.keys.first()}`"
                }
            },
            "FROM `$tableName`",
            joinClauseSql,
            whereClauseSql,
            groupByKeyword,
            havingKeyword,
            orderByKeywords,
            limitedSuffix
        ).joinToString(" ")

        // 返回构建好的KronosAtomicTask对象
        return KronosAtomicQueryTask(
            sql,
            paramMap,
            operationType = KOperationType.SELECT
        )
    }
}