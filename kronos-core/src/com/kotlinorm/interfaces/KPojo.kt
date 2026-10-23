/**
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

package com.kotlinorm.interfaces

import com.kotlinorm.Kronos.createTimeStrategy
import com.kotlinorm.Kronos.logicDeleteStrategy
import com.kotlinorm.Kronos.optimisticLockStrategy
import com.kotlinorm.Kronos.updateTimeStrategy
import com.kotlinorm.annotations.Ignore
import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.KTableIndex
import com.kotlinorm.enums.IgnoreAction
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * Runtime contract implemented by compiler-generated Kronos table models.
 *
 * The compiler plugin supplies metadata, field access and map conversion for a
 * concrete KPojo. The default bodies exist only so dynamic/manual KPojo
 * implementations can override the required metadata explicitly; generated
 * models must not rely on those defaults.
 */
interface KPojo {
    /**
     * Complete declared type used by factory, mapper and metadata lookup.
     *
     * The compiler plugin generates the backing value. Dynamic KPojo instances
     * should expose `typeOf<KPojo>()` as their runtime marker rather than a
     * guessed subclass; top-level nullability is normalized only at lookup boundaries.
     */
    @Suppress("PropertyName")
    @Ignore([IgnoreAction.ALL])
    var __kType: KType
        get() = typeOf<KPojo>()
        set(_) {}

    /**
     * Converts mapped properties to a mutable name-to-value map.
     *
     * Generated implementations preserve field names and apply the same value
     * mapping metadata used by query results. The map may contain null values.
     *
     * @return mutable map containing this object's mapped values
     */
    fun toDataMap() = mutableMapOf<String, Any?>()

    /**
     * Applies a result map with safe generated conversion semantics.
     *
     * This variant is used at boundaries where a failed assignment should be
     * reported as a mapping failure instead of relying on an unchecked cast.
     *
     * @param map source values keyed by generated field name or column name
     * @return this KPojo after generated assignments, typed as [T]
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : KPojo> safeFromMapData(map: Map<String, Any?>) = this as T

    /**
     * Applies a result map through the generated field-assignment path.
     *
     * @param map source values keyed by generated field name or column name
     * @return this KPojo after generated assignments, typed as [T]
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : KPojo> fromMapData(map: Map<String, Any?>) = this as T

    /**
     * Reads one mapped property by generated field or column name.
     *
     * @param name field name or mapped column name
     * @return current property value, or `null` when the property is null or unknown
     */
    operator fun get(name: String): Any? = null

    /**
     * Assigns one mapped property by generated field or column name.
     *
     * Generated implementations apply the property's complete KType and
     * storage metadata before assignment.
     *
     * @param name field name or mapped column name
     * @param value value to assign; `null` is accepted only by nullable fields
     */
    operator fun set(name: String, value: Any?){}

    /**
     * Physical table name used by SQL rendering and runtime metadata lookup.
     *
     * The compiler plugin generates this value from table/naming annotations;
     * dynamic implementations must provide it explicitly.
     */
    @Suppress("PropertyName")
    @Ignore([IgnoreAction.ALL])
    var __tableName: String
        get() = error("__tableName must be overridden by the compiler plugin")
        set(_) {}

    /**
     * Optional table comment carried into DDL metadata.
     *
     * The generated value is stable for the model and may be blank when no
     * table comment is declared.
     */
    @Suppress("PropertyName")
    @Ignore([IgnoreAction.ALL])
    var __tableComment: String
        get() = error("__tableComment must be overridden by the compiler plugin")
        set(_) {}

    /**
     * Ordered field metadata for this table model.
     *
     * The list includes mapped and non-column fields needed by the DSL; callers
     * that need database columns must filter by [Field.isColumn].
     */
    @Suppress("PropertyName")
    @Ignore([IgnoreAction.ALL])
    var __columns: MutableList<Field>
        get() = mutableListOf()
        set(_) {}

    /**
     * Declared table-index metadata used by DDL synchronization.
     */
    @Suppress("PropertyName")
    @Ignore([IgnoreAction.ALL])
    var __tableIndexes: MutableList<KTableIndex>
        get() = mutableListOf()
        set(_) {}

    /**
     * Strategy metadata for automatic creation-time values.
     *
     * Disabled strategies remain represented so callers can distinguish an
     * absent strategy from one configured by global policy.
     */
    @Suppress("PropertyName")
    @Ignore([IgnoreAction.ALL])
    var __createTime: KronosCommonStrategy
        get() = createTimeStrategy
        set(_) {}

    /**
     * Strategy metadata for automatic update-time values.
     */
    @Suppress("PropertyName")
    @Ignore([IgnoreAction.ALL])
    var __updateTime: KronosCommonStrategy
        get() = updateTimeStrategy
        set(_) {}

    /**
     * Strategy metadata for logical-delete writes and query filtering.
     */
    @Suppress("PropertyName")
    @Ignore([IgnoreAction.ALL])
    var __logicDelete: KronosCommonStrategy
        get() = logicDeleteStrategy
        set(_) {}

    /**
     * Strategy metadata for optimistic-lock version increments and matching.
     */
    @Suppress("PropertyName")
    @Ignore([IgnoreAction.ALL])
    var __optimisticLock: KronosCommonStrategy
        get() = optimisticLockStrategy
        set(_) {}
}
