package com.kotlinorm.orm.upsert

import com.kotlinorm.beans.dsl.KPojo
import com.kotlinorm.types.KTableField


inline fun <reified T : KPojo> T.upsert(noinline setUpdateFields: KTableField<T, Any?> = null): UpsertClause<T> {
    return UpsertClause(this, false, setUpdateFields)
}

inline fun <reified T : KPojo> T.upsertExcept(noinline setUpdateFields: KTableField<T, Any?> = null): UpsertClause<T> {
    return UpsertClause(this, true, setUpdateFields)
}

// TODO: 支持批量upsert


// 添加批量upsert功能
inline fun <reified T : KPojo> Iterable<T>.upsert(noinline setUpdateFields: KTableField<T, Any?> = null): List<UpsertClause<T>> {
    return map { entity ->
        UpsertClause(entity, false, setUpdateFields)
    }
}

// 添加批量upsert except功能
inline fun <reified T : KPojo> Iterable<T>.upsertExcept(noinline setUpdateFields: KTableField<T, Any?> = null): List<UpsertClause<T>> {
    return map { entity ->
        UpsertClause(entity, true, setUpdateFields)
    }
}

// 对于Array类型的批量upsert功能
inline fun <reified T : KPojo> Array<T>.upsert(noinline setUpdateFields: KTableField<T, Any?> = null): List<UpsertClause<T>> {
    return map { entity ->
        UpsertClause(entity, false, setUpdateFields)
    }
}

// 对于Array类型的批量upsert except功能
inline fun <reified T : KPojo> Array<T>.upsertExcept(noinline setUpdateFields: KTableField<T, Any?> = null): List<UpsertClause<T>> {
    return map { entity ->
        UpsertClause(entity, true, setUpdateFields)
    }
}