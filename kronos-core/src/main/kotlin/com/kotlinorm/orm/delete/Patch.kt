package com.kotlinorm.orm.delete

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.update.UpdateClause


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
        allFields.addAll(fields)
    }
}

fun initDeleteClauseList(clauses: List<DeleteClause<*>>, name: String, vararg fields: Field): List<DeleteClause<*>> {
    return clauses.onEach {
        it.tableName = name
        it.allFields.addAll(fields)
    }
}