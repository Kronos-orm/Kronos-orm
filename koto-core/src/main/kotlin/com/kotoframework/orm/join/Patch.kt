package com.kotoframework.orm.join

import com.kotoframework.interfaces.KPojo


inline fun <reified T1 : KPojo, reified T2 : KPojo> T1.join(
    table2: T2,
    selectFrom: SelectFrom2<T1, T2>.(T1, T2) -> Unit
): SelectFrom2<T1, T2> {
    TODO()
}