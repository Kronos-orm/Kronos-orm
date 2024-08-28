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

package com.kotlinorm.orm.cascade

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.beans.task.KronosQueryTask
import com.kotlinorm.beans.task.KronosQueryTask.Companion.toKronosQueryTask
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.enums.QueryType.*
import com.kotlinorm.orm.cascade.CascadeSelectClause.setValues
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.javaField

/**
 * Defines the logic for building and executing cascade join clauses in the context of ORM operations.
 *
 * This object encapsulates methods for constructing and executing tasks that perform cascading operations
 * on database entities. It supports operations where cascading is conditional based on the presence of
 * cascade flags and limit constraints. The primary functionality includes generating tasks for cascading
 * operations and setting values on target entities based on the results of these operations.
 */
object CascadeJoinClause {
    /**
     * Builds a task for performing cascading operations based on the provided parameters.
     *
     * This function decides whether to proceed with generating a cascading task based on the `cascade` flag
     * and the `limit` parameter. If cascading is enabled and the limit is not zero, it generates a task
     * that performs cascading operations. Otherwise, it returns the root task without modifications.
     *
     * 根据提供的参数构建执行级联操作的任务。
     *
     * 此函数根据 `cascade` 标志 和 `limit` 参数决定是否继续生成级联任务。如果启用了级联且限制不为零，它将生成执行级联操作的任务。否则，它将返回不经修改的根任务。
     *
     * @param cascade Indicates whether cascading should be performed.
     * @param cascadeAllowed The maximum depth of cascading. A limit of 0 indicates no cascading.
     * @param listOfPojo A list of [KPojo] instances on which cascading operations are to be performed.
     * @param rootTask The root [KronosAtomicQueryTask] from which cascading operations may be initiated.
     * @param selectFields A map of field names to [Field] instances that are to be considered for cascading operations.
     * @return A [KronosQueryTask] that represents the task to be executed, potentially including cascading operations.
     */
    fun build(
        cascade: Boolean,
        cascadeAllowed: Array<out KProperty<*>>,
        listOfPojo: List<KPojo>,
        rootTask: KronosAtomicQueryTask,
        selectFields: MutableMap<String, Field>
    ) =
        if (cascade) generateTask(
            cascadeAllowed,
            listOfPojo.map {
                it to it.kronosColumns().filter { col -> selectFields.values.contains(col) }
            },
            rootTask
        ) else rootTask.toKronosQueryTask()

    @Suppress("UNCHECKED_CAST")
    private fun generateTask(
        cascadeAllowed: Array<out KProperty<*>>,
        listOfColumns: List<Pair<KPojo, List<Field>>>,
        prevTask: KronosAtomicQueryTask
    ): KronosQueryTask {
        val listOfValidReferences = listOfColumns.map { columns ->
            findValidRefs(
                columns.second,
                KOperationType.SELECT,
                cascadeAllowed.filterReceiver(columns.first::class).map { it.name }.toSet()
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
                                        setValues(it, prop, validRef, cascadeAllowed, wrapper)
                                    }
                                }
                            }

                            QueryOne, QueryOneOrNull -> {
                                val lastStepResult = this as KPojo? // this为主表查询的结果
                                if (lastStepResult != null) {
                                    val prop =
                                        lastStepResult::class.findPropByName(validRef.field.name) // 获取级联字段的属性如：GroupClass.students
                                    setValues(lastStepResult, prop, validRef, cascadeAllowed, wrapper)
                                }
                            }

                            else -> {}
                        }
                    }
                }
            }
        }
    }
}