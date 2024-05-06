package com.kotoframework.beans.task

data class KronosOperationResult(
    val affectedRows: Int = 0,
    val lastInsertId: Long? = 0
)