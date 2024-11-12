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

package com.kotlinorm.beans.task

import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.utils.DataSourceUtil.orDefault
import com.kotlinorm.utils.execute

/**
 * KronosActionTask class represents a Kronos action task used to execute multiple Kronos atomic action tasks.
 */
class KronosActionTask {
    internal val atomicTasks: MutableList<KronosAtomicActionTask> = mutableListOf() //原子任务列表
    private var beforeExecute: (KronosActionTask.(KronosDataSourceWrapper) -> Unit)? = null //在执行之前执行的操作
    var afterExecute: (KronosOperationResult.(KronosDataSourceWrapper) -> Unit)? =
        null //在执行之后执行的操作(返回一个新的KronosActionTask)

    fun doBeforeExecute(beforeExecute: KronosActionTask.(KronosDataSourceWrapper) -> Unit): KronosActionTask { //设置在执行之前执行的操作
        this.beforeExecute = beforeExecute
        return this
    }

    fun doAfterExecute(afterExecute: (KronosOperationResult.(KronosDataSourceWrapper) -> Unit)?): KronosActionTask { //设置在执行之前执行的操作
        this.afterExecute = afterExecute
        return this
    }

    /**
     * Groups a list of KronosAtomicActionTask by their SQL statements.
     *
     * This function takes a list of KronosAtomicActionTask as input and groups them by their SQL statements.
     * It uses the fold function to iterate over the list of tasks and groups them into sublists.
     * If the accumulator list is empty or the SQL statement of the last task in the last sublist is not the same as the SQL statement of the current task,
     * it adds a new sublist to the accumulator list with the current task as its first element.
     * Otherwise, it adds the current task to the last sublist in the accumulator list.
     * The function returns the accumulator list which is a list of sub lists of tasks grouped by their SQL statements.
     *
     * 保证只有连续的sql语句才会被分组，[a, a, b, b, b, c, c, a, a] => [[a, a], [b, b, b], [c, c], [a, a]]
     *
     * @param listOfTask List<KronosAtomicActionTask> the list of KronosAtomicActionTask to group.
     * @return List<List<KronosAtomicActionTask>> returns a list of sub lists of KronosAtomicActionTask grouped by their SQL statements.
     */
    private fun groupBySql(listOfTask: List<KronosAtomicActionTask>): List<List<KronosAtomicActionTask>> {
        return listOfTask.fold(mutableListOf<MutableList<KronosAtomicActionTask>>()) { acc, task ->
            if (acc.isEmpty() || acc.last().last().sql != task.sql) {
                acc.add(mutableListOf(task))
            } else {
                acc.last().add(task)
            }
            acc
        }
    }

    fun execute(wrapper: KronosDataSourceWrapper? = null): KronosOperationResult {
        val dataSource = wrapper.orDefault() //获取数据源
        beforeExecute?.invoke(this, dataSource) // 在执行之前执行的操作

        val groupedTasks = groupBySql(atomicTasks).map { //按照sql分组
            if (it.size > 1) { //如果有多个任务
                KronosAtomicBatchTask( //创建一个批量任务
                    it.first().sql, it.map { task -> task.paramMap }.toTypedArray(), it.first().operationType
                )
            } else { //如果只有一个任务
                it.first()
            }
        }

        @Suppress("UNCHECKED_CAST")
        val results = dataSource.transact { //执行事务
            groupedTasks.map {
                it.execute(dataSource)
            }
        } as List<KronosOperationResult>
        val affectRows = results.sumOf { it.affectedRows } //受影响的行数
        val lastInsertId = results.mapNotNull { it.lastInsertId }.lastOrNull() //最后插入的id
        return KronosOperationResult(affectRows, lastInsertId).apply {
            afterExecute?.invoke(this, dataSource) //在执行之后执行的操作
        }
    }

    companion object {
        /**
         * Converts a list of KronosAtomicActionTask to a single KronosActionTask.
         *
         * This function creates a new KronosActionTask and adds all the atomic tasks from the list to it.
         * Each atomic task in the list is split out using the trySplitOut function, and the resulting tasks are flattened into a single list.
         * The flattened list of tasks is then added to the atomic tasks of the new KronosActionTask.
         *
         * @receiver List<KronosAtomicActionTask> the list of KronosAtomicActionTask to convert.
         * @return KronosActionTask returns a new KronosActionTask with all the atomic tasks from the list.
         */
        fun List<KronosAtomicActionTask>.toKronosActionTask(): KronosActionTask {
            return KronosActionTask().apply {
                atomicTasks.addAll(map { it.trySplitOut() }.flatten())
            }
        }

        /**
         * Converts a list of KronosAtomicActionTask to a single KronosActionTask.
         *
         * This function creates a new KronosActionTask and adds all the atomic tasks from the list to it.
         * Each atomic task in the list is split out using the trySplitOut function, and the resulting tasks are flattened into a single list.
         * The flattened list of tasks is then added to the atomic tasks of the new KronosActionTask.
         *
         * @receiver List<KronosAtomicActionTask> the list of KronosAtomicActionTask to convert.
         * @return KronosActionTask returns a new KronosActionTask with all the atomic tasks from the list.
         */
        fun KronosAtomicActionTask.toKronosActionTask(): KronosActionTask {
            return KronosActionTask().apply {
                atomicTasks.addAll(trySplitOut())
            }
        }

        /**
         * Merges a list of KronosActionTask into a single KronosActionTask.
         *
         * This function creates a new KronosActionTask and adds all the atomic tasks from each KronosActionTask in the list to it.
         * It uses the flatMap function to flatten the list of atomic tasks from each KronosActionTask into a single list.
         * The flattened list of atomic tasks is then added to the atomic tasks of the new KronosActionTask.
         *
         * @receiver List<KronosActionTask> the list of KronosActionTask to merge.
         * @return KronosActionTask returns a new KronosActionTask with all the atomic tasks from each KronosActionTask in the list.
         */
        fun List<KronosActionTask>.merge(): KronosActionTask {
            return KronosActionTask().apply {
                atomicTasks.addAll(flatMap { it.atomicTasks })
                if(any { it.beforeExecute != null }) {
                    beforeExecute = { wrapper -> forEach { it.beforeExecute?.invoke(this, wrapper) } }
                }
                if (any { it.afterExecute != null }) {
                    afterExecute = { wrapper -> forEach { it.afterExecute?.invoke(this, wrapper) } }
                }
            }
        }
    }

    private val firstTask by lazy { atomicTasks.first() }

    operator fun component1(): String {
        return firstTask.sql
    }

    operator fun component2(): Map<String, Any?> {
        return firstTask.paramMap
    }

    operator fun component3(): MutableList<KronosAtomicActionTask> {
        return atomicTasks
    }
}