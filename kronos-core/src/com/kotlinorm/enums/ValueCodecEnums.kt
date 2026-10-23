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

package com.kotlinorm.enums

/**
 * Identifies the direction of one semantic value conversion.
 *
 * The direction is part of codec matching and is preserved in mapping errors.
 */
enum class ValueCodecDirection {
    /**
     * Converts a logical Kotlin value to a JDBC-bindable representation.
     * The logical target remains available even when a physical JDBC type is used.
     */
    ENCODE,
    /**
     * Converts a Map or physical database value to the logical Kotlin target type.
     * The complete target [kotlin.reflect.KType] controls output validation.
     */
    DECODE
}

/**
 * Identifies the boundary that created a value conversion request.
 *
 * Codecs can use the origin for fine-grained matching without maintaining
 * separate mapper, serializer, or transformer registries.
 */
enum class ValueCodecOrigin {
    /**
     * Safe Map or KPojo-to-KPojo mapping with declared target metadata.
     */
    MAP,
    /**
     * Typed JDBC result mapping after physical reader normalization.
     */
    DATABASE,
    /**
     * Serialized property delegate read or write through serialized storage.
     */
    DELEGATE,
    /**
     * ORM or raw SQL parameter preparation before JDBC binding.
     */
    PARAMETER
}

/**
 * Storage protocol gate applied before codec selection.
 *
 * A serialized request is handled only by a user codec registered for serialized
 * storage; it never falls through to scalar built-ins.
 */
enum class ValueStorage {
    /**
     * Normal scalar storage; enum, temporal, basic and identity built-ins may participate.
     */
    NONE,
    /**
     * Serialized text storage; only a matching user codec may participate.
     */
    SERIALIZED
}
