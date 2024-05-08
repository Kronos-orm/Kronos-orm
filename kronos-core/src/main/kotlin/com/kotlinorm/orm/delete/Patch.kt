package com.kotlinorm.orm.delete

import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.orm.update.UpdateClause


inline fun <reified T : KPojo> T.delete(): DeleteClause<T> {
    return DeleteClause(this)
}
inline fun <reified T : KPojo> Array<T>.delete(): List<DeleteClause<T>> {
    return map { DeleteClause(it) }
}

inline fun <reified T : KPojo> Iterable<T>.delete(): List<DeleteClause<T>> {
    return map { DeleteClause(it) }
}

fun initDeleteClause(clause: DeleteClause<*>, name: String,
                     updateTime: KronosCommonStrategy,
                     logicDelete: KronosCommonStrategy, vararg fields: Field): DeleteClause<*> {
    return clause.apply {
        tableName = name
        updateTimeStrategy = updateTime
        logicDeleteStrategy = logicDelete
        allFields += fields
    }
}

fun initDeleteClauseList(clauses: List<DeleteClause<*>>, name: String,
                         updateTime: KronosCommonStrategy,
                         logicDelete: KronosCommonStrategy, vararg fields: Field): List<DeleteClause<*>> {
    return clauses.onEach {
        it.tableName = name
        it.updateTimeStrategy = updateTime
        it.logicDeleteStrategy = logicDelete
        it.allFields += fields
    }
}