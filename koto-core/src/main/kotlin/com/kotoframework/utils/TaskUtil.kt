package com.kotoframework.utils

import com.kotoframework.beans.task.*
import com.kotoframework.enums.DBType
import com.kotoframework.enums.KotoAtomicOperationType
import com.kotoframework.interfaces.KAtomicTask
import com.kotoframework.interfaces.KBatchTask
import com.kotoframework.interfaces.KotoDataSourceWrapper
import com.kotoframework.interfaces.KotoSQLTask
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

fun Iterable<KAtomicTask>.toTask(): KotoOperationTask {
    return KotoOperationTask(this.toList())
}

fun Iterable<KAtomicTask>.toAsyncTask(): KotoAsyncOperationTask {
    return KotoAsyncOperationTask(this.toList())
}

fun Array<KAtomicTask>.toTask(): KotoOperationTask {
    return KotoOperationTask(this.toList())
}

fun Array<KAtomicTask>.toAsyncTask(): KotoAsyncOperationTask {
    return KotoAsyncOperationTask(this.toList())
}

fun KotoOperationTask.toAsyncTask(): KotoAsyncOperationTask {
    return KotoAsyncOperationTask(this.tasks)
}

fun KotoAsyncOperationTask.toSyncTask(): KotoOperationTask {
    return KotoOperationTask(this.tasks)
}

fun KAtomicTask.execute(wrapper: KotoDataSourceWrapper?): KotoOperationResult {
    val affectRows = if (this is KBatchTask) {
        wrapper.orDefault().batchUpdate(this as KotoAtomicBatchTask).sum()
    } else {
        wrapper.orDefault().update(this as KotoAtomicTask)
    }
    var lastInsertId: Long? = null
    if (operationType == KotoAtomicOperationType.INSERT) {
        lastInsertId = wrapper.orDefault()
            .forObject(
                KotoAtomicTask(lastInsertIdObtainSql(wrapper.orDefault().dbType))
            , kClass = Long::class) as Long
    }
    return KotoOperationResult(affectRows, lastInsertId)
}

private fun KotoSQLTask.execute(wrapper: KotoDataSourceWrapper?): List<KotoOperationResult> {
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

fun KotoOperationTask.execute(wrapper: KotoDataSourceWrapper? = null) = (this as KotoSQLTask).execute(wrapper)

fun KotoAsyncOperationTask.execute(wrapper: KotoDataSourceWrapper? = null) = (this as KotoSQLTask).execute(wrapper)