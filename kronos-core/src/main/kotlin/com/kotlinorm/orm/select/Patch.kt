package com.kotlinorm.orm.select

import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.types.KTableField


inline fun <reified T : KPojo> T.select(noinline fields: KTableField<T, Any?> = null): SelectClause<T> {
    return SelectClause(this, fields)
}