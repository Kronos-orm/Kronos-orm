package com.kotlinorm.orm.upsert

import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.beans.dsl.Criteria
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTable
import com.kotlinorm.beans.task.KronosAtomicTask
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.types.KTableField

class UpsertClause<T : KPojo>(t: T, fields: (KTable<T>.() -> Unit)? = null) {

    internal lateinit var tableName: String
    internal lateinit var createTimeStrategy: KronosCommonStrategy
    internal lateinit var updateTimeStrategy: KronosCommonStrategy
    internal lateinit var logicDeleteStrategy: KronosCommonStrategy
    internal var allFields: MutableList<Field> = mutableListOf()
    private var toUpsertFields: MutableList<Field> = mutableListOf()
    private var condition: Criteria? = null
    private var paramMap: MutableMap<String, Any?> = mutableMapOf()
    private var paramMapNew: MutableMap<Field, Any?> = mutableMapOf()

    fun on(lambda: KTableField<T, Unit>): UpsertClause<T> {
        TODO()
    }

    fun onDuplicateKey(): UpsertClause<T> {
        TODO()
    }

    fun execute() {
        TODO()
    }

    fun build(): KronosAtomicTask {
        TODO()
    }
}