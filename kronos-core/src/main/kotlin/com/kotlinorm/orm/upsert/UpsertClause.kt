package com.kotlinorm.orm.upsert

import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.beans.dsl.Criteria
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTable
import com.kotlinorm.beans.dsl.KTable.Companion.tableRun
import com.kotlinorm.beans.task.KronosAtomicTask
import com.kotlinorm.beans.task.KronosOperationResult
import com.kotlinorm.exceptions.NeedFieldsException
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import com.kotlinorm.types.KTableField
import com.kotlinorm.utils.execute

class UpsertClause<T : KPojo>(private val t: T, private var isExcepted: Boolean = false, fields: (KTable<T>.() -> Unit)? = null) {

    internal lateinit var tableName: String
    internal lateinit var createTimeStrategy: KronosCommonStrategy
    internal lateinit var updateTimeStrategy: KronosCommonStrategy
    internal lateinit var logicDeleteStrategy: KronosCommonStrategy
    internal var allFields: MutableList<Field> = mutableListOf()
    private var onDuplicateKey:Boolean = false
    private var toUpsertFields: MutableList<Field> = mutableListOf()
    private var duplicateFeilds: MutableList<Field> = mutableListOf()
    private var paramMap: MutableMap<String, Any?> = mutableMapOf()
    private var paramMapNew: MutableMap<Field, Any?> = mutableMapOf()

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
        TODO()
    }
}