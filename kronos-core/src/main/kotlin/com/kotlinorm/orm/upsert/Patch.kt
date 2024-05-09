package com.kotlinorm.orm.upsert

import com.kotlinorm.beans.dsl.KTable
import com.kotlinorm.interfaces.KPojo


inline fun <reified T : KPojo> T.upsert(noinline fields: (KTable<T>.() -> Unit)? = null): UpsertClause<T> {
    return UpsertClause(this, fields)
}

inline fun <reified T : KPojo> T.upsertExcept(noinline fields: (KTable<T>.() -> Unit)? = null): UpsertClause<T> {
    return UpsertClause(this, fields)
}