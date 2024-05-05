package com.kotoframework.beans.task

import com.kotoframework.beans.dsw.NamedParameterUtils.parseSqlStatement
import com.kotoframework.enums.KotoAtomicOperationType
import com.kotoframework.interfaces.KAtomicTask

data class KotoAtomicTask(
    override val sql: String,
    override val paramMap: Map<String, Any?> = mapOf(),
    override val operationType: KotoAtomicOperationType
) : KAtomicTask {
    fun parsed() = parseSqlStatement(sql, paramMap)
}