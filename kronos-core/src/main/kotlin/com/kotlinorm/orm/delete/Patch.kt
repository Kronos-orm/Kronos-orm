package com.kotlinorm.orm.delete

import com.kotlinorm.beans.dsl.KPojo


inline fun <reified T : KPojo> T.delete(): DeleteClause<T> {
    return DeleteClause(this)
}

inline fun <reified T : KPojo> Array<T>.delete(): List<DeleteClause<T>> {
    return map { DeleteClause(it) }
}

inline fun <reified T : KPojo> Iterable<T>.delete(): List<DeleteClause<T>> {
    return map { DeleteClause(it) }
}