/**
 * Copyright 2022-2026 kronos-orm
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

package com.kotlinorm.compiler.core

import com.kotlinorm.compiler.utils.ErrorMessages
import com.kotlinorm.compiler.utils.dispatchReceiverArgument
import com.kotlinorm.compiler.utils.extensionReceiver
import com.kotlinorm.compiler.utils.extensionReceiverArgument
import com.kotlinorm.compiler.utils.funcName
import com.kotlinorm.compiler.utils.getValueArgumentSafe
import com.kotlinorm.compiler.utils.valueArguments
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.IrBlockBuilder
import org.jetbrains.kotlin.ir.builders.irBoolean
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrPropertyReference
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.expressions.IrWhen
import org.jetbrains.kotlin.ir.expressions.impl.IrGetEnumValueImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.dumpKotlinLike
import org.jetbrains.kotlin.ir.util.properties

/**
 * Condition analysis and construction
 *
 * Analyzes condition expressions and directly constructs Criteria IR nodes.
 * Criteria constructor: (field, type, not, value, tableName, noValueStrategyType, children)
 */

/**
 * Analyzes a condition element and builds a Criteria IR expression.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBlockBuilder)
fun analyzeAndBuildCriteria(
    irFunction: IrFunction,
    element: IrElement,
    errorReporter: ErrorReporter,
    setNot: Boolean = false
): IrExpression? {
    return when (element) {
        is IrCall -> analyzeCallCriteria(irFunction, element, errorReporter, setNot)
        is IrWhen -> analyzeWhenCriteria(irFunction, element, errorReporter, setNot)
        is IrReturn -> analyzeAndBuildCriteria(irFunction, element.value, errorReporter, setNot)
        is IrBlockBody -> {
            val children = element.statements.mapNotNull {
                analyzeAndBuildCriteria(irFunction, it, errorReporter, setNot)
            }
            if (children.isEmpty()) return null
            buildCriteriaNode(type = "ROOT", not = false, children = children)
        }
        is IrExpression -> {
            errorReporter.reportWarning(
                element,
                ErrorMessages.unsupportedConditionExprType(element::class.simpleName, element.dumpKotlinLike())
            )
            null
        }
        else -> null
    }
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBlockBuilder)
private fun analyzeCallCriteria(
    irFunction: IrFunction,
    call: IrCall,
    errorReporter: ErrorReporter,
    setNot: Boolean
): IrExpression? {
    return when (call.origin) {
        IrStatementOrigin.EQEQ -> {
            val left = call.getValueArgumentSafe(0) ?: run {
                errorReporter.reportError(call, ErrorMessages.MISSING_LEFT_OPERAND_EQ)
                return null
            }
            val right = call.getValueArgumentSafe(1) ?: run {
                errorReporter.reportError(call, ErrorMessages.MISSING_RIGHT_OPERAND_EQ)
                return null
            }
            buildEqualCriteria(left, right, not = setNot, errorReporter = errorReporter)
        }
        IrStatementOrigin.EXCLEQ -> {
            val left = call.getValueArgumentSafe(0) ?: run {
                errorReporter.reportError(call, ErrorMessages.MISSING_LEFT_OPERAND_NEQ)
                return null
            }
            val right = call.getValueArgumentSafe(1) ?: run {
                errorReporter.reportError(call, ErrorMessages.MISSING_RIGHT_OPERAND_NEQ)
                return null
            }
            buildEqualCriteria(left, right, not = !setNot, errorReporter = errorReporter)
        }
        IrStatementOrigin.ANDAND -> {
            val left = call.getValueArgumentSafe(0)?.let { analyzeAndBuildCriteria(irFunction, it, errorReporter, setNot) }
            val right = call.getValueArgumentSafe(1)?.let { analyzeAndBuildCriteria(irFunction, it, errorReporter, setNot) }
            buildLogicalCriteria("AND", listOfNotNull(left, right))
        }
        IrStatementOrigin.OROR -> {
            val left = call.getValueArgumentSafe(0)?.let { analyzeAndBuildCriteria(irFunction, it, errorReporter, setNot) }
            val right = call.getValueArgumentSafe(1)?.let { analyzeAndBuildCriteria(irFunction, it, errorReporter, setNot) }
            buildLogicalCriteria("OR", listOfNotNull(left, right))
        }
        IrStatementOrigin.EXCL -> {
            val arg = call.dispatchReceiverArgument ?: call.extensionReceiverArgument ?: run {
                errorReporter.reportError(call, ErrorMessages.MISSING_RECEIVER_NOT)
                return null
            }
            analyzeAndBuildCriteria(irFunction, arg, errorReporter, !setNot)
        }
        IrStatementOrigin.GT -> buildBinaryComparisonCriteria(irFunction, call, "GT", setNot, errorReporter)
        IrStatementOrigin.LT -> buildBinaryComparisonCriteria(irFunction, call, "LT", setNot, errorReporter)
        IrStatementOrigin.GTEQ -> buildBinaryComparisonCriteria(irFunction, call, "GE", setNot, errorReporter)
        IrStatementOrigin.LTEQ -> buildBinaryComparisonCriteria(irFunction, call, "LE", setNot, errorReporter)
        else -> {
            val funcName = call.symbol.owner.name.asString()
            errorReporter.reportWarning(call, ErrorMessages.debugAnalyzeCallCriteria(call.origin, funcName))
            when (funcName) {
                "lt" -> buildBinaryComparisonCriteria(irFunction, call, "LT", setNot, errorReporter)
                "gt" -> buildBinaryComparisonCriteria(irFunction, call, "GT", setNot, errorReporter)
                "le" -> buildBinaryComparisonCriteria(irFunction, call, "LE", setNot, errorReporter)
                "ge" -> buildBinaryComparisonCriteria(irFunction, call, "GE", setNot, errorReporter)
                else -> analyzeMethodCriteria(irFunction, call, errorReporter, setNot)
            }
        }
    }
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBlockBuilder)
private fun analyzeMethodCriteria(
    irFunction: IrFunction,
    call: IrCall,
    errorReporter: ErrorReporter,
    setNot: Boolean
): IrExpression? {
    val funcName = call.funcName()
    val extensionReceiver = call.extensionReceiverArgument
    val dispatchReceiver = call.dispatchReceiverArgument

    return when (funcName) {
        "not" -> {
            val receiver = dispatchReceiver ?: extensionReceiver ?: run {
                errorReporter.reportError(call, ErrorMessages.MISSING_RECEIVER_NOT_CALL)
                return null
            }
            analyzeAndBuildCriteria(irFunction, receiver, errorReporter, !setNot)
        }
        "isNull" -> {
            val receiver = extensionReceiver ?: dispatchReceiver ?: run {
                errorReporter.reportError(call, ErrorMessages.MISSING_RECEIVER_ISNULL)
                return null
            }
            val fieldExpr = extractFieldExpression(receiver, errorReporter) ?: run {
                errorReporter.reportError(call, ErrorMessages.CANNOT_EXTRACT_FIELD_ISNULL)
                return null
            }
            buildCriteriaNode(field = fieldExpr, type = "ISNULL", not = setNot)
        }
        "notNull" -> {
            val receiver = extensionReceiver ?: dispatchReceiver ?: run {
                errorReporter.reportError(call, ErrorMessages.MISSING_RECEIVER_NOTNULL)
                return null
            }
            val fieldExpr = extractFieldExpression(receiver, errorReporter) ?: run {
                errorReporter.reportError(call, ErrorMessages.CANNOT_EXTRACT_FIELD_NOTNULL)
                return null
            }
            buildCriteriaNode(field = fieldExpr, type = "ISNULL", not = !setNot)
        }
        "lt", "gt", "le", "ge" -> {
            // Check if this is a binary comparison (it.age < 18) or no-arg form (it.age.lt)
            val hasArguments = call.getValueArgumentSafe(0) != null
            if (hasArguments) {
                // Binary comparison: it.age < 18
                buildBinaryComparisonCriteria(irFunction, call, funcName.uppercase(), setNot, errorReporter)
            } else {
                // No-arg form: it.age.lt - get value from criteriaParamMap
                buildNoArgComparisonCriteria(irFunction, call, funcName.uppercase(), setNot, errorReporter)
            }
        }
        "less" -> buildBinaryComparisonCriteria(irFunction, call, "LT", setNot, errorReporter)
        "greater" -> buildBinaryComparisonCriteria(irFunction, call, "GT", setNot, errorReporter)
        "lessOrEqual" -> buildBinaryComparisonCriteria(irFunction, call, "LE", setNot, errorReporter)
        "greaterOrEqual" -> buildBinaryComparisonCriteria(irFunction, call, "GE", setNot, errorReporter)
        "eq" -> {
            val receiver = extensionReceiver ?: run {
                errorReporter.reportError(call, ErrorMessages.MISSING_RECEIVER_EQ)
                return null
            }
            val arg = call.getValueArgumentSafe(0)
            if (arg != null) {
                // With argument: it.age.eq("value")
                buildEqualCriteria(receiver, arg, not = setNot, errorReporter = errorReporter)
            } else {
                // No-arg form: it.age.eq
                buildNoArgEqualCriteria(irFunction, call, not = setNot, errorReporter = errorReporter)
            }
        }
        "neq" -> {
            val receiver = extensionReceiver ?: run {
                errorReporter.reportError(call, ErrorMessages.MISSING_RECEIVER_NEQ)
                return null
            }
            val arg = call.getValueArgumentSafe(0)
            if (arg != null) {
                // With argument: it.age.neq("value")
                buildEqualCriteria(receiver, arg, not = !setNot, errorReporter = errorReporter)
            } else {
                // No-arg form: it.age.neq
                buildNoArgEqualCriteria(irFunction, call, not = !setNot, errorReporter = errorReporter)
            }
        }
        "between", "notBetween" -> {
            val receiver = extensionReceiver ?: dispatchReceiver ?: run {
                errorReporter.reportError(call, ErrorMessages.missingReceiverFor(funcName))
                return null
            }
            val range = call.getValueArgumentSafe(0) ?: run {
                errorReporter.reportError(call, ErrorMessages.missingRangeArgFor(funcName))
                return null
            }
            val fieldExpr = extractFieldExpression(receiver, errorReporter) ?: run {
                errorReporter.reportError(call, ErrorMessages.cannotExtractFieldFor(funcName))
                return null
            }
            buildCriteriaNode(field = fieldExpr, type = "BETWEEN", not = funcName == "notBetween", value = range)
        }
        "like", "notLike" -> {
            val receiver = extensionReceiver ?: dispatchReceiver ?: run {
                errorReporter.reportError(call, ErrorMessages.missingReceiverFor(funcName))
                return null
            }
            val pattern = call.getValueArgumentSafe(0)
            val fieldExpr = extractFieldExpression(receiver, errorReporter) ?: return null
            if (pattern != null) {
                // With argument: it.username.like("A%")
                buildCriteriaNode(field = fieldExpr, type = "LIKE", not = funcName == "notLike", value = pattern)
            } else {
                // No-arg form: it.username.like
                buildNoArgLikeCriteria(irFunction, call, not = funcName == "notLike", errorReporter = errorReporter)
            }
        }
        "startsWith" -> {
            val receiver = extensionReceiver ?: dispatchReceiver ?: run {
                errorReporter.reportError(call, ErrorMessages.MISSING_RECEIVER_STARTSWITH)
                return null
            }
            val prefix = call.getValueArgumentSafe(0)
            val fieldExpr = extractFieldExpression(receiver, errorReporter) ?: return null
            if (prefix != null) {
                // With argument: it.username.startsWith("A")
                buildCriteriaNode(field = fieldExpr, type = "LIKE", not = setNot, value = concatIrString(prefix, builder.irString("%")))
            } else {
                // No-arg form: it.username.startsWith
                buildNoArgStartsWithCriteria(irFunction, call, setNot, errorReporter)
            }
        }
        "endsWith" -> {
            val receiver = extensionReceiver ?: dispatchReceiver ?: run {
                errorReporter.reportError(call, ErrorMessages.MISSING_RECEIVER_ENDSWITH)
                return null
            }
            val suffix = call.getValueArgumentSafe(0)
            val fieldExpr = extractFieldExpression(receiver, errorReporter) ?: return null
            if (suffix != null) {
                // With argument: it.username.endsWith("A")
                buildCriteriaNode(field = fieldExpr, type = "LIKE", not = setNot, value = concatIrString(builder.irString("%"), suffix))
            } else {
                // No-arg form: it.username.endsWith
                buildNoArgEndsWithCriteria(irFunction, call, setNot, errorReporter)
            }
        }
        "contains" -> {
            val receiver = extensionReceiver ?: dispatchReceiver ?: run {
                errorReporter.reportError(call, ErrorMessages.MISSING_RECEIVER_CONTAINS)
                return null
            }
            val arg = call.getValueArgumentSafe(0)
            if (arg != null) {
                // With argument: it.username.contains("A")
                if (receiver.type.classFqName?.asString() == "kotlin.String") {
                    // String.contains(it.field) -> LIKE %value%
                    val fieldExpr = extractFieldExpression(receiver, errorReporter) ?: return null
                    val pattern = concatIrString(concatIrString(builder.irString("%"), arg), builder.irString("%"))
                    buildCriteriaNode(field = fieldExpr, type = "LIKE", not = setNot, value = pattern)
                } else {
                    // collection.contains(it.field) -> IN
                    val fieldExpr = extractFieldExpression(arg, errorReporter) ?: return null
                    buildCriteriaNode(field = fieldExpr, type = "IN", not = setNot, value = receiver)
                }
            } else {
                // No-arg form: it.username.contains
                buildNoArgContainsCriteria(irFunction, call, setNot, errorReporter)
            }
        }
        "asSql" -> {
            val receiver = extensionReceiver ?: dispatchReceiver ?: run {
                errorReporter.reportError(call, ErrorMessages.MISSING_RECEIVER_ASSQL)
                return null
            }
            buildCriteriaNode(field = null, type = "SQL", not = false, value = receiver)
        }
        "ifNoValue" -> {
            val receiver = extensionReceiver ?: dispatchReceiver ?: run {
                errorReporter.reportError(call, ErrorMessages.MISSING_RECEIVER_IFNOVALUE)
                return null
            }
            analyzeAndBuildCriteria(irFunction, receiver, errorReporter, setNot)
        }
        "takeIf" -> {
            val receiver = extensionReceiver ?: dispatchReceiver ?: run {
                errorReporter.reportError(call, ErrorMessages.MISSING_RECEIVER_TAKEIF)
                return null
            }
            // If the predicate is a constant false, takeIf returns null — skip this condition
            val predicate = call.getValueArgumentSafe(0)
            if (predicate is IrConst && predicate.value == false) return null
            analyzeAndBuildCriteria(irFunction, receiver, errorReporter, setNot)
        }
        else -> {
            errorReporter.reportWarning(
                call,
                ErrorMessages.unrecognizedConditionFunction(funcName)
            )
            null
        }
    }
}

// In K2 IR, && and || are lowered to IrWhen
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBlockBuilder)
private fun analyzeWhenCriteria(
    irFunction: IrFunction,
    expression: IrWhen,
    errorReporter: ErrorReporter,
    setNot: Boolean
): IrExpression? {
    val children = expression.branches.mapNotNull { branch ->
        // Skip the else-false branch that K2 adds for &&
        val cond = branch.condition
        if (cond is IrConst && cond.value == false) return@mapNotNull null
        errorReporter.reportWarning(irFunction, ErrorMessages.debugAnalyzeWhenCriteria(branch.result.dumpKotlinLike().take(100), cond.dumpKotlinLike().take(100)))
        analyzeAndBuildCriteria(irFunction, branch.result, errorReporter, setNot)
            ?: analyzeAndBuildCriteria(irFunction, branch.condition, errorReporter, setNot)
    }
    if (children.isEmpty()) return null
    if (children.size == 1) return children.first()
    return buildLogicalCriteria(if (expression.branches.size <= 2) "AND" else "OR", children)
}

// ============================================================================
// Field extraction
// ============================================================================

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBlockBuilder)
internal fun extractFieldExpression(expression: IrExpression, errorReporter: ErrorReporter): IrExpression? {
    return when (expression) {
        is IrCall -> if (expression.origin == IrStatementOrigin.GET_PROPERTY)
            buildFieldFromPropertyAccess(expression, errorReporter) else null
        is IrPropertyReference -> buildFieldFromPropertyRef(expression, errorReporter)
        // Handle !! (not-null assertion) — unwrap and extract from the inner expression
        is IrTypeOperatorCall -> extractFieldExpression(expression.argument, errorReporter)
        else -> null
    }
}

// ============================================================================
// Logical criteria
// ============================================================================

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBlockBuilder)
private fun buildLogicalCriteria(operator: String, children: List<IrExpression>): IrExpression {
    return buildCriteriaNode(type = operator, not = false, children = children)
}

// ============================================================================
// Equal criteria (handles KPojo object comparison too)
// ============================================================================

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBlockBuilder)
private fun buildEqualCriteria(
    left: IrExpression,
    right: IrExpression,
    not: Boolean,
    errorReporter: ErrorReporter
): IrExpression? {
    val leftField = extractFieldExpression(left, errorReporter)
    val rightField = extractFieldExpression(right, errorReporter)

    return when {
        leftField != null -> buildCriteriaNode(field = leftField, type = "EQUAL", not = not, value = right)
        rightField != null -> buildCriteriaNode(field = rightField, type = "EQUAL", not = not, value = left)
        left.type.isKPojoType() -> buildKPojoEqualCriteria(left, right, not, errorReporter)
        right.type.isKPojoType() -> buildKPojoEqualCriteria(right, left, not, errorReporter)
        else -> {
            errorReporter.reportError(
                left,
                ErrorMessages.CANNOT_BUILD_EQUALITY
            )
            null
        }
    }
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBlockBuilder)
private fun buildKPojoEqualCriteria(
    kpojoExpr: IrExpression,
    valueExpr: IrExpression,
    not: Boolean,
    errorReporter: ErrorReporter
): IrExpression? {
    val irClass = kpojoExpr.type.classOrNull?.owner ?: run {
        errorReporter.reportError(kpojoExpr, ErrorMessages.CANNOT_RESOLVE_KPOJO_CLASS)
        return null
    }
    val columnProps = irClass.properties.filter { it.isColumnType() }.toList()
    if (columnProps.isEmpty()) {
        errorReporter.reportWarning(kpojoExpr, ErrorMessages.kpojoNoColumnProperties(irClass.name))
        return null
    }

    val children = columnProps.mapNotNull { prop ->
        val fieldExpr = buildFieldFromProperty(prop)
        val getter = prop.getter ?: return@mapNotNull null
        val getterCall = builder.irCall(getter.symbol).apply { dispatchReceiver = valueExpr }
        buildCriteriaNode(field = fieldExpr, type = "EQUAL", not = not, value = getterCall)
    }
    return buildLogicalCriteria("AND", children)
}

// ============================================================================
// Binary comparison criteria (for `it.age < 18` and `18 > it.age` forms)
// ============================================================================

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBlockBuilder)
private fun buildBinaryComparisonCriteria(
    irFunction: IrFunction,
    call: IrCall,
    operator: String,
    not: Boolean,
    errorReporter: ErrorReporter
): IrExpression? {
    // In K2 IR, `it.age < 18` is represented as:
    //   IrCall(funcName="less", valueArguments=[compareTo(it.age, 18), 0])
    //
    // The compareTo call is in valueArguments[0], with structure:
    //   compareTo(dispatchReceiver=$this$where, extensionReceiver=it.<get-age>(), valueArgument[0]=18)
    //
    // For comparison, we need to extract extensionReceiver (field access) and valueArgument[0] (value)

    val args = call.valueArguments

    // Try to find compareTo call in valueArguments
    val compareToCall = args.filterIsInstance<IrCall>().firstOrNull { it.symbol.owner.name.asString() == "compareTo" }
    if (compareToCall != null) {
        // In K2, the field access is typically in extensionReceiver for Comparable.compareTo
        val extReceiver = compareToCall.extensionReceiverArgument
        val dispatchReceiver = compareToCall.dispatchReceiverArgument
        val compareValue = compareToCall.getValueArgumentSafe(0)

        // Try extensionReceiver as field access first, then dispatchReceiver
        val fieldAccess = extReceiver ?: dispatchReceiver

        if (fieldAccess != null && compareValue != null) {
            val leftField = extractFieldExpression(fieldAccess, errorReporter)
            if (leftField != null) {
                return buildCriteriaNode(field = leftField, type = operator, not = not, value = compareValue)
            }

            // Try reversed: maybe fieldAccess is actually the value and compareValue is the field
            val rightField = extractFieldExpression(compareValue, errorReporter)
            if (rightField != null) {
                val reversedOp = when (operator) {
                    "GT" -> "LT"; "LT" -> "GT"; "GE" -> "LE"; "LE" -> "GE"
                    else -> operator
                }
                return buildCriteriaNode(field = rightField, type = reversedOp, not = not, value = fieldAccess)
            }
        }
    }

    errorReporter.reportError(call, ErrorMessages.missingOperandFor(operator))
    return null
}

/**
 * Handles comparison operators from IR origins (GT, LT, GTEQ, LTEQ).
 * Supports both `it.age > 18` and `18 < it.age` (reversed) forms.
 *
 * In K2 IR, `it.age < 18` is lowered to:
 *   IrCall(origin=LT, value_arg_0 = compareTo(this=it.age, other=18))
 * So we need to look inside the compareTo call to find the field and value.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBlockBuilder)
private fun getNoArgValue(irFunction: IrFunction, call: IrCall): IrExpression? {
    val receiver = call.extensionReceiverArgument ?: call.dispatchReceiverArgument ?: return null
    if (receiver !is IrCall) return null
    val propName = receiver.symbol.owner.correspondingPropertySymbol?.owner?.name?.asString() ?: return null
    // The KTableForCondition instance is the extension receiver of the lambda, not the User instance
    val tableParam = irFunction.parameters.extensionReceiver ?: return null
    return builder.irCall(getValueByFieldNameMethodSymbol).apply {
        dispatchReceiver = builder.irGet(tableParam)
        arguments[1] = builder.irString(propName)
    }
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBlockBuilder)
private fun buildNoArgEqualCriteria(irFunction: IrFunction, call: IrCall, not: Boolean, errorReporter: ErrorReporter): IrExpression? {
    val receiver = call.extensionReceiverArgument ?: call.dispatchReceiverArgument ?: return null
    val fieldExpr = extractFieldExpression(receiver, errorReporter) ?: return null
    val valueExpr = getNoArgValue(irFunction, call) ?: return null
    return buildCriteriaNode(field = fieldExpr, type = "EQUAL", not = not, value = valueExpr)
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBlockBuilder)
private fun buildNoArgComparisonCriteria(irFunction: IrFunction, call: IrCall, operator: String, not: Boolean, errorReporter: ErrorReporter): IrExpression? {
    val receiver = call.extensionReceiverArgument ?: call.dispatchReceiverArgument ?: return null
    val fieldExpr = extractFieldExpression(receiver, errorReporter) ?: return null
    val valueExpr = getNoArgValue(irFunction, call) ?: return null
    return buildCriteriaNode(field = fieldExpr, type = operator, not = not, value = valueExpr)
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBlockBuilder)
private fun buildNoArgLikeCriteria(irFunction: IrFunction, call: IrCall, not: Boolean, errorReporter: ErrorReporter): IrExpression? {
    val receiver = call.extensionReceiverArgument ?: call.dispatchReceiverArgument ?: return null
    val fieldExpr = extractFieldExpression(receiver, errorReporter) ?: return null
    val rawValue = getNoArgValue(irFunction, call) ?: return null
    // Convert Any? to String via "".plus(value)
    val valueStr = concatIrString(builder.irString(""), rawValue)
    return buildCriteriaNode(field = fieldExpr, type = "LIKE", not = not, value = valueStr)
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBlockBuilder)
private fun buildNoArgStartsWithCriteria(irFunction: IrFunction, call: IrCall, not: Boolean, errorReporter: ErrorReporter): IrExpression? {
    val receiver = call.extensionReceiverArgument ?: call.dispatchReceiverArgument ?: return null
    val fieldExpr = extractFieldExpression(receiver, errorReporter) ?: return null
    val rawValue = getNoArgValue(irFunction, call) ?: return null
    val valueStr = concatIrString(builder.irString(""), rawValue)
    val pattern = concatIrString(valueStr, builder.irString("%"))
    return buildCriteriaNode(field = fieldExpr, type = "LIKE", not = not, value = pattern)
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBlockBuilder)
private fun buildNoArgEndsWithCriteria(irFunction: IrFunction, call: IrCall, not: Boolean, errorReporter: ErrorReporter): IrExpression? {
    val receiver = call.extensionReceiverArgument ?: call.dispatchReceiverArgument ?: return null
    val fieldExpr = extractFieldExpression(receiver, errorReporter) ?: return null
    val rawValue = getNoArgValue(irFunction, call) ?: return null
    val valueStr = concatIrString(builder.irString(""), rawValue)
    val pattern = concatIrString(builder.irString("%"), valueStr)
    return buildCriteriaNode(field = fieldExpr, type = "LIKE", not = not, value = pattern)
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBlockBuilder)
private fun buildNoArgContainsCriteria(irFunction: IrFunction, call: IrCall, not: Boolean, errorReporter: ErrorReporter): IrExpression? {
    val receiver = call.extensionReceiverArgument ?: call.dispatchReceiverArgument ?: return null
    val fieldExpr = extractFieldExpression(receiver, errorReporter) ?: return null
    val rawValue = getNoArgValue(irFunction, call) ?: return null
    val valueStr = concatIrString(builder.irString(""), rawValue)
    val pattern = concatIrString(concatIrString(builder.irString("%"), valueStr), builder.irString("%"))
    return buildCriteriaNode(field = fieldExpr, type = "LIKE", not = not, value = pattern)
}

// ============================================================================
// Core Criteria node builder
// Criteria(field, type, not, value, tableName, noValueStrategyType, children)
// ============================================================================

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBlockBuilder)
fun buildCriteriaNode(
    field: IrExpression? = null,
    type: String,
    not: Boolean,
    value: IrExpression? = null,
    children: List<IrExpression> = emptyList()
): IrExpression {
    val conditionTypeEnum = conditionTypeEnumSymbol.owner
    val enumEntry = conditionTypeEnum.declarations
        .filterIsInstance<IrEnumEntry>()
        .firstOrNull { it.name.asString().equals(type, ignoreCase = true) }
        ?: run {
            error("Unknown ConditionType '$type'. Valid values: ${conditionTypeEnum.declarations.filterIsInstance<IrEnumEntry>().map { it.name }}")
        }

    val conditionTypeValue = IrGetEnumValueImpl(
        builder.startOffset,
        builder.endOffset,
        conditionTypeEnumSymbol.defaultType,
        enumEntry.symbol
    )

    val fieldArg = field ?: builder.irCall(fieldConstructorSymbol).apply {
        arguments[0] = builder.irString("")
        arguments[1] = builder.irString("")
    }

    val criteriaVar = builder.irTemporary(
        builder.irCall(criteriaConstructorSymbol).apply {
            arguments[0] = fieldArg
            arguments[1] = conditionTypeValue
            arguments[2] = builder.irBoolean(not)
            arguments[3] = value ?: builder.irNull()
            arguments[4] = builder.irString("") // tableName
            arguments[5] = builder.irNull() // noValueStrategyType
            // children uses default value - don't pass it explicitly to avoid vararg issues
        }
    )

    children.forEach { child ->
        builder.run {
            +irCall(addChildMethodSymbol).apply {
                dispatchReceiver = irGet(criteriaVar)
                arguments[1] = child
            }
        }
    }

    return builder.irGet(criteriaVar)
}

// ============================================================================
// String concat helper
// ============================================================================

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBlockBuilder)
internal fun concatIrString(left: IrExpression, right: IrExpression): IrExpression =
    builder.irCall(stringPlusSymbol).apply {
        dispatchReceiver = left
        arguments[1] = right
    }
