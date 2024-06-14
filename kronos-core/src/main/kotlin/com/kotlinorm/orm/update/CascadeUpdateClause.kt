package com.kotlinorm.orm.update

import com.kotlinorm.beans.dsl.Criteria
import com.kotlinorm.beans.task.KronosAtomicActionTask

object CascadeUpdateClause {
    /**
     * Build a cascade update clause.
     * 构建级联更新子句。
     *
     * @param pojo The pojo to be deleted.
     * @param condition The condition to be met.
     * @return The list of atomic tasks.
     */
    fun <T> build(pojo: T, condition: Criteria?): Array<KronosAtomicActionTask> {
        TODO()
    }
}