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

import com.kotlinorm.annotations.InternalKronosApi
import com.kotlinorm.beans.task.ResultColumnMetadata
import kotlin.reflect.KType

/**
 * Atomic query contract including the logical result type and planned column metadata.
 *
 * A data-source wrapper reads physical values first and then decodes each typed result value
 * once using [targetType] or the matching [resultColumns] entry. Untyped raw-map behavior is
 * wrapper-defined and must not be inferred from the erased classifier alone.
 */
interface KAtomicQueryTask : KAtomicTask {
    /**
     * Complete logical result type, including generic arguments and nullability.
     * Wrappers use this value to choose map, KPojo, or scalar mapping without erasing generics.
     */
    val targetType: KType

    /**
     * Runtime hints carried from query planning into JDBC parameter binding.
     * Implementations may override the default empty map for operation-local data.
     */
    val stash: MutableMap<String, Any?>
        get() = mutableMapOf()

    /**
     * Planned result metadata keyed by exact output label.
     *
     * Wrappers may fall back to a case-insensitive label match only when it is unambiguous.
     */
    @InternalKronosApi
    val resultColumns: Map<String, ResultColumnMetadata>
        get() = emptyMap()
}
