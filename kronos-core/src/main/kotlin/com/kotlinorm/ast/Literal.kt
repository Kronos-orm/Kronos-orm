/**
 * Copyright 2022-2025 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * ```
 *     http://www.apache.org/licenses/LICENSE-2.0
 * ```
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.kotlinorm.ast

/**
 * Literal
 *
 * Sealed class representing SQL literal values (constants). Includes string literals, numeric
 * literals, boolean literals, and NULL.
 *
 * @author OUSC
 */
sealed class Literal : Expression {
    /**
     * StringLiteral
     *
     * Represents a string literal value.
     *
     * @property value The string value
     */
    data class StringLiteral(val value: String) : Literal()

    /**
     * NumberLiteral
     *
     * Represents a numeric literal value. Can represent integers, decimals, etc.
     *
     * @property value The numeric value as a string (to preserve precision)
     */
    data class NumberLiteral(val value: String) : Literal()

    /**
     * BooleanLiteral
     *
     * Represents a boolean literal value (TRUE or FALSE).
     *
     * @property value The boolean value
     */
    data class BooleanLiteral(val value: Boolean) : Literal()

    /**
     * NullLiteral
     *
     * Represents a NULL literal value.
     */
    object NullLiteral : Literal()

    /**
     * DateLiteral
     *
     * Represents a date literal value.
     *
     * @property value The date value as a string (format depends on database)
     */
    data class DateLiteral(val value: String) : Literal()

    /**
     * TimeLiteral
     *
     * Represents a time literal value.
     *
     * @property value The time value as a string (format depends on database)
     */
    data class TimeLiteral(val value: String) : Literal()

    /**
     * TimestampLiteral
     *
     * Represents a timestamp literal value.
     *
     * @property value The timestamp value as a string (format depends on database)
     */
    data class TimestampLiteral(val value: String) : Literal()
}
