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
import com.kotlinorm.orm.cascade.NodeOfKPojo.Companion.toTreeNode
import com.kotlinorm.orm.insert.InsertClause.Companion.execute
import com.kotlinorm.orm.insert.insert
import java.util.*

object CascadeInsertClause {
    fun <T : KPojo> build(pojo: T, rootTask: KronosAtomicActionTask) = generateTask(pojo, rootTask)

    private fun generateTask(
        pojo: KPojo, prevTask: KronosAtomicActionTask
    ) = prevTask.toKronosActionTask().doAfterExecute { wrapper -> //在执行之后执行的操作
        //为何要放在doAfterExecute中执行：因为子插入任务需要等待父插入任务执行完毕，才能获取到父插入任务的主键值（若使用了自增主键）
        val listOfPojo = mutableListOf<NodeOfKPojo>()// 前序遍历 非递归
        val stack = Stack<NodeOfKPojo>()
        stack.push(pojo.toTreeNode(true))
        while (stack.isNotEmpty()) {
            val node = stack.pop()
            listOfPojo.add(node)
            node.children.forEach {
                stack.push(it)
            }
        }
        listOfPojo.drop(1).map { it.kPojo }.insert().execute(wrapper)
    }
}