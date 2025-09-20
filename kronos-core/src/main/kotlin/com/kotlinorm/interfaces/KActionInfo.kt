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

package com.kotlinorm.interfaces

import com.kotlinorm.ast.Expression
import com.kotlinorm.ast.Statement
import kotlin.reflect.KClass

interface KActionInfo {
    val kClass: KClass<out KPojo>?
    /** Optional: direct AST statement if available (preferred). */
    val statement: Statement?
    /**
     * Fallback tableName and where for legacy paths when statement is null.
     */
    val tableName: String?
    /**
     * Where condition represented as AST Expression. Null means no WHERE.
     */
    val where: Expression?
}