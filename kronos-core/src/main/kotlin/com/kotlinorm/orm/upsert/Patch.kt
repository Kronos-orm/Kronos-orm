package com.kotlinorm.orm.upsert

import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.beans.dsl.KTable


inline fun <reified T : KPojo> T.upsert(noinline setUpdateFields: (KTable<T>.() -> Unit)? = null): UpsertClause<T> {
    return UpsertClause(this, false, setUpdateFields)
}

inline fun <reified T : KPojo> T.upsertExcept(noinline setUpdateFields: (KTable<T>.() -> Unit)? = null): UpsertClause<T> {
    return UpsertClause(this, true, setUpdateFields)
}