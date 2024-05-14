package com.kotlinorm.orm.insert

import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.utils.Extensions.toMap


inline fun <reified T : KPojo> T.insert(): InsertClause<T> {
    return InsertClause(this)
}

inline fun <reified T : KPojo> Array<T>.insert(): List<InsertClause<T>> {
    return map { InsertClause(it) }
}

inline fun <reified T : KPojo> Iterable<T>.insert(): List<InsertClause<T>> {
    return map { InsertClause(it) }
}

// For compiler plugin to init the InsertClause
@Suppress("UNUSED")
fun initInsertClause(
    clause: InsertClause<*>,
    name: String,
    createTime: KronosCommonStrategy,
    updateTime: KronosCommonStrategy,
    logicDelete: KronosCommonStrategy,
    classDataMap: Map<String, Any?>,
    vararg fields: Field
): InsertClause<*> {
    return clause.apply {
        tableName = name
        createTimeStrategy = createTime
        updateTimeStrategy = updateTime
        logicDeleteStrategy = logicDelete
        allFields.addAll(fields)
        paramMap.putAll(classDataMap)
    }
}

// For compiler plugin to init the list of InsertClause
@Suppress("UNUSED")
fun initInsertClauseList(
    clauses: List<InsertClause<*>>, name: String,
    createTime: KronosCommonStrategy,
    updateTime: KronosCommonStrategy,
    logicDelete: KronosCommonStrategy,
    classDataMaps: List<Map<String, Any?>>,
    vararg fields: Field
): List<InsertClause<*>> {
    return clauses.onEachIndexed { index, it ->
        with(it) {
            tableName = name
            createTimeStrategy = createTime
            updateTimeStrategy = updateTime
            logicDeleteStrategy = logicDelete
            allFields += fields
            paramMap.putAll(classDataMaps.getOrNull(index) ?: pojo.toMap())
        }
    }
}