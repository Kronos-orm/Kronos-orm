package com.kotlinorm.interfaces

import com.kotlinorm.enums.KOperationType

interface KAtomicTask {
    val sql: String
    val paramMap: Map<String, Any?>
    val operationType: KOperationType
}