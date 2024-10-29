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
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.beans.task.KronosQueryTask
import com.kotlinorm.beans.task.KronosQueryTask.Companion.toKronosQueryTask
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.enums.QueryType.QueryOne
import com.kotlinorm.enums.QueryType.QueryOneOrNull
import com.kotlinorm.enums.QueryType.QueryList
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.select.select
import com.kotlinorm.utils.Extensions.patchTo

/**
 * Used to build a cascade select clause.
 *
 * This object is used to construct a cascade select clause for a database operation.
 *
 * 构建级联查询子句
 *
 * 该对象用于为数据库操作构建级联查询子句
 */
object CascadeSelectClause {
    /**
     * Build a cascade select clause.
     *
     * 构建级联查询子句
     *
     * @param cascade Whether the cascade is enabled.
     * @param cascadeAllowed The properties that are allowed to cascade.
     * @param pojo The POJO to select.
     * @param rootTask The root task.
     * @param selectFields The fields to select.
     * @param operationType The operation type， the cascade operation type, may be Query, Delete, Update(Delete and Update operations need to cascade query)
     * @param cascadeSelectedProps The properties that have been selected for cascading, preventing infinite recursion.
     * @return A KronosQueryTask object representing the cascade select operation.
     */
    fun <T : KPojo> build(
        cascade: Boolean,
        cascadeAllowed: Set<Field>? = null,
        pojo: T,
        rootTask: KronosAtomicQueryTask,
        selectFields: LinkedHashSet<Field>,
        operationType: KOperationType,
        cascadeSelectedProps: Set<Field>
    ) = if (cascade) generateTask(
        cascadeAllowed,
        pojo,
        pojo.kronosColumns().filter { selectFields.contains(it) },
        operationType,
        rootTask,
        cascadeSelectedProps
    ) else rootTask.toKronosQueryTask()

    /**
     * Generate a task for the cascade select operation.
     *
     * 为级联查询操作生成任务
     *
     * @param cascadeAllowed The properties that are allowed to cascade.
     * @param pojo The POJO to select.
     * @param columns The columns to select.
     * @param operationType The operation type.
     * @param prevTask The previous task.
     * @param cascadeSelectedProps The properties that have been selected for cascading, preventing infinite recursion.
     * @return A KronosQueryTask object representing the cascade select operation.
     */
    @Suppress("UNCHECKED_CAST")
    private fun generateTask(
        cascadeAllowed: Set<Field>?,
        pojo: KPojo,
        columns: List<Field>,
        operationType: KOperationType,
        prevTask: KronosAtomicQueryTask,
        cascadeSelectedProps: Set<Field>
    ): KronosQueryTask {
        val tableName = pojo.kronosTableName()
        val validCascades = findValidRefs(
            pojo::class,
            columns,
            operationType,
            cascadeAllowed?.filter { it.tableName == tableName }?.map { it.name }?.toSet(), // 获取当前Pojo内允许级联的属性
            cascadeAllowed.isNullOrEmpty() // 是否允许所有属性级联
        ) // 获取所有的非数据库列、有关联注解且用于删除操作
        return prevTask.toKronosQueryTask().apply {
            // 若没有关联信息，返回空（在deleteClause的build中，有对null值的判断和默认值处理）
            // 为何不直接返回deleteTask: 因为此处的deleteTask构建sql语句时带有表名，而普通的deleteTask不带表名，因此需要重新构建
            if (validCascades.isNotEmpty()) {
                doAfterQuery { queryType, wrapper ->
                    validCascades.forEach { validRef ->
                        when (queryType) {
                            QueryList -> { // 若是查询KPojo列表
                                val lastStepResult = this as List<KPojo> // this为主表查询的结果
                                if (lastStepResult.isNotEmpty()) {
                                    val prop = validRef.field // 获取级联字段的属性如：GroupClass.students
                                    if (!cascadeSelectedProps.contains(validRef.field)) {
                                        if (cascadeAllowed.isNullOrEmpty() || prop in cascadeAllowed) lastStepResult.forEach rowMapper@{
                                            setValues(
                                                it,
                                                prop.name,
                                                validRef,
                                                cascadeAllowed,
                                                mutableSetOf(
                                                    *cascadeSelectedProps.toTypedArray(),
                                                    *validCascades.map { cascade -> cascade.field }.toTypedArray()
                                                ),
                                                operationType,
                                                wrapper
                                            )
                                        }
                                    }
                                }
                            }

                            QueryOne, QueryOneOrNull -> {
                                val lastStepResult = this as KPojo? // this为主表查询的结果
                                if (lastStepResult != null) {
                                    val prop = validRef.field // 获取级联字段的属性如：GroupClass.students
                                    if (!cascadeSelectedProps.contains(validRef.field)) {
                                        if (cascadeAllowed.isNullOrEmpty() || prop in cascadeAllowed) {
                                            setValues(
                                                lastStepResult,
                                                prop.name,
                                                validRef,
                                                cascadeAllowed,
                                                mutableSetOf(
                                                    *cascadeSelectedProps.toTypedArray(),
                                                    *validCascades.map { cascade -> cascade.field }.toTypedArray()
                                                ),
                                                operationType,
                                                wrapper
                                            )
                                        }
                                    }
                                }
                            }

                            else -> {}
                        }
                    }
                }
            }
        }
    }

    fun setValues(
        pojo: KPojo,
        prop: String,
        validRef: ValidCascade,
        cascadeAllowed: Set<Field>?,
        cascadeSelectedProps: Set<Field>,
        operationType: KOperationType,
        wrapper: KronosDataSourceWrapper
    ) {
        // 将KPojo转为Map，该map将用于级联查询
        val dataMap = pojo.toDataMap()
        // 获取KPojo对应的表名
        val tableName = pojo.kronosTableName()

        // 获取Pair列表，用于将Map内的值填充到引用的类的POJO中
        // Pair的构建需要判断KPojo对象是ValidRef所在的表还是引用的表，然后根据不同的情况填充Pair
        val listOfPair = validRef.kCascade.targetProperties.mapIndexed { index, targetProperty ->
            if (tableName == validRef.tableName) {
                targetProperty to (dataMap[validRef.kCascade.properties[index]] ?: return)
            } else {
                validRef.kCascade.properties[index] to (dataMap[targetProperty] ?: return)
            }
        }

        // 通过反射创建引用的类的POJO，支持类型为KPojo/Collections<KPojo>，将级联需要用到的字段填充
        val refPojo = validRef.refPojo.patchTo(
            validRef.refPojo::class, *listOfPair.toTypedArray()
        )

        pojo[prop] = if (pojo.kronosColumns().first { it.name == prop }.cascadeIsCollectionOrArray) { // 判断属性是否为集合
            refPojo.select().apply {
                this.operationType = operationType
                this.cascadeAllowed = cascadeAllowed
                this.cascadeSelectedProps = cascadeSelectedProps
            }.queryList(wrapper) // 查询级联的POJO
        } else {
            refPojo.select().apply {
                this.operationType = operationType
                this.cascadeAllowed = cascadeAllowed
                this.cascadeSelectedProps = cascadeSelectedProps
            }.queryOneOrNull(wrapper) // 查询级联的POJO
        }
    }
}