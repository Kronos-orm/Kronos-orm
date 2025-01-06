package com.kotlinorm.orm.union

import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.SelectClause

open class UnionClause(tasks: List<SelectClause<out KPojo>>) {
    val tasks = tasks.toMutableList()

}