package com.kotoframework.interfaces

import com.kotoframework.enums.KOperationType

interface KAtomicTask {
    val sql: String
    val paramMap: Map<String, Any?>
    val operationType: KOperationType
}