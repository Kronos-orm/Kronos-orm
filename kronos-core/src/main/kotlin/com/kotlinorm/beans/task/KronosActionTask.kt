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
    private var beforeExecute: () -> Any? = {}
    private val atomicTasks: MutableList<KronosAtomicActionTask> = mutableListOf()
    private var afterExecute: (KronosOperationResult.() -> KronosActionTask)? = null

    fun setBeforeExecute(beforeExecute: () -> Any?) {
        this.beforeExecute = beforeExecute
    }

    fun execute(wrapper: KronosDataSourceWrapper? = null): KronosOperationResult {
        val dataSource = wrapper.orDefault()
        beforeExecute()
        val groupedTasks = atomicTasks.groupBy { it.sql }.map {
            if (it.value.size > 1) {
                KronosAtomicBatchTask(
                    it.key, it.value.map { task -> task.paramMap }.toTypedArray(), it.value.first().operationType
                )
            } else {
                KronosAtomicActionTask(
                    it.key, it.value.first().paramMap, it.value.first().operationType
                )
            }
        }
        val results = dataSource.transact {
            groupedTasks.map {
                it.execute(dataSource)
            }
        }
        val affectRows = results.sumOf { it.affectedRows }
        val lastInsertId = results.mapNotNull { it.lastInsertId }.lastOrNull()
        return KronosOperationResult(affectRows, lastInsertId).apply {
            afterExecute?.invoke(this)
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
        fun List<KronosAtomicActionTask>.toKronosActionTask(doAfterExecute: (KronosOperationResult.() -> KronosActionTask)? = null): KronosActionTask {
            return KronosActionTask().apply {
                atomicTasks.addAll(map { it.trySplitOut() }.flatten())
                afterExecute = doAfterExecute
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
        fun KronosAtomicActionTask.toKronosActionTask(doAfterExecute: (KronosOperationResult.() -> KronosActionTask)? = null): KronosActionTask {
            return KronosActionTask().apply {
                atomicTasks.addAll(trySplitOut())
                afterExecute = doAfterExecute
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
                afterExecute = { mapNotNull { it.afterExecute?.invoke(this) }.merge() }
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