package com.kotlinorm.orm.insert

import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.interfaces.KPojo


inline fun <reified T : KPojo> T.insert(): InsertClause<T> {
    return InsertClause(this)
}

inline fun <reified T : KPojo> Array<T>.insert(): List<InsertClause<T>> {
    return map { InsertClause(it) }
}

inline fun <reified T : KPojo> Iterable<T>.insert(): List<InsertClause<T>> {
    return map { InsertClause(it) }
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