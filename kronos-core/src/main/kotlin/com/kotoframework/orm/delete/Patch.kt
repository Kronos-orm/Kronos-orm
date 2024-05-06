package com.kotoframework.orm.delete

import com.kotoframework.beans.dsl.Field
import com.kotoframework.interfaces.KPojo
import com.kotoframework.orm.update.UpdateClause


inline fun <reified T : KPojo> T.delete(): DeleteClause<T> {
    return DeleteClause(this)
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