package com.kotoframework.types

import com.kotoframework.beans.dsl.KTableConditional
import com.kotoframework.beans.dsl.KTableSortable
import com.kotoframework.beans.dsl.KTable

typealias KTableField<T, R> = (KTable<T>.() -> R)?
typealias KTableSortableField<T, R> = (KTableSortable<T>.() -> R)?
typealias KTableConditionalField<T, R> = (KTableConditional<T>.() -> R)?