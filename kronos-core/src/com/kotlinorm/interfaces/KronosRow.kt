/**
 * Copyright 2022-2025 kronos-orm
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

package com.kotlinorm.interfaces

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * One active query row exposed to a synchronous row mapper.
 *
 * A row is valid only while its mapper is running. Implementations may reuse a live database
 * cursor, so callers must materialize values they need before returning from the mapper.
 */
abstract class KronosRow {
    /** Zero-based index of this row in the current result set. */
    abstract val rowNumber: Int

    /**
     * Reads the one-based JDBC [position] and decodes it as [targetType].
     */
    abstract fun get(position: Int, targetType: KType): Any?

    /**
     * Reads the column selected with [label] and decodes it as [targetType].
     */
    abstract fun get(label: String, targetType: KType): Any?

    /** Reads one-based JDBC [position] using the complete reified Kotlin type. */
    @Suppress("UNCHECKED_CAST")
    inline fun <reified T> get(position: Int): T = get(position, typeOf<T>()) as T

    /** Reads a JDBC column label or SQL alias using the complete reified Kotlin type. */
    @Suppress("UNCHECKED_CAST")
    inline fun <reified T> get(label: String): T = get(label, typeOf<T>()) as T

    /** Reads a selected [field] through its output name, falling back to its database column name. */
    fun get(field: Field, targetType: KType): Any? = get(field.name.ifBlank { field.columnName }, targetType)

    /** Reads a selected [field] using the complete reified Kotlin type. */
    @Suppress("UNCHECKED_CAST")
    inline fun <reified T> get(field: Field): T = get(field, typeOf<T>()) as T
}

/**
 * Optional row-mapping capability implemented by wrappers that can keep a live row cursor.
 *
 * [KronosDataSourceWrapper] remains a materialized-result contract. Code that needs this
 * capability should use [requireKronosRowMapping] rather than requiring every wrapper to expose
 * JDBC-like state.
 */
interface KronosRowMappingDataSourceWrapper : KronosDataSourceWrapper {
    fun <T> toList(task: KAtomicQueryTask, mapper: (KronosRow) -> T): List<T>

    fun <T> first(task: KAtomicQueryTask, mapper: (KronosRow) -> T): KronosRowFirstResult<T>
}

/** Presence-aware first-row result that preserves a mapper's legitimate null result. */
sealed interface KronosRowFirstResult<out T> {
    data object Empty : KronosRowFirstResult<Nothing>

    data class Present<T>(val value: T) : KronosRowFirstResult<T>
}

/** Returns this wrapper's row-mapping capability or reports that the wrapper does not provide it. */
internal fun KronosDataSourceWrapper.requireKronosRowMapping(): KronosRowMappingDataSourceWrapper =
    this as? KronosRowMappingDataSourceWrapper
        ?: throw UnsupportedOperationException(
            "KronosRow mapping requires a KronosRowMappingDataSourceWrapper, such as KronosJdbcWrapper"
        )
