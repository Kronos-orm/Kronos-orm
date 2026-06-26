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
import com.kotlinorm.ast.FunctionCall
import com.kotlinorm.ast.RenderContext
import com.kotlinorm.beans.dsl.FunctionField
import com.kotlinorm.enums.DBType

/**
 *@program: kronos-orm
 *@author: Jieyao Lu
 *@description:
 *@create: 2024/10/21 16:05
 **/
interface FunctionBuilder {
    val supportFunctionNames: (String) -> Array<DBType>

    fun support(field: FunctionField, dbType: DBType): Boolean {
        return supportFunctionNames(field.functionName).contains(dbType)
    }
    
    fun support(functionName: String, dbType: DBType): Boolean {
        return supportFunctionNames(functionName).contains(dbType)
    }

    /**
     * Transform FunctionField to SQL string (legacy DSL-based API)
     */
    fun transform(
        field: FunctionField,
        dataSource: KronosDataSourceWrapper,
        showTable: Boolean,
        showAlias: Boolean
    ): String
    
    /**
     * Transform FunctionCall AST node to SQL string (new AST-based API)
     * 
     * @param function The FunctionCall AST node to transform
     * @param context The render context containing database type and rendering configuration
     * @param renderExpression Callback to render nested expressions
     * @return The rendered SQL string for this function call
     */
    fun transformAst(
        function: FunctionCall,
        context: RenderContext,
        renderExpression: (Expression, RenderContext) -> String
    ): String? = null // Default implementation returns null (not supported)

}