package com.kotlinorm.types

import com.kotlinorm.beans.dsl.KTableConditional
import com.kotlinorm.beans.dsl.KTableSortable
import com.kotlinorm.beans.dsl.KTable

typealias KTableField<T, R> = (KTable<T>.() -> R)?
typealias KTableSortableField<T, R> = (KTableSortable<T>.() -> R)?
typealias KTableConditionalField<T, R> = (KTableConditional<T>.() -> R)?