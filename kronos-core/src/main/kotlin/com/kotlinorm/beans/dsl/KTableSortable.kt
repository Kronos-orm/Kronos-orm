package com.kotlinorm.beans.dsl

import com.kotlinorm.enums.ASC
import com.kotlinorm.enums.DESC
import com.kotlinorm.enums.SortType
import com.kotlinorm.interfaces.KPojo

class KTableSortable<T : KPojo>(override val it: T): com.kotlinorm.beans.dsl.KTable<T>(it) {
    val Any?.desc get(): Pair<Any?, SortType> = this to DESC
    val Any?.asc get(): Pair<Any?, SortType> = this to ASC
}