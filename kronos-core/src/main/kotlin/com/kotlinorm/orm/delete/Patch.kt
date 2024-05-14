package com.kotlinorm.orm.delete

import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.utils.Extensions.toMutableMap


inline fun <reified T : KPojo> T.delete(): DeleteClause<T> {
    return DeleteClause(this, toMutableMap())
}

inline fun <reified T : KPojo> Array<T>.delete(): List<DeleteClause<T>> {
    return map { DeleteClause(it, it.toMutableMap()) }
}

inline fun <reified T : KPojo> Iterable<T>.delete(): List<DeleteClause<T>> {
    return map { DeleteClause(it, it.toMutableMap()) }
}

@Suppress("UNUSED")
fun initDeleteClause(
    clause: DeleteClause<*>,
    name: String,
    updateTime: KronosCommonStrategy,
    logicDelete: KronosCommonStrategy,
    vararg fields: Field
): DeleteClause<*> {
    return clause.apply {
        tableName = name
        updateTimeStrategy = updateTime
        logicDeleteStrategy = logicDelete
        allFields += fields
    }
}

@Suppress("UNUSED")
fun initDeleteClauseList(
    clauses: List<DeleteClause<*>>,
    name: String,
    updateTime: KronosCommonStrategy,
    logicDelete: KronosCommonStrategy,
    vararg fields: Field
): List<DeleteClause<*>> {
    return clauses.onEach {
        it.tableName = name
        it.updateTimeStrategy = updateTime
        it.logicDeleteStrategy = logicDelete
        it.allFields += fields
    }
}