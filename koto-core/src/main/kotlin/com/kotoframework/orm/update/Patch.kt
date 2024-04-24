package com.kotoframework.orm.update

import com.kotoframework.interfaces.KPojo
import com.kotoframework.types.KTableField


inline fun <reified T : KPojo> T.update(noinline setUpdateFields: KTableField<T, Any?> = null): UpdateClause<T> {
    return UpdateClause(this, setUpdateFields)
}
inline fun <reified T : KPojo> T.updateExcept(noinline setUpdateFields: KTableField<T, Any?> = null): UpdateClause<T> {
    return UpdateClause(this, setUpdateFields)
}

fun setUpdateClauseTableName(clause: UpdateClause<*>, name: String): UpdateClause<*> {
    return clause.apply {
        tableName = name
    }
}