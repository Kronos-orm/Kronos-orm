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

package com.kotlinorm.utils

import com.kotlinorm.Kronos
import com.kotlinorm.Kronos.defaultLogger
import com.kotlinorm.beans.logging.KLogMessage
import com.kotlinorm.beans.logging.KLogMessage.Companion.kMsgOf
import com.kotlinorm.beans.task.KronosAtomicActionTask
import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.beans.task.KronosOperationResult
import com.kotlinorm.enums.ColorPrintCode.Companion.Black
import com.kotlinorm.enums.ColorPrintCode.Companion.Blue
import com.kotlinorm.enums.ColorPrintCode.Companion.Bold
import com.kotlinorm.enums.ColorPrintCode.Companion.Green
import com.kotlinorm.enums.ColorPrintCode.Companion.Magenta
import com.kotlinorm.enums.ColorPrintCode.Companion.Red
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.DBType.*
import com.kotlinorm.enums.KOperationType.*
import com.kotlinorm.enums.QueryType
import com.kotlinorm.enums.QueryType.*
import com.kotlinorm.interfaces.*
import com.kotlinorm.utils.DataSourceUtil.orDefault

// Generates the SQL statement needed to obtain the last inserted ID based on the provided database type.
fun lastInsertIdObtainSql(dbType: DBType): String {
    return when (dbType) {
        Mysql, H2, OceanBase -> "SELECT LAST_INSERT_ID()"
        Oracle -> "SELECT * FROM DUAL"
        Mssql -> "SELECT SCOPE_IDENTITY()"
        Postgres -> "SELECT LASTVAL()"
        DB2 -> "SELECT IDENTITY_VAL_LOCAL() FROM SYSIBM.SYSDUMMY1"
        Sybase -> "SELECT @@IDENTITY"
        SQLite -> "SELECT last_insert_rowid()"
        else -> throw UnsupportedOperationException("Unsupported database type: $dbType")
    }
}

/**
 * Executes the given atomic action task using the provided data source wrapper.
 *
 * @param wrapper The data source wrapper to use for executing the task. Can be null.
 * @return A KronosOperationResult object containing the number of affected rows and the last inserted ID (if applicable).
 *         If the ID of the operation is specified by the user, the ID after the previous auto-increment is returned
 */
fun KAtomicActionTask.execute(wrapper: KronosDataSourceWrapper?): KronosOperationResult {
    val affectRows = if (this is KBatchTask) {
        wrapper.orDefault().batchUpdate(this as KronosAtomicBatchTask).sum()
    } else {
        (this as KronosAtomicActionTask).trySplitOut().sumOf {
            wrapper.orDefault().update(it)
        }
    }
    var lastInsertId: Long? = null
    if (operationType == INSERT && useIdentity) {
        lastInsertId = wrapper.orDefault().forObject(
            KronosAtomicQueryTask(lastInsertIdObtainSql(wrapper.orDefault().dbType)), kClass = Long::class
        ) as Long
    }
    return logAndReturn(KronosOperationResult(affectRows, lastInsertId))
}

fun KAtomicQueryTask.query(wrapper: KronosDataSourceWrapper? = null) =
    logAndReturn(wrapper.orDefault().forList(this), Query)

@Suppress("UNCHECKED_CAST")
inline fun <reified T> KAtomicQueryTask.queryList(wrapper: KronosDataSourceWrapper? = null) =
    logAndReturn(wrapper.orDefault().forList(this, T::class) as List<T>, QueryList)

fun KAtomicQueryTask.queryMap(wrapper: KronosDataSourceWrapper? = null): Map<String, Any> =
    logAndReturn(wrapper.orDefault().forMap(this)!!, QueryMap)

fun KAtomicQueryTask.queryMapOrNull(wrapper: KronosDataSourceWrapper? = null): Map<String, Any>? =
    logAndReturn(wrapper.orDefault().forMap(this), QueryMapOrNull)

inline fun <reified T> KAtomicQueryTask.queryOne(wrapper: KronosDataSourceWrapper? = null) =
    logAndReturn(wrapper.orDefault().forObject(this, T::class) as T? ?: throw NullPointerException("No such record"))

inline fun <reified T> KAtomicQueryTask.queryOneOrNull(wrapper: KronosDataSourceWrapper? = null) =
    logAndReturn(wrapper.orDefault().forObject(this, T::class) as T?, QueryOneOrNull)

var kronosDoLog: (task: KAtomicTask, result: Any?, queryType: QueryType?) -> Unit = { task, result, queryType ->
    fun resultArr(): Array<KLogMessage> {
        return when (task.operationType) {
            SELECT -> when (queryType) {
                QueryList, Query -> arrayOf(
                    kMsgOf("Found rows: ${(result as List<*>?)!!.size}", Black, Bold).endl(),
                )

                QueryMap, QueryMapOrNull, QueryOne, QueryOneOrNull -> arrayOf(
                    kMsgOf("Found rows: 1", Black, Bold).endl(),
                )

                else -> arrayOf()
            }

            UPDATE, UPSERT, INSERT, DELETE -> {
                result as KronosOperationResult
                listOfNotNull(
                    kMsgOf("Affected rows: ${result.affectedRows}", Black, Bold).endl(),
                    kMsgOf(
                        "Last insert ID: ${result.lastInsertId}", Black, Bold
                    ).takeIf { result.lastInsertId != null && result.lastInsertId != 0L }?.endl(),
                ).toTypedArray()
            }
        }
    }
    if (task is KronosAtomicBatchTask) {
        defaultLogger(Kronos).info(
            arrayOf(
                kMsgOf("Executing [", Green),
                kMsgOf(task.operationType.name, Red, Bold),
                kMsgOf("] task:", Green).endl(),
                kMsgOf("SQL:\t", Black, Bold),
                kMsgOf(task.sql, Blue).endl(),
                kMsgOf("PARAM:\t", Black, Bold),
                *(task.paramMapArr ?: arrayOf()).map { map ->
                    kMsgOf(map.filterNot { it.value == null }.toString(), Magenta).endl()
                }.toTypedArray(),
                *resultArr(),
                kMsgOf("-----------------------", Black, Bold).endl(),
            )
        )
    } else {
        defaultLogger(Kronos).info(
            arrayOf(
                kMsgOf("Executing [", Green),
                kMsgOf(task.operationType.name, Red, Bold),
                kMsgOf("] task:", Green).endl(),
                kMsgOf("SQL:\t", Black, Bold),
                kMsgOf(task.sql, Blue).endl(),
                kMsgOf("PARAM:\t", Black, Bold),
                kMsgOf(task.paramMap.filterNot { it.value == null }.toString(), Magenta).endl(),
                *resultArr(),
                kMsgOf("-----------------------", Black, Bold).endl(),
            )
        )
    }
}

fun <T : Any?> KAtomicTask.logAndReturn(result: T, queryType: QueryType? = null): T {
    kronosDoLog(this, result, queryType)
    return result
}