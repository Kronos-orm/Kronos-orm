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
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.getPropertySetter
import org.jetbrains.kotlin.ir.util.getSimpleFunction

const val KTABLE_CONDITIONAL_CLASS = "com.kotlinorm.beans.dsl.KTableConditional"

context(IrPluginContext)
internal val criteriaSetterSymbol
    get() = referenceClass("com.kotlinorm.beans.dsl.KTableConditional")!!.getPropertySetter("criteria")!!

context(IrPluginContext)
private val criteriaClassSymbol
    get() = referenceClass("com.kotlinorm.beans.dsl.Criteria")!!

context(IrBuilderWithScope, IrPluginContext)
private val addCriteriaChild
    get() = criteriaClassSymbol.getSimpleFunction("addChild")!!

context(IrPluginContext)
private val string2ConditionTypeSymbol
    get() = referenceFunctions("com.kotlinorm.enums", "toConditionType").first()

context(IrPluginContext)
internal val stringPlusSymbol
    get() = referenceFunctions("kotlin.String", "plus").first()

/**
 * Returns a string representing the function name based on the IrExpression type and origin, with optional logic for setNot parameter.
 *
 * @param setNot a boolean value indicating whether to add the "not" prefix to the function name
 * @return a string representing the function name
 */
context(IrPluginContext)
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
            origin == IrStatementOrigin.OROR && !setNot -> "OR"
            origin == IrStatementOrigin.ANDAND && !setNot -> "AND"
            origin == IrStatementOrigin.OROR && setNot -> "AND"
            origin == IrStatementOrigin.ANDAND && setNot -> "OR"
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
            string2ConditionType(type),
            irBoolean(not),
            value,
            tableName,
            noValueStrategy
        )
    )
    //添加子条件
    children.forEach {
        +applyIrCall(
            addCriteriaChild,
            irGet(it)
        ) {
            dispatchBy(irGet(irVariable))
        }
    }
    return irVariable
}

/**
 * Converts a string to a condition type and returns an IrFunctionAccessExpression.
 *
 * @param str The string to be converted.
 * @return The IrFunctionAccessExpression representing the condition type.
 */
context(IrBuilderWithScope, IrPluginContext)
fun string2ConditionType(str: String): IrFunctionAccessExpression {
    return applyIrCall(string2ConditionTypeSymbol, irString(str))
}
