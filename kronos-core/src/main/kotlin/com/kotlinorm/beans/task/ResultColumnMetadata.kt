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

package com.kotlinorm.beans.task

import com.kotlinorm.annotations.InternalKronosApi
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.enums.ValueStorage
import kotlin.reflect.KType

/**
 * Logical metadata carried from query planning to the JDBC decode boundary.
 *
 * [type] is the complete Kotlin declaration type to produce. [field] retains
 * source-field policies such as `dateFormat` and `@Serialize`, including for
 * aliased generated projections. [columnLabel] is the exact planned output
 * label; JDBC lookup may use it case-insensitively only when the match is unique.
 *
 * [storage] may be supplied without a field for internal metadata, but whenever
 * [field] is present it must agree with that field's serialization policy.
 *
 * @property type complete logical result KType, including generic arguments and nullability
 * @property field optional field metadata used for date format and serialized storage
 * @property storage storage gate used by the single ValueCodec decode pass
 * @property columnLabel planned JDBC label included in lookup and error context
 * @throws IllegalArgumentException when [storage] contradicts a supplied [field]
 */
@InternalKronosApi
data class ResultColumnMetadata(
    val type: KType,
    val field: Field? = null,
    val storage: ValueStorage = field.storage(),
    val columnLabel: String? = null
) {
    init {
        if (field != null) {
            require(storage == field.storage()) {
                "Value storage $storage conflicts with field '${field.name}' storage ${field.storage()}"
            }
        }
    }
}

private fun Field?.storage(): ValueStorage =
    if (this?.serializable == true) ValueStorage.SERIALIZED else ValueStorage.NONE
