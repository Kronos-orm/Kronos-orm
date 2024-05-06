package com.kotoframework.orm.upsert

import com.kotoframework.beans.dsl.KTable
import com.kotoframework.interfaces.KPojo
import com.kotoframework.types.KTableConditionalField
import com.kotoframework.types.KTableField

class UpsertClause<T : KPojo>(t: T, fields: (KTable<T>.() -> Unit)? = null) {
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