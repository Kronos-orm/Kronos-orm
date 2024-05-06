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
    fun parsed() = (paramMapArr ?: arrayOf()).map { parseSqlStatement(sql, it) }.let {
        Pair(it.firstOrNull()?.jdbcSql, it.map { parsedSql ->  parsedSql.jdbcParamList })
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KotoAtomicBatchTask

        if (sql != other.sql) return false
        if (paramMapArr != null) {
            if (other.paramMapArr == null) return false
            if (!paramMapArr.contentEquals(other.paramMapArr)) return false
        } else if (other.paramMapArr != null) return false
        if (operationType != other.operationType) return false
        if (paramMap != other.paramMap) return false

        return true
    }

    override fun hashCode(): Int {
        var result = sql.hashCode()
        result = 31 * result + (paramMapArr?.contentHashCode() ?: 0)
        result = 31 * result + operationType.hashCode()
        result = 31 * result + paramMap.hashCode()
        return result
    }
}