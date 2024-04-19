package com.kotoframework.orm.delete

import com.kotoframework.beans.dsl.KTable
import com.kotoframework.interfaces.KPojo
import com.kotoframework.types.KTableConditionalField

class DeleteClause<T : KPojo>(t: T) {
    fun logic(): DeleteClause<T> {
        TODO()
    }

    fun by(lambda: KTable<T>.() -> Unit): DeleteClause<T> {
        TODO()
    }

    fun where(lambda: KTableConditionalField<T, Boolean?> = null): DeleteClause<T> {
        TODO()
    }

    fun execute() {
        TODO()
    }
}