package com.kotlinorm.orm.insert

import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.utils.Extensions.toMutableMap


inline fun <reified T : KPojo> T.insert(): InsertClause<T> {
    return InsertClause(this, toMutableMap())
}

inline fun <reified T : KPojo> Array<T>.insert(): List<InsertClause<T>> {
    return map { InsertClause(it, it.toMutableMap()) }
}

inline fun <reified T : KPojo> Iterable<T>.insert(): List<InsertClause<T>> {
    return map { InsertClause(it, it.toMutableMap()) }
}

// For compiler plugin to init the InsertClause
@Suppress("UNUSED")
fun initInsertClause(
    clause: InsertClause<*>,
    name: String,
    createTime: KronosCommonStrategy,
    updateTime: KronosCommonStrategy,
    logicDelete: KronosCommonStrategy,
    vararg fields: Field
): InsertClause<*> {
    return clause.apply {
        tableName = name
        createTimeStrategy = createTime
        updateTimeStrategy = updateTime
        logicDeleteStrategy = logicDelete
        allFields.addAll(fields)
    }
}

// For compiler plugin to init the list of InsertClause
@Suppress("UNUSED")
fun initInsertClauseList(
    clauses: List<InsertClause<*>>, name: String,
    createTime: KronosCommonStrategy,
    updateTime: KronosCommonStrategy,
    logicDelete: KronosCommonStrategy,
    vararg fields: Field
): List<InsertClause<*>> {
    return clauses.onEach {
        it.tableName = name
        it.createTimeStrategy = createTime
        it.updateTimeStrategy = updateTime
        it.logicDeleteStrategy = logicDelete
        it.allFields += fields
    }
}