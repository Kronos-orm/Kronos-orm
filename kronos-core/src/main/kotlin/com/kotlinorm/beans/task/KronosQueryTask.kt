package com.kotlinorm.beans.task

import com.kotlinorm.enums.QueryType
import com.kotlinorm.enums.QueryType.*
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.utils.DataSourceUtil.orDefault
import com.kotlinorm.utils.logAndReturn

/**
 * KronosQueryTask class represents a collection of Kronos atomic query tasks used to execute SELECT operations.
 * It provides functionality to manage and execute a batch of read-only SQL queries.
 */
class KronosQueryTask(val atomicTask: KronosAtomicQueryTask) { //原子任务
    var beforeQuery: (KronosQueryTask.() -> Any?)? = null //在执行之前执行的操作
    var afterQuery: (Any?.(QueryType, KronosDataSourceWrapper) -> Any?)? = null //在执行之后执行的操作

    fun doBeforeQuery(beforeQuery: KronosQueryTask.() -> Any?): KronosQueryTask { //设置在执行之前执行的操作
        this.beforeQuery = beforeQuery
        return this
    }

    fun doAfterQuery(afterQuery: (Any?.(QueryType, KronosDataSourceWrapper) -> Any?)): KronosQueryTask { // 设置在执行之后执行的操作(返回一个新的KronosQueryTask)
        this.afterQuery = afterQuery
        return this
    }

    fun query(wrapper: KronosDataSourceWrapper? = null): List<Map<String, Any>> {
        beforeQuery?.invoke(this)
        val result = atomicTask.logAndReturn(wrapper.orDefault().forList(atomicTask), Query)
        afterQuery?.invoke(result, Query, wrapper.orDefault())
        return result
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T> queryList(wrapper: KronosDataSourceWrapper? = null): List<T> {
        beforeQuery?.invoke(this)
        val result = atomicTask.logAndReturn(
            wrapper.orDefault().forList(atomicTask, T::class) as List<T>, QueryList
        )
        afterQuery?.invoke(result, QueryList, wrapper.orDefault())
        return result
    }

    fun queryMap(wrapper: KronosDataSourceWrapper? = null): Map<String, Any> {
        beforeQuery?.invoke(this)
        val result = atomicTask.logAndReturn(wrapper.orDefault().forMap(atomicTask)!!, QueryMap)
        afterQuery?.invoke(result, QueryMap, wrapper.orDefault())
        return result
    }

    fun queryMapOrNull(wrapper: KronosDataSourceWrapper? = null): Map<String, Any>? {
        beforeQuery?.invoke(this)
        val result = atomicTask.logAndReturn(wrapper.orDefault().forMap(atomicTask), QueryMapOrNull)
        afterQuery?.invoke(result, QueryMapOrNull, wrapper.orDefault())
        return result
    }

    inline fun <reified T> queryOne(wrapper: KronosDataSourceWrapper? = null): T {
        beforeQuery?.invoke(this)
        val result = atomicTask.logAndReturn(
            wrapper.orDefault().forObject(atomicTask, T::class) as T ?: throw NullPointerException("No such record"),
            QueryOne
        )
        afterQuery?.invoke(result, QueryOne, wrapper.orDefault())
        return result
    }

    inline fun <reified T> queryOneOrNull(wrapper: KronosDataSourceWrapper? = null): T? {
        beforeQuery?.invoke(this)
        val result =
            atomicTask.logAndReturn(
                wrapper.orDefault().forObject(atomicTask, T::class) as T?,
                QueryOneOrNull
            )
        afterQuery?.invoke(
            result, QueryOneOrNull, wrapper.orDefault()
        )
        return result
    }

    companion object {
        fun KronosAtomicQueryTask.toKronosQueryTask(): KronosQueryTask {
            return KronosQueryTask(this)
        }
    }

    operator fun component1(): String {
        return atomicTask.sql
    }

    operator fun component2(): Map<String, Any?> {
        return atomicTask.paramMap
    }

    operator fun component3() = this
}