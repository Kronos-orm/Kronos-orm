package com.kotoframework.orm.insert

import com.kotoframework.beans.dsl.Field
import com.kotoframework.interfaces.KPojo
import com.kotoframework.orm.update.UpdateClause
import com.kotoframework.types.KTableField


inline fun <reified T : KPojo> T.insert(): InsertClause<T> {
    return InsertClause(this)
}
inline fun <reified T : KPojo> Array<T>.insert(): List<InsertClause<T>> {
    return map { InsertClause(it) }
}

inline fun <reified T : KPojo> Iterable<T>.insert(): List<UpdateClause<T>> {
    return map { UpdateClause(it) }
}

fun initInsertClause(clause: InsertClause<*>, name: String, vararg fields: Field): InsertClause<*> {
    return clause.apply {
        tableName = name
        allFields.addAll(fields)
    }
}

fun initInsertClauseList(clauses: List<InsertClause<*>>, name: String, vararg fields: Field): List<InsertClause<*>> {
    return clauses.onEach {
        it.tableName = name
        it.allFields.addAll(fields)
    }
}