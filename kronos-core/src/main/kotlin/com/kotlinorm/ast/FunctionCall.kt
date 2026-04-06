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
 * FunctionCall
 *
 * Represents a SQL function call expression (e.g., COUNT(*), MAX(column), SUM(amount)). Supports
 * aggregate functions, scalar functions, and window functions.
 *
 * @property functionName The name of the function (e.g., "COUNT", "MAX", "SUM", "UPPER")
 * @property arguments List of function arguments (expressions)
 * @property distinct Whether to use DISTINCT keyword (for aggregate functions)
 * @property filter Optional FILTER clause (for aggregate functions with WHERE)
 * @property over Optional OVER clause for window functions
 *
 * @author OUSC
 */
data class FunctionCall(
        val functionName: String,
        val arguments: List<Expression> = emptyList(),
        val distinct: Boolean = false,
        val filter: Expression? = null,
        val over: WindowClause? = null
) : Expression

/**
 * WindowClause
 *
 * Represents a window function OVER clause.
 *
 * @property partitionBy Optional list of expressions for PARTITION BY
 * @property orderBy Optional list of order by items for ORDER BY
 * @property frame Optional window frame specification
 *
 * @author OUSC
 */
data class WindowClause(
        val partitionBy: List<Expression>? = null,
        val orderBy: List<OrderByItem>? = null,
        val frame: WindowFrame? = null
)

/**
 * WindowFrame
 *
 * Represents a window frame specification (ROWS, RANGE, etc.).
 *
 * @property type The frame type (ROWS, RANGE, GROUPS)
 * @property start The start boundary
 * @property end The end boundary
 * @property exclude Optional EXCLUDE clause
 *
 * @author OUSC
 */
sealed class WindowFrame {
    /**
     * FrameBoundary
     *
     * Represents a frame boundary.
     */
    sealed class FrameBoundary {
        /**
         * UnboundedPreceding
         *
         * UNBOUNDED PRECEDING
         */
        object UnboundedPreceding : FrameBoundary()

        /**
         * Preceding
         *
         * n PRECEDING
         */
        data class Preceding(val value: Expression) : FrameBoundary()

        /**
         * CurrentRow
         *
         * CURRENT ROW
         */
        object CurrentRow : FrameBoundary()

        /**
         * Following
         *
         * n FOLLOWING
         */
        data class Following(val value: Expression) : FrameBoundary()

        /**
         * UnboundedFollowing
         *
         * UNBOUNDED FOLLOWING
         */
        object UnboundedFollowing : FrameBoundary()
    }

    /**
     * FrameType
     *
     * Window frame type.
     */
    enum class FrameType {
        ROWS,
        RANGE,
        GROUPS
    }

    /**
     * BetweenFrame
     *
     * BETWEEN start AND end
     */
    data class BetweenFrame(
            val type: FrameType,
            val start: FrameBoundary,
            val end: FrameBoundary,
            val exclude: ExcludeType? = null
    ) : WindowFrame()

    /**
     * SingleBoundaryFrame
     *
     * Single boundary frame (e.g., ROWS UNBOUNDED PRECEDING)
     */
    data class SingleBoundaryFrame(val type: FrameType, val boundary: FrameBoundary) :
            WindowFrame()

    /**
     * ExcludeType
     *
     * EXCLUDE clause type.
     */
    enum class ExcludeType {
        CURRENT_ROW,
        GROUP,
        TIES,
        NO_OTHERS
    }
}
