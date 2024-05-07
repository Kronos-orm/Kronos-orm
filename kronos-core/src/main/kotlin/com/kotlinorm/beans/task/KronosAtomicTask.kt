package com.kotlinorm.beans.task

import com.kotlinorm.beans.dsw.NamedParameterUtils.parseSqlStatement
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.interfaces.KAtomicTask

data class KronosAtomicTask(
    override val sql: String,
    override val paramMap: Map<String, Any?> = mapOf(),
    override val operationType: KOperationType = KOperationType.SELECT
) : KAtomicTask {
    fun parsed() = parseSqlStatement(sql, paramMap)
}