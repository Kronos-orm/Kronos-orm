package com.kotlinorm.utils

import com.kotlinorm.beans.task.KronosAtomicActionTask
import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.beans.task.KronosOperationResult
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.interfaces.KAtomicActionTask
import com.kotlinorm.interfaces.KAtomicQueryTask
import com.kotlinorm.interfaces.KBatchTask
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.utils.DataSourceUtil.orDefault
import com.kotlinorm.utils.Extensions.mapperTo

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

fun KAtomicActionTask.execute(wrapper: KronosDataSourceWrapper?): KronosOperationResult {
    val affectRows = if (this is KBatchTask) {
        wrapper.orDefault().batchUpdate(this as KronosAtomicBatchTask).sum()
    } else {
        wrapper.orDefault().update(this as KronosAtomicActionTask)
    }
    var lastInsertId: Long? = null
    if (operationType == KOperationType.INSERT) {
        lastInsertId = wrapper.orDefault()
            .forObject(
                KronosAtomicQueryTask(lastInsertIdObtainSql(wrapper.orDefault().dbType)), kClass = Long::class
            ) as Long
    }
    return KronosOperationResult(affectRows, lastInsertId)
}

fun KAtomicQueryTask.fetchAll(wrapper: KronosDataSourceWrapper? = null): List<Map<String, Any>> {
    return wrapper.orDefault().forList(this)
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T> KAtomicQueryTask.fetchList(wrapper: KronosDataSourceWrapper? = null): List<T> {
    return wrapper.orDefault().forList(this , T::class) as List<T>
}

fun KAtomicQueryTask.singleMap(wrapper: KronosDataSourceWrapper? = null): Map<String, Any> {
    return wrapper.orDefault().forMap(this)!!
}

fun KAtomicQueryTask.singleMapOrNull(wrapper: KronosDataSourceWrapper? = null): Map<String, Any>? {
    return wrapper.orDefault().forMap(this)
}

inline fun <reified T> KAtomicQueryTask.single(wrapper: KronosDataSourceWrapper? = null): T {
    val rst = wrapper.orDefault().forObject(this , T::class) as T ?: throw NullPointerException("No such record")
    return rst
}

inline fun <reified T> KAtomicQueryTask.singleOrNull(wrapper: KronosDataSourceWrapper? = null): T? {
    return wrapper.orDefault().forObject(this , T::class) as T
}