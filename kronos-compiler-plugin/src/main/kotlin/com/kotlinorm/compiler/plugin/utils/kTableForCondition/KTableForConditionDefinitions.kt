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

package com.kotlinorm.compiler.plugin.utils.kTableForCondition

import com.kotlinorm.compiler.helpers.extensionReceiver
import com.kotlinorm.compiler.helpers.invoke
import com.kotlinorm.compiler.helpers.irEnum
import com.kotlinorm.compiler.helpers.referenceClass
import com.kotlinorm.compiler.plugin.utils.getColumnOrValue
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.builders.IrBlockBuilder
import org.jetbrains.kotlin.ir.builders.irBoolean
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.getPropertySetter
import org.jetbrains.kotlin.ir.util.getSimpleFunction
import org.jetbrains.kotlin.ir.util.properties

const val KTABLE_FOR_CONDITION_CLASS = "com.kotlinorm.beans.dsl.KTableForCondition"

context(_: IrPluginContext)
internal val conditionTypeSymbol
    get() = referenceClass("com.kotlinorm.enums.ConditionType")!!

context(_: IrPluginContext)
private val kTableForConditionSymbol
    get() = referenceClass(KTABLE_FOR_CONDITION_CLASS)!!

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(_: IrPluginContext)
internal val criteriaSetterSymbol
    get() = kTableForConditionSymbol.getPropertySetter("criteria")!!

context(_: IrPluginContext)
private val criteriaClassSymbol
    get() = referenceClass("com.kotlinorm.beans.dsl.Criteria")!!

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(_: IrPluginContext)
private val addCriteriaChild
    get() = criteriaClassSymbol.getSimpleFunction("addChild")!!

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(_: IrPluginContext)
internal val stringPlusSymbol
    get() = referenceClass("kotlin.String")!!.getSimpleFunction("plus")!!

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(_: IrPluginContext)
internal val ComparableEq
    get() = kTableForConditionSymbol.owner.properties.first {
        it.name.toString() == "eq" && it.getter?.parameters?.extensionReceiver?.type?.classFqName?.asString() == "kotlin.Comparable"
        }

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(_: IrPluginContext)
internal val getValueByFieldNameSymbol
    get() = kTableForConditionSymbol.getSimpleFunction("getValueByFieldName")!!

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(_: IrPluginContext)
internal val buildContainsStrSymbol
    get() = kTableForConditionSymbol.getSimpleFunction("buildContainsStr")!!

/**
 * Parses the condition type based on the given function name.
 *
 * @param funcName The name of the function.
 * @return A pair containing the condition type and a boolean indicating whether the condition is negated.
 * @throws IllegalArgumentException If the condition type is unknown.
 */
context(_: IrPluginContext)
fun parseConditionType(funcName: String): Pair<String, Boolean> {
    return when (funcName) {
        "isNull" -> funcName to false
        "notNull" -> "isNull" to true
        "lt", "gt", "le", "ge" -> funcName to false
        "equal" -> "equal" to false
        "eq" -> "equal" to false
        "neq" -> "equal" to true
        "between" -> "between" to false
        "notBetween" -> "between" to true
        "like", "startsWith", "endsWith" -> "like" to false
        "notLike" -> "like" to true
        "contains" -> "in" to false // Extra judgment is needed to determine if it's `like` or `in`
        "asSql" -> "sql" to false
        "ifNoValue" -> "ifNoValue" to false
        "regexp" -> "regexp" to false
        "notRegexp" -> "regexp" to true
        else -> throw IllegalArgumentException("Unknown condition type: $funcName")
    }
}

/**
 * Creates a Criteria object with the given parameters.
 *
 * @param parameterName The parameter name for the Criteria object. Default is null.
 * @param type The type of the Criteria object.
 * @param not Whether the Criteria object is negated. Default is false.
 * @param value The value for the Criteria object. Default is null.
 * @param children The list of child Criteria objects. Default is an empty list.
 * @param tableName The table name for the Criteria object. Default is null.
 * @param noValueStrategyType The strategy for handling missing values. Default is null.
 * @return The created Criteria object.
 */
context(_: IrPluginContext, builder: IrBlockBuilder)
@OptIn(UnsafeDuringIrConstructionAPI::class)
fun createCriteria(
    parameterName: IrExpression? = null,
    type: String,
    not: Boolean,
    value: IrExpression? = null,
    children: List<IrVariable> = listOf(),
    tableName: IrExpression? = null,
    noValueStrategyType: IrExpression? = null
): IrVariable {
    //创建Criteria
    with(builder) {
        val irVariable = builder.irTemporary(
            criteriaClassSymbol.constructors.first()(
                parameterName,
                irEnum(conditionTypeSymbol, type),
                builder.irBoolean(not),
                value,
                tableName,
                noValueStrategyType
            )
        )
        //添加子条件
        children.forEach {
            +addCriteriaChild(
                irGet(irVariable),
                irGet(it)
            )
        }
        return irVariable
    }
}

/**
 * Analyzes the given expression by running it and returns a triple containing the analyzed left expression,
 * reversed operator, and analyzed right expression.
 *
 * @param left the left expression to analyze
 * @param operator the operator to analyze
 * @param right the right expression to analyze
 * @return a triple containing the analyzed left expression, reversed operator, and analyzed right expression
 */
context(_: IrPluginContext, builder: IrBlockBuilder)
fun runExpressionAnalysis(
    left: IrExpression?,
    operator: String,
    right: IrExpression?
): Triple<IrExpression?, String, IrExpression?> {
    if (!left.isKPojo() && right.isKPojo()) {
        return Triple(getColumnOrValue(right), getOperatorRevered(operator), getColumnOrValue(left))
    }

    return Triple(getColumnOrValue(left), operator, getColumnOrValue(right))
}

/**
 * Returns the reversed operator for the given operator.
 *
 * @param operator the operator to reverse
 * @return the reversed operator or the original operator if it cannot be reversed
 */
fun getOperatorRevered(operator: String): String {
    return when (operator) {
        "lt" -> "gt"
        "le" -> "ge"
        "ge" -> "le"
        "gt" -> "lt"
        else -> operator
    }
}