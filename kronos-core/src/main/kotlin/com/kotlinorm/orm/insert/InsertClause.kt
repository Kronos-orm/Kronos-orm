package com.kotlinorm.orm.insert

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.beans.task.KronosAtomicActionTask
import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.KronosOperationResult
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.utils.execute
import com.kotlinorm.utils.setCommonStrategy
import com.kotlinorm.utils.toLinkedSet

class InsertClause<T : KPojo>(pojo: T) {
    private var paramMap = pojo.toDataMap()
    private var tableName = pojo.kronosTableName()
    private var createTimeStrategy = pojo.kronosCreateTime()
    private var updateTimeStrategy = pojo.kronosUpdateTime()
    private var logicDeleteStrategy = pojo.kronosLogicDelete()
    private var allFields = pojo.kronosColumns().toLinkedSet()
    private val toInsertFields = linkedSetOf<Field>()

    private val updateInsertFields = { field: Field, value: Any? ->
        if (value != null) {
            toInsertFields += field
            paramMap[field.name] = value
        }
    }

    fun build(): KronosAtomicActionTask {
        toInsertFields.addAll(allFields.filter { it.name in paramMap.keys })

        setCommonStrategy(createTimeStrategy, true, callBack = updateInsertFields)
        setCommonStrategy(updateTimeStrategy, true, callBack = updateInsertFields)
        setCommonStrategy(logicDeleteStrategy, false, callBack = updateInsertFields)

        val sql = """
            INSERT INTO `$tableName` (${toInsertFields.joinToString { it.quoted() }}) VALUES (${toInsertFields.joinToString { ":$it" }})
        """.trimIndent()

        return KronosAtomicActionTask(
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