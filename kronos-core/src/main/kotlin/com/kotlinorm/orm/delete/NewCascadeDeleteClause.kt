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

package com.kotlinorm.orm.delete

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.beans.dsl.KReference
import com.kotlinorm.beans.task.KronosActionTask
import com.kotlinorm.beans.task.KronosActionTask.Companion.toKronosActionTask
import com.kotlinorm.beans.task.KronosAtomicActionTask
import com.kotlinorm.enums.CascadeDeleteAction.*
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.orm.select.select
import com.kotlinorm.utils.Extensions.mapperTo
import java.util.*
import kotlin.reflect.full.createInstance

/**
 * Used to build a cascade delete clause.
 *
 * 构建级联删除子句。
 *
 * This object is used to construct a cascade delete clause for a database operation.
 * It contains a nested Counter class for counting operations, a nested ValidRef data class for storing references and referenced POJOs,
 * and several functions for building the cascade delete clause and generating SQL statements.
 *
 * The main function is build, which takes a POJO, a SQL where clause, a logic flag, a parameter map, and a delete task,
 * and returns an array of KronosAtomicActionTask objects representing the cascade delete operations.
 *
 * The other functions are helper functions used by build. They include findValidRefs, which finds valid references in a list of fields,
 * generateReferenceDeleteSql, which generates a delete SQL statement for a referenced POJO, and getDefaultUpdates, which generates a default update SQL clause.
 *
 */
object NewCascadeDeleteClause {
    data class ValidRef(
        val field: Field, val references: KReference, val refPojo: KPojo
    )

    /**
     * Build a cascade delete clause.
     * 构建级联删除子句。
     *
     * @param pojo The pojo to be deleted.
     * @param whereClauseSql The condition to be met.
     * @param logic The logic to be used.
     * @param paramMap The map of parameters.
     * @param deleteTask The delete task.
     * @return The list of atomic tasks.
     */
    fun <T : KPojo> build(
        pojo: T, logic: Boolean, paramMap: MutableMap<String, Any?>, rootTask: KronosAtomicActionTask
    ): KronosActionTask {
        return generateDeleteTask(pojo, pojo.kronosColumns(), logic, paramMap, rootTask)
    }

    fun <T : KPojo> generateDeleteTask(
        pojo: T,
        columns: List<Field>,
        logic: Boolean,
        paramMap: MutableMap<String, Any?>,
        rootTask: KronosAtomicActionTask
    ): KronosActionTask {
        val validReferences = findValidRefs(
            columns, paramMap
        ) // 获取所有的非数据库列、有关联注解且用于删除操作
        return rootTask.toKronosActionTask().apply {
            doBeforeExecute {
                validReferences.forEach { reference ->
                    if (reference.references.onDelete == RESTRICT) {
                        val existsRestrict = reference.refPojo.select { "count(1)" }.queryOne<Int>()
                        if (existsRestrict > 0) {
                            this.atomicTasks.clear()
                        }
                    }
                }
            }

            doAfterExecute {
                validReferences.forEach { reference ->
                    when (reference.references.onDelete) {
                        CASCADE -> {}
                        SET_NULL -> {}
                        SET_DEFAULT -> {}
                        else -> {}
                    }
                }
            }
        }
    }

    private fun findValidRefs(columns: List<Field>, dataMap: Map<String, Any?>): List<ValidRef> {
        //columns 为的非数据库列、有关联注解且用于删除操作的Field
        return columns.filter { !it.isColumn }.map { col ->
            val ref = Class.forName(
                col.referenceKClassName ?: throw UnsupportedOperationException("The reference class is not supported!")
            ).kotlin.createInstance() as KPojo // 通过反射创建引用的类的POJO，支持类型为KPojo/Collections<KPojo>

            val references =
                ref.kronosColumns().filter { it.cascadeMapperBy(col.tableName) && it.refUseFor(KOperationType.DELETE) }
                    .map { it.reference!! } // 若没有级联映射，返回引用的所有关于本表级联映射

            val listOfRef = mutableListOf<KPojo>()

            references.forEach { reference ->
                val temp = mutableMapOf<String, Any?>()
                reference.targetColumns.forEachIndexed { index, targetColumn ->
                    temp[ref.kronosColumns().first { it.columnName == reference.referenceColumns[index] }.name] =
                        dataMap[columns.first { it.columnName == targetColumn }.name] // 从dataMap中获取引用的列名和值
                }
                listOfRef.add(temp.mapperTo(ref::class) as KPojo)
            }

            listOfRef.map { refPojo ->
                if (col.cascadeMapperBy() && col.refUseFor(KOperationType.DELETE)) {
                    listOf(col.reference!!) // 若有级联映射，返回引用
                } else {
                    ref.kronosColumns()
                        .filter { it.cascadeMapperBy(col.tableName) && it.refUseFor(KOperationType.DELETE) }
                        .map { it.reference!! } // 若没有级联映射，返回引用的所有关于本表级联映射
                }.map { ref ->
                    ValidRef(col, ref, refPojo)
                }
            }.flatten()
        }.flatten()
    }
}