package com.kotoframework.beans.task

data class KotoOperationResult(
    val affectedRows: Int = 0,
    val lastInsertId: Long? = 0
)