package com.kotoframework.orm.insert

import com.kotoframework.beans.dsl.Field
import com.kotoframework.interfaces.KPojo
import com.kotoframework.orm.update.UpdateClause


inline fun <reified T : KPojo> T.insert(): InsertClause<T> {
    return InsertClause(this)
}

fun initInsertClause(clause: InsertClause<*>, name: String, vararg fields: Field): InsertClause<*> {
    return clause.apply {
        tableName = name
        allFields.addAll(fields)
    }
}