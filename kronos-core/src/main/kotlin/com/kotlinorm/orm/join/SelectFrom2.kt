package com.kotlinorm.orm.join

import com.kotlinorm.beans.dsl.Criteria
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.beans.dsl.KSelectable
import com.kotlinorm.beans.dsl.KTable.Companion.tableRun
import com.kotlinorm.beans.dsl.KTableConditional.Companion.conditionalRun
import com.kotlinorm.beans.dsl.KTableSortable.Companion.sortableRun
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.enums.SortType
import com.kotlinorm.exceptions.NeedFieldsException
import com.kotlinorm.exceptions.UnsupportedDatabaseTypeException
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.pagination.PagedClause
import com.kotlinorm.types.KTableConditionalField
import com.kotlinorm.types.KTableField
import com.kotlinorm.types.KTableSortableField
import com.kotlinorm.utils.ConditionSqlBuilder
import com.kotlinorm.utils.DataSourceUtil.orDefault
import com.kotlinorm.utils.Extensions.asSql
import com.kotlinorm.utils.Extensions.eq
import com.kotlinorm.utils.Extensions.toCriteria
import com.kotlinorm.utils.setCommonStrategy
import com.kotlinorm.utils.tableCache.TableCache.getTable
import com.kotlinorm.utils.toLinkedSet

class SelectFrom2<T1 : KPojo, T2 : KPojo>(
    var t1: T1,
    var t2: T2,
    val doAction: SelectFrom2<T1, T2>.(T1, T2) -> Unit
) : KSelectable<T1>(t1) {

    private var tableName = t1.kronosTableName()
    private var paramMap = t1.toDataMap()
    private var logicDeleteStrategy = t1.kronosLogicDelete()
    private var allFields = t1.kronosColumns().toLinkedSet()
    private var condition: Criteria? = null
    private var lastCondition: Criteria? = null
    private var havingCondition: Criteria? = null
    override var selectFields: LinkedHashSet<Field> = linkedSetOf()
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

    init {
        doAction(t1, t2)
    }

    inline fun <reified T : KPojo> leftJoin(another: T, noinline on: KTableConditionalField<Nothing, Boolean?>) {
        TODO()
    }

    inline fun <reified T : KPojo> rightJoin(another: T, noinline on: KTableConditionalField<Nothing, Boolean?>) {
        TODO()
    }

    inline fun <reified T : KPojo> crossJoin(another: T, noinline on: KTableConditionalField<Nothing, Boolean?>) {
        TODO()
    }

    inline fun <reified T : KPojo> innerJoin(another: T, noinline on: KTableConditionalField<Nothing, Boolean?>) {
        TODO()
    }

    inline fun <reified T : KPojo> fullJoin(another: T, noinline on: KTableConditionalField<Nothing, Boolean?>) {
        TODO()
    }

    fun select(someFields: KTableField<T1, Any?>) {
        if (null == someFields) return
        pojo.tableRun {
            someFields(t1)
            selectFields = fields.toLinkedSet()
        }
    }

    fun orderBy(someFields: KTableSortableField<T1, Any?>) {
        if (someFields == null) throw NeedFieldsException()

        isOrder = true
        pojo.sortableRun {
            someFields(t1)// 在这里对排序操作进行封装，为后续的链式调用提供支持。
            orderByFields = sortFields.toLinkedSet()
        }
    }

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

    fun distinct() {
        this.isDistinct = true
    }

    fun limit(num: Int) {
        this.isLimit = true
        this.limitCapacity = num
    }

    fun page(pi: Int, ps: Int) {
        this.isPage = true
        this.ps = ps
        this.pi = (pi - 1) * ps
    }

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

    fun withTotal(): PagedClause<T1, SelectFrom2<T1, T2>> {
        TODO()
    }

    fun query(): List<Map<String, Any>> {
        TODO()
    }

    operator fun component1(): String {
        TODO()
    }

    operator fun component2(): Map<String, Any?> {
        TODO()
    }

    override fun build(wrapper: KronosDataSourceWrapper?): KronosAtomicQueryTask {
        var buildCondition = condition
        // 初始化所有字段集合
        allFields = getTable(wrapper.orDefault(), tableName).columns.toLinkedSet()

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
        val (whereClauseSql, paramMap) = ConditionSqlBuilder.buildConditionSqlWithParams(buildCondition, mutableMapOf(), showTable = true)
            .toWhereClause()

        if (selectFields.isEmpty()) {
            selectFields = allFields
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
                    if (it.type == "string") it.toString()
                    else "${it.quoted(true)} AS `$it`"
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
}
