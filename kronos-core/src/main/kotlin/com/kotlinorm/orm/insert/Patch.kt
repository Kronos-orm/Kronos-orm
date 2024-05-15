package com.kotlinorm.orm.insert

import com.kotlinorm.beans.dsl.KPojo


inline fun <reified T : KPojo> T.insert(): InsertClause<T> {
    return InsertClause(this)
}

inline fun <reified T : KPojo> Array<T>.insert(): List<InsertClause<T>> {
    return map { InsertClause(it) }
}

inline fun <reified T : KPojo> Iterable<T>.insert(): List<InsertClause<T>> {
    return map { InsertClause(it) }
}