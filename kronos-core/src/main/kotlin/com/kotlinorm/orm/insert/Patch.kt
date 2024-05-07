package com.kotlinorm.orm.insert

import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.update.UpdateClause
import com.kotlinorm.types.KTableField


inline fun <reified T : KPojo> T.insert(): InsertClause<T> {
    return InsertClause(this)
}
inline fun <reified T : KPojo> Array<T>.insert(): List<InsertClause<T>> {
    return map { InsertClause(it) }
}

inline fun <reified T : KPojo> Iterable<T>.insert(): List<InsertClause<T>> {
    return map { InsertClause(it) }
}

fun initInsertClause(
    clause: InsertClause<*>,
    name: String,
    createTime: KronosCommonStrategy,
    updateTime: KronosCommonStrategy,
    vararg fields: Field
): InsertClause<*> {
    return clause.apply {
        tableName = name
        createTimeStrategy = createTime
        updateTimeStrategy = updateTime
        allFields.addAll(fields)
    }
}

fun initInsertClauseList(clauses: List<InsertClause<*>>, name: String, vararg fields: Field): List<InsertClause<*>> {
    return clauses.onEach {
        it.tableName = name
        it.allFields.addAll(fields)
    }
}