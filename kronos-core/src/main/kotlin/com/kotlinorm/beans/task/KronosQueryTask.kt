package com.kotlinorm.beans.task

import com.kotlinorm.beans.dsw.NamedParameterUtils.parseSqlStatement
import com.kotlinorm.beans.task.KronosActionTask.Companion.merge
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.interfaces.KAtomicQueryTask
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.utils.DataSourceUtil.orDefault
import com.kotlinorm.utils.doTaskLog
import com.kotlinorm.utils.execute
import java.util.ArrayList

/**
 * KronosQueryTask class represents a collection of Kronos atomic query tasks used to execute SELECT operations.
 * It provides functionality to manage and execute a batch of read-only SQL queries.
 */
class KronosQueryTask {
    internal val atomicTasks: MutableList<KronosAtomicQueryTask> = mutableListOf() //原子任务列表
    var beforeQuery: (KronosQueryTask.() -> Any?)? = null //在执行之前执行的操作
    var afterQuery: (List<Map<String, Any>>.() -> KronosQueryTask)? = null //在执行之后执行的操作

    fun doBeforeQuery(beforeQuery: KronosQueryTask.() -> Any?): KronosQueryTask { //设置在执行之前执行的操作
        this.beforeQuery = beforeQuery
        return this
    }

    fun doAfterQuery(afterQuery: (List<Map<String, Any>>.() -> KronosQueryTask)?): KronosQueryTask { // 设置在执行之后执行的操作(返回一个新的KronosQueryTask)
        this.afterQuery = afterQuery
        return this
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

    companion object {

        fun List<KronosAtomicQueryTask>.toKronosQueryTask(): KronosQueryTask {
            return KronosQueryTask().apply {
                atomicTasks.addAll(map { it.trySplitOut() }.flatten())
            }
        }

        fun KronosAtomicQueryTask.toKronosQueryTask(): KronosQueryTask {
            return KronosQueryTask().apply {
                atomicTasks.addAll(trySplitOut())
            }
        }

        fun List<KronosQueryTask>.merge(): KronosQueryTask {
            return KronosQueryTask().apply {
                atomicTasks.addAll(flatMap { it.atomicTasks })
                if (any { it.afterQuery != null }) {
                    afterQuery = { mapNotNull { afterQuery?.invoke(this) }.merge() }
                }
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

    operator fun component3(): MutableList<KronosAtomicQueryTask> {
        return atomicTasks
    }
}