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
import kotlin.reflect.KClass

@PublishedApi
internal data class KPojoRuntimeMetadata(
    val kClass: KClass<out KPojo>,
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

@PublishedApi
internal fun KPojo.resolveRuntimeMetadata(): KPojoRuntimeMetadata {
    val resolvedKClass = runtimeKClass()
    val tableName = __tableName
    if (resolvedKClass == KPojo::class) {
        return dynamicRuntimeMetadata(tableName)
    }
    @Suppress("UNCHECKED_CAST")
    val cacheKey = resolvedKClass as KClass<KPojo>
    return KPojoRuntimeMetadata(
        kClass = resolvedKClass,
        tableName = tableName,
        allFields = kPojoAllFieldsCache[resolvedKClass]!!,
        allColumns = kPojoAllColumnsCache[resolvedKClass]!!,
        fieldMap = fieldsMapCache[cacheKey]!!,
        primaryKey = resolvePrimaryKeyOrNull(kPojoAllColumnsCache[resolvedKClass]!!),
        tableIndexes = kPojoInstanceCache[resolvedKClass]!!.runtimeTableIndexes(),
        createTimeStrategy = kPojoCreateTimeCache[resolvedKClass],
        updateTimeStrategy = kPojoUpdateTimeCache[resolvedKClass],
        logicDeleteStrategy = kPojoLogicDeleteCache[resolvedKClass],
        optimisticLockStrategy = kPojoOptimisticLockCache[resolvedKClass],
        dynamic = false
    )
}

internal fun KPojo.runtimeKClass(): KClass<out KPojo> =
    __kClass

internal fun KPojo.runtimeColumns(): MutableList<Field> =
    __columns

internal fun KPojo.runtimeTableIndexes(): MutableList<KTableIndex> =
    __tableIndexes

internal fun KPojo.runtimeCreateTimeStrategy(): KronosCommonStrategy =
    __createTime

internal fun KPojo.runtimeUpdateTimeStrategy(): KronosCommonStrategy =
    __updateTime

internal fun KPojo.runtimeLogicDeleteStrategy(): KronosCommonStrategy =
    __logicDelete

internal fun KPojo.runtimeOptimisticLockStrategy(): KronosCommonStrategy =
    __optimisticLock

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

private fun KPojo.dynamicRuntimeMetadata(tableName: String): KPojoRuntimeMetadata {
    val allFields = runtimeColumns()
        .map { it.bindDynamicTableName(tableName) }
        .toLinkedSet()
    val allColumns = allFields.filter { it.isColumn }
    val fieldMap = buildRuntimeFieldMap(allFields)
    return KPojoRuntimeMetadata(
        kClass = KPojo::class,
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

internal fun resolvePrimaryKey(kClass: KClass<out KPojo>, columns: List<Field>): Field =
    resolvePrimaryKeyOrNull(columns) ?: error("No primary key found for ${kClass.simpleName}!")

internal fun resolvePrimaryKeyOrNull(columns: List<Field>): Field? =
    columns.firstOrNull { it.primaryKey != PrimaryKeyType.NOT } ?: primaryKeyStrategy
        .takeIf { it.enabled }
        ?.field
        ?.takeIf { strategyField ->
            columns.any { it.name == strategyField.name || it.columnName == strategyField.columnName }
        }

internal fun KronosCommonStrategy?.runtimeBind(
    tableName: String,
    columns: List<Field>
): KronosCommonStrategy? =
    this
        ?.takeIf { it.enabled }
        ?.bind(tableName)
        ?.takeIf { strategy -> columns.any { it.name == strategy.field.name || it.columnName == strategy.field.columnName } }
