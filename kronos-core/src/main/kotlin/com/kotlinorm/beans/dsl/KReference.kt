package com.kotlinorm.beans.dsl

import com.kotlinorm.enums.CascadeAction.NO_ACTION
import kotlin.reflect.KClass

class KReference(
    val referenceColumns: Array<String> = arrayOf(),
    val targetColumns: Array<String> = arrayOf(),
    val onDelete: String = NO_ACTION,
    val defaultValue: Array<String> = arrayOf(),
    val mapperBy: KClass<out KPojo> = KPojo::class
)