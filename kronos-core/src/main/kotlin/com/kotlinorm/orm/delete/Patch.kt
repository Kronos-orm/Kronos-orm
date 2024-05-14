package com.kotlinorm.orm.delete

import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.utils.Extensions.toMap


inline fun <reified T : KPojo> T.delete(): DeleteClause<T> {
    return DeleteClause(this)
}

inline fun <reified T : KPojo> Array<T>.delete(): List<DeleteClause<T>> {
    return map { DeleteClause(it) }
}

inline fun <reified T : KPojo> Iterable<T>.delete(): List<DeleteClause<T>> {
    return map { DeleteClause(it) }
}

@Suppress("UNUSED")
fun initDeleteClause(
    clause: DeleteClause<*>,
    name: String,
    updateTime: KronosCommonStrategy,
    logicDelete: KronosCommonStrategy,
    classDataMap: Map<String, Any?>,
    vararg fields: Field
): DeleteClause<*> {
    return clause.apply {
        tableName = name
        updateTimeStrategy = updateTime
        logicDeleteStrategy = logicDelete
        allFields += fields
        paramMap.putAll(classDataMap)
    }
}

@Suppress("UNUSED")
fun initDeleteClauseList(
    clauses: List<DeleteClause<*>>,
    name: String,
    updateTime: KronosCommonStrategy,
    logicDelete: KronosCommonStrategy,
    classDataMaps: List<Map<String, Any?>>,
    vararg fields: Field
): List<DeleteClause<*>> {
    return clauses.onEachIndexed { index, it ->
        with(it) {
            tableName = name
            updateTimeStrategy = updateTime
            logicDeleteStrategy = logicDelete
            allFields += fields
            paramMap.putAll(classDataMaps.getOrNull(index) ?: pojo.toMap())
        }
    }
}