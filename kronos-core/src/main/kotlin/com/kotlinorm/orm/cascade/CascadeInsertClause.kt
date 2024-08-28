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

object CascadeInsertClause {
    fun <T : KPojo> build(
        cascade: Boolean,
        cascadeAllowed: Array<out KProperty<*>>,
        pojo: T, rootTask: KronosAtomicActionTask
    ) =
        if (cascade) generateTask(cascadeAllowed, pojo, rootTask) else rootTask.toKronosActionTask()

    private fun generateTask(
        cascadeAllowed: Array<out KProperty<*>>,
        pojo: KPojo,
        prevTask: KronosAtomicActionTask
    ) = prevTask.toKronosActionTask().doAfterExecute { wrapper -> //在执行之后执行的操作
        //为何要放在doAfterExecute中执行：因为子插入任务需要等待父插入任务执行完毕，才能获取到父插入任务的主键值（若使用了自增主键）
        val operationResult = this
        pojo.toTreeNode(NodeInfo(true), cascadeAllowed, KOperationType.INSERT) {
            val identity = kPojo.kronosColumns().find { it.identity } ?: return@toTreeNode
            val (_, lastInsertId) = if (kPojo != pojo) {
                kPojo.insert().cascade(enabled = false).execute(wrapper)
            } else {
                operationResult
            }
            if (lastInsertId != null && lastInsertId != 0L && dataMap[identity.name] == null) {
                val prop = kPojo::class.findPropByName(identity.name)
                val typeSafeId =
                    getTypeSafeValue(
                        prop.returnType.withNullability(false).toString(),
                        lastInsertId
                    )
                dataMap[identity.name] = typeSafeId
                kPojo[prop] = typeSafeId
            }
        }
    }
}