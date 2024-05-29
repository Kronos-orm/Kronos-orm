package com.kotlinorm.orm.join

import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.beans.dsl.KSelectable
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.pagination.PagedClause
import com.kotlinorm.types.KTableConditionalField
import com.kotlinorm.types.KTableField
import com.kotlinorm.types.KTableSortableField
                        
class SelectFrom11<T1: KPojo, T2: KPojo, T3: KPojo, T4: KPojo, T5: KPojo, T6: KPojo, T7: KPojo, T8: KPojo, T9: KPojo, T10: KPojo, T11: KPojo>(
    var t1: T1,
    var t2: T2,
    var t3: T3,
    var t4: T4,
    var t5: T5,
    var t6: T6,
    var t7: T7,
    var t8: T8,
    var t9: T9,
    var t10: T10,
    var t11: T11
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

    fun select(fields: KTableField<Nothing, Any?>) {
        TODO()
    }

    fun orderBy(lambda: KTableSortableField<Nothing, Any?>) {
        TODO()
    }

    fun groupBy(lambda: KTableField<Nothing, Any?>) {
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
                        
    fun by(lambda: KTableField<Nothing, Any?>) {
        TODO()
    }
                        
    fun where(lambda: KTableConditionalField<Nothing, Boolean?> = null) {
        TODO()
    }
                        
    fun having(lambda: KTableConditionalField<Nothing, Boolean?> = null) {
        TODO()
    }
                        
    fun withTotal(): PagedClause<T1, SelectFrom11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>> {
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
