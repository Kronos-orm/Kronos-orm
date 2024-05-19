package com.kotlinorm.types

import com.kotlinorm.beans.dsl.KTable
import com.kotlinorm.beans.dsl.KTableConditional
import com.kotlinorm.beans.dsl.KTableSortable

typealias KTableField<T, R> = (KTable<T>.(it: T) -> R)?
typealias KTableSortableField<T, R> = (KTableSortable<T>.(it: T) -> R)?
typealias KTableConditionalField<T, R> = (KTableConditional<T>.(it: T) -> R)?