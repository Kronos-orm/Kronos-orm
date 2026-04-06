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
 * UnaryExpression
 *
 * Represents a unary operation expression (e.g., NOT x, -y, +z). Supports logical negation,
 * arithmetic negation, and other unary operators.
 *
 * @property operator The unary operator
 * @property operand The operand expression
 *
 * @author OUSC
 */
data class UnaryExpression(val operator: UnaryOperator, val operand: Expression) : Expression
