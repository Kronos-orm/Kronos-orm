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
 * LimitClause
 *
 * Represents the LIMIT clause in a SQL query, used for pagination. Specifies the maximum number of
 * rows to return and optionally an offset.
 *
 * @property limit The maximum number of rows to return
 * @property offset Optional offset for pagination (number of rows to skip)
 *
 * @author OUSC
 */
data class LimitClause(val limit: Int, val offset: Int?)
