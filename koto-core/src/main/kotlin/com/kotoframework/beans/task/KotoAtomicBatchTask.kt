package com.kotoframework.beans.task

import com.kotoframework.beans.dsw.NamedParameterUtils.parseSqlStatement
import com.kotoframework.enums.KotoAtomicOperationType
import com.kotoframework.interfaces.KAtomicTask
import com.kotoframework.interfaces.KBatchTask

data class KotoAtomicBatchTask(
    override val sql: String,
    override val paramMapArr: Array<Map<String, Any?>>? = null,
    override val operationType: KotoAtomicOperationType
) : KAtomicTask, KBatchTask {

    @Deprecated("Please use 'paramMapArr' instead.")
    override val paramMap: Map<String, Any?> = mapOf()
    fun parsed() = (paramMapArr ?: arrayOf()).map { parseSqlStatement(sql, it) }
}