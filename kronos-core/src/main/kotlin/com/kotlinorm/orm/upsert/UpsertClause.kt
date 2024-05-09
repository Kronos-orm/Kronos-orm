package com.kotlinorm.orm.upsert

import com.kotlinorm.beans.dsl.KTable
import com.kotlinorm.beans.task.KronosAtomicTask
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.types.KTableField

class UpsertClause<T : KPojo>(t: T, fields: (KTable<T>.() -> Unit)? = null) {
    fun on(lambda: KTableField<T, Unit>): UpsertClause<T> {
        TODO()
    }

    fun onDuplicateKey(): UpsertClause<T> {
        TODO()
    }

    fun execute() {
        TODO()
    }

    fun build(): KronosAtomicTask {
        TODO()
    }
}