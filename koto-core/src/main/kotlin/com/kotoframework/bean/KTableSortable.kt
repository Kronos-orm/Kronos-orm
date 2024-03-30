package com.kotoframework.bean

import com.kotoframework.enums.DESC
import com.kotoframework.enums.SortType
import com.kotoframework.interfaces.KPojo

class KTableSortable<T : KPojo>(override val it: T): KTable<T>(it) {
    val Any?.desc get(): Pair<Any?, SortType> = this to DESC
    val Any?.asc get(): Pair<Any?, SortType> = this to SortType.ASC
}