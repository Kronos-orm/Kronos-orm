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
 * UpdateStatement
 *
 * Represents an UPDATE SQL statement in the AST. All fields are mutable to allow direct modification without copy().
 *
 * @property table The table reference to update
 * @property assignments List of column assignments (SET clause)
 * @property where Optional WHERE clause expression
 *
 * @author OUSC
 */
class UpdateStatement(
        var table: TableReference,
        var assignments: MutableList<Assignment> = mutableListOf(),
        var where: Expression? = null
) : Statement
