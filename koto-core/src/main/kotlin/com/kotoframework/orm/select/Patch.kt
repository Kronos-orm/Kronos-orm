package com.kotoframework.orm.select

import com.kotoframework.interfaces.KPojo
import com.kotoframework.types.KTableField


inline fun <reified T : KPojo> T.select(noinline fields: KTableField<T, Unit> = null): SelectClause<T> {
    return SelectClause(this, fields)
}