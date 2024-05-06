package com.kotoframework.orm.delete

import com.kotoframework.beans.dsl.Criteria
import com.kotoframework.beans.dsl.Field
import com.kotoframework.beans.dsl.KTable
import com.kotoframework.interfaces.KPojo
import com.kotoframework.types.KTableConditionalField
import com.kotoframework.types.KTableField
import com.kotoframework.utils.Extensions.toMap
import kotlin.reflect.full.createInstance

class DeleteClause<T : KPojo>(
    private val pojo: T, setUpdateFields: KTableField<T, Any?> = null
) {
    internal lateinit var tableName: String
    internal var allFields: MutableSet<Field> = mutableSetOf()
    private var toUpdateFields: MutableSet<Field> = mutableSetOf()
    private var condition: Criteria? = null
    private var paramMap: MutableMap<String, Any?> = mutableMapOf()
    private var paramMapNew: MutableMap<Field, Any?> = mutableMapOf()

    init {
        paramMap.putAll(pojo.toMap().filter { it.value != null })
        if (setUpdateFields != null) {
            with(KTable(pojo::class.createInstance())) {
                setUpdateFields()
                toUpdateFields.addAll(fields)
                toUpdateFields.forEach {
                    paramMapNew[
                        Field(
                            it.columnName,
                            it.name + "New"
                        )
                    ] = paramMap[it.name]
                }
            }
        }
    }

    fun logic(): DeleteClause<T> {
        TODO()
    }

    fun by(lambda: KTable<T>.() -> Unit): DeleteClause<T> {
        TODO()
    }

    fun where(lambda: KTableConditionalField<T, Boolean?> = null): DeleteClause<T> {
        TODO()
    }

    fun execute() {
        TODO()
    }

    operator fun component1(): String {
        TODO()
    }

    operator fun component2(): Map<String, Any?> {
        TODO()
    }
}