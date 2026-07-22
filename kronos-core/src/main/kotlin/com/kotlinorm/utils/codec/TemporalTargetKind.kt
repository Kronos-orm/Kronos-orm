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

package com.kotlinorm.utils.codec

/**
 * Semantic temporal targets supported by the built-in codec.
 *
 * Target matching is exact: in particular, an arbitrary subclass of
 * [java.util.Date] is not treated as constructible because producing a base
 * `Date` would violate the declared subclass KType. [STRING] and [LONG] are
 * output shapes and only participate when the source or physical target makes
 * the request temporal.
 */
internal enum class TemporalTargetKind(val temporalType: Boolean = true) {
    SQL_DATE,
    SQL_TIME,
    SQL_TIMESTAMP,
    UTIL_DATE,
    LOCAL_DATE_TIME,
    LOCAL_DATE,
    LOCAL_TIME,
    JAVA_INSTANT,
    ZONED_DATE_TIME,
    OFFSET_DATE_TIME,
    KOTLIN_INSTANT,
    LONG(false),
    STRING(false)
}
