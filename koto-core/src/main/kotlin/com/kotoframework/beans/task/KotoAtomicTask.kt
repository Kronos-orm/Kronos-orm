package com.kotoframework.beans.task

import com.kotoframework.enums.KotoAtomicOperationType

data class KotoAtomicTask(
    val sql: String,
    val paramMap: Map<String, Any?> = mapOf(),
    val batchExecute: Boolean = false,
    val paramMapArr: Array<Map<String, Any?>>? = null,
    val operationType: KotoAtomicOperationType
)