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

import com.kotlinorm.enums.KColumnType
import com.kotlinorm.enums.PrimaryKeyType

/**
 * ColumnDefinition
 *
 * Represents a column definition in CREATE TABLE or ALTER TABLE statements.
 *
 * @property name The column name
 * @property type The column data type
 * @property length Optional length for the column type
 * @property scale Optional scale for numeric types
 * @property nullable Whether the column allows NULL values
 * @property primaryKey The primary key type (NOT, DEFAULT, IDENTITY, etc.)
 * @property defaultValue Optional default value expression
 *
 * @author OUSC
 */
data class ColumnDefinition(
        val name: String,
        val type: KColumnType,
        val length: Int = 0,
        val scale: Int = 0,
        val nullable: Boolean = true,
        val primaryKey: PrimaryKeyType = PrimaryKeyType.NOT,
        val defaultValue: Expression? = null
)
