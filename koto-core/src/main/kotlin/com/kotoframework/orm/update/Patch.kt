package com.kotoframework.orm.update

import com.kotoframework.beans.dsl.Field
import com.kotoframework.interfaces.KPojo
import com.kotoframework.types.KTableField


inline fun <reified T : KPojo> T.update(noinline setUpdateFields: KTableField<T, Any?> = null): UpdateClause<T> {
    return UpdateClause(this, false, setUpdateFields)
}

inline fun <reified T : KPojo> T.updateExcept(noinline setUpdateFields: KTableField<T, Any?> = null): UpdateClause<T> {
    return UpdateClause(this, true, setUpdateFields)
}

fun initUpdateClause(clause: UpdateClause<*>, name: String, vararg fields: Field): UpdateClause<*> {
    return clause.apply {
        tableName = name
        allFields.addAll(fields)
    }
}