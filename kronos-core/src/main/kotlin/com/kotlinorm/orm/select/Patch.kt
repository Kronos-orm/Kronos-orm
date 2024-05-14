package com.kotlinorm.orm.select

import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.types.KTableField


inline fun <reified T : KPojo> T.select(noinline fields: KTableField<T, Unit> = null): SelectClause<T> {
    return SelectClause(this, fields)
}

// For compiler plugin to init the UpdateClause
@Suppress("UNUSED")
fun initSelectClause(
    clause: SelectClause<*>,
    name: String,
    logicDelete: KronosCommonStrategy,
    vararg fields: Field
): SelectClause<*> {
    return clause.apply {
        tableName = name
        logicDeleteStrategy = logicDelete
        allFields += fields
    }
}

// For compiler plugin to init the list of UpdateClause
@Suppress("UNUSED")
fun initSelectClauseList(
    clauses: List<SelectClause<*>>,
    name: String,
    logicDelete: KronosCommonStrategy,
    vararg fields: Field
): List<SelectClause<*>> {
    return clauses.onEach {
        it.tableName = name
        it.logicDeleteStrategy = logicDelete
        it.allFields += fields
    }
}