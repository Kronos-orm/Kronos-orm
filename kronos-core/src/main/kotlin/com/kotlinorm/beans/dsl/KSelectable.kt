package com.kotlinorm.beans.dsl

import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.interfaces.KronosDataSourceWrapper

abstract class KSelectable<T : KPojo>(
    internal open val pojo: T
) {
    open var selectFields: LinkedHashSet<Field> = linkedSetOf()
    abstract fun build(wrapper: KronosDataSourceWrapper? = null): KronosAtomicQueryTask
}