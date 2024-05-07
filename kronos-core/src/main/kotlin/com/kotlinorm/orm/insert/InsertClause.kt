package com.kotlinorm.orm.insert

import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.KronosAtomicTask
import com.kotlinorm.beans.task.KronosOperationResult
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.utils.Extensions.toMap
import com.kotlinorm.utils.execute
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class InsertClause<T : KPojo>(t: T) {

    internal lateinit var tableName: String
    internal lateinit var createTimeStrategy: KronosCommonStrategy
    internal lateinit var updateTimeStrategy: KronosCommonStrategy
    internal var allFields: MutableSet<Field> = mutableSetOf()
    private var paramMap: MutableMap<String, Any?> = mutableMapOf()
    private val toInsertFields: MutableSet<String> = mutableSetOf()

    init {
        paramMap.putAll(t.toMap().filter { it.value != null })
    }

    fun build(): KronosAtomicTask {

        toInsertFields.addAll(allFields.filter { it.name in paramMap.keys }.map { it.columnName })

        if (createTimeStrategy.enabled) {
            val format = (createTimeStrategy.config ?: "yyyy-MM-dd HH:mm:ss").toString()
            toInsertFields.add(createTimeStrategy.field.columnName)
            paramMap[createTimeStrategy.field.name] = DateTimeFormatter.ofPattern(format).format(LocalDateTime.now())
        }

        if (updateTimeStrategy.enabled) {
            val format = (updateTimeStrategy.config ?: "yyyy-MM-dd HH:mm:ss").toString()
            toInsertFields.add(updateTimeStrategy.field.columnName)
            paramMap[updateTimeStrategy.field.name] = DateTimeFormatter.ofPattern(format).format(LocalDateTime.now())
        }

        val sql = listOfNotNull(
            "INSERT INTO",
            "`${tableName}`",
            "(" + toInsertFields.joinToString { "`${it}`" } + ")",
            "VALUES",
            "(" + paramMap.keys.joinToString { ":${it}" } + ")").joinToString(" ")

        return KronosAtomicTask(
            sql,
            paramMap,
            operationType = KOperationType.INSERT
        )

    }

    fun execute(wrapper: KronosDataSourceWrapper? = null): KronosOperationResult {
        return build().execute(wrapper)
    }

    companion object {
        fun <T : KPojo> List<InsertClause<T>>.build(): KronosAtomicBatchTask {
            val tasks = this.map { it.build() }
            return KronosAtomicBatchTask(
                sql = tasks.first().sql,
                paramMapArr = tasks.map { it.paramMap }.toTypedArray(),
                operationType = KOperationType.INSERT
            )
        }

        fun <T : KPojo> List<InsertClause<T>>.execute(wrapper: KronosDataSourceWrapper? = null): KronosOperationResult {
            return build().execute(wrapper)
        }
    }
}