package com.kotoframework.beans.task

import com.kotoframework.beans.dsw.NamedParameterUtils.parseSqlStatement
import com.kotoframework.enums.KOperationType
import com.kotoframework.interfaces.KAtomicTask

data class KronosAtomicTask(
    override val sql: String,
    override val paramMap: Map<String, Any?> = mapOf(),
    override val operationType: KOperationType = KOperationType.SELECT
) : KAtomicTask {
    fun parsed() = parseSqlStatement(sql, paramMap)
}