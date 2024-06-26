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
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.interfaces.*
import com.kotlinorm.utils.DataSourceUtil.orDefault

// Generates the SQL statement needed to obtain the last inserted ID based on the provided database type.
fun lastInsertIdObtainSql(dbType: DBType): String {
    return when (dbType) {
        DBType.Mysql, DBType.H2, DBType.OceanBase -> "SELECT LAST_INSERT_ID()"
        DBType.Oracle -> "SELECT * FROM DUAL"
        DBType.Mssql -> "SELECT SCOPE_IDENTITY()"
        DBType.Postgres -> "SELECT LASTVAL()"
        DBType.DB2 -> "SELECT IDENTITY_VAL_LOCAL() FROM SYSIBM.SYSDUMMY1"
        DBType.Sybase -> "SELECT @@IDENTITY"
        DBType.SQLite -> "SELECT last_insert_rowid()"
        else -> throw UnsupportedOperationException("Unsupported database type: $dbType")
    }
}

fun KAtomicActionTask.execute(wrapper: KronosDataSourceWrapper?): KronosOperationResult {
    val affectRows = if (this is KBatchTask) {
        wrapper.orDefault().batchUpdate(this as KronosAtomicBatchTask).sum()
    } else {
        doTaskLog()
        (this as KronosAtomicActionTask).trySplitOut().sumOf {
            wrapper.orDefault().update(it)
        }
    }
    var lastInsertId: Long? = null
    if (operationType == KOperationType.INSERT) {
        lastInsertId = wrapper.orDefault().forObject(
            KronosAtomicQueryTask(lastInsertIdObtainSql(wrapper.orDefault().dbType)), kClass = Long::class
        ) as Long?
    }
    return KronosOperationResult(affectRows, lastInsertId)
}

fun KAtomicQueryTask.query(wrapper: KronosDataSourceWrapper? = null): List<Map<String, Any>> {
    doTaskLog()
    return wrapper.orDefault().forList(this)
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T> KAtomicQueryTask.queryList(wrapper: KronosDataSourceWrapper? = null): List<T> {
    doTaskLog()
    return wrapper.orDefault().forList(this, T::class) as List<T>
}

fun KAtomicQueryTask.queryMap(wrapper: KronosDataSourceWrapper? = null): Map<String, Any> {
    doTaskLog()
    return wrapper.orDefault().forMap(this)!!
}

fun KAtomicQueryTask.queryMapOrNull(wrapper: KronosDataSourceWrapper? = null): Map<String, Any>? {
    doTaskLog()
    return wrapper.orDefault().forMap(this)
}

inline fun <reified T> KAtomicQueryTask.queryOne(wrapper: KronosDataSourceWrapper? = null): T {
    doTaskLog()
    return wrapper.orDefault().forObject(this, T::class) as T ?: throw NullPointerException("No such record")
}

inline fun <reified T> KAtomicQueryTask.queryOneOrNull(wrapper: KronosDataSourceWrapper? = null): T? {
    doTaskLog()
    return wrapper.orDefault().forObject(this, T::class) as T
}

var howToLog: (KAtomicTask) -> Unit = { task ->
    defaultLogger(Kronos).info(
        arrayOf(
            kMsgOf("Executing [", Green),
            kMsgOf(task.operationType.name, Red, Bold),
            kMsgOf("] task:", Green).endl(),
            kMsgOf("SQL:\t", Black, Bold),
            kMsgOf(task.sql, Blue).endl(),
            kMsgOf("PARAM:\t", Black, Bold),
            kMsgOf(task.paramMap.filterNot { it.value == null }.toString(), Magenta).endl(),
            kMsgOf("Task execution result:", Black, Bold).endl(),
            kMsgOf("-----------------------", Black, Bold).endl(),
        )
    )
}

fun KAtomicTask.doTaskLog() {
    howToLog(this)
}