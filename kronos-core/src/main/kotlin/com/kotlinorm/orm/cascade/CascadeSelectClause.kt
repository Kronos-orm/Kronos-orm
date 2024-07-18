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
    ) =
        if (cascade && limit != 0) generateTask(
            limit,
            pojo.kronosColumns().filter { selectFields.contains(it) },
            rootTask
        ) else rootTask.toKronosQueryTask()

    @Suppress("UNCHECKED_CAST")
    private fun generateTask(
        limit: Int,
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
     * @param limit The maximum depth of cascading. A limit of 0 indicates no further cascading should occur.
     * @param wrapper A [KronosDataSourceWrapper] providing access to the data source for executing queries.
     */
    fun setValues(
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