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

import com.kotlinorm.compiler.helpers.dispatchReceiverArgument
import com.kotlinorm.compiler.helpers.extensionReceiver
import com.kotlinorm.compiler.helpers.extensionReceiverArgument
import com.kotlinorm.compiler.helpers.invoke
import com.kotlinorm.compiler.helpers.irCast
import com.kotlinorm.compiler.helpers.sub
import com.kotlinorm.compiler.helpers.valueArguments
import com.kotlinorm.compiler.plugin.beans.CriteriaIR
import com.kotlinorm.compiler.plugin.utils.ARRAY_OR_COLLECTION_FQ_NAMES
import com.kotlinorm.compiler.plugin.utils.CascadeAnnotationsFqName
import com.kotlinorm.compiler.plugin.utils.IgnoreAnnotationsFqName
import com.kotlinorm.compiler.plugin.utils.KPojoFqName
import com.kotlinorm.compiler.plugin.utils.KronosColumnValueType
import com.kotlinorm.compiler.plugin.utils.SerializeAnnotationsFqName
import com.kotlinorm.compiler.plugin.utils.getColumnOrValue
import com.kotlinorm.compiler.plugin.utils.getTableName
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.IrBlockBuilder
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrPropertyReference
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrWhen
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetEnumValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrWhenImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.ir.util.superTypes
import org.jetbrains.kotlin.name.FqName

/**
 * Generates IR for setting simple criteria.
 *
 * This function applies an IR call to set the criteria using the criteriaSetterSymbol.
 * It takes the result of building the criteria using the body of the current function.
 * The criteria are then dispatched by getting the extension receiver parameter.
 *
 * @return the IR expression for setting the criteria
 * @author OUSC
 */

context(_: IrPluginContext, builder: IrBlockBuilder)
fun updateCriteriaIr(irFunction: IrFunction) = criteriaSetterSymbol(
    builder.irGet(irFunction.parameters.extensionReceiver!!),
    builder.irGet(buildCriteria(irFunction, irFunction.body!!)!!)
)

/**
 * Builds a criteria IR variable based on the given element.
 *
 * This function recursively builds a criteria IR variable based on the given element and its children.
 * It handles different types of elements such as IrBlockBody, IrIfThenElseImpl, IrCall, IrReturn, and IrConstImpl.
 * The criteria are built based on the function name and the value arguments of the IrCall element.
 * The criteria type and not flag are determined based on the function name.
 * The column name, table name, value, and children are extracted from the element and its arguments.
 * The criteria IR variable is then returned.
 *
 * @param element The element to build the criteria IR from.
 * @param setNot Whether to set the not flag. Default is false.
 * @param noValueStrategyType The strategy to use when there is no value. Default is null.
 * @return The built criteria IR variable, or null if the element is a constant.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(_: IrPluginContext, builder: IrBlockBuilder)
fun buildCriteria(
    irFunction: IrFunction,
    element: IrElement,
    setNot: Boolean = false,
    noValueStrategyType: IrExpression? = null
): IrVariable? {
    with(builder) {
        with(
            LocalState(
                paramName = null,
                type = "ROOT",
                not = setNot,
                value = null,
                children = mutableListOf(),
                tableName = null,
                strategy = noValueStrategyType
            )
        ) {
            when (element) {
                is IrBlockBody -> handleIrBlockBody(irFunction, element)
                is IrWhenImpl -> handleIrWhen(irFunction, element, setNot)
                is IrCall -> handleIrCall(irFunction, element)?.let { return it }
                is IrReturn -> return buildCriteria(irFunction, element.value)
                is IrConstImpl -> handleIrConst(element)
            }

            return CriteriaIR(
                paramName,
                type,
                not,
                value,
                children.filterNotNull(),
                tableName,
                strategy
            ).toIrVariable()
        }
    }
}

/**
 * A local state for building a criteria.
 *
 * @property paramName The name of the parameter.
 * @property type The type of the criteria.
 * @property not Whether to set the not flag.
 * @property value The value of the criteria.
 * @property children The children of the criteria.
 * @property tableName The name of the table.
 * @property strategy The strategy to use when there is no value.
 * @constructor Creates a new instance of [LocalState].
 */
private data class LocalState(
    var paramName: IrExpression?,
    var type: String,
    var not: Boolean,
    var value: IrExpression?,
    val children: MutableList<IrVariable?>,
    var tableName: IrExpression?,
    var strategy: IrExpression?
)

/**
 * Handles an IrBlockBody element.
 * This function recursively handles the children of the IrBlockBody element and builds a criteria IR variable based on the result.
 *
 * @param irFunction The function to build the criteria for.
 * @param element The IrBlockBody element to handle.
 * @throws IllegalArgumentException if the element is not an IrBlockBody element.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(_: IrPluginContext, builder: IrBlockBuilder, state: LocalState)
private fun handleIrBlockBody(
    irFunction: IrFunction,
    element: IrBlockBody
) {
    element.statements.forEach { statement ->
        state.children.add(buildCriteria(irFunction, statement))
    }
}

/**
 * Handles an IrWhen element.
 * This function recursively handles the children of the IrWhen element and builds a criteria IR variable based on the result.
 *
 * @param irFunction The function to build the criteria for.
 * @param element The IrWhen element to handle.
 * @param setNot Whether to set the not flag.
 * @throws IllegalArgumentException if the element is not an IrWhen element.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(_: IrPluginContext, builder: IrBlockBuilder, state: LocalState)
private fun handleIrWhen(
    irFunction: IrFunction,
    element: IrWhenImpl,
    setNot: Boolean
) {
    state.type = element.funcName(setNot)
    element.branches.forEach {
        state.children.add(buildCriteria(irFunction, it.condition, setNot))
        state.children.add(buildCriteria(irFunction, it.result, setNot))
    }
}

/**
 * Handles an IrCall element.
 * This function builds a criteria IR variable based on the given IrCall element.
 *
 * @param irFunction The function to build the criteria for.
 * @param element The IrCall element to handle.
 * @return The built criteria IR variable, or null if the element is a constant.
 * @throws IllegalArgumentException if the element is not an IrCall element.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(_: IrPluginContext, builder: IrBlockBuilder, state: LocalState)
private fun handleIrCall(
    irFunction: IrFunction,
    element: IrCall
): IrVariable? {
    val funcName = element.funcName()
    var args = element.valueArguments
    val dispatchReceiver = element.dispatchReceiverArgument
    val extensionReceiver = element.extensionReceiverArgument
    if (args.isEmpty() && dispatchReceiver != null && dispatchReceiver is IrCall) {
        args = dispatchReceiver.arguments
    }

    if ("not" == funcName) {
        return buildCriteria(irFunction, dispatchReceiver!!, !state.not)
    }

    val (conditionType, isNot) = parseConditionType(funcName)
    state.type = conditionType
    state.not = state.not xor isNot

    when (funcName) {
        "isNull", "notNull" -> handleNullChecks(extensionReceiver, dispatchReceiver)
        "lt", "gt", "le", "ge" -> handleCompareOps(irFunction, funcName, extensionReceiver, dispatchReceiver, args)
        "equal" -> handleEqualOp(element, funcName, args)
        "eq", "neq" -> handleEqNeqOp(irFunction, element, extensionReceiver)
        "between", "like", "regexp", "notBetween", "notLike", "notRegexp" -> handleBetweenLikeRegexpOps(irFunction, element, extensionReceiver, args)
        "startsWith" -> handleStartsWithOp(irFunction, element, extensionReceiver, args)
        "endsWith" -> handleEndsWithOp(irFunction, extensionReceiver, dispatchReceiver, args)
        "contains" -> handleContainsOp(irFunction, extensionReceiver, dispatchReceiver, args)
        "asSql" -> handleAsSqlOp(extensionReceiver)
        "ifNoValue" -> return handleIfNoValueOp(irFunction, extensionReceiver, args)
    }
    return null
}

/**
 * Handles null checks.
 * This function builds a criteria IR variable based on the given null checks.
 *
 * @param extensionReceiver The extension receiver of the null check.
 * @param dispatchReceiver The dispatch receiver of the null check.
 * @throws IllegalArgumentException if the extension receiver is not an IrCall element.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(_: IrPluginContext, builder: IrBlockBuilder, state: LocalState)
private fun handleNullChecks(
    extensionReceiver: IrExpression?,
    dispatchReceiver: IrExpression?
) {
    state.paramName = getColumnOrValue(extensionReceiver!!)
    state.tableName = getTableName(dispatchReceiver!!.type.sub()!!.getClass()!!)
}

/**
 * Handles compare operations.
 * This function builds a criteria IR variable based on the given compare operations.
 *
 * @param irFunction The function to build the criteria for.
 * @param funcName The name of the compare operation.
 * @param extensionReceiver The extension receiver of the compare operation.
 * @param dispatchReceiver The dispatch receiver of the compare operation.
 * @param args The arguments of the compare operation.
 * @throws IllegalArgumentException if the extension receiver is not an IrCall element.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(_: IrPluginContext, builder: IrBlockBuilder, state: LocalState)
private fun handleCompareOps(
    irFunction: IrFunction,
    funcName: String,
    extensionReceiver: IrExpression?,
    dispatchReceiver: IrExpression?,
    args: List<IrExpression?>
) {
    if (args.isEmpty()) {
        state.paramName = getColumnOrValue(extensionReceiver!!)
        state.tableName = getTableName(dispatchReceiver!!.type.sub()!!.getClass()!!)
        state.value = getValueByFieldNameSymbol(
            builder.irGet(irFunction.parameters.extensionReceiver!!),
            builder.irString(extensionReceiver.irCast<IrCall>().funcName())
        )
    } else {
        val irCall = args.first()!!.irCast<IrCall>()
        val (left, operator, right) = runExpressionAnalysis(
            irCall.extensionReceiverArgument,
            funcName,
            irCall.valueArguments.firstOrNull() ?: args.getOrNull(1)
        )
        state.paramName = left
        state.type = operator
        state.value = right
        state.tableName = getTableName(irCall.findKronosColumn()!!.irCast<IrCall>().dispatchReceiverArgument!!)
    }
}

/**
 * Handles equal operations.
 * This function builds a criteria IR variable based on the given equal operations.
 *
 * @param element The IrCall element representing the equal operation.
 * @param funcName The name of the equal operation.
 * @param args The arguments of the equal operation.
 * @throws IllegalArgumentException if the element is not an IrCall element.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(_: IrPluginContext, builder: IrBlockBuilder, state: LocalState)
private fun handleEqualOp(
    element: IrCall,
    funcName: String,
    args: List<IrExpression?>
) {
    state.not = state.not xor element.valueArguments.isEmpty()
    val index = when {
        args[0].isKPojo() || args[0].isKronosFunction() -> 0
        args[1].isKPojo() || args[0].isKronosFunction() -> 1
        else -> {
            state.type = "sql"
            state.value = element
            -1
        }
    }
    if (index != -1) {
        val irCall = args[index]!!.irCast<IrCall>()
        val (left, _, right) = runExpressionAnalysis(
            irCall,
            funcName,
            args[1 - index]
        )
        state.paramName = left
        state.value = right
        state.tableName = getTableName(irCall.dispatchReceiverArgument!!)
    }
}

/**
 * Handles equal and not equal operations.
 * This function builds a criteria IR variable based on the given equal and not equal operations.
 *
 * @param irFunction The function to build the criteria for.
 * @param element The IrCall element representing the equal or not equal operation.
 * @param extensionReceiver The extension receiver of the equal or not equal operation.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(_: IrPluginContext, builder: IrBlockBuilder, state: LocalState)
private fun handleEqNeqOp(
    irFunction: IrFunction,
    element: IrCall,
    extensionReceiver: IrExpression?
) {
    if (extensionReceiver != null && extensionReceiver.isKPojo()) {
        state.paramName = getColumnOrValue(extensionReceiver)
        state.value = getValueByFieldNameSymbol(
            builder.irGet(irFunction.parameters.extensionReceiver!!),
            builder.irString(
                extensionReceiver.irCast<IrCall>().correspondingName!!.asString()
            )
        )
        state.tableName = getTableName(extensionReceiver.irCast<IrCall>().dispatchReceiverArgument!!)
    } else if (extensionReceiver != null) {
        val irClass = element.extensionReceiverArgument?.type?.getClass()

        fun generateEq(kPojo: IrClass, receiver: IrExpression, excludes: List<String>) {
            kPojo.properties.forEach { prop ->
                if (prop.isColumn() && prop.name.asString() !in excludes) {
                    state.children.add(
                        buildCriteria(
                            irFunction,
                            ComparableEq.getter!!.symbol(
                                extensionReceiver,
                                builder.irGet(
                                    prop.backingField!!.type, receiver,
                                    prop.getter!!.symbol
                                )
                            ),
                            state.not
                        )
                    )
                }
            }
        }

        if (irClass?.kotlinFqName == KPojoFqName) {
            if (extensionReceiver is IrCallImpl && extensionReceiver.irCast<IrCall>().origin == IrStatementOrigin.MINUS) {
                state.type = "AND"
                val (kPojoClass, kPojo, excludes) = analyzeMinusExpression(extensionReceiver.irCast<IrCall>())
                generateEq(kPojoClass, kPojo, excludes)
            } else if (irClass.superTypes.any { it.classFqName == KPojoFqName }) {
                state.type = "AND"
                generateEq(irClass, extensionReceiver, listOf())
            }
        }
    }
}

/**
 * Handles between, like, regexp, notBetween, notLike, and notRegexp operations.
 * This function builds a criteria IR variable based on the given between, like, regexp, notBetween, notLike, and notRegexp operations.
 * 
 * @param irFunction The function to build the criteria for.
 * @param element The IrCall element representing the between, like, regexp, notBetween, notLike, and notRegexp operation.
 * @param extensionReceiver The extension receiver of the between, like, regexp, notBetween, notLike, and notRegexp operation.
 * @param args The arguments of the between, like, regexp, notBetween, notLike, and notRegexp operation.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(_: IrPluginContext, builder: IrBlockBuilder, state: LocalState)
private fun handleBetweenLikeRegexpOps(
    irFunction: IrFunction,
    element: IrCall,
    extensionReceiver: IrExpression?,
    args: List<IrExpression?>
) {
    state.paramName = getColumnOrValue(extensionReceiver!!)
    state.value = if (args.isEmpty()) {
        getValueByFieldNameSymbol(
            builder.irGet(irFunction.parameters.extensionReceiver!!),
            builder.irString(extensionReceiver.irCast<IrCall>().correspondingName!!.asString()),
        )
    } else {
        getColumnOrValue(args.first())
    }
    state.tableName = getTableName(element.dispatchReceiver!!.type.sub()!!.getClass()!!)
}

/**
 * Handles startsWith, endsWith, and contains operations.
 * This function builds a criteria IR variable based on the given startsWith, endsWith, and contains operations.
 * 
 * @param irFunction The function to build the criteria for.
 * @param element The IrCall element representing the startsWith, endsWith, or contains operation.
 * @param extensionReceiver The extension receiver of the startsWith, endsWith, or contains operation.
 * @param args The arguments of the startsWith, endsWith, or contains operation.
 * @throws IllegalArgumentException if the extension receiver is not an IrCall element.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(_: IrPluginContext, builder: IrBlockBuilder, state: LocalState)
private fun handleStartsWithOp(
    irFunction: IrFunction,
    element: IrCall,
    extensionReceiver: IrExpression?,
    args: List<IrExpression?>
) {
    val str = if (args.isEmpty()) {
        getValueByFieldNameSymbol(
            builder.irGet(irFunction.parameters.extensionReceiver!!),
            builder.irString(extensionReceiver!!.irCast<IrCall>().correspondingName!!.asString())
        )
    } else {
        getColumnOrValue(args.first())
    }
    state.paramName = getColumnOrValue(extensionReceiver!!)
    state.value = stringPlusSymbol(str, builder.irString("%"))
    state.tableName = getTableName(element.dispatchReceiver!!.type.sub()!!.getClass()!!)
}

/**
 * Handles endsWith, and contains operations.
 * This function builds a criteria IR variable based on the given endsWith and contains operations.
 * 
 * @param irFunction The function to build the criteria for.
 * @param extensionReceiver The extension receiver of the endsWith, or contains operation.
 * @param dispatchReceiver The dispatch receiver of the endsWith, or contains operation.
 * @param args The arguments of the endsWith, or contains operation.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(_: IrPluginContext, builder: IrBlockBuilder, state: LocalState)
private fun handleEndsWithOp(
    irFunction: IrFunction,
    extensionReceiver: IrExpression?,
    dispatchReceiver: IrExpression?,
    args: List<IrExpression?>
) {
    val str = if (args.isEmpty()) {
        getValueByFieldNameSymbol(
            builder.irGet(irFunction.parameters.extensionReceiver!!),
            builder.irString(extensionReceiver!!.irCast<IrCall>().correspondingName!!.asString())
        )
    } else {
        getColumnOrValue(args.first())
    }
    state.paramName = getColumnOrValue(extensionReceiver!!)
    state.value = stringPlusSymbol(builder.irString("%"), str)
    state.tableName = getTableName(dispatchReceiver!!.type.sub()!!.getClass()!!)
}

/**
 * Handles in, notIn, and contains operations.
 * This function builds a criteria IR variable based on the given in, notIn, and contains operations.
 * 
 * @param irFunction The function to build the criteria for.
 * @param extensionReceiver The extension receiver of the in, notIn, or contains operation.
 * @param dispatchReceiver The dispatch receiver of the in, notIn, or contains operation.
 * @param args The arguments of the in, notIn, or contains operation.
 * @throws IllegalArgumentException if the dispatch receiver is not an IrCall element.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(_: IrPluginContext, builder: IrBlockBuilder, state: LocalState)
private fun handleContainsOp(
    irFunction: IrFunction,
    extensionReceiver: IrExpression?,
    dispatchReceiver: IrExpression?,
    args: List<IrExpression?>
) {
    val left = extensionReceiver ?: dispatchReceiver
    if (left!!.type.classFqName in ARRAY_OR_COLLECTION_FQ_NAMES || left.type.superTypes()
            .any { it.classFqName in ARRAY_OR_COLLECTION_FQ_NAMES }
    ) {
        state.tableName = getTableName(args.first()!!.irCast<IrCall>().dispatchReceiver!!.type.getClass()!!)
        state.paramName = getColumnOrValue(args.first()!!)
        state.value = left
    } else {
        state.tableName = getTableName(left.irCast<IrCall>().dispatchReceiver!!.type.getClass()!!)
        state.type = "like"
        val str = if (args.isEmpty()) {
            state.paramName = getColumnOrValue(left)
            getValueByFieldNameSymbol(
                builder.irGet(irFunction.parameters.extensionReceiver!!),
                builder.irString(left.irCast<IrCall>().correspondingName!!.asString())
            )
        } else {
            state.paramName = getColumnOrValue(left)
            getColumnOrValue(args.first())
        }

        state.value = if (str is IrConstImpl && str.value is String) {
            builder.irString("%${str.value}%")
        } else {
            buildContainsStrSymbol(builder.irGet(irFunction.parameters.extensionReceiver!!), str)
        }
    }
}

/**
 * Handles asSql operations.
 * This function builds a criteria IR variable based on the given asSql operation.
 *
 * @param extensionReceiver The extension receiver of the asSql operation.
 */
