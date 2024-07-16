package com.kotlinorm.orm.cascade

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.beans.task.KronosQueryTask
import com.kotlinorm.beans.task.KronosQueryTask.Companion.toKronosQueryTask
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.enums.QueryType.*
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.select.select
import com.kotlinorm.utils.Extensions.patchTo
import kotlin.reflect.KMutableProperty

/**
 * 用于构建级联选择子句的对象。
 * 该对象提供了一种方式来生成针对KPojo对象的级联查询任务。
 */

object CascadeJoinClause {
    fun build(
        cascade: Boolean,
        limit: Int,
        listOfPojo: List<KPojo>,
        rootTask: KronosAtomicQueryTask,
        selectFields: MutableMap<String, Field>
    ) =
        if (cascade && limit != 0) generateTask(
            limit,
            listOfPojo.map { it.kronosColumns().filter { col -> selectFields.values.contains(col) } },
            rootTask
        ) else rootTask.toKronosQueryTask()

    @Suppress("UNCHECKED_CAST")
    private fun generateTask(
        limit: Int,
        listOfColumns: List<List<Field>>,
        prevTask: KronosAtomicQueryTask
    ): KronosQueryTask {
        val listOfValidReferences = listOfColumns.map { columns ->
            findValidRefs(
                columns, KOperationType.SELECT
            ) // 获取所有的非数据库列、有关联注解且用于删除操作
        }
        val validReferences = listOfValidReferences.flatten()
        return prevTask.toKronosQueryTask().apply {
            // 若没有关联信息，返回空（在deleteClause的build中，有对null值的判断和默认值处理）
            // 为何不直接返回deleteTask: 因为此处的deleteTask构建sql语句时带有表名，而普通的deleteTask不带表名，因此需要重新构建
            if (validReferences.isNotEmpty()) {
                doAfterQuery { queryType, wrapper ->
                    validReferences.forEach { validRef ->
                        when (queryType) {
                            QueryList -> { // 若是查询KPojo列表
                                val lastStepResult = this as List<KPojo> // this为主表查询的结果
                                if (lastStepResult.isNotEmpty()) {
                                    val prop =
                                        lastStepResult.first()::class.findPropByName(validRef.field.name) // 获取级联字段的属性如：GroupClass.students
                                    lastStepResult.forEach rowMapper@{
                                        setValues(it, prop, validRef, limit, wrapper)
                                    }
                                }
                            }

                            QueryOne, QueryOneOrNull -> {
                                val lastStepResult = this as KPojo? // this为主表查询的结果
                                if (lastStepResult != null) {
                                    val prop =
                                        lastStepResult::class.findPropByName(validRef.field.name) // 获取级联字段的属性如：GroupClass.students
                                    setValues(lastStepResult, prop, validRef, limit, wrapper)
                                }
                            }

                            else -> {}
                        }
                    }
                }
            }
        }
    }

    private fun setValues(
        pojo: KPojo,
        prop: KMutableProperty<*>,
        validRef: ValidRef,
        limit: Int,
        wrapper: KronosDataSourceWrapper
    ) { // 将KPojo转为Map，该map将用于级联查询
        val dataMap = pojo.toDataMap()
        val listOfPair = validRef.reference.targetFields.mapIndexed { index, targetColumn ->
            val targetColumnValue = dataMap[targetColumn] ?: return
            val originalColumn = validRef.reference.referenceFields[index]
            originalColumn to targetColumnValue
        }
        val refPojo = validRef.refPojo.patchTo(
            validRef.refPojo::class,
            *listOfPair.toTypedArray()
        ) // 通过反射创建引用的类的POJO，支持类型为KPojo/Collections<KPojo>，将级联需要用到的字段填充

        pojo[prop] = if (prop.isIterable) { // 判断属性是否为集合
            refPojo.select().cascade(true, limit - 1).queryList(wrapper) // 查询级联的POJO
        } else {
            refPojo.select().cascade(true, limit - 1).queryOneOrNull(wrapper) // 查询级联的POJO
        }

    }
}