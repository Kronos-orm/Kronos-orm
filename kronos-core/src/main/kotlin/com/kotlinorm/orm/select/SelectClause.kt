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
import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.beans.dsl.KSelectable
import com.kotlinorm.beans.dsl.KTable.Companion.tableRun
import com.kotlinorm.beans.dsl.KTableConditional.Companion.conditionalRun
import com.kotlinorm.beans.dsl.KTableSortable.Companion.sortableRun
import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.KColumnType.CUSTOM_CRITERIA_SQL
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.enums.SortType
import com.kotlinorm.exceptions.NeedFieldsException
import com.kotlinorm.exceptions.UnsupportedDatabaseTypeException
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.pagination.PagedClause
import com.kotlinorm.types.KTableConditionalField
import com.kotlinorm.types.KTableField
import com.kotlinorm.types.KTableSortableField
import com.kotlinorm.utils.*
import com.kotlinorm.utils.DataSourceUtil.orDefault
import com.kotlinorm.utils.Extensions.asSql
import com.kotlinorm.utils.Extensions.eq
import com.kotlinorm.utils.Extensions.toCriteria

class SelectClause<T : KPojo>(
    override val pojo: T, setSelectFields: KTableField<T, Any?> = null
) : KSelectable<T>(pojo) {
    private var tableName = pojo.kronosTableName()
    private var paramMap = pojo.toDataMap()
    private var logicDeleteStrategy = pojo.kronosLogicDelete()
    private var allFields = pojo.kronosColumns().toLinkedSet()
    private var condition: Criteria? = null
    private var lastCondition: Criteria? = null
    private var havingCondition: Criteria? = null
    override var selectFields: LinkedHashSet<Field> = linkedSetOf()
    private var groupByFields: LinkedHashSet<Field> = linkedSetOf()
    private var orderByFields: LinkedHashSet<Pair<Field, SortType>> = linkedSetOf()
    private var limitCapacity = 0
    private var isDistinct = false
    private var isPage = false
    private var isGroup = false
    private var isHaving = false
    private var isOrder = false
    private var ps = 0
    private var pi = 0

    /**
     * 初始化函数：用于在对象初始化时配置选择字段。
     * 该函数不接受参数，也不返回任何值。
     * 它首先检查[setSelectFields]是否为非空，如果是，则调用[pojo.tableRun]块，
     * 在该块内调用[setSelectFields]方法来设置选择的字段，并将当前字段集合转换为链接集合后赋值给[selectFields]属性。
     */
    init {
        if (setSelectFields != null) {
            pojo.tableRun {
                setSelectFields(it) // 设置选择的字段
                selectFields = fields.toLinkedSet() // 将字段集合转换为不可变的链接集合并赋值给selectFields
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


    /**
     * 根据指定的字段对当前对象进行排序。
     *
     * @param someFields 可排序字段的集合，这里的字段类型为 [KTableSortableField]，单位为 [Unit]。
     *                   该参数指定了排序时所依据的字段。
     * @return 返回 [SelectClause] 对象，允许链式调用。
     */
    fun orderBy(someFields: KTableSortableField<T, Any?>): SelectClause<T> {
        if (someFields == null) throw NeedFieldsException()

        isOrder = true
        pojo.sortableRun {
            someFields(it)// 在这里对排序操作进行封装，为后续的链式调用提供支持。
            orderByFields = sortFields.toLinkedSet()
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
    fun groupBy(someFields: KTableField<T, Any?>): SelectClause<T> {
        isGroup = true
        // 检查 someFields 参数是否为空，如果为空则抛出异常
        if (someFields == null) throw NeedFieldsException()
        pojo.tableRun {
            someFields(it)
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
        isDistinct = true // 标记为Distinct，去除结果中的重复项
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
        isPage = true
        this.ps = ps
        this.pi = pi
        return this
    }

    /**
     * 根据指定的字段构建查询条件，并返回SelectClause实例。
     *
     * @param someFields KTableField类型，表示要用来构建查询条件的字段。
     *                   不能为空，否则会抛出NeedFieldsException异常。
     * @return 返回当前SelectClause实例，允许链式调用。
     */
    fun by(someFields: KTableField<T, Any?>): SelectClause<T> {
        // 检查someFields是否为空，为空则抛出异常
        if (someFields == null) throw NeedFieldsException()
        pojo.tableRun { t ->
            // 执行someFields中定义的查询逻辑
            someFields(t)
            // 构建查询条件，将字段名映射到参数值，并转换为查询条件对象
            havingCondition = fields.map { it.eq(paramMap[it.name]) }.toCriteria()
        }
        return this // 返回当前SelectClause实例，允许链式调用
    }


    /**
     * 根据提供的选择条件构建查询条件。
     *
     * @param selectCondition 一个函数，用于定义条件查询。该函数接收一个 [KTableConditionalField] 类型的参数，
     *                        并返回一个 [Boolean]? 类型的值，用于指定条件是否成立。如果为 null，则表示选择所有字段。
     * @return [SelectClause] 的实例，代表了一个查询的选择子句。
     */
    fun where(selectCondition: KTableConditionalField<T, Boolean?> = null): SelectClause<T> {
        if (selectCondition == null) return this.apply {
            // 当没有提供选择条件时，构建一个查询所有字段的条件
            condition = paramMap.keys.map { propName ->
                allFields.first { it.name == propName }.eq(paramMap[propName])
            }.toCriteria()
        }
        pojo.conditionalRun {
            propParamMap = paramMap
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
    fun having(selectCondition: KTableConditionalField<T, Boolean?> = null): SelectClause<T> {
        isHaving = true // 标记为HAVING条件
        // 检查是否提供了条件，未提供则抛出异常
        if (selectCondition == null) throw NeedFieldsException()
        pojo.conditionalRun {
            propParamMap = paramMap // 设置属性参数映射
            selectCondition(it) // 执行传入的条件函数
            havingCondition = criteria // 设置HAVING条件
        }
        return this // 允许链式调用
    }

    fun withTotal(): PagedClause<T, SelectClause<T>> {
        return PagedClause(this)
    }

    /**
     * 构建一个KronosAtomicTask对象。
     *
     * 该方法主要用于根据提供的KronosDataSourceWrapper（如果存在）和其他参数构建一个用于执行数据库操作的KronosAtomicTask对象。
     * 这包括构建SQL查询语句及其参数映射，配置逻辑删除策略，并根据不同的标志（如分页、去重、分组等）调整查询语句的构造。
     *
     * @param wrapper 可选的KronosDataSourceWrapper对象，用于提供数据库表信息等。
     * @return 构建好的KronosAtomicTask对象，包含了完整的SQL查询语句和对应的参数映射。
     */
    override fun build(wrapper: KronosDataSourceWrapper?): KronosAtomicQueryTask {
        var buildCondition = condition
        // 初始化所有字段集合
        allFields = pojo.kronosColumns().toLinkedSet()

        if (selectFields.isEmpty()) {
            selectFields += allFields.filter { it.isColumn }
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
                buildCondition, "${logicDeleteStrategy.field.quoted()} = $value".asSql()
            ).toCriteria()
        }

        // 如果存在额外的最后条件，则将其添加到查询条件中
        if (lastCondition != null) {
            buildCondition = listOfNotNull(
                buildCondition, lastCondition
            ).toCriteria()
        }

        // 构建带有参数的查询条件SQL
        val (whereClauseSql, paramMap) = ConditionSqlBuilder.buildConditionSqlWithParams(buildCondition, mutableMapOf())
            .toWhereClause()

        // 检查并设置是否使用去重（DISTINCT）
        val selectKeyword = if (isDistinct) "SELECT DISTINCT" else "SELECT"

        //检查是否设置排序
        val orderByKeywords = if (isOrder && orderByFields.isNotEmpty()) "ORDER BY " +
                orderByFields.joinToString(", ") {
                    if (it.first.type == "string") it.first.toString() else it.first.quoted() +
                            " " + it.second
                } else null

        // 检查并设置是否分组
        val groupByKeyword = if (isGroup) "GROUP BY " + (groupByFields.takeIf { it.isNotEmpty() }
            ?.joinToString(", ") { it.quoted() }) else null

        // 检查并设置是否使用HAVING条件
        val havingKeyword = if (isHaving) "HAVING " + (havingCondition.let {
            it?.children?.joinToString(" AND ") { it?.field?.equation().toString() }
        }) else null

        // 如果分页，则将分页参数添加到SQL中
        var limitedPrefix: String? = null
        var limitedSuffix: String? = null
        if (isPage) when (wrapper.orDefault().dbType) {
            DBType.Mysql, DBType.SQLite, DBType.Postgres -> limitedSuffix = "LIMIT $ps OFFSET ${ps * (pi - 1)}"
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
            selectFields.joinToString(", ") {
                it.let {
                    when {
                        it.type == CUSTOM_CRITERIA_SQL -> it.toString()
                        it.name != it.columnName -> "${it.quoted()} AS `$it`"
                        else -> it.quoted()
                    }
                }
            },
            "FROM `$tableName`",
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

    inline fun <reified T> queryList(wrapper: KronosDataSourceWrapper? = null): List<T> {
        return this.build().queryList(wrapper)
    }

    @JvmName("queryForList")
    @Suppress("UNCHECKED_CAST")
    fun queryList(wrapper: KronosDataSourceWrapper? = null): List<T> {
        return this.build().let {
            wrapper.orDefault().forList(it, pojo::class)
        } as List<T>
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
    fun queryOne(wrapper: KronosDataSourceWrapper? = null): T {
        return this.build().let {
            it.doTaskLog()
            wrapper.orDefault().forObject(it, pojo::class) ?: throw NullPointerException("No such record")
        } as T
    }

    inline fun <reified T> queryOneOrNull(wrapper: KronosDataSourceWrapper? = null): T? {
        return this.build().queryOneOrNull(wrapper)
    }

    @JvmName("queryForObjectOrNull")
    @Suppress("UNCHECKED_CAST")
    fun queryOneOrNull(wrapper: KronosDataSourceWrapper? = null): T? {
        return this.build().let {
            it.doTaskLog()
            wrapper.orDefault().forObject(it, pojo::class)
        } as T?
    }

    companion object {

        fun <T : KPojo> List<SelectClause<T>>.by(someFields: KTableField<T, Any?>): List<SelectClause<T>> {
            return map { it.by(someFields) }
        }

        /**
         * Applies the `where` operation to each update clause in the list based on the provided update condition.
         *
         * @param updateCondition the condition for the update clause. Defaults to null.
         * @return a list of UpdateClause objects with the updated condition
         */
        fun <T : KPojo> List<SelectClause<T>>.where(selectCondition: KTableConditionalField<T, Boolean?> = null): List<SelectClause<T>> {
            return map { it.where(selectCondition) }
        }

        /**
         * Builds a KronosAtomicBatchTask from a list of UpdateClause objects.
         *
         * @param T The type of KPojo objects in the list.
         * @return A KronosAtomicBatchTask object with the SQL and parameter map array from the UpdateClause objects.
         */
        fun <T : KPojo> List<SelectClause<T>>.build(): KronosAtomicBatchTask {
            val tasks = this.map { it.build() }
            return KronosAtomicBatchTask(
                sql = tasks.first().sql,
                paramMapArr = tasks.map { it.paramMap }.toTypedArray(),
                operationType = KOperationType.SELECT
            )
        }

        fun <T : KPojo> List<SelectClause<T>>.query(wrapper: KronosDataSourceWrapper? = null): List<List<Map<String, Any>>> {
            return map { it.query(wrapper) }
        }

        inline fun <reified T : KPojo> List<SelectClause<T>>.queryList(wrapper: KronosDataSourceWrapper? = null): List<List<T>> {
            return map { it.queryList<T>(wrapper) }
        }

        fun <T : KPojo> List<SelectClause<T>>.queryMap(wrapper: KronosDataSourceWrapper? = null): List<Map<String, Any>> {
            return map { it.queryMap(wrapper) }
        }

        fun <T : KPojo> List<SelectClause<T>>.queryMapOrNull(wrapper: KronosDataSourceWrapper? = null): List<Map<String, Any>?> {
            return map { it.queryMapOrNull(wrapper) }
        }

        inline fun <reified T : KPojo> List<SelectClause<T>>.queryOne(wrapper: KronosDataSourceWrapper? = null): List<T> {
            return map { it.queryOne(wrapper) }
        }

        inline fun <reified T : KPojo> List<SelectClause<T>>.queryOneOrNull(wrapper: KronosDataSourceWrapper? = null): List<T?> {
            return map { it.queryOneOrNull(wrapper) }
        }
    }
}