package com.kotlinorm.orm.delete

import com.kotlinorm.beans.dsl.Criteria
import com.kotlinorm.beans.task.KronosAtomicActionTask

/**
 * Used to build a cascade delete clause.
 * 用于构建级联删除子句。
 */
object CascadeDeleteClause {
    /**
     * Build a cascade delete clause.
     * 构建级联删除子句。
     *
     * @param pojo The pojo to be deleted.
     * @param condition The condition to be met.
     * @return The list of atomic tasks.
     */
    fun <T> build(pojo: T, condition: Criteria?): Array<KronosAtomicActionTask> {
        TODO()
    }
}