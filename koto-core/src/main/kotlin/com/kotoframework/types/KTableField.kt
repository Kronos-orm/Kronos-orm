package com.kotoframework.types

import com.kotoframework.bean.KTableConditional
import com.kotoframework.bean.KTableSortable
import com.kotoframework.bean.KTable

typealias KTableField<T, R> = (KTable<T>.() -> R)?
typealias KTableSortableField<T, R> = (KTableSortable<T>.() -> R)?
typealias KTableConditionalField<T, R> = (KTableConditional<T>.() -> R)?