/**
 * Copyright 2022-2025 kronos-orm
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

import com.kotlinorm.Kronos
import com.kotlinorm.beans.dsl.Criteria
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.logging.log
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.beans.task.KronosQueryTask
import com.kotlinorm.beans.task.KronosQueryTask.Companion.toKronosQueryTask
import com.kotlinorm.cache.kPojoAllFieldsCache
import com.kotlinorm.enums.ConditionType
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.enums.QueryType.QueryList
import com.kotlinorm.enums.QueryType.QueryOne
import com.kotlinorm.enums.QueryType.QueryOneOrNull
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.select.select
import com.kotlinorm.utils.Extensions.patchTo
import kotlin.reflect.KClass

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
        kClass: KClass<KPojo>,
        rootTask: KronosAtomicQueryTask,
        selectFields: LinkedHashSet<Field>,
        operationType: KOperationType,
        cascadeSelectedProps: Set<Field>
    ) = if (cascade) generateTask(
        cascadeAllowed,
        pojo,
        kClass,
        kPojoAllFieldsCache[kClass]!!.filter { selectFields.contains(it) },
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
        kClass: KClass<KPojo>,
        columns: List<Field>,
        operationType: KOperationType,
        prevTask: KronosAtomicQueryTask,
        cascadeSelectedProps: Set<Field>
    ): KronosQueryTask {
        val tableName = pojo.__tableName
        val validCascades = findValidRefs(
            kClass,
            columns,
            operationType,
            cascadeAllowed?.filter { it.tableName == tableName }?.map { it.name }?.toSet(), // 获取当前Pojo内允许级联的属性
            cascadeAllowed.isNullOrEmpty() // 是否允许所有属性级联
        ) // 获取所有的非数据库列、有关联注解且用于删除操作
        return prevTask.toKronosQueryTask().apply {
            // 若没有关联信息，返回空（在deleteClause的build中，有对null值的判断和默认值处理）
            // 为何不直接返回deleteTask: 因为此处的deleteTask构建sql语句时带有表名，而普通的deleteTask不带表名，因此需要重新构建
            if (validCascades.isEmpty()) return@apply
            doAfterQuery { queryType, wrapper ->
                validCascades.forEach { validRef ->
                    when (queryType) {
                        QueryList -> { // 若是查询KPojo列表
                            val lastStepResult = this as List<KPojo> // this为主表查询的结果
                            if (lastStepResult.isEmpty()) return@forEach // 若该级联属性查询结果为空，不进行级联查询
                            val prop = validRef.field // 获取级联字段的属性如：GroupClass.students
                            if (cascadeSelectedProps.contains(validRef.field)) return@forEach // 若该级联属性未被select，不进行级联查询
                            if (!cascadeAllowed.isNullOrEmpty() && prop !in cascadeAllowed) return@forEach // 若设置了级联忽略，且该属性不在白名单内，不进行级联查询
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

                        QueryOne, QueryOneOrNull -> {
                            val lastStepResult = this as KPojo? // this为主表查询的结果
                            if (lastStepResult == null) return@forEach // 若该级联属性查询结果为空，不进行级联查询
                            val prop = validRef.field // 获取级联字段的属性如：GroupClass.students
                            if (cascadeSelectedProps.contains(validRef.field)) return@forEach // 若该级联属性未被select，不进行级联查询
                            if (!cascadeAllowed.isNullOrEmpty() && prop !in cascadeAllowed) return@forEach // 若设置了级联忽略，且该属性不在白名单内，不进行级联查询
                            setValues(
                                listOf(lastStepResult),
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

                        else -> {}
                    }
                }
            }
        }
    }

    private var cascadeFieldNotFoundWarned = mutableSetOf<String>()

    /**
     * Sets cascade values for a list of parent KPojo rows.
     *
     * For a single row or composite FK keys, performs per-row queries.
     * For multiple rows with a single FK key, optimizes into a batch `WHERE col IN (...)` query
     * to avoid the N+1 problem, then distributes results back to parent rows by FK value.
     *
     * If the cascade field is not found in the parent POJO's columns (e.g., when using DTO projections),
     * a warning is logged and the cascade is skipped.
     *
     * 为父行列表设置级联查询值。
     *
     * 单行或复合外键时逐行查询；多行且单外键时，使用 `WHERE col IN (...)` 批量查询以避免 N+1 问题，
     * 然后按外键值将子结果分配回父行。
     *
     * 若父 POJO 中未找到级联字段（如使用 DTO 投影时），记录警告并跳过级联。
     *
     * @param parentRows The list of parent KPojo instances to set cascade values on.
     * @param prop The name of the cascade property on the parent POJO.
     * @param validRef The validated cascade reference containing FK mapping info.
     * @param cascadeAllowed The set of fields allowed for cascading, null means all allowed.
     * @param cascadeSelectedProps Fields already selected for cascading (prevents infinite recursion).
     * @param operationType The operation type (SELECT, DELETE, UPDATE).
     * @param wrapper The data source wrapper for executing queries.
     */
    fun setValues(
        parentRows: List<KPojo>,
        prop: String,
        validRef: ValidCascade,
        cascadeAllowed: Set<Field>?,
        cascadeSelectedProps: Set<Field>,
        operationType: KOperationType,
        wrapper: KronosDataSourceWrapper
    ) {
        if (parentRows.isEmpty()) return
        val parentFirst = parentRows.first()
        val propField = parentFirst.kronosColumns().firstOrNull { it.name == prop }
        if (propField == null) {
            val key = "${parentFirst::class.simpleName}.$prop"
            if (cascadeFieldNotFoundWarned.add(key)) {
                Kronos.defaultLogger("CascadeSelectClause").warn(
                    log {
                        +"Cascade field '$prop' not found in ${parentFirst::class.simpleName}, skipping cascade query. Consider disabling cascade for this query."
                    }
                )
            }
            return
        }
        val isCollection = propField.cascadeIsCollectionOrArray
        val tableName = parentFirst.__tableName

        // 确定 FK 映射方向：本地属性 → 远程属性
        val isLocalTable = tableName == validRef.tableName
        val (localProps, remoteProps) = if (isLocalTable) {
            validRef.kCascade.properties to validRef.kCascade.targetProperties
        } else {
            validRef.kCascade.targetProperties to validRef.kCascade.properties
        }

        // 构建级联查询的 SelectClause
        fun cascadeSelect(refPojo: KPojo) = refPojo.select().apply {
            this.operationType = operationType
            this.cascadeAllowed = cascadeAllowed
            this.cascadeSelectedProps = cascadeSelectedProps
        }

        // 从父行 dataMap 中提取 FK 键值对，用于填充子 POJO 查询条件
        fun buildFkPairs(dataMap: Map<String, Any?>): List<Pair<String, Any>>? {
            return validRef.kCascade.targetProperties.mapIndexed { index, targetProp ->
                val (key, valueProp) = if (isLocalTable) {
                    targetProp to validRef.kCascade.properties[index]
                } else {
                    validRef.kCascade.properties[index] to targetProp
                }
                key to (dataMap[valueProp] ?: return null)
            }
        }

        // 单行或复合键：逐行查询
        if (parentRows.size == 1 || localProps.size > 1) {
            for (row in parentRows) {
                val fkPairs = buildFkPairs(row.toDataMap()) ?: continue
                val refPojo = validRef.refPojo.patchTo(validRef.refPojo::class, *fkPairs.toTypedArray())
                row[prop] = if (isCollection) cascadeSelect(refPojo).queryList(wrapper)
                else cascadeSelect(refPojo).queryOneOrNull(wrapper)
            }
            return
        }

        // 批量查询：单FK列 + 多行，使用 WHERE col IN (...) 避免 N+1
        val localProp = localProps[0]
        val remoteProp = remoteProps[0]

        val parentsByFk = mutableMapOf<Any, MutableList<KPojo>>()
        for (row in parentRows) {
            val fkValue = row.toDataMap()[localProp] ?: continue
            parentsByFk.getOrPut(fkValue) { mutableListOf() }.add(row)
        }
        if (parentsByFk.isEmpty()) return

        val refPojo = validRef.refPojo.patchTo(validRef.refPojo::class)
        val remoteField = refPojo.kronosColumns().first { it.name == remoteProp }
        val allChildren = cascadeSelect(refPojo).where {
            criteria = Criteria(
                field = remoteField,
                type = ConditionType.IN,
                value = parentsByFk.keys.toList()
            )
            true
        }.queryList(wrapper)

        val childrenByFk = allChildren.groupBy { it.toDataMap()[remoteProp] }
        for ((fkValue, rows) in parentsByFk) {
            val matched = childrenByFk[fkValue] ?: emptyList()
            for (row in rows) {
                row[prop] = if (isCollection) matched else matched.firstOrNull()
            }
        }
    }
}