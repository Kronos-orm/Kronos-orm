package com.kotoframework.orm.delete

import com.kotoframework.beans.dsl.Field
import com.kotoframework.interfaces.KPojo


inline fun <reified T : KPojo> T.delete(): DeleteClause<T> {
    return DeleteClause(this)
}
inline fun <reified T : KPojo> Array<T>.delete(): List<DeleteClause<T>> {
    return map { DeleteClause(it) }
}

inline fun <reified T : KPojo> Iterable<T>.delete(): List<DeleteClause<T>> {
    return map { DeleteClause(it) }
}

fun initDeleteClause(clause: DeleteClause<*>, name: String, vararg fields: Field): DeleteClause<*> {
    return clause.apply {
        tableName = name
    }
}

fun initDeleteClauseList(clauses: List<DeleteClause<*>>, name: String, vararg fields: Field): List<DeleteClause<*>> {
    return clauses.onEach {
        it.tableName = name
    }
}