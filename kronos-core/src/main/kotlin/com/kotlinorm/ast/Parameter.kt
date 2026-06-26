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
 * Parameter
 *
 * Represents a parameter placeholder in SQL (e.g., :paramName or ?). Used for prepared statements
 * and parameterized queries.
 *
 * @property name The parameter name (without the colon prefix)
 * @property index Optional positional index for positional parameters (?)
 *
 * @author OUSC
 */
sealed class Parameter : Expression {
    /**
     * NamedParameter
     *
     * Represents a named parameter (e.g., :userId, :name).
     *
     * @property name The parameter name (without the colon prefix)
     */
    data class NamedParameter(val name: String) : Parameter()

    /**
     * PositionalParameter
     *
     * Represents a positional parameter (e.g., ?).
     *
     * @property index The parameter index (0-based or 1-based depending on database)
     */
    data class PositionalParameter(val index: Int) : Parameter()
}
