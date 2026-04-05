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

import com.kotlinorm.beans.dsl.Criteria
import com.kotlinorm.interfaces.DatabasesSupport
import com.kotlinorm.interfaces.KronosDataSourceWrapper

/**
 * AstSqlRenderer
 *
 * Compatibility object for AST-based SQL rendering. Provides type aliases and utility methods for
 * backward compatibility.
 *
 * @author OUSC
 */
object AstSqlRenderer {
    /** RenderedSql type alias for backward compatibility. */
    typealias RenderedSql = com.kotlinorm.ast.RenderedSql

    /**
     * Render criteria directly using the database support's renderer. This is a compatibility
     * method that delegates to the database support's renderer.
     *
     * @param dataSource The data source wrapper
     * @param support The database support implementation
     * @param criteria The criteria to render
     * @return The rendered SQL string
     */
    fun renderCriteriaDirect(
            dataSource: KronosDataSourceWrapper,
            support: DatabasesSupport,
            criteria: Criteria
    ): String {
        // This is a placeholder - actual implementation would convert Criteria to AST
        // For now, we'll return an empty string or delegate to existing implementation
        // TODO: Implement Criteria to AST conversion
        return ""
    }
}
