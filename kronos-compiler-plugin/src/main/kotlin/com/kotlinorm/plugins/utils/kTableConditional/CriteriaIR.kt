/**
 * Copyright 2022-2024 kronos-orm
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

package com.kotlinorm.plugins.utils.kTableConditional

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.builders.IrBlockBuilder
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrExpression

/**
 * Criteria IR
 *
 * Constructing a condition IR, which can be used to specify how parts of the IR should be built based on certain criteria.
 * @author: OUSC, Jieyao Lu
 */
class CriteriaIR(
    // The name of the parameter
    // 参数的名称
    private var parameterName: IrExpression? = null,
    // The type of the criterion
    // 条件的类型
    private var type: String,
    // Whether the condition is negated
    // 是否对条件进行否定
    var not: Boolean,
    // The value to compare with, optional
    // 用于比较的值，可选
    private val value: IrExpression? = null,
    // List of child variables, optional
    // 子变量列表，可选
    private val children: List<IrVariable> = listOf(),
    // The name of the table, optional
    // 表的名称，可选
    private var tableName: IrExpression? = null,

    private var noValueStrategy: IrExpression? = null
) {

    /**
     * Converts the current object to an IrVariable by creating a criteria using the provided parameters.
     *
     * @return an IrVariable representing the created criteria
     */
    context(IrBlockBuilder, IrPluginContext)
    fun toIrVariable(): IrVariable {
        return createCriteria(parameterName, type, not, value, children, tableName, noValueStrategy)
    }
}