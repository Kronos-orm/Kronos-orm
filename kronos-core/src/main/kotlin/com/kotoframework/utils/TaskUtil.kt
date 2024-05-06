package com.kotoframework.utils

import com.kotoframework.beans.task.*
import com.kotoframework.enums.DBType
import com.kotoframework.enums.KOperationType
import com.kotoframework.interfaces.KAtomicTask
import com.kotoframework.interfaces.KBatchTask
import com.kotoframework.interfaces.KronosDataSourceWrapper
import com.kotoframework.interfaces.KronosSQLTask
import com.kotoframework.utils.DataSourceUtil.orDefault

// Generates the SQL statement needed to obtain the last inserted ID based on the provided database type.
fun lastInsertIdObtainSql(dbType: DBType): String {
    return when (dbType) {
        DBType.Mysql, DBType.H2, DBType.OceanBase -> "SELECT LAST_INSERT_ID();"
        DBType.Oracle -> "SELECT * FROM DUAL;"
        DBType.Mssql -> "SELECT SCOPE_IDENTITY();"
        DBType.Postgres -> "SELECT LASTVAL();"
        DBType.DB2 -> "SELECT IDENTITY_VAL_LOCAL() FROM SYSIBM.SYSDUMMY1;"
        DBType.Sybase -> "SELECT @@IDENTITY;"
        DBType.SQLite -> "SELECT last_insert_rowid();"
        else -> throw UnsupportedOperationException("Unsupported database type: $dbType")
    }
}

fun Iterable<KAtomicTask>.toTask(): KronosOperationTask {
    return KronosOperationTask(this.toList())
}

fun Iterable<KAtomicTask>.toAsyncTask(): KronosAsyncOperationTask {
    return KronosAsyncOperationTask(this.toList())
}

fun Array<KAtomicTask>.toTask(): KronosOperationTask {
    return KronosOperationTask(this.toList())
}

fun Array<KAtomicTask>.toAsyncTask(): KronosAsyncOperationTask {
    return KronosAsyncOperationTask(this.toList())
}

fun KronosOperationTask.toAsyncTask(): KronosAsyncOperationTask {
    return KronosAsyncOperationTask(this.tasks)
}

fun KronosAsyncOperationTask.toSyncTask(): KronosOperationTask {
    return KronosOperationTask(this.tasks)
}

fun KAtomicTask.execute(wrapper: KronosDataSourceWrapper?): KronosOperationResult {
    val affectRows = if (this is KBatchTask) {
        wrapper.orDefault().batchUpdate(this as KronosAtomicBatchTask).sum()
    } else {
        wrapper.orDefault().update(this as KronosAtomicTask)
    }
    var lastInsertId: Long? = null
    if (operationType == KOperationType.INSERT) {
        lastInsertId = wrapper.orDefault()
            .forObject(
                KronosAtomicTask(lastInsertIdObtainSql(wrapper.orDefault().dbType))
            , kClass = Long::class) as Long
    }
    return KronosOperationResult(affectRows, lastInsertId)
}

private fun KronosSQLTask.execute(wrapper: KronosDataSourceWrapper?): List<KronosOperationResult> {
    if (async) {
        // TODO: if async is true, use coroutines to execute tasks
    } else {
        return tasks.map {
            it.execute(wrapper).apply {
                // TODO: log Task Info and Result Info
            }
        }
    }
    return emptyList()
}

fun KronosOperationTask.execute(wrapper: KronosDataSourceWrapper? = null) = (this as KronosSQLTask).execute(wrapper)

fun KronosAsyncOperationTask.execute(wrapper: KronosDataSourceWrapper? = null) = (this as KronosSQLTask).execute(wrapper)