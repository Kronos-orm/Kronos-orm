package com.kotlinorm.orm.join

import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.pagination.PagedClause
import com.kotlinorm.utils.toLinkedSet

class SelectFrom2<T1: KPojo, T2: KPojo>(
    override var t1: T1,
    var t2: T2
) : SelectFrom<T1>(t1) {
    override var tableName = t1.kronosTableName()
    override var paramMap = t1.toDataMap()
    override var logicDeleteStrategy = t1.kronosLogicDelete()
    override var allFields = t1.kronosColumns().toLinkedSet()
    
    fun withTotal(): PagedClause<T1, SelectFrom2<T1, T2>> {
        return PagedClause(this)
    }
}