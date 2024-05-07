package com.kotlinorm.orm.upsert

import com.kotlinorm.beans.dsl.KTable
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.types.KTableConditionalField
import com.kotlinorm.types.KTableField

class UpsertClause<T : KPojo>(t: T, fields: (com.kotlinorm.beans.dsl.KTable<T>.() -> Unit)? = null) {
    fun set(lambda: KTableField<T, Unit>): UpsertClause<T> {
        TODO()
    }

    fun by(lambda: KTableField<T, Unit>): UpsertClause<T> {
        TODO()
    }

    fun where(lambda: KTableConditionalField<T, Boolean?> = null): UpsertClause<T> {
        TODO()
    }

    fun onDuplicateKey(): UpsertClause<T> {
        TODO()
    }

    fun execute() {
        TODO()
    }

    operator fun component1(): String {
        TODO()
    }

    operator fun component2(): Map<String, Any?> {
        TODO()
    }
}