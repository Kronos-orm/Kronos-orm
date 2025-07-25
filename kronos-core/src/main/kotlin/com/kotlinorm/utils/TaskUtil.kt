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

package com.kotlinorm.utils

import com.kotlinorm.Kronos
import com.kotlinorm.Kronos.defaultLogger
import com.kotlinorm.beans.logging.KLogMessage
import com.kotlinorm.beans.logging.log
import com.kotlinorm.beans.task.ActionEvent
import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.KronosOperationResult
import com.kotlinorm.enums.KOperationType.SELECT
import com.kotlinorm.enums.QueryType
import com.kotlinorm.enums.QueryType.Query
import com.kotlinorm.enums.QueryType.QueryList
import com.kotlinorm.enums.QueryType.QueryMap
import com.kotlinorm.enums.QueryType.QueryMapOrNull
import com.kotlinorm.enums.QueryType.QueryOne
import com.kotlinorm.enums.QueryType.QueryOneOrNull
import com.kotlinorm.interfaces.KAtomicActionTask
import com.kotlinorm.interfaces.KAtomicTask
import com.kotlinorm.interfaces.KBatchTask
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.plugins.LastInsertIdPlugin.lastInsertId
import com.kotlinorm.utils.DataSourceUtil.orDefault

/**
 * Executes the given atomic action task using the provided data source wrapper.
 *
 * @param wrapper The data source wrapper to use for executing the task. Can be null.
 * @return A KronosOperationResult object containing the number of affected rows and the last inserted ID (if applicable).
 *         If the ID of the operation is specified by the user, the ID after the previous auto-increment is returned
 */
fun KAtomicActionTask.execute(wrapper: KronosDataSourceWrapper?): KronosOperationResult {
    val task = this
    var affectRows: Int
    val dataSource = wrapper.orDefault()
    ActionEvent.beforeActionEvents.forEach { e -> e.invoke(task, dataSource) }
    affectRows = if (task is KBatchTask) {
        dataSource.batchUpdate(task as KronosAtomicBatchTask).sum()
    } else {
        dataSource.update(task)
    }
    ActionEvent.afterActionEvents.forEach { e -> e.invoke(task, dataSource) }
    stash.putAll(task.stash)
    return logAndReturn(KronosOperationResult(affectRows).apply {
        stash.putAll(task.stash)
    })
}

var handleLogResult: (task: KAtomicTask, result: Any?, queryType: QueryType?) -> Unit = { task, result, queryType ->
    fun resultArr(): Array<KLogMessage> {
        return when (task.operationType) {
            SELECT -> when (queryType) {
                QueryList, Query -> log {
                    -"Found rows: ${(result as List<*>?)!!.size}"[black, bold]
                }

                QueryMap, QueryMapOrNull, QueryOne, QueryOneOrNull -> log {
                    -"Found rows: 1"[black, bold]
                }

                else -> arrayOf()
            }

            else -> {
                result as KronosOperationResult
                log {
                    -"Affected rows: ${result.affectedRows}"[black, bold]
                    if (result.lastInsertId != null && result.lastInsertId != 0L) {
                        +"Last insert ID: ${result.lastInsertId}"[black, bold]
                    }
                }
            }
        }
    }
    defaultLogger(Kronos).info(
        if (task is KronosAtomicBatchTask) {
            log {
                -"Executing ["[green]
                -task.operationType.name[red, bold]
                +"] task:"[green]
                -" ♦ "[cyan]
                +"SQL:\t\t"[black, bold]
                +task.sql[blue]
                -" ♦ "[cyan]
                +"PARAMS:\t"[black, bold]
                task.paramMapArr
                    ?.map { it.filterValues { v -> v != null }.toString() }
                    ?.forEach {
                        +it[magenta]
                    }
                +resultArr()
                +"-----------------------"[black, bold]
            }
        } else {
            log {
                -"Executing ["[green]
                -task.operationType.name[red, bold]
                +"] task:"[green]
                -" ♦ "[cyan]
                +"SQL:\t"[black, bold]
                +task.sql[blue]
                -" ♦ "[cyan]
                +"PARAMS:\t"[black, bold]
                +task.paramMap.filterNot { it.value == null }.toString()[magenta]
                +resultArr()
                +"-----------------------"[black, bold]
            }
        }
    )
}

fun <T : Any?> KAtomicTask.logAndReturn(
    result: T, queryType: QueryType? = null
) = result.also {
    handleLogResult(this, it, queryType)
}