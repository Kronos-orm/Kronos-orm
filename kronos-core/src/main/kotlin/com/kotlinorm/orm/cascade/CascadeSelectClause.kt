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

object CascadeSelectClause {
    fun <T : KPojo> build(
        cascade: Boolean,
        limit: Int,
        pojo: T,
        rootTask: KronosAtomicQueryTask,
        selectFields: LinkedHashSet<Field>
    ): KronosQueryTask {
        if (!cascade || limit == 0) return rootTask.toKronosQueryTask()
        return generateQueryTask(limit, pojo, pojo.kronosColumns().filter { selectFields.contains(it) }, rootTask)
    }

    @Suppress("UNCHECKED_CAST")
    private fun generateQueryTask(
        limit: Int,
        pojo: KPojo,
        columns: List<Field>,
        prevTask: KronosAtomicQueryTask
    ): KronosQueryTask {
        val validReferences =
            findValidRefs(
                columns, KOperationType.SELECT
            ) // 获取所有的非数据库列、有关联注解且用于删除操作
        return prevTask.toKronosQueryTask().apply {
            // 若没有关联信息，返回空（在deleteClause的build中，有对null值的判断和默认值处理）
            // 为何不直接返回deleteTask: 因为此处的deleteTask构建sql语句时带有表名，而普通的deleteTask不带表名，因此需要重新构建
            if (validReferences.isNotEmpty()) {
                doAfterQuery { queryType, wrapper ->
                    validReferences.forEach { validRef ->
                        val prop =
                            pojo::class.findPropByName(validRef.field.name) // 获取级联字段的属性如：GroupClass.students

                        when (queryType) {
                            QueryList -> { // 若是查询KPojo列表
                                val lastStepResult = this as List<KPojo> // this为主表查询的结果
                                lastStepResult.forEach rowMapper@{
                                    setValues(it, prop, validRef, limit, wrapper)
                                }
                            }

                            QueryOne, QueryOneOrNull -> {
                                setValues(this as KPojo, prop, validRef, limit, wrapper)
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