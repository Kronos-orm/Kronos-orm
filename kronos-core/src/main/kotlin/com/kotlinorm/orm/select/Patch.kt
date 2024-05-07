package com.kotlinorm.orm.select

import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.types.KTableField


inline fun <reified T : KPojo> T.select(noinline fields: KTableField<T, Unit> = null): SelectClause<T> {
    return SelectClause(this, fields)
}