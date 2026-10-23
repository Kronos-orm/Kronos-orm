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

package com.kotlinorm.beans.task

import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.utils.DataSourceUtil.orDefault
import com.kotlinorm.utils.execute

/**
 * Transactional execution facade for an ordered sequence of atomic mutation tasks.
 *
 * Adjacent tasks with identical named SQL are combined into a JDBC batch; non-adjacent tasks
 * retain their original execution order. The aggregate result sums affected rows and exposes
 * the final executed task's insert id and stash.
 */
class KronosActionTask {
    /** Ordered atomic mutations that will execute in one transaction. */
    internal val atomicTasks: MutableList<KronosAtomicActionTask> = mutableListOf()

    /** Hook invoked before this task is materialized for execution. */
    private var beforeExecute: (KronosActionTask.(KronosDataSourceWrapper) -> Unit)? = null

    /** Hook invoked with the aggregate result after all mutations complete successfully. */
    var afterExecute: (KronosOperationResult.(KronosDataSourceWrapper) -> Unit)? =
        null

    /** Replaces the before-execute hook and returns this task for fluent configuration. */
    fun doBeforeExecute(beforeExecute: KronosActionTask.(KronosDataSourceWrapper) -> Unit): KronosActionTask {
        this.beforeExecute = beforeExecute
        return this
    }

    /** Replaces or clears the after-execute hook and returns this task. */
    fun doAfterExecute(afterExecute: (KronosOperationResult.(KronosDataSourceWrapper) -> Unit)?): KronosActionTask {
        this.afterExecute = afterExecute
        return this
    }

    internal fun appendPrepared(
        task: KronosActionTask,
        wrapper: KronosDataSourceWrapper,
        transform: (KronosAtomicActionTask) -> KronosAtomicActionTask = { it }
    ) {
        task.beforeExecute?.invoke(task, wrapper)
        atomicTasks.addAll(task.atomicTasks.map(transform))
        val currentAfterExecute = afterExecute
        val nextAfterExecute = task.afterExecute
        if (nextAfterExecute != null) {
            afterExecute = { dataSource ->
                currentAfterExecute?.invoke(this, dataSource)
                nextAfterExecute.invoke(this, dataSource)
            }
        }
    }

    /**
     * Groups only adjacent tasks with identical SQL, preserving global execution order.
     *
     * For example, `a, a, b, a` becomes `(a, a), (b), (a)` rather than merging both
     * `a` runs across the intervening statement.
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

    /**
     * Executes all atomic tasks in one transaction.
     *
     * @param wrapper wrapper to use, or `null` to resolve the configured default
     * @return aggregate affected rows, final insert id, and final task stash
     */
    fun execute(wrapper: KronosDataSourceWrapper? = null): KronosOperationResult {
        val dataSource = wrapper.orDefault()
        @Suppress("UNCHECKED_CAST")
        return dataSource.transact {
            beforeExecute?.invoke(this@KronosActionTask, dataSource)

            val groupedTasks = groupBySql(atomicTasks).map {
                val first = it.first()
                if (it.size > 1) {
                    KronosAtomicBatchTask(
                        first.sql,
                        it.map { task -> task.paramMap }.toTypedArray(),
                        first.operationType,
                        first.statement,
                        stash = first.stash.toMutableMap(),
                        generatedKeyField = first.generatedKeyField,
                        listParameterOccurrences = first.listParameterOccurrences
                    )
                } else {
                    first
                }
            }

            val results = groupedTasks.map {
                it.execute(dataSource)
            }
            val affectRows = results.sumOf { it.affectedRows }
            val lastResult = results.lastOrNull()
            KronosOperationResult(affectRows, lastResult?.lastInsertId).apply {
                if(results.isNotEmpty()) {
                    stash.putAll(results.last().stash)
                }
                afterExecute?.invoke(this, dataSource)
            }
        } as KronosOperationResult
    }

    companion object {
        fun List<KronosAtomicActionTask>.toKronosActionTask(): KronosActionTask {
            return KronosActionTask().also {
                it.atomicTasks.addAll(this)
            }
        }

        /**
         * Wraps one atomic task and copies its stash into the wrapped task.
         *
         * @return a new action task containing this atomic task
         */
        fun KronosAtomicActionTask.toKronosActionTask(): KronosActionTask {
            return KronosActionTask().also {
                it.atomicTasks.add(this)
                it.atomicTasks.forEach { task -> task.stash.putAll(stash) }
            }
        }

        /**
         * Concatenates action tasks while preserving atomic-task order and hooks.
         *
         * @return a new action task containing all source tasks
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

    private val firstTask by lazy { atomicTasks.firstOrNull() }

    operator fun component1(): String {
        return firstTask?.sql ?: ""
    }

    operator fun component2(): Map<String, Any?> {
        return firstTask?.paramMap ?: mapOf()
    }

    operator fun component3(): MutableList<KronosAtomicActionTask> {
        return atomicTasks
    }
}
