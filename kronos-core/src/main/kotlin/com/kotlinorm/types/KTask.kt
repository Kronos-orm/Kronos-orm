package com.kotlinorm.types

import com.kotlinorm.interfaces.KAtomicActionTask
import com.kotlinorm.interfaces.KAtomicQueryTask
import com.kotlinorm.interfaces.KronosDataSourceWrapper

typealias ActionTaskEvent = (KAtomicActionTask.(
    wrapper: KronosDataSourceWrapper
) -> Unit)

typealias ActionTaskEventList = MutableList<ActionTaskEvent>

typealias QueryTaskEvent = (KAtomicQueryTask.(
    wrapper: KronosDataSourceWrapper
) -> Unit)

typealias QueryTaskEventList = MutableList<QueryTaskEvent>