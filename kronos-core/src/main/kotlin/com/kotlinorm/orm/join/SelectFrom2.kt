package com.kotlinorm.orm.join

import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.beans.dsl.KSelectable
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.pagination.PagedClause
import com.kotlinorm.types.KTableConditionalField
import com.kotlinorm.types.KTableField
import com.kotlinorm.types.KTableSortableField
                        
class SelectFrom2<T1: KPojo, T2: KPojo>(
    var t1: T1,
    var t2: T2
) : KSelectable<T1>(t1) {
    inline fun <reified T : KPojo> leftJoin(another: T, noinline on: KTableConditionalField<Nothing, Boolean?>) {
        TODO()
    }

    inline fun <reified T : KPojo> rightJoin(another: T, noinline on: KTableConditionalField<Nothing, Boolean?>) {
        TODO()
    }

    inline fun <reified T : KPojo> crossJoin(another: T, noinline on: KTableConditionalField<Nothing, Boolean?>) {
        TODO()
    }

    inline fun <reified T : KPojo> innerJoin(another: T, noinline on: KTableConditionalField<Nothing, Boolean?>) {
        TODO()
    }

    inline fun <reified T : KPojo> fullJoin(another: T, noinline on: KTableConditionalField<Nothing, Boolean?>) {
        TODO()
   }

    fun select(fields: KTableField<Nothing, Unit>) {
        TODO()
    }

    fun orderBy(lambda: KTableSortableField<Nothing, Unit>) {
        TODO()
    }

    fun groupBy(lambda: KTableField<Nothing, Unit>) {
        TODO()
    }
                        
    fun distinct() {
        TODO()
    }
                        
    fun limit(num: Int) {
        TODO()
    }
                        
    fun offset(num: Int) {
        TODO()
    }
                        
    fun page(pi: Int, ps: Int) {
        TODO()
    }
                        
    fun by(lambda: KTableField<Nothing, Unit>) {
        TODO()
    }
                        
    fun where(lambda: KTableConditionalField<Nothing, Boolean?> = null) {
        TODO()
    }
                        
    fun having(lambda: KTableConditionalField<Nothing, Boolean?> = null) {
        TODO()
    }
                        
    fun withTotal(): PagedClause<T1, SelectFrom2<T1, T2>> {
        TODO()
    }
                        
    fun query(): List<Map<String, Any>> {
        TODO()
    }
                        
    fun count(): List<Map<String, Any>> {
        TODO()
    }
                        
    operator fun component1(): String {
        TODO()
    }
                        
    operator fun component2(): Map<String, Any?> {
        TODO()
    }
                        
    override fun build(wrapper: KronosDataSourceWrapper?): KronosAtomicQueryTask {
        TODO("Not yet implemented")
    }
}
