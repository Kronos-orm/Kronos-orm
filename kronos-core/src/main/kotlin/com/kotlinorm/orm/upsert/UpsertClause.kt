package com.kotlinorm.orm.upsert

import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTable
import com.kotlinorm.beans.dsl.KTable.Companion.tableRun
import com.kotlinorm.beans.task.KronosAtomicTask
import com.kotlinorm.beans.task.KronosOperationResult
import com.kotlinorm.enums.KOperationType
import com.kotlinorm.exceptions.NeedFieldsException
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.types.KTableField
import com.kotlinorm.utils.Extensions.toMap
import com.kotlinorm.utils.execute

class UpsertClause<T : KPojo>(
    private val t: T,
    private var isExcept: Boolean = false,
    setUpsertFields: (KTable<T>.() -> Unit)? = null
) {

    internal lateinit var tableName: String
    internal lateinit var createTimeStrategy: KronosCommonStrategy
    internal lateinit var updateTimeStrategy: KronosCommonStrategy
    internal lateinit var logicDeleteStrategy: KronosCommonStrategy
    internal var allFields: LinkedHashSet<Field> = linkedSetOf()
    private var onDuplicateKey: Boolean = false
    private var toUpsertFields: LinkedHashSet<Field> = linkedSetOf()
    private var duplicateFeilds: LinkedHashSet<Field> = linkedSetOf()
    private var paramMap: MutableMap<String, Any?> = mutableMapOf()
    private var paramMapNew: MutableMap<Field, Any?> = mutableMapOf()

    init {
        paramMap.putAll(t.toMap().filter { null != it.value })
        if (setUpsertFields != null) {
            t.tableRun {
                setUpsertFields()
                toUpsertFields += fields
            }
            toUpsertFields.toSet().forEach {
                paramMapNew[it + "New"] = paramMap[it.name]
            }
        }
    }

    fun on(someFields: KTableField<T, Unit>): UpsertClause<T> {
        if (null == someFields) throw NeedFieldsException()
        t.tableRun {
            someFields()
            duplicateFeilds += fields.toSet()
        }
        return this
    }

    fun onDuplicateKey(): UpsertClause<T> {
        onDuplicateKey = true
        return this
    }

    fun execute(wrapper: KronosDataSourceWrapper? = null): KronosOperationResult {
        return build().execute(wrapper)
    }

    fun build(): KronosAtomicTask {
        if (isExcept) {
            toUpsertFields = (allFields - toUpsertFields.toSet()) as LinkedHashSet<Field>
            toUpsertFields.forEach {
                paramMapNew[it + "New"] = paramMap[it.name]
            }
        }

        if (toUpsertFields.isEmpty()) {
            // 全都更新
            toUpsertFields = allFields
            toUpsertFields.forEach {
                paramMapNew[it + "New"] = paramMap[it.name]
            }
        }

        var sql = ""

        return KronosAtomicTask(
            sql,
            paramMap,
            operationType = KOperationType.UPDATE
        )
    }
}