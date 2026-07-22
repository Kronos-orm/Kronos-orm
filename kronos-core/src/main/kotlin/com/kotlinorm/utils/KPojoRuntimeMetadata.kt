/**
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.utils

import com.kotlinorm.Kronos.primaryKeyStrategy
import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableIndex
import com.kotlinorm.cache.fieldsMapCache
import com.kotlinorm.cache.kPojoAllColumnsCache
import com.kotlinorm.cache.kPojoAllFieldsCache
import com.kotlinorm.cache.kPojoCreateTimeCache
import com.kotlinorm.cache.kPojoInstanceCache
import com.kotlinorm.cache.kPojoLogicDeleteCache
import com.kotlinorm.cache.kPojoOptimisticLockCache
import com.kotlinorm.cache.kPojoUpdateTimeCache
import com.kotlinorm.enums.PrimaryKeyType
import com.kotlinorm.interfaces.KPojo
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * Fully resolved runtime view consumed by ORM planners for one KPojo type.
 *
 * Static models reuse KType-keyed caches populated from generated metadata.
 * Dynamic models are bound to the instance table name and are never inserted
 * into those static caches.
 *
 * @property kType complete runtime type used as the cache/factory identity
 * @property tableName physical table name after dynamic binding
 * @property allFields ordered mapped fields, including non-column DSL fields
 * @property allColumns database-backed subset of [allFields]
 * @property fieldMap case-tolerant lookup by property and column name
 * @property primaryKey resolved explicit or global-strategy primary key
 * @property tableIndexes table indexes visible to DDL operations
 * @property createTimeStrategy enabled creation-time strategy bound to a real column
 * @property updateTimeStrategy enabled update-time strategy bound to a real column
 * @property logicDeleteStrategy enabled logical-delete strategy bound to a real column
 * @property optimisticLockStrategy enabled optimistic-lock strategy bound to a real column
 * @property dynamic whether metadata came from the current object rather than static caches
 */
@PublishedApi
internal data class KPojoRuntimeMetadata(
    val kType: KType,
    val tableName: String,
    val allFields: LinkedHashSet<Field>,
    val allColumns: List<Field>,
    val fieldMap: Map<String, Field>,
    val primaryKey: Field?,
    val tableIndexes: List<KTableIndex>,
    val createTimeStrategy: KronosCommonStrategy?,
    val updateTimeStrategy: KronosCommonStrategy?,
    val logicDeleteStrategy: KronosCommonStrategy?,
    val optimisticLockStrategy: KronosCommonStrategy?,
    val dynamic: Boolean
)

/**
 * Resolves generated or dynamic metadata for this KPojo exactly once per call.
 *
 * Static metadata reads the KType-keyed caches and therefore requires compiler-
 * generated factory/column metadata to be available. A `typeOf<KPojo>()` marker
 * selects instance-owned dynamic metadata instead.
 *
 * @receiver KPojo whose runtime table metadata is required
 * @return a coherent metadata snapshot with strategies bound to actual columns
 * @throws IllegalStateException when generated static metadata is unavailable
 */
@PublishedApi
internal fun KPojo.resolveRuntimeMetadata(): KPojoRuntimeMetadata {
    val resolvedType = __kType
    val tableName = __tableName
    if (KTypeKey.from(resolvedType, ignoreTopLevelNullability = true) == KTypeKey.from(typeOf<KPojo>())) {
        return dynamicRuntimeMetadata(resolvedType, tableName)
    }
    return KPojoRuntimeMetadata(
        kType = resolvedType,
        tableName = tableName,
        allFields = kPojoAllFieldsCache[resolvedType]!!,
        allColumns = kPojoAllColumnsCache[resolvedType]!!,
        fieldMap = fieldsMapCache[resolvedType]!!,
        primaryKey = resolvePrimaryKeyOrNull(kPojoAllColumnsCache[resolvedType]!!),
        tableIndexes = kPojoInstanceCache[resolvedType]!!.runtimeTableIndexes(),
        createTimeStrategy = kPojoCreateTimeCache[resolvedType],
        updateTimeStrategy = kPojoUpdateTimeCache[resolvedType],
        logicDeleteStrategy = kPojoLogicDeleteCache[resolvedType],
        optimisticLockStrategy = kPojoOptimisticLockCache[resolvedType],
        dynamic = false
    )
}

/**
 * Returns the generated or explicitly supplied mutable field metadata list.
 * Dynamic metadata binds copies later and does not mutate this source list.
 */
internal fun KPojo.runtimeColumns(): MutableList<Field> =
    __columns

/**
 * Returns table indexes from generated or explicitly supplied runtime metadata.
 * Callers snapshot the list before exposing it through resolved metadata.
 */
internal fun KPojo.runtimeTableIndexes(): MutableList<KTableIndex> =
    __tableIndexes

/**
 * Returns the raw creation-time strategy before its field is bound to the
 * runtime table and actual column metadata.
 */
internal fun KPojo.runtimeCreateTimeStrategy(): KronosCommonStrategy =
    __createTime

/**
 * Returns the raw update-time strategy before its field is bound to the
 * runtime table and actual column metadata.
 */
internal fun KPojo.runtimeUpdateTimeStrategy(): KronosCommonStrategy =
    __updateTime

/**
 * Returns the raw logical-delete strategy before its field is bound to the
 * runtime table and actual column metadata.
 */
internal fun KPojo.runtimeLogicDeleteStrategy(): KronosCommonStrategy =
    __logicDelete

/**
 * Returns the raw optimistic-lock strategy before its field is bound to the
 * runtime table and actual column metadata.
 */
internal fun KPojo.runtimeOptimisticLockStrategy(): KronosCommonStrategy =
    __optimisticLock

/**
 * Builds a case-tolerant property/column lookup without mutating field metadata.
 * Exact, upper-case and lower-case aliases map to the same [Field].
 *
 * @param fields mapped fields to index
 * @return lookup map keyed by property and column aliases
 */
internal fun buildRuntimeFieldMap(fields: Iterable<Field>): Map<String, Field> =
    fields.flatMap { field ->
        listOf(
            field.name,
            field.columnName,
            field.name.uppercase(),
            field.columnName.uppercase(),
            field.name.lowercase(),
            field.columnName.lowercase()
        )
            .distinct()
            .map { it to field }
    }.toMap()

private fun KPojo.dynamicRuntimeMetadata(type: KType, tableName: String): KPojoRuntimeMetadata {
    val allFields = runtimeColumns()
        .map { it.bindDynamicTableName(tableName) }
        .toLinkedSet()
    val allColumns = allFields.filter { it.isColumn }
    val fieldMap = buildRuntimeFieldMap(allFields)
    return KPojoRuntimeMetadata(
        kType = type,
        tableName = tableName,
        allFields = allFields,
        allColumns = allColumns,
        fieldMap = fieldMap,
        primaryKey = resolvePrimaryKeyOrNull(allColumns),
        tableIndexes = runtimeTableIndexes().toList(),
        createTimeStrategy = runtimeCreateTimeStrategy().runtimeBind(tableName, allColumns),
        updateTimeStrategy = runtimeUpdateTimeStrategy().runtimeBind(tableName, allColumns),
        logicDeleteStrategy = runtimeLogicDeleteStrategy().runtimeBind(tableName, allColumns),
        optimisticLockStrategy = runtimeOptimisticLockStrategy().runtimeBind(tableName, allColumns),
        dynamic = true
    )
}

private fun Field.bindDynamicTableName(tableName: String): Field =
    if (this.tableName.isBlank()) copy(tableName = tableName) else copy()

/**
 * Resolves a required primary key from explicit column metadata first and the
 * enabled global primary-key strategy second.
 *
 * @param type complete KPojo type included in a missing-key failure
 * @param columns database-backed columns eligible for key selection
 * @return the resolved primary-key field
 * @throws IllegalStateException when neither explicit nor strategy metadata matches
 */
internal fun resolvePrimaryKey(type: KType, columns: List<Field>): Field =
    resolvePrimaryKeyOrNull(columns) ?: error("No primary key found for $type!")

/**
 * Returns the first explicit primary key, otherwise a matching enabled global
 * strategy field. A global strategy that does not correspond to a real column
 * is ignored.
 */
internal fun resolvePrimaryKeyOrNull(columns: List<Field>): Field? =
    columns.firstOrNull { it.primaryKey != PrimaryKeyType.NOT } ?: primaryKeyStrategy
        .takeIf { it.enabled }
        ?.field
        ?.takeIf { strategyField ->
            columns.any { it.name == strategyField.name || it.columnName == strategyField.columnName }
        }

/**
 * Binds an enabled strategy to the actual runtime column and table name.
 *
 * The strategy-specific date format overrides the column format; an absent
 * format inherits the column metadata. Disabled or unmatched strategies return
 * `null` so planners cannot emit writes for a nonexistent column.
 *
 * @receiver optional generated/global strategy
 * @param tableName runtime physical table name
 * @param columns database-backed fields eligible for binding
 * @return a bound enabled strategy, or `null` when disabled/unmatched
 */
internal fun KronosCommonStrategy?.runtimeBind(
    tableName: String,
    columns: List<Field>
): KronosCommonStrategy? {
    val strategy = this?.takeIf { it.enabled } ?: return null
    val column = columns.firstOrNull {
        it.name == strategy.field.name || it.columnName == strategy.field.columnName
    } ?: return null
    return KronosCommonStrategy(
        enabled = true,
        field = column.copy(
            dateFormat = strategy.field.dateFormat ?: column.dateFormat,
            tableName = tableName
        )
    )
}
