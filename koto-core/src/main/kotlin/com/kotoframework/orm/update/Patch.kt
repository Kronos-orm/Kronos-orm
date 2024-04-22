package com.kotoframework.orm.update

import com.kotoframework.interfaces.KPojo
import com.kotoframework.types.KTableField


inline fun <reified T : KPojo> T.update(noinline fields: KTableField<T, Any?> = null): UpdateClause<T> {
    return UpdateClause(this, fields)
}
inline fun <reified T : KPojo> T.updateExcept(noinline fields: KTableField<T, Any?> = null): UpdateClause<T> {
    return UpdateClause(this, fields)
}