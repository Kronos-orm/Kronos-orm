package com.kotoframework.orm.insert

import com.kotoframework.beans.dsl.Criteria
import com.kotoframework.beans.dsl.Field
import com.kotoframework.beans.task.KronosAtomicTask
import com.kotoframework.beans.task.KronosOperationResult
import com.kotoframework.enums.KOperationType
import com.kotoframework.interfaces.KPojo
import com.kotoframework.interfaces.KronosDataSourceWrapper
import com.kotoframework.utils.Extensions.toMap
import com.kotoframework.utils.execute

class InsertClause<T : KPojo>(t: T) {

    internal lateinit var tableName: String
    internal var allFields: MutableSet<Field> = mutableSetOf()
    private var paramMap: MutableMap<String, Any?> = mutableMapOf()
    private val toInsertFields: MutableSet<String> = mutableSetOf()

    init {
        paramMap.putAll(t.toMap().filter { it.value != null })

    }

    fun build(): KronosAtomicTask {

        toInsertFields.addAll(allFields.filter { it.name in paramMap.keys }.map { it.columnName })
        val sql = listOfNotNull("INSERT INTO", "`${tableName}`", "(" + toInsertFields.joinToString { "`${it}`" } + ")", "VALUES", "(" + paramMap.keys.joinToString { ":${it}" } + ")" ).joinToString(" ")

        return KronosAtomicTask(
            sql,
            paramMap,
            operationType = KOperationType.INSERT
        )

    }

    fun execute(wrapper: KronosDataSourceWrapper? = null): KronosOperationResult {
        return build().execute(wrapper)
    }
}