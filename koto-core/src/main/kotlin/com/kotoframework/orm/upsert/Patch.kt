package com.kotoframework.orm.upsert

import com.kotoframework.bean.KTable
import com.kotoframework.interfaces.KPojo


inline fun <reified T : KPojo> T.upsert(noinline fields: (KTable<T>.() -> Unit)? = null): UpsertClause<T> {
    return UpsertClause(this, fields)
}

inline fun <reified T : KPojo> T.upsertExcept(noinline fields: (KTable<T>.() -> Unit)? = null): UpsertClause<T> {
    return UpsertClause(this, fields)
}