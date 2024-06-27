package com.kotlinorm.orm.insert

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.beans.task.KronosActionTask
import com.kotlinorm.beans.task.KronosActionTask.Companion.merge
import com.kotlinorm.beans.task.KronosActionTask.Companion.toKronosActionTask
import com.kotlinorm.beans.task.KronosAtomicActionTask
import com.kotlinorm.enums.KOperationType
import kotlin.reflect.full.createInstance

object CascadeInsertClause {
    fun <T : KPojo> build(pojo: T, rootTask: KronosAtomicActionTask): KronosActionTask {
        val columns = pojo.kronosColumns() // get all columns of the pojo
        return generateTask(
            pojo.toDataMap(),
            columns,
            rootTask
        )
    }

    /**
     * Generates a list of KronosAtomicActionTask for each column in the provided list.
     *
     * This function iterates over the provided list of columns and for each column, it checks if the data value associated with the column name in the data map is null or empty.
     * If the data value is not null or empty, it creates a list of data from the data value.
     * Then, it creates a referenced POJO from the class name specified in the column.
     * If the column has a cascade mapper or any of the columns in the referenced POJO has a cascade mapper for the table of the column, and the column is used for insert operations,
     * it generates a list of KronosAtomicActionTask for the list of data and adds it to the list of tasks.
     *
     * @param dataMap Map<String, Any?> the map of data values associated with the column names.
     * @param columns List<Field> the list of columns for which to generate tasks.
     * @return List<KronosAtomicActionTask> returns a list of KronosAtomicActionTask objects representing the tasks for the columns.
     * @throws UnsupportedOperationException if the reference class is not supported.
     */
    @Suppress("UNCHECKED_CAST")
    private fun generateTask(
        dataMap: Map<String, Any?>,
        columns: List<Field>,
        prevTask: KronosAtomicActionTask
    ): KronosActionTask {
        return prevTask.toKronosActionTask().doAfterExecute { //在执行之后执行的操作
            //为何要放在doAfterExecute中执行：因为子插入任务需要等待父插入任务执行完毕，才能获取到父插入任务的主键值（若使用了自增主键）
            columns.filter { !it.isColumn }.mapNotNull { col ->
                val dataVal = dataMap[col.name] // 获取KPojo或者Collection<KPojo>的数据值，有值才需要级联插入
                if (dataVal == null || (dataVal is Collection<*> && dataVal.isEmpty())) return@mapNotNull null // 数据值为空，不需要级联插入
                val listOfData = if (dataVal is Collection<*>) { // 数据值是集合
                    dataVal.toList() // 转换为列表
                } else { // 数据值是单个对象
                    listOf(dataVal) // 转换为列表
                } as List<KPojo>

                val ref = Class.forName(
                    col.referenceKClassName
                        ?: throw UnsupportedOperationException("The reference class is not supported!")
                ).kotlin.createInstance() as KPojo // 创建引用的POJO对象

                val cascadeRefs = ref.kronosColumns()
                    .filter { it.cascadeMapperBy(col.tableName) && it.refUseFor(KOperationType.INSERT) } // 获取引用的POJO对象中级联引用的列

                return@mapNotNull if ((col.cascadeMapperBy() && col.refUseFor(KOperationType.INSERT)) || cascadeRefs.isNotEmpty()
                ) { // 如果当前列有级联映射器，或者引用的POJO对象中有级联映射器才需要级联插入
                    listOfData.map nextPojo@{ refPojo ->
                        val updateFactory: (Map<String, Any?>) -> Map<Field, Any?> =
                            { paramMap -> //要先获取到父插入任务的paramMap，再去构建sql语句
                                val toUpdate = mutableMapOf<Field, Any?>()
                                val references = (cascadeRefs.map { it.reference } + col.reference) // 获取引用的POJO对象中的引用
                                references.forEach { reference -> // 遍历引用
                                    reference?.targetColumns?.forEachIndexed { indexOfTargetField, target -> // 遍历引用的POJO对象中的列
                                        val targetField =
                                            ref.kronosColumns()
                                                .find { o -> o.columnName == reference.referenceColumns[indexOfTargetField] } // 获取引用的POJO对象中的列
                                        val refField =
                                            columns.find { o -> o.columnName == target } // 获取引用的POJO对象中的列
                                        if (refField != null && targetField != null) { // 列不为空
                                            if (paramMap[targetField.name] != null) return@forEach // 如果引用的POJO对象中的列已经有值了，就不需要再传递值了（避免覆盖已有值的情况）
                                            if (refField.identity) { // 如果引用的POJO对象中的列是自增主键
                                                toUpdate[targetField] = dataMap[refField.name]
                                                    ?: lastInsertId // 将当前列的值传递给引用的POJO对象, 如果当前列的值为空，就传递上一次插入的主键值
                                            } else {
                                                toUpdate[targetField] = dataMap[refField.name] // 将当前列的值传递给引用的POJO对象
                                            }
                                        }
                                    }
                                }
                                toUpdate
                            }
                        val toUpdateFields = updateFactory(refPojo.toDataMap()) // 获取要更新值的引用列
                        val task = refPojo.insert().apply {
                            toUpdateFields.forEach { // 将引用的POJO对象中的列的值传递给引用的POJO对象
                                updateInsertFields(it.key, it.value)
                            }
                        }.build().atomicTasks.first() // 生成插入任务
                        return@nextPojo build(refPojo, task)
                    }.merge()
                } else {
                    null
                }

            }.merge()
        }
    }
}