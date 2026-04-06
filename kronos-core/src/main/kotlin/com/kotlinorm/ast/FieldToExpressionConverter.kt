/**
 * Copyright 2022-2025 kronos-orm
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

import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.dsl.FunctionField
import com.kotlinorm.enums.KColumnType

/**
 * FieldToExpressionConverter
 *
 * Utility object for converting Field objects to AST Expression nodes.
 * Handles special field types like CUSTOM_CRITERIA_SQL and FunctionField.
 *
 * @author OUSC
 */
object FieldToExpressionConverter {
    
    /**
     * Converts a Field to an Expression.
     * 
     * For FunctionField, creates a FunctionCall expression with arguments.
     * For CUSTOM_CRITERIA_SQL fields, the columnName contains a literal SQL value
     * that should be rendered as-is (e.g., "123" becomes the number 123).
     * For regular fields, creates a ColumnReference.
     *
     * @param field The field to convert
     * @param useTableAlias Whether to include table alias in column reference (for JOIN queries)
     * @return The converted Expression
     */
    fun fieldToExpression(field: Field, useTableAlias: Boolean = false): Expression {
        // Handle FunctionField - convert to FunctionCall
        if (field is FunctionField) {
            val arguments = field.fields.flatMap { (argField, argValue) ->
                when {
                    // If argField is present, it's a column reference
                    argField != null -> listOf(fieldToExpression(argField, useTableAlias))
                    // If argValue is present, convert it to a literal
                    argValue != null -> {
                        when (argValue) {
                            is String -> listOf(Literal.StringLiteral(argValue))
                            is Number -> listOf(Literal.NumberLiteral(argValue.toString()))
                            is Boolean -> listOf(Literal.BooleanLiteral(argValue))
                            is Collection<*> -> argValue.filterNotNull().map { convertValueToExpression(it) }
                            is Array<*> -> argValue.filterNotNull().map { convertValueToExpression(it) }
                            is IntArray -> argValue.map { Literal.NumberLiteral(it.toString()) }
                            is LongArray -> argValue.map { Literal.NumberLiteral(it.toString()) }
                            is ShortArray -> argValue.map { Literal.NumberLiteral(it.toString()) }
                            is FloatArray -> argValue.map { Literal.NumberLiteral(it.toString()) }
                            is DoubleArray -> argValue.map { Literal.NumberLiteral(it.toString()) }
                            is BooleanArray -> argValue.map { Literal.BooleanLiteral(it) }
                            is ByteArray -> argValue.map { Literal.NumberLiteral(it.toString()) }
                            else -> listOf(Literal.StringLiteral(argValue.toString()))
                        }
                    }
                    else -> listOf(Literal.NullLiteral)
                }
            }
            return FunctionCall(
                functionName = field.functionName,
                arguments = arguments
            )
        }
        
        return when (field.type) {
            KColumnType.CUSTOM_CRITERIA_SQL -> {
                // For CUSTOM_CRITERIA_SQL fields, the columnName contains the literal SQL value
                // Parse it as a literal (number or string) or raw SQL
                val literalValue = field.columnName
                when {
                    // Try to parse as integer
                    literalValue.toIntOrNull() != null -> {
                        Literal.NumberLiteral(literalValue)
                    }
                    // Try to parse as double
                    literalValue.toDoubleOrNull() != null -> {
                        Literal.NumberLiteral(literalValue)
                    }
                    // Check if it's a boolean
                    literalValue.equals("true", ignoreCase = true) || 
                    literalValue.equals("false", ignoreCase = true) -> {
                        Literal.BooleanLiteral(literalValue.toBoolean())
                    }
                    // Check if it's NULL
                    literalValue.equals("null", ignoreCase = true) -> {
                        Literal.NullLiteral
                    }
                    // Otherwise treat as raw SQL (not a string literal)
                    // This allows SQL fragments like "COUNT(1) as count" to be rendered without quotes
                    else -> {
                        SpecialExpression.RawSqlExpression(literalValue)
                    }
                }
            }
            else -> {
                // Regular field - create ColumnReference
                ColumnReference(
                    tableAlias = if (useTableAlias) field.tableName.takeIf { it.isNotEmpty() } else null,
                    columnName = field.columnName
                )
            }
        }
    }
    
    /**
     * Converts a value to an Expression.
     * Used for function arguments that are not Fields.
     *
     * @param value The value to convert
     * @return The converted Expression
     */
    private fun convertValueToExpression(value: Any): Expression {
        return when (value) {
            is Expression -> value
            is String -> Literal.StringLiteral(value)
            is Boolean -> Literal.BooleanLiteral(value)
            is Number -> Literal.NumberLiteral(value.toString())
            is Char -> Literal.StringLiteral(value.toString())
            else -> Literal.StringLiteral(value.toString())
        }
    }
    
    /**
     * Converts a Field to a ColumnReference.
     * 
     * For CUSTOM_CRITERIA_SQL fields and FunctionField, this will throw an exception
     * as they should not be used as column references.
     *
     * @param field The field to convert
     * @param useTableAlias Whether to include table alias (for JOIN queries)
     * @return The ColumnReference
     * @throws IllegalArgumentException if field is CUSTOM_CRITERIA_SQL or FunctionField
     */
    fun fieldToColumnReference(field: Field, useTableAlias: Boolean = false): ColumnReference {
        if (field is FunctionField) {
            throw IllegalArgumentException(
                "FunctionField cannot be used as column reference. " +
                "Function: ${field.functionName}"
            )
        }
        
        if (field.type == KColumnType.CUSTOM_CRITERIA_SQL) {
            throw IllegalArgumentException(
                "CUSTOM_CRITERIA_SQL fields cannot be used as column references. " +
                "Field: ${field.columnName}"
            )
        }
        
        return ColumnReference(
            tableAlias = if (useTableAlias) field.tableName.takeIf { it.isNotEmpty() } else null,
            columnName = field.columnName
        )
    }
}
