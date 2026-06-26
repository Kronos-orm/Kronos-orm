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
 * SqlOperator
 *
 * Enumeration of SQL operators used in expressions.
 *
 * @author OUSC
 */
enum class SqlOperator(val symbol: String) {
    // Comparison operators
    EQUAL("="),
    NOT_EQUAL("!="),
    NOT_EQUAL_ALT("<>"),
    GREATER_THAN(">"),
    GREATER_THAN_OR_EQUAL(">="),
    LESS_THAN("<"),
    LESS_THAN_OR_EQUAL("<="),

    // Logical operators
    AND("AND"),
    OR("OR"),

    // Arithmetic operators
    ADD("+"),
    SUBTRACT("-"),
    MULTIPLY("*"),
    DIVIDE("/"),
    MODULO("%"),

    // String operators
    CONCAT("||"),
    LIKE("LIKE"),
    NOT_LIKE("NOT LIKE"),
    ILIKE("ILIKE"),
    NOT_ILIKE("NOT ILIKE"),

    // Membership operators
    IN("IN"),
    NOT_IN("NOT IN"),

    // Null operators
    IS_NULL("IS NULL"),
    IS_NOT_NULL("IS NOT NULL"),

    // Range operators
    BETWEEN("BETWEEN"),
    NOT_BETWEEN("NOT BETWEEN"),

    // Pattern matching
    REGEXP("REGEXP"),
    NOT_REGEXP("NOT REGEXP"),

    // Bitwise operators
    BITWISE_AND("&"),
    BITWISE_OR("|"),
    BITWISE_XOR("^"),
    BITWISE_NOT("~"),
    BITWISE_LEFT_SHIFT("<<"),
    BITWISE_RIGHT_SHIFT(">>")
}

/**
 * UnaryOperator
 *
 * Enumeration of unary SQL operators.
 *
 * @author OUSC
 */
enum class UnaryOperator(val symbol: String) {
    NOT("NOT"),
    NEGATE("-"),
    POSITIVE("+"),
    BITWISE_NOT("~")
}
