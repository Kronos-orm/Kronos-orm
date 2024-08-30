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

import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.beans.task.KronosActionTask.Companion.toKronosActionTask
import com.kotlinorm.beans.task.KronosAtomicActionTask
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.orm.cascade.NodeOfKPojo.Companion.toTreeNode
import com.kotlinorm.orm.insert.insert
import com.kotlinorm.utils.getTypeSafeValue
import kotlin.reflect.KProperty
import kotlin.reflect.full.withNullability

/**
 * Used to build a cascade insert clause.
 *
 * 构建级联插入子句。
 *
 * This object is used to construct a cascade insert clause for a database operation.
 * It contains a nested Counter class for counting operations, a nested ValidCascade data class for storing cascades and cascaded POJOs,
 * and several functions for building the cascade insert clause and generating SQL statements.
 *
 * The main function is build, which takes a POJO, a cascade flag, a list of allowed properties, and a root task,
 * and returns a KronosAtomicActionTask object representing the cascade insert operation.
 *
 * The other function is generateTask, which generates a task for the cascade insert operation.
 *
 */
object CascadeInsertClause {
    /**
     * Build a cascade insert clause.
     * 构建级联插入子句。
     *
     * @param cascade Whether the cascade is enabled.
     * @param cascadeAllowed The properties that are allowed to cascade.
     * @param pojo The POJO to insert.
     * @param rootTask The root task.
     * @return A KronosAtomicActionTask object representing the cascade insert operation.
     */
    fun <T : KPojo> build(
        cascade: Boolean,
        cascadeAllowed: Array<out KProperty<*>>,
        pojo: T, rootTask: KronosAtomicActionTask
    ) =
        if (cascade) generateTask(cascadeAllowed, pojo, rootTask) else rootTask.toKronosActionTask()

    /**
     * Generate a task for the cascade insert operation.
     * 为级联插入操作生成任务。
     *
     * @param cascadeAllowed The properties that are allowed to cascade.
     * @param pojo The POJO to insert.
     * @param prevTask The previous task.
     * @return A KronosAtomicActionTask object representing the cascade insert operation.
     */
    private fun generateTask(
        cascadeAllowed: Array<out KProperty<*>>,
        pojo: KPojo,
        prevTask: KronosAtomicActionTask
    ) = prevTask.toKronosActionTask().doAfterExecute { wrapper ->
        //因为子插入任务需要等待父插入任务执行完毕，才能获取到父插入任务的主键值（若使用了自增主键），因此级联操作放在doAfterExecute中执行：
        val operationResult = this //当前任务的执行结果, 用于获取自增主键值
        pojo.toTreeNode(NodeInfo(true), cascadeAllowed, KOperationType.INSERT) {
            val identity = kPojo.kronosColumns().find { it.identity } ?: return@toTreeNode // 若没有自增主键，直接返回
            val (_, lastInsertId) = if (kPojo != pojo) { // 判断当前进行的插入操作是否为最外层的插入操作
                kPojo.insert().cascade(enabled = false).execute(wrapper) // 若不是最外层的插入操作，执行当前任务，获取当前任务的执行结果
            } else {
                operationResult // 若是最外层的插入操作，直接获取当前任务的执行结果
            }
            if (lastInsertId != null && lastInsertId != 0L && dataMap[identity.name] == null) { // 若自增主键值不为空且未被赋值
                val prop = kPojo::class.findPropByName(identity.name)
                val typeSafeId =
                    getTypeSafeValue(
                        prop.returnType.withNullability(false).toString(),
                        lastInsertId
                    ) // 获取自增主键值的类型安全值，如将Long转为Int/Short等
                dataMap[identity.name] = typeSafeId // 将自增主键值赋给当前插入任务的数据映射
                kPojo[prop] = typeSafeId // 将自增主键值赋给当前插入任务的POJO
            }
        }
    }
}