/**
 * Copyright 2022-2024 kronos-orm
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
 * Condition types.
 *
 * @author ousc
 */
enum class ConditionType {
    LIKE,
    EQUAL,
    IN,
    ISNULL,
    SQL,
    GT,
    GE,
    LT,
    LE,
    BETWEEN,
    AND,
    OR,
    ROOT
}

/**
 * Converts a string to a ConditionType enum value.
 *
 * @param str The string to convert.
 * @return The corresponding ConditionType enum value.
 * @throws IllegalArgumentException If the string does not match any of the enum values.
 */
internal fun toConditionType(str: String): ConditionType {
    return ConditionType.valueOf(str.uppercase())
}