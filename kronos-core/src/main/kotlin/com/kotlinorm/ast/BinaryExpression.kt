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
 * BinaryExpression
 *
 * Represents a binary operation expression (e.g., a = b, x + y, a AND b). Supports comparison,
 * logical, arithmetic, and other binary operators.
 *
 * @property left The left operand expression
 * @property operator The binary operator
 * @property right The right operand expression
 *
 * @author OUSC
 */
data class BinaryExpression(
        val left: Expression,
        val operator: SqlOperator,
        val right: Expression
) : Expression
