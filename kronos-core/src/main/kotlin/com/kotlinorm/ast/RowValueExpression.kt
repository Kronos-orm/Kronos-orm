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

package com.kotlinorm.ast

/**
 * Row-value expression used by tuple predicates such as `(a, b) IN (SELECT x, y ...)`.
 */
data class RowValueExpression(
    val values: List<Expression>
) : Expression {
    init {
        require(values.size > 1) {
            "Row-value tuple requires at least two expressions; use a plain expression for single-column IN."
        }
    }
}
