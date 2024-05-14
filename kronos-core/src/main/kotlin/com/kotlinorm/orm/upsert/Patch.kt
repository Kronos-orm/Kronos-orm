package com.kotlinorm.orm.upsert

import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTable
import com.kotlinorm.interfaces.KPojo



inline fun <reified T : KPojo> T.upsert(noinline setUpdateFields: (KTable<T>.() -> Unit)? = null): UpsertClause<T> {
    return UpsertClause(this, false, setUpdateFields)
}

inline fun <reified T : KPojo> T.upsertExcept(noinline setUpdateFields: (KTable<T>.() -> Unit)? = null): UpsertClause<T> {
    return UpsertClause(this, true, setUpdateFields)
}

// For compiler plugin to init the UpsertClause
@Suppress("UNUSED")
fun initUpsertClause(
    clause: UpsertClause<*>,
    name: String,
    createTime: KronosCommonStrategy,
    updateTime: KronosCommonStrategy,
    logicDelete: KronosCommonStrategy,
    vararg fields: Field
): UpsertClause<*> {
    return clause.apply {
        tableName = name
        createTimeStrategy = createTime
        updateTimeStrategy = updateTime
        logicDeleteStrategy = logicDelete
        allFields += fields
    }
}

// For compiler plugin to init the list of UpsertClause
@Suppress("UNUSED")
fun initUpsertClauseList(
    clauses: List<UpsertClause<*>>,
    name: String,
    createTime: KronosCommonStrategy,
    updateTime: KronosCommonStrategy,
    logicDelete: KronosCommonStrategy,
    vararg fields: Field
): List<UpsertClause<*>> {
    return clauses.onEach {
        it.tableName = name
        it.createTimeStrategy = createTime
        it.updateTimeStrategy = updateTime
        it.logicDeleteStrategy = logicDelete
        it.allFields += fields
    }
}