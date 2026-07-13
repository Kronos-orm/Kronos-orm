package com.kotlinorm.orm.pagination

interface OffsetPageable {
    fun applyOffsetPage(pageIndex: Int, pageSize: Int)
}
