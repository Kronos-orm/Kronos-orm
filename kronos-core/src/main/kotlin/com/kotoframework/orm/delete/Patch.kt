package com.kotoframework.orm.delete

import com.kotoframework.interfaces.KPojo


inline fun <reified T : KPojo> T.delete(): DeleteClause<T> {
    return DeleteClause(this)
}