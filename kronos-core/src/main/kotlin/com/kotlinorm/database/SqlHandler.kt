package com.kotlinorm.database

import com.kotlinorm.beans.task.KronosAtomicActionTask
import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.interfaces.KronosDataSourceWrapper

object SqlHandler {
    fun KronosDataSourceWrapper.query(sql: String, paramMap: Map<String, Any?> = emptyMap()): List<Map<String, Any>> {
        return this.forList(KronosAtomicQueryTask(sql, paramMap))
    }

    fun KronosDataSourceWrapper.queryMap(sql: String, paramMap: Map<String, Any?> = emptyMap()): Map<String, Any>? {
        return this.forMap(KronosAtomicQueryTask(sql, paramMap))
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T> KronosDataSourceWrapper.queryList(
        sql: String,
        paramMap: Map<String, Any?> = emptyMap()
    ): List<T> {
        return this.forList(KronosAtomicQueryTask(sql, paramMap), T::class) as List<T>
    }

    inline fun <reified T> KronosDataSourceWrapper.queryOne(sql: String, paramMap: Map<String, Any?> = emptyMap()): T {
        return this.forObject(KronosAtomicQueryTask(sql, paramMap), T::class) as T?
            ?: throw Exception("No result found")
    }

    inline fun <reified T> KronosDataSourceWrapper.queryOneOrNull(
        sql: String,
        paramMap: Map<String, Any?> = emptyMap()
    ): T? {
        return this.forObject(KronosAtomicQueryTask(sql, paramMap), T::class) as T?
    }

    fun KronosDataSourceWrapper.execute(sql: String, paramMap: Map<String, Any?> = emptyMap()): Int {
        return this.update(KronosAtomicActionTask(sql, paramMap))
    }

    fun KronosDataSourceWrapper.batchExecute(sql: String, paramMapList: Array<Map<String, Any?>>): IntArray {
        return this.batchUpdate(
            KronosAtomicBatchTask(
                sql, paramMapList
            )
        )
    }
}