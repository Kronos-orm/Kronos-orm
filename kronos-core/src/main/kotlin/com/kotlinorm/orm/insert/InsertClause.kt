package com.kotlinorm.orm.insert

import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.KronosAtomicTask
import com.kotlinorm.beans.task.KronosOperationResult
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.utils.execute
import com.kotlinorm.utils.setCommonStrategy

class InsertClause<T : KPojo>(
    internal val pojo: T,
    internal var paramMap: MutableMap<String, Any?>
) {
    internal lateinit var tableName: String
    internal lateinit var createTimeStrategy: KronosCommonStrategy
    internal lateinit var updateTimeStrategy: KronosCommonStrategy
    internal lateinit var logicDeleteStrategy: KronosCommonStrategy
    internal var allFields: LinkedHashSet<Field> = linkedSetOf()
    private val toInsertFields: LinkedHashSet<Field> = linkedSetOf()

    private val updateInsertFields = { field: Field, value: Any? ->
        if (value != null) {
            toInsertFields += field
            paramMap[field.name] = value
        }
    }

    fun build(): KronosAtomicTask {
        toInsertFields.addAll(allFields.filter { it.name in paramMap.keys })

        setCommonStrategy(createTimeStrategy, true, callBack = updateInsertFields)
        setCommonStrategy(updateTimeStrategy, true, callBack = updateInsertFields)
        setCommonStrategy(logicDeleteStrategy, false, callBack = updateInsertFields)

        val sql = """
            INSERT INTO `$tableName` (${toInsertFields.joinToString { it.quoted() }}) VALUES (${toInsertFields.joinToString { ":$it" }})
        """.trimIndent()

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