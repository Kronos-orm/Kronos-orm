package com.kotoframework.orm.join;

import com.kotoframework.interfaces.KPojo;
import com.kotoframework.orm.select.SelectClause
import com.kotoframework.pagination.PagedClause
import com.kotoframework.types.KTableConditionalField
import com.kotoframework.types.KTableField
import com.kotoframework.types.KTableSortableField

public class SelectFrom2<T1 : KPojo, T2 : KPojo>(
    var t1: T1? = null,
    var t2: T2? = null,
) {
    fun leftJoin(table2: T2, on: KTableConditionalField<Nothing, Boolean?>) {
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

    fun distinct(){
        TODO()
    }

    fun limit(num: Int){
        TODO()
    }

    fun offset(num: Int){
        TODO()
    }

    fun page(pi: Int, ps: Int){
        TODO()
    }

    fun by(lambda: KTableField<Nothing, Unit>){
        TODO()
    }

    fun where(lambda: KTableConditionalField<Nothing, Boolean?> = null) {
        TODO()
    }

    fun having(lambda: KTableConditionalField<Nothing, Boolean?> = null) {
        TODO()
    }

    fun withTotal(): PagedClause<SelectFrom2<T1, T2>> {
        TODO()
    }

    fun query(): List<Map<String, Any>> {
        TODO()
    }

    fun count(): List<Map<String, Any>> {
        TODO()
    }
}