context(state: LocalState)
private fun handleAsSqlOp(
    extensionReceiver: IrExpression?
) {
    state.value = extensionReceiver
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(_: IrPluginContext, builder: IrBlockBuilder, state: LocalState)
private fun handleIfNoValueOp(
    irFunction: IrFunction,
    extensionReceiver: IrExpression?,
    args: List<IrExpression?>
): IrVariable? {
    state.strategy = args.first()
    return buildCriteria(irFunction, extensionReceiver!!, state.not, state.strategy)
}

context(context: IrPluginContext, _: IrBlockBuilder, state: LocalState)
private fun handleIrConst(
    element: IrConstImpl
) {
    if (element.type == context.irBuiltIns.stringType) {
        state.type = "sql"
        state.value = element
    }
}

context(_: IrPluginContext)
fun analyzeMinusExpression(irCall: IrCall): Triple<IrClass, IrExpression, List<String>> {
    val (kPojo, properties) = getIrMinusParent(irCall)
    return Triple(
        kPojo.type.getClass()!!,
        kPojo,
        properties
    )
}

context(_: IrPluginContext)
fun getIrMinusParent(irCall: IrCall): Pair<IrExpression, List<String>> {
    val property = listOfNotNull(
            irCall.valueArguments.find { it is IrCallImpl && it.origin == IrStatementOrigin.GET_PROPERTY }?.funcName()
        )
    val extensionReceiver = irCall.extensionReceiverArgument
    val (kPojo, properties) = if (extensionReceiver is IrCallImpl) {
        getIrMinusParent(extensionReceiver.irCast<IrCall>())
    } else {
        extensionReceiver!! to listOf()
    }
    return kPojo to (properties + property)
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
internal val IrCall.correspondingName
    get() = symbol.owner.correspondingPropertySymbol?.owner?.name

/**
 * Returns a string representing the function name based on the IrExpression type and origin, with optional logic for setNot parameter.
 *
 * @param setNot a boolean value indicating whether to add the "not" prefix to the function name
 * @return a string representing the function name
 */
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
 * Finds a Kronos Column in the given IrExpression.
 *
 * This function checks if the given IrExpression is a Kronos Column. If it is, it returns the expression itself.
 * If the expression is an instance of IrBlock and its origin is SAFE_CALL, it returns null.
 * If the expression is not an instance of IrCall, it returns the expression itself.
 * If the extension receiver or the dispatch receiver of the expression is an instance of IrCall, it recursively calls this function with the receiver.
 * If none of the above conditions are met, it iterates over the value arguments of the expression. If it finds an argument that is an instance of IrCall, it recursively calls this function with the argument.
 * If no Kronos Column is found, it returns the expression itself.
 *
 * @receiver the `IrExpression` to find the Kronos Column in.
 * @return returns the found Kronos Column `IrExpression`, or null if no Kronos Column is found.
 */
context(_: IrPluginContext)
fun IrExpression.findKronosColumn(): IrExpression? {
    if (this is IrBlock && origin == IrStatementOrigin.SAFE_CALL) return null
    if (this !is IrCall) return this
    if (isKPojo()) {
        return this
    } else if (extensionReceiverArgument is IrCall) {
        return extensionReceiverArgument!!.findKronosColumn()
    } else if (dispatchReceiverArgument is IrCall) {
        return dispatchReceiverArgument!!.findKronosColumn()
    } else {
        for (arg in valueArguments) {
            if (arg is IrCall) {
                return arg.findKronosColumn()
            }
        }
        return this
    }
}

/**
 * Checks if the given IrExpression is a Kronos Column.
 *
 * This function checks if the given IrExpression is an instance of IrCallImpl and if its origin is either GET_PROPERTY or EQ.
 * If these conditions are met, it retrieves the property name from the IrExpression and finds the corresponding property in the class.
 * It then checks if any of the super types of the parent class of the property is "com.kotlinorm.interfaces.KPojo".
 *
 * @receiver the `IrExpression` to check. It can be null.
 * @return returns true if the IrExpression is a Kronos Column, false otherwise.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(_: IrPluginContext)
fun IrExpression?.isKPojo(): Boolean {
    if (this == null) return false
    return (
            this is IrCallImpl &&
                    this.symbol.owner.correspondingPropertySymbol?.owner is IrProperty &&
                    this.let {
                        val propertyName = correspondingName!!.asString()
                        (
                                dispatchReceiver!!.type.getClass()!!
                                    .properties
                                    .first { it.name.asString() == propertyName }
                                    .parent as IrClass
                                ).isKPojo()
                    }
            ) || (
            this is IrPropertyReference &&
                    this.symbol.owner.parent is IrClass &&
                    (this.symbol.owner.parent as IrClass)
                        .isKPojo()
            )
}

context(_: IrPluginContext)
fun IrType.isKPojo(): Boolean {
    return superTypes().any { it.classFqName == KPojoFqName }
}

context(_: IrPluginContext)
fun IrClass.isKPojo(): Boolean {
    return superTypes.any { it.classFqName == KPojoFqName }
}

/**
 * Determines the type and value of a Kronos Column.
 *
 * This function checks if the given IrExpression is a Kronos Column. If it is, it returns a pair with the type as ColumnName and the expression itself.
 * If the function name of the expression is "value", it returns a pair with the type as Value and the expression itself.
 * Otherwise, it tries to find a Kronos Column in the expression and returns a pair with the type as ColumnName and the found Kronos Column.
 * If no Kronos Column is found, it throws an IllegalStateException.
 *
 * @receiver the `IrExpression` to check.
 * @return returns a pair with the type and value of the Kronos Column.
 * @throws IllegalStateException if no Kronos Column is found in the expression, and the function name is not "value".
 */
context(_: IrPluginContext)
fun IrExpression.columnValueGetter(): Pair<KronosColumnValueType, IrExpression> {
    return if (this.isKPojo()) {
        KronosColumnValueType.ColumnName to this
    } else if (this.funcName() == "value") {
        KronosColumnValueType.Value to this
    } else if (this.isKronosFunction()) {
        KronosColumnValueType.Function to this
    } else {
        KronosColumnValueType.ColumnName to (findKronosColumn()
            ?: error("`?.` is not supported in CriteriaBuilder. Unless using `.value to get the real expression value."))
    }
}

/**
 * Checks if the given IrExpression is a Kronos function.
 * This function checks if the IrExpression is an instance of IrCallImpl and if its extension receiver argument's type has the fully qualified name "com.kotlinorm.functions.FunctionHandler".
 * "com.kotlinorm.functions.FunctionHandler" is the marker interface for all Kronos functions.
 *
 * @return true if the IrExpression is a Kronos function, false otherwise.
 */
context(_: IrPluginContext)
fun IrExpression?.isKronosFunction(): Boolean {
    if (this == null) return false
    return this is IrCallImpl && this.extensionReceiverArgument?.type?.classFqName == FqName("com.kotlinorm.functions.FunctionHandler")
}

/**
 * For properties that are not columns, we need to check if :
 * 1. the field using @Ignore annotation
 * 2. the type is a KPojo or its super types are KPojo
 * 3. is a Collection of KPojo, such as List<KPojo>
 * 4. has Annotation `@Cascade`
 *
 * Specially, if the property is using `@Serialize` annotation, it will be treated as a column.
 * but the priority of `@Serialize` is lower than `Ignore` annotation and `@Cascade` annotation.
 */
context(context: IrPluginContext)
fun IrProperty.isColumn(
    irPropertyType: IrType = this.backingField?.type ?: context.irBuiltIns.anyNType,
    ignored: IrConstructorCall? = ignoreAnnotationValue()
): Boolean {
    if (ignored.ignore("all")) return false
    if (hasAnnotation(CascadeAnnotationsFqName)) return false
    if (hasAnnotation(SerializeAnnotationsFqName)) return true
    if (irPropertyType.isKPojo() || irPropertyType.sub()?.isKPojo() == true) return false
    return true
}

/**
 * Finds the Ignore annotation value.
 *
 * @return the Ignore annotation value, or null if the property is not annotated with Ignore.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
fun IrProperty.ignoreAnnotationValue(): IrConstructorCall? {
    return annotations.find { it.symbol.owner.returnType.getClass()!!.fqNameWhenAvailable == IgnoreAnnotationsFqName }
}

/**
 * Checks if the given IrConstructorCall is an instance of Ignore annotation.
 *
 * @param name the name of the Ignore annotation.
 * @return true if the IrConstructorCall is an instance of Ignore annotation, false otherwise.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
fun IrConstructorCall?.ignore(name: String): Boolean {
    if (this == null) return false
    val action = valueArguments[0] ?: return true
    return (
            action is IrVarargImpl &&
                    action.elements.isNotEmpty() &&
                    (action.elements.first() is IrGetEnumValueImpl) &&
                    (action.elements.first() as IrGetEnumValueImpl).symbol.owner.name.asString() == name.uppercase()
            )
}