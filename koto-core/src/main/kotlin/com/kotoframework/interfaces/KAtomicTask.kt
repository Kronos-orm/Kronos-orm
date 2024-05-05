package com.kotoframework.interfaces

import com.kotoframework.enums.KotoAtomicOperationType

interface KAtomicTask {
    val sql: String
    val paramMap: Map<String, Any?>
    val operationType: KotoAtomicOperationType
}