package com.kotlinorm.orm.update

import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.types.KTableField


inline fun <reified T : KPojo> T.update(noinline setUpdateFields: KTableField<T, Any?> = null): UpdateClause<T> {
    return UpdateClause(this, false, setUpdateFields)
}

inline fun <reified T : KPojo> T.updateExcept(noinline setUpdateFields: KTableField<T, Any?> = null): UpdateClause<T> {
    return UpdateClause(this, true, setUpdateFields)
}

inline fun <reified T : KPojo> Iterable<T>.update(noinline setUpdateFields: KTableField<T, Any?> = null): List<UpdateClause<T>> {
    return map { UpdateClause(it, false, setUpdateFields) }
}

inline fun <reified T : KPojo> Iterable<T>.updateExcept(noinline setUpdateFields: KTableField<T, Any?> = null): List<UpdateClause<T>> {
    return map { UpdateClause(it, true, setUpdateFields) }
}

inline fun <reified T : KPojo> Array<T>.update(noinline setUpdateFields: KTableField<T, Any?> = null): List<UpdateClause<T>> {
    return map { UpdateClause(it, false, setUpdateFields) }
}

inline fun <reified T : KPojo> Array<T>.updateExcept(noinline setUpdateFields: KTableField<T, Any?> = null): List<UpdateClause<T>> {
    return map { UpdateClause(it, true, setUpdateFields) }
}

fun initUpdateClause(
    clause: UpdateClause<*>,
    name: String,
    updateTime: KronosCommonStrategy,
    logicDelete: KronosCommonStrategy,
    vararg fields: Field
): UpdateClause<*> {
    return clause.apply {
        tableName = name
        updateTimeStrategy = updateTime
        logicDeleteStrategy = logicDelete
        allFields.addAll(fields)
    }
}

fun initUpdateClauseList(
    clauses: List<UpdateClause<*>>,
    name: String,
    updateTime: KronosCommonStrategy,
    logicDelete: KronosCommonStrategy,
    vararg fields: Field
): List<UpdateClause<*>> {
    return clauses.onEach {
        it.tableName = name
        it.updateTimeStrategy = updateTime
        it.logicDeleteStrategy = logicDelete
        it.allFields.addAll(fields)
    }
}