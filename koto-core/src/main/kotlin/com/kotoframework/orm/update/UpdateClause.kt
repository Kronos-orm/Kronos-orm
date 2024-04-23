package com.kotoframework.orm.update

import com.kotoframework.beans.dsl.Criteria
import com.kotoframework.beans.dsl.KTable
import com.kotoframework.beans.dsl.KTableConditional
import com.kotoframework.enums.AND
import com.kotoframework.enums.Equal
import com.kotoframework.interfaces.KPojo
import com.kotoframework.interfaces.KotoDataSourceWrapper
import com.kotoframework.types.KTableConditionalField
import com.kotoframework.types.KTableField
import com.kotoframework.utils.Extensions.javaInstance
import com.kotoframework.utils.Extensions.toMap

class UpdateClause<T : KPojo>(private val pojo: T, setUpdateFields: KTableField<T, Any?> = null) {
    private var toUpdateFields: MutableSet<String> = mutableSetOf()
    private var condition: Criteria? = null
    private var paramMap: MutableMap<String, Any?> = mutableMapOf()
    private var paramMapNew: MutableMap<String, Any?> = mutableMapOf()

    init {
        paramMap.putAll(pojo.toMap())
        if (setUpdateFields != null) {
            KTable<T>(pojo::class.javaInstance()).apply {
                setUpdateFields.invoke(this)
                toUpdateFields.addAll(fields)
            }
        }
    }

    fun set(rowData: KTableField<T, Unit>): UpdateClause<T> {
        KTable<T>(pojo::class.javaInstance()).apply {
            rowData?.invoke(this)
            fields.let { toUpdateFields.addAll(it) }
            paramMapNew.putAll(this.fieldParamMap)
        }
        return this
    }

    fun by(someFields: KTableField<T, Any?>): UpdateClause<T> {
        KTable<T>(pojo::class.javaInstance()).apply {
            someFields?.invoke(this)
            if (fields.isEmpty()) {
//                throw NeedUpdateConditionException(needUpdateConditionMessage)
            } else {
                condition = Criteria(
                    type = AND,
                ).apply {
                    children = fields.map {
                        Criteria(
                            type = Equal,
                            parameterName = it,
                            value = paramMap[it]
                        )
                    }.toMutableList()
                }
            }
        }
        return this
    }

    fun where(updateCondition: KTableConditionalField<T, Boolean?> = null): UpdateClause<T> {
        KTableConditional<T>(pojo::class.javaInstance()).apply {
            updateCondition?.invoke(this)
            condition = criteria ?: Criteria(
                type = AND
            ).apply {
                children = paramMap.keys.map {
                    Criteria(
                        type = Equal,
                        parameterName = it,
                        value = paramMap[it]
                    )
                }.toMutableList()
            }
        }
        return this
    }

    fun build(): Pair<String, Map<String, Any?>> {
        TODO()
    }

    fun execute(wrapper: KotoDataSourceWrapper? = null) {
//        wrapper.orDefault().update("update xxx set xxx where xxx", paramMap)
    }

    operator fun component1(): String {
        TODO()
    }

    operator fun component2(): Map<String, Any?> {
        TODO()
    }
}