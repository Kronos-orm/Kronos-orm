package com.kotlinorm.orm.union

import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.select.SelectClause

fun union(vararg task: SelectClause<out KPojo>): UnionClause {
    return UnionClause(task.toList())
}