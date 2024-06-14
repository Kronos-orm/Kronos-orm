package com.kotlinorm.orm.insert

import com.kotlinorm.beans.dsl.Criteria
import com.kotlinorm.beans.task.KronosAtomicActionTask

object CascadeInsertClause {
    /**
     * Build a cascade insert clause.
     * 构建级联插入子句。
     *
     * @param pojo The pojo to be deleted.
     * @return The list of atomic tasks.
     */
    fun <T> build(pojo: T): Array<KronosAtomicActionTask> {
        TODO()
    }
}