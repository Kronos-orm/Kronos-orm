/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:OptIn(com.kotlinorm.annotations.InternalKronosApi::class)

package com.kotlinorm.interfaces

import com.kotlinorm.beans.task.KronosAtomicActionTask
import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.KronosAtomicQueryTask

/**
 * Adapts the legacy JVM atomic query task to the common execution boundary.
 *
 * The adapter deliberately keeps [KronosAtomicQueryTask.targetType] and JDBC
 * parsing on the JVM. Native planners create [KronosQueryExecutionPlan]
 * directly and therefore do not need a JVM [kotlin.reflect.KType].
 */
fun KronosAtomicQueryTask.toKronosQueryExecutionPlan(): KronosQueryExecutionPlan =
    KronosQueryExecutionPlan(
        sql = sql,
        parameters = paramMap,
        operationType = operationType,
        targetType = targetTypeInfo,
        resultColumns = resultColumns.mapValues { (label, metadata) ->
            KronosResultColumnMetadata(
                typeInfo = metadata.typeInfo,
                storage = metadata.storage,
                fieldName = metadata.field?.name,
                columnLabel = metadata.columnLabel ?: label
            )
        }
    )

/** Adapts one legacy JVM mutation task to the common action boundary. */
fun KronosAtomicActionTask.toKronosActionExecutionPlan(): KronosActionExecutionPlan =
    KronosActionExecutionPlan(
        sql = sql,
        parameters = paramMap,
        operationType = operationType,
        generatedKey = generatedKeyField?.let { KronosGeneratedKeyRequest(it.columnName) }
    )

/** Adapts a legacy JVM batch task without changing its JDBC-specific parser. */
fun KronosAtomicBatchTask.toKronosBatchExecutionPlan(): KronosBatchExecutionPlan =
    KronosBatchExecutionPlan(
        sql = sql,
        entries = executionEntries,
        operationType = operationType,
        generatedKey = generatedKeyField?.let { KronosGeneratedKeyRequest(it.columnName) }
    )

/**
 * Carries the old wrapper identity into the common contract. A JDBC URL is
 * retained only as optional diagnostic data; [databaseName] is the stable
 * identity exposed to common planning code.
 */
fun KronosDataSourceWrapper.toKronosDatabaseIdentity(): KronosDatabaseIdentity =
    KronosDatabaseIdentity(
        dbType = dbType,
        databaseName = url,
        url = url,
        userName = userName
    )
