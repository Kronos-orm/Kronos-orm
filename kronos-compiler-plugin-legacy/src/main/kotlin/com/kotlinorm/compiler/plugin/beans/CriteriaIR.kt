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

package com.kotlinorm.compiler.plugin.beans

import com.kotlinorm.compiler.plugin.utils.kTableForCondition.createCriteria
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.builders.IrBlockBuilder
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrExpression

/**
 * Criteria IR
 *
 * Constructing a condition IR, which can be used to specify how parts of the IR should be built based on certain criteria.
 *
 * @property parameterName The name of the parameter
 * @property type The type of the criterion
 * @property not Whether the condition is negated
 * @property value The value to compare with, optional
 * @property children List of child variables, optional
 * @property tableName The name of the table, optional
 * @property noValueStrategyType The strategy for handling missing values
 * @author: OUSC, Jieyao Lu
 */
class CriteriaIR(
    var parameterName: IrExpression? = null,
    var type: String,
    var not: Boolean,
    val value: IrExpression? = null,
    val children: List<IrVariable> = listOf(),
    var tableName: IrExpression? = null,
    var noValueStrategyType: IrExpression? = null
) {
    /**
     * Converts the current object to an IrVariable by creating a criteria using the provided parameters.
     *
     * @return an IrVariable representing the created criteria
     */
    context(_: IrPluginContext, builder: IrBlockBuilder)
    fun toIrVariable(): IrVariable {
        return createCriteria(parameterName, type, not, value, children, tableName, noValueStrategyType)
    }
}