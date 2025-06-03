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

package com.kotlinorm.functions

import com.kotlinorm.beans.dsl.FunctionField
import com.kotlinorm.exceptions.UnSupportedFunctionException
import com.kotlinorm.functions.bundled.builders.MathFunctionBuilder
import com.kotlinorm.functions.bundled.builders.PolymerizationFunctionBuilder
import com.kotlinorm.functions.bundled.builders.StringFunctionBuilder
import com.kotlinorm.interfaces.FunctionBuilder
import com.kotlinorm.interfaces.KronosDataSourceWrapper

/**
 *@program: kronos-orm
 *@author: Jieyao Lu
 *@description:
 *@create: 2024/10/21 15:35
 **/
object FunctionManager {
    private val registeredFunctionBuilders = mutableListOf<FunctionBuilder>(
        PolymerizationFunctionBuilder,
        MathFunctionBuilder,
        StringFunctionBuilder
    )

    fun registerFunctionBuilder(transformer: FunctionBuilder) {
        registeredFunctionBuilders.add(0, transformer)
    }

    fun getBuiltFunctionField(
        field: FunctionField, dataSource: KronosDataSourceWrapper,
        showTable: Boolean = false, showAlias: Boolean = true
    ): String {
        return registeredFunctionBuilders.firstOrNull { it.support(field, dataSource.dbType) }
            ?.transform(field, dataSource, showTable, showAlias)
            ?: throw UnSupportedFunctionException(dataSource.dbType, field.functionName)
    }
}