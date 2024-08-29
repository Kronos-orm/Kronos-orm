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
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.select.select
import com.kotlinorm.utils.Extensions.patchTo
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.javaField

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
        cascadeAllowed: Array<out KProperty<*>>,
        pojo: T,
        rootTask: KronosAtomicQueryTask,
        selectFields: LinkedHashSet<Field>,
        operationType: KOperationType,
        cascadeSelectedProps: MutableSet<KProperty<*>>
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
        cascadeAllowed: Array<out KProperty<*>>,
        pojo: KPojo,
        columns: List<Field>,
        operationType: KOperationType,
        prevTask: KronosAtomicQueryTask,
        cascadeSelectedProps: MutableSet<KProperty<*>>
    ): KronosQueryTask {
        val validReferences = findValidRefs(
            columns,
            operationType,
            cascadeAllowed.filterReceiver(pojo::class).map { it.name }.toSet(), // 获取当前Pojo内允许级联的属性
            cascadeAllowed.isEmpty() // 是否允许所有属性级联
        ) // 获取所有的非数据库列、有关联注解且用于删除操作
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
                                    if (!cascadeSelectedProps.contains(prop)) {
                                        if (cascadeAllowed.isEmpty() || prop in cascadeAllowed) lastStepResult.forEach rowMapper@{
                                            cascadeSelectedProps.add(prop)
                                            setValues(
                                                it,
                                                prop,
                                                validRef,
                                                cascadeAllowed,
                                                cascadeSelectedProps,
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
                                    val prop =
                                        lastStepResult::class.findPropByName(validRef.field.name) // 获取级联字段的属性如：GroupClass.students
                                    if (!cascadeSelectedProps.contains(prop)) {
                                        cascadeSelectedProps.add(prop)
                                        setValues(
                                            lastStepResult,
                                            prop,
                                            validRef,
                                            cascadeAllowed,
                                            cascadeSelectedProps,
                                            operationType,
                                            wrapper
                                        )
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

    /**
     * Sets the values on a [KPojo] instance for a specified property using cascading logic.
     *
     * This function is a key component of the ORM's cascading feature, allowing for the dynamic setting of property values
     * on [KPojo] instances based on the results of cascading operations. It first converts the [KPojo] instance into a map
     * to facilitate the retrieval of values for specified target fields. These values are then used to patch a reference [KPojo]
     * instance, which is subsequently used in a cascading select operation. The result of this select operation is then
     * assigned to the specified property of the original [KPojo] instance. This process enables the ORM to automatically
     * populate properties of an object based on the relationships defined between entities.
     *
     * 使用级联逻辑设置 [KPojo] 实例上指定属性的值。
     *
     * 此功能是 ORM 级联功能的关键组件，允许根据级联操作的结果动态设置 [KPojo] 实例上的属性值。它首先将 [KPojo] 实例转换为映射，以便于检索指定目标字段的值。然后使用这些值修补引用 [KPojo] 实例，该实例随后用于级联选择操作。然后将此选择操作的结果分配给原始 [KPojo] 实例的指定属性。此过程使 ORM 能够根据实体之间定义的关系自动填充对象的属性。
     *
     * @param pojo The [KPojo] instance on which values are to be set.
     * @param prop The [KMutableProperty] representing the property to be set on the [KPojo] instance.
     * @param validRef A [ValidRef] instance containing information about the reference to be used for cascading.
     * @param cascadeAllowed The maximum depth of cascading. A limit of 0 indicates no further cascading should occur.
     * @param wrapper A [KronosDataSourceWrapper] providing access to the data source for executing queries.
     */
    fun setValues(
        pojo: KPojo,
        prop: KProperty<*>,
        validRef: ValidRef,
        cascadeAllowed: Array<out KProperty<*>>,
        cascadeSelectedProps: MutableSet<KProperty<*>>,
        operationType: KOperationType,
        wrapper: KronosDataSourceWrapper
    ) {
        // 将KPojo转为Map，该map将用于级联查询
        val dataMap = pojo.toDataMap()
        // 获取KPojo对应的表名
        val tableName = pojo.kronosTableName()

        // 获取Pair列表，用于将Map内的值填充到引用的类的POJO中
        // Pair的构建需要判断KPojo对象是ValidRef所在的表还是引用的表，然后根据不同的情况填充Pair
        val listOfPair = validRef.reference.targetFields.mapIndexed { index, targetField ->
            if (tableName == validRef.tableName) {
                targetField to (dataMap[validRef.reference.referenceFields[index]] ?: return)
            } else {
                validRef.reference.referenceFields[index] to (dataMap[targetField] ?: return)
            }
        }
        // 通过反射创建引用的类的POJO，支持类型为KPojo/Collections<KPojo>，将级联需要用到的字段填充
        val refPojo = validRef.refPojo.patchTo(
            validRef.refPojo::class, *listOfPair.toTypedArray()
        )

        pojo[prop] = if (prop.isIterable) { // 判断属性是否为集合
            refPojo.select().cascade(*cascadeAllowed).apply {
                this.operationType = operationType
                this.cascadeSelectedProps = cascadeSelectedProps
            }.queryList(wrapper) // 查询级联的POJO
        } else {
            refPojo.select().cascade(*cascadeAllowed).apply {
                this.operationType = operationType
                this.cascadeSelectedProps = cascadeSelectedProps
            }.queryOneOrNull(wrapper) // 查询级联的POJO
        }
    }
}