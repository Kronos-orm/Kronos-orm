package com.kotoframework.orm.insert

import com.kotoframework.interfaces.KPojo


inline fun <reified T : KPojo> T.insert(): InsertClause<T> {
    return InsertClause(this)
}