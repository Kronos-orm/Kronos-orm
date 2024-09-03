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

import com.kotlinorm.plugins.helpers.applyIrCall
import com.kotlinorm.plugins.helpers.dispatchBy
import com.kotlinorm.plugins.helpers.referenceClass
import com.kotlinorm.plugins.helpers.referenceFunctions
import com.kotlinorm.plugins.utils.kTable.correspondingName
import com.kotlinorm.plugins.utils.kTable.getColumnOrValue
import com.kotlinorm.plugins.utils.kTable.isKronosColumn
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrWhen
import org.jetbrains.kotlin.ir.expressions.impl.IrGetEnumValueImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.getPropertySetter
import org.jetbrains.kotlin.ir.util.getSimpleFunction

const val KTABLE_CONDITIONAL_CLASS = "com.kotlinorm.beans.dsl.KTableConditional"

context(IrBuilderWithScope, IrPluginContext)
internal val conditionTypeSymbol
    get() = referenceClass("com.kotlinorm.enums.ConditionType")!!

/**
 * Retrieves the condition type enum value based on the given type string.
 *
 * @param type The type string to retrieve the condition type for.
 * @return The IrExpression representing the condition type enum value.
 */
context(IrBuilderWithScope, IrPluginContext)
@OptIn(UnsafeDuringIrConstructionAPI::class)
internal fun getConditionType(type: String): IrExpression {
    val enumEntries = conditionTypeSymbol.owner.declarations.filterIsInstance<IrEnumEntry>()
    val enumEntry = enumEntries.find { it.name.asString() == type.uppercase() }!!
    return IrGetEnumValueImpl(startOffset, endOffset, conditionTypeSymbol.defaultType, enumEntry.symbol)
}

context(IrPluginContext)
@OptIn(UnsafeDuringIrConstructionAPI::class)
internal val criteriaSetterSymbol
    get() = referenceClass("com.kotlinorm.beans.dsl.KTableConditional")!!.getPropertySetter("criteria")!!

context(IrPluginContext)
private val criteriaClassSymbol
    get() = referenceClass("com.kotlinorm.beans.dsl.Criteria")!!

context(IrBuilderWithScope, IrPluginContext)
@OptIn(UnsafeDuringIrConstructionAPI::class)
private val addCriteriaChild
    get() = criteriaClassSymbol.getSimpleFunction("addChild")!!

context(IrPluginContext)
@OptIn(UnsafeDuringIrConstructionAPI::class)
internal val stringPlusSymbol
    get() = referenceClass("kotlin.String")!!.getSimpleFunction("plus")!!

/**
 * Returns a string representing the function name based on the IrExpression type and origin, with optional logic for setNot parameter.
 *
 * @param setNot a boolean value indicating whether to add the "not" prefix to the function name
 * @return a string representing the function name
 */
context(IrPluginContext)
@OptIn(UnsafeDuringIrConstructionAPI::class)
fun IrExpression.funcName(setNot: Boolean = false): String {
    return when (this) {
        is IrCall -> when (origin) {
            IrStatementOrigin.EQEQ, IrStatementOrigin.EXCLEQ -> "equal"
            IrStatementOrigin.GT -> "gt"
            IrStatementOrigin.LT -> "lt"
            IrStatementOrigin.GTEQ -> "ge"
            IrStatementOrigin.LTEQ -> "le"
            else -> correspondingName?.asString() ?: symbol.owner.name.asString()
        }

        is IrWhen -> when {
            (origin == IrStatementOrigin.OROR && !setNot) || (origin == IrStatementOrigin.ANDAND && setNot) -> "OR"
            (origin == IrStatementOrigin.ANDAND && !setNot) || (origin == IrStatementOrigin.OROR && setNot) -> "AND"
            else -> origin.toString()
        }

        else -> ""
    }

}

/**
 * Parses the condition type based on the given function name.
 *
 * @param funcName The name of the function.
 * @return A pair containing the condition type and a boolean indicating whether the condition is negated.
 * @throws IllegalArgumentException If the condition type is unknown.
 */
context(IrPluginContext)
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
        "like", "matchLeft", "matchRight", "matchBoth" -> "like" to false
        "notLike" -> "like" to true
        "contains" -> "in" to false
        "asSql" -> "sql" to false
        "ifNoValue" -> "ifNoValue" to false
        "regexp" -> "regexp" to false
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
 * @param noValueStrategy The strategy for handling missing values. Default is null.
 * @return The created Criteria object.
 */
context(IrBlockBuilder, IrPluginContext)
@OptIn(UnsafeDuringIrConstructionAPI::class)
fun createCriteria(
    parameterName: IrExpression? = null,
    type: String,
    not: Boolean,
    value: IrExpression? = null,
    children: List<IrVariable> = listOf(),
    tableName: IrExpression? = null,
    noValueStrategy: IrExpression? = null
): IrVariable {
    //创建Criteria
    val irVariable = irTemporary(
        applyIrCall(
            criteriaClassSymbol.constructors.first(),
            parameterName,
            getConditionType(type),
            irBoolean(not),
            value,
            tableName,
            noValueStrategy
        )
    )
    //添加子条件
    children.forEach {
        +applyIrCall(
            addCriteriaChild, irGet(it)
        ) {
            dispatchBy(irGet(irVariable))
        }
    }
    return irVariable
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
context(IrBlockBuilder, IrPluginContext)
fun runExpressionAnalysis(
    left: IrExpression?,
    operator: String,
    right: IrExpression?
): Triple<IrExpression?, String, IrExpression?> {
    if (!left.isKronosColumn() && right.isKronosColumn()) {
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