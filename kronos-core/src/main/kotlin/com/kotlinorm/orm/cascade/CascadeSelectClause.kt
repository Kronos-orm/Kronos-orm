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
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.logging.log
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.beans.task.KronosQueryTask
import com.kotlinorm.beans.task.KronosQueryTask.Companion.toKronosQueryTask
import com.kotlinorm.cache.kPojoAllFieldsCache
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.enums.QueryType.First
import com.kotlinorm.enums.QueryType.ToList
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.orm.select.selectWithType
import com.kotlinorm.syntax.expr.SqlBinaryOperator
import com.kotlinorm.syntax.expr.SqlExpr
import com.kotlinorm.syntax.expr.SqlInRightOperand
import com.kotlinorm.syntax.expr.SqlParameter
import com.kotlinorm.utils.Extensions.patchTo
import com.kotlinorm.utils.LinkedHashSet
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
    private const val MAX_CASCADE_SELECT_PARAMETERS = 900

    private fun Field.matches(other: Field): Boolean =
        name == other.name && tableName == other.tableName

    private fun Collection<Field>.containsField(field: Field): Boolean =
        any { it.matches(field) }

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
    ) = if (cascade) {
        val tableName = pojo.__tableName
        val selectedFieldNames = selectFields.map { it.name }.toSet()
        val allowedCascadeFieldNames = cascadeAllowed
            ?.filter { it.tableName == tableName }
            ?.map { it.name }
            ?.toSet()
            .orEmpty()
        generateTask(
            cascadeAllowed,
            pojo,
            kClass,
            kPojoAllFieldsCache[kClass]!!.filter {
                it.name in selectedFieldNames || it.name in allowedCascadeFieldNames
            },
            operationType,
            rootTask,
            cascadeSelectedProps
        )
    } else rootTask.toKronosQueryTask()

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
        val selectedCascadeProps = cascadeSelectedProps.filter { it.tableName == tableName }
        val validCascades = findValidRefs(
            kClass,
            columns,
            operationType,
            cascadeAllowed?.filter { it.tableName == tableName }?.map { it.name }?.toSet(), // 获取当前Pojo内允许级联的属性
            cascadeAllowed.isNullOrEmpty() // 是否允许所有属性级联
        ).filter { operationType == KOperationType.SELECT || !it.mapperByThis } // 获取所有的非数据库列、有关联注解且用于删除/更新操作
        return prevTask.toKronosQueryTask().apply {
            // 若没有关联信息，返回空（在deleteClause的build中，有对null值的判断和默认值处理）
            // 为何不直接返回deleteTask: 因为此处的deleteTask构建sql语句时带有表名，而普通的deleteTask不带表名，因此需要重新构建
            if (validCascades.isEmpty()) return@apply
            doAfterQuery { queryType, wrapper ->
                validCascades.forEach { validRef ->
                    when (queryType) {
                        ToList -> { // 若是查询KPojo列表
                            val lastStepResult = this as List<KPojo> // this为主表查询的结果
                            if (lastStepResult.isEmpty()) return@forEach // 若该级联属性查询结果为空，不进行级联查询
                            val prop = validRef.field // 获取级联字段的属性如：GroupClass.students
                            if (selectedCascadeProps.isNotEmpty() && !selectedCascadeProps.containsField(prop)) return@forEach // 若该级联属性未被select，不进行级联查询
                            if (!cascadeAllowed.isNullOrEmpty() && !cascadeAllowed.containsField(prop)) return@forEach // 若设置了级联忽略，且该属性不在白名单内，不进行级联查询
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

                        First -> {
                            val lastStepResult = this as KPojo? // this为主表查询的结果
                            if (lastStepResult == null) return@forEach // 若该级联属性查询结果为空，不进行级联查询
                            val prop = validRef.field // 获取级联字段的属性如：GroupClass.students
                            if (selectedCascadeProps.isNotEmpty() && !selectedCascadeProps.containsField(prop)) return@forEach // 若该级联属性未被select，不进行级联查询
                            if (!cascadeAllowed.isNullOrEmpty() && !cascadeAllowed.containsField(prop)) return@forEach // 若设置了级联忽略，且该属性不在白名单内，不进行级联查询
                            setValues(
                                [lastStepResult],
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
        // 确定 FK 映射方向：本地属性 → 远程属性
        val isLocalTable = validRef.mapperByThis
        val (localProps, remoteProps) = if (isLocalTable) {
            validRef.kCascade.properties to validRef.kCascade.targetProperties
        } else {
            validRef.kCascade.targetProperties to validRef.kCascade.properties
        }

        // 构建级联查询的 SelectClause
        val targetType = requireNotNull(propField.elementKType ?: propField.kType) {
            "Missing Kotlin type metadata for cascade field ${propField.name}."
        }
        fun cascadeSelect(refPojo: KPojo) = refPojo.selectWithType(targetType).apply {
            val inheritedOperationType = operationType
            val inheritedCascadeAllowed = cascadeAllowed
            val inheritedCascadeSelectedProps = cascadeSelectedProps
            with(context) {
                this.operationType = inheritedOperationType
                this.cascadeAllowed = inheritedCascadeAllowed
                this.cascadeSelectedProps = inheritedCascadeSelectedProps
            }
            if (inheritedOperationType == KOperationType.SELECT) {
                cascade(false)
            }
        }

        // 从父行 dataMap 中提取 FK 键值对，用于填充子 POJO 查询条件
        fun buildFkPairs(dataMap: Map<String, Any?>): List<Pair<String, Any?>>? {
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
            if (parentRows.size > 1 && localProps.size > 1) {
                val refPojo = validRef.refPojo.patchTo(validRef.refPojo::class)
                val remoteFields = remoteProps.map { remoteProp ->
                    refPojo.kronosColumns().first { it.name == remoteProp }
                }
                val parentsByKey = linkedMapOf<CascadeKey, MutableList<KPojo>>()
                for (row in parentRows) {
                    val fkPairs = buildFkPairs(row.toDataMap()) ?: continue
                    parentsByKey.getOrPut(CascadeKey(fkPairs.map { it.second })) { mutableListOf() }.add(row)
                }
                if (parentsByKey.isEmpty()) return

                val chunkSize = maxOf(1, MAX_CASCADE_SELECT_PARAMETERS / remoteFields.size)
                val allChildren = mutableListOf<KPojo>()
                parentsByKey.keys.chunked(chunkSize).forEach { keyChunk ->
                    val condition = compositeFkCondition(remoteFields, keyChunk)
                    allChildren += cascadeSelect(refPojo).apply {
                        context.andWhere(condition.expr, condition.parameters)
                    }.toList(wrapper)
                }

                val childrenByKey = allChildren.groupBy { child ->
                    CascadeKey(remoteProps.map { remoteProp -> child.toDataMap()[remoteProp] })
                }
                for ((key, rows) in parentsByKey) {
                    val matched = childrenByKey[key].orEmpty()
                    for (row in rows) {
                        row[prop] = if (isCollection) matched else matched.firstOrNull()
                    }
                }
                return
            }

            for (row in parentRows) {
                val fkPairs = buildFkPairs(row.toDataMap()) ?: continue
                val refPojo = validRef.refPojo.patchTo(validRef.refPojo::class, *fkPairs.toTypedArray())
                val remoteFields = refPojo.kronosColumns().filter { field -> fkPairs.any { it.first == field.name } }
                val clause = cascadeSelect(refPojo).apply {
                    context.addFieldConditions(remoteFields, fkPairs.toMap())
                }
                row[prop] = if (isCollection) clause.toList(wrapper)
                else clause.firstOrNull(wrapper)
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
        val allChildren = mutableListOf<KPojo>()
        parentsByFk.keys.chunked(MAX_CASCADE_SELECT_PARAMETERS).forEach { keyChunk ->
            val condition = singleFkCondition(remoteField, keyChunk)
            allChildren += cascadeSelect(refPojo).apply {
                context.andWhere(condition.expr, condition.parameters)
            }.toList(wrapper)
        }

        val childrenByFk = allChildren.groupBy { it.toDataMap()[remoteProp] }
        for ((fkValue, rows) in parentsByFk) {
            val matched = childrenByFk[fkValue] ?: emptyList()
            for (row in rows) {
                row[prop] = if (isCollection) matched else matched.firstOrNull()
            }
        }
    }

    private data class CascadeKey(val values: List<Any?>)

    private data class CascadeCondition(
        val expr: SqlExpr,
        val parameters: Map<String, Any?>
    )

    private fun singleFkCondition(field: Field, values: List<Any>): CascadeCondition {
        val parameters = linkedMapOf<String, Any?>()
        val parameterExprs = values.mapIndexed { index, value ->
            val parameterName = suffixedParameterName(field.name, index)
            parameters[parameterName] = value
            SqlExpr.Parameter(SqlParameter.Named(parameterName))
        }
        return CascadeCondition(
            SqlExpr.In(
                expr = SqlExpr.Column(columnName = field.columnName),
                `in` = SqlInRightOperand.Values(parameterExprs)
            ),
            parameters
        )
    }

    private fun compositeFkCondition(fields: List<Field>, keys: List<CascadeKey>): CascadeCondition {
        val parameters = linkedMapOf<String, Any?>()
        val keyExpressions = keys.mapIndexed { keyIndex, key ->
            fields.mapIndexed { fieldIndex, field ->
                val parameterName = suffixedParameterName(field.name, keyIndex)
                parameters[parameterName] = key.values[fieldIndex]
                SqlExpr.Binary(
                    SqlExpr.Column(columnName = field.columnName),
                    SqlBinaryOperator.Equal,
                    SqlExpr.Parameter(SqlParameter.Named(parameterName))
                )
            }.andAll()
        }
        return CascadeCondition(keyExpressions.orAll(), parameters)
    }

    private fun suffixedParameterName(baseName: String, index: Int): String =
        if (index == 0) baseName else "$baseName@$index"

    private fun List<SqlExpr>.andAll(): SqlExpr =
        drop(1).fold(first()) { left, right -> SqlExpr.Binary(left, SqlBinaryOperator.And, right) }

    private fun List<SqlExpr>.orAll(): SqlExpr =
        drop(1).fold(first()) { left, right -> SqlExpr.Binary(left, SqlBinaryOperator.Or, right) }
}
