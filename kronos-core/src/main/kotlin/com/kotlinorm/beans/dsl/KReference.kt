package com.kotlinorm.beans.dsl

import com.kotlinorm.enums.CascadeDeleteAction
import com.kotlinorm.enums.CascadeDeleteAction.NO_ACTION
import com.kotlinorm.enums.KOperationType
import kotlin.reflect.KClass

class KReference(
    val referenceFields: Array<String> = arrayOf(),
    val targetFields: Array<String> = arrayOf(),
    val onDelete: CascadeDeleteAction = NO_ACTION,
    val defaultValue: Array<String> = arrayOf(),
    val mapperBy: KClass<out KPojo> = KPojo::class,
    val usage: Array<KOperationType> = arrayOf(
        KOperationType.INSERT,
        KOperationType.UPDATE,
        KOperationType.DELETE,
        KOperationType.SELECT,
        KOperationType.UPSERT
    )
)