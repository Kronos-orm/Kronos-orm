package com.kotlinorm.orm.upsert

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.task.KronosAtomicActionTask

object CascadeUpsertClause {
    /**
     * Build a cascade upsert clause.
     * 构建级联插入或更新子句。
     *
     * @param pojo The pojo to be deleted.
     * @param condition The condition to be met.
     * @return The list of atomic tasks.
     */
    fun <T> build(
        pojo: T,
        onFields: LinkedHashSet<Field>,
        toUpdateFields: LinkedHashSet<Field>,
        toInsertFields: LinkedHashSet<Field>
    ): Array<KronosAtomicActionTask> {
        TODO()
    }
}