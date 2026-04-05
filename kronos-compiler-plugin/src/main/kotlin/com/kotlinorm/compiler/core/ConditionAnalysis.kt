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
import com.kotlinorm.compiler.utils.isKronosFunction
import com.kotlinorm.compiler.utils.valueArguments
import com.kotlinorm.compiler.transformers.getTableNameExpr
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.IrBlockBuilder
import org.jetbrains.kotlin.ir.builders.irBoolean
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irIfThenElse
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
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.dumpKotlinLike
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.ir.util.superTypes

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
            buildEqualCriteria(irFunction, left, right, not = setNot, errorReporter = errorReporter)
        }
        IrStatementOrigin.EXCLEQ -> {
            // In K2 IR, `a != b` is lowered to `EQEQ(a, b).not()` with EXCLEQ origin on BOTH calls.
            // The outer not() has 1 arg (the inner EQEQ), the inner EQEQ has 2 args.
            val funcName = call.symbol.owner.name.asString()
            if (funcName == "EQEQ" || funcName == "ieee754equals") {
                // This is the inner equality call — extract operands directly
                val left = call.getValueArgumentSafe(0) ?: run {
                    errorReporter.reportError(call, ErrorMessages.MISSING_LEFT_OPERAND_NEQ)
                    return null
                }
                val right = call.getValueArgumentSafe(1) ?: run {
                    errorReporter.reportError(call, ErrorMessages.MISSING_RIGHT_OPERAND_NEQ)
                    return null
                }
                buildEqualCriteria(irFunction, left, right, not = !setNot, errorReporter = errorReporter)
            } else {
                // This is the outer not() call — delegate to the inner EQEQ without flipping not
                // (the inner EQEQ branch above handles the negation)
                val inner = call.arguments.firstOrNull { it != null } as? IrCall
                if (inner != null) {
                    analyzeCallCriteria(irFunction, inner, errorReporter, setNot)
                } else {
                    errorReporter.reportError(call, ErrorMessages.MISSING_LEFT_OPERAND_NEQ)
                    null
                }
            }
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
            val fieldExpr = extractFieldExpression(irFunction, receiver, errorReporter) ?: run {
                errorReporter.reportError(call, ErrorMessages.CANNOT_EXTRACT_FIELD_ISNULL)
                return null
            }
            buildCriteriaNode(field = fieldExpr, type = "ISNULL", not = setNot, tableName = extractTableNameExpr(receiver))
        }
        "notNull" -> {
            val receiver = extensionReceiver ?: dispatchReceiver ?: run {
                errorReporter.reportError(call, ErrorMessages.MISSING_RECEIVER_NOTNULL)
                return null
            }
            val fieldExpr = extractFieldExpression(irFunction, receiver, errorReporter) ?: run {
                errorReporter.reportError(call, ErrorMessages.CANNOT_EXTRACT_FIELD_NOTNULL)
                return null
            }
            buildCriteriaNode(field = fieldExpr, type = "ISNULL", not = !setNot, tableName = extractTableNameExpr(receiver))
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
                buildEqualCriteria(irFunction, receiver, arg, not = setNot, errorReporter = errorReporter)
            } else {
                // No-arg form: it.age.eq or (it - it.gender).eq or it.eq
                buildKPojoOrFieldEq(irFunction, call, receiver, not = setNot, errorReporter = errorReporter)
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
                buildEqualCriteria(irFunction, receiver, arg, not = !setNot, errorReporter = errorReporter)
            } else {
                // No-arg form: it.age.neq or (it - it.gender).neq
                buildKPojoOrFieldEq(irFunction, call, receiver, not = !setNot, errorReporter = errorReporter)
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
            val fieldExpr = extractFieldExpression(irFunction, receiver, errorReporter) ?: run {
                errorReporter.reportError(call, ErrorMessages.cannotExtractFieldFor(funcName))
                return null
            }
            buildCriteriaNode(field = fieldExpr, type = "BETWEEN", not = funcName == "notBetween", value = range, tableName = extractTableNameExpr(receiver))
        }
        "like", "notLike" -> {
            val receiver = extensionReceiver ?: dispatchReceiver ?: run {
                errorReporter.reportError(call, ErrorMessages.missingReceiverFor(funcName))
                return null
            }
            val pattern = call.getValueArgumentSafe(0)
            val fieldExpr = extractFieldExpression(irFunction, receiver, errorReporter) ?: return null
            val tblName = extractTableNameExpr(receiver)
            if (pattern != null) {
                // With argument: it.username.like("A%") or it.username like f.concat(...)
                buildCriteriaNode(field = fieldExpr, type = "LIKE", not = funcName == "notLike", value = resolveValueExpression(irFunction, pattern, errorReporter), tableName = tblName)
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
            val fieldExpr = extractFieldExpression(irFunction, receiver, errorReporter) ?: return null
            val tblName = extractTableNameExpr(receiver)
            if (prefix != null) {
                // With argument: it.username.startsWith("A")
                buildCriteriaNode(field = fieldExpr, type = "LIKE", not = setNot, value = concatIrString(prefix, builder.irString("%")), tableName = tblName)
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
            val fieldExpr = extractFieldExpression(irFunction, receiver, errorReporter) ?: return null
            val tblName = extractTableNameExpr(receiver)
            if (suffix != null) {
                // With argument: it.username.endsWith("A")
                buildCriteriaNode(field = fieldExpr, type = "LIKE", not = setNot, value = concatIrString(builder.irString("%"), suffix), tableName = tblName)
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
                    val fieldExpr = extractFieldExpression(irFunction, receiver, errorReporter) ?: return null
                    val pattern = concatIrString(concatIrString(builder.irString("%"), arg), builder.irString("%"))
                    buildCriteriaNode(field = fieldExpr, type = "LIKE", not = setNot, value = pattern, tableName = extractTableNameExpr(receiver))
                } else {
                    // collection.contains(it.field) -> IN
                    val fieldExpr = extractFieldExpression(irFunction, arg, errorReporter) ?: return null
                    buildCriteriaNode(field = fieldExpr, type = "IN", not = setNot, value = receiver, tableName = extractTableNameExpr(arg))
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
        "regexp", "notRegexp" -> {
            val receiver = extensionReceiver ?: dispatchReceiver ?: run {
                errorReporter.reportError(call, ErrorMessages.missingReceiverFor(funcName))
                return null
            }
            val arg = call.getValueArgumentSafe(0)
            val fieldExpr = extractFieldExpression(irFunction, receiver, errorReporter) ?: return null
            if (arg != null) {
                buildCriteriaNode(field = fieldExpr, type = "REGEXP", not = funcName == "notRegexp", value = arg, tableName = extractTableNameExpr(receiver))
            } else {
                buildNoArgRegexpCriteria(irFunction, call, not = funcName == "notRegexp", errorReporter = errorReporter)
            }
        }
        "ifNoValue" -> {
            val receiver = extensionReceiver ?: dispatchReceiver ?: run {
                errorReporter.reportError(call, ErrorMessages.MISSING_RECEIVER_IFNOVALUE)
                return null
            }
            val strategyArg = call.getValueArgumentSafe(0)
            val innerCriteria = analyzeAndBuildCriteria(irFunction, receiver, errorReporter, setNot) ?: return null
            // Set noValueStrategyType on the criteria
            if (strategyArg != null) {
                val setter = criteriaClassSymbol.owner.declarations
                    .filterIsInstance<org.jetbrains.kotlin.ir.declarations.IrProperty>()
                    .first { it.name.asString() == "noValueStrategyType" }
                    .setter!!.symbol
                val tmpVar = builder.irTemporary(innerCriteria, nameHint = "criteriaWithStrategy")
                builder.run {
                    +irCall(setter).apply {
                        this.dispatchReceiver = irGet(tmpVar)
                        arguments[1] = strategyArg
                    }
                }
                builder.irGet(tmpVar)
            } else {
                innerCriteria
            }
        }
        "takeIf" -> {
            val receiver = extensionReceiver ?: dispatchReceiver ?: run {
                errorReporter.reportError(call, ErrorMessages.MISSING_RECEIVER_TAKEIF)
                return null
            }
            val predicate = call.getValueArgumentSafe(0) ?: return null
            // Generate runtime: if (predicate) criteria else null
            val criteriaExpr = analyzeAndBuildCriteria(irFunction, receiver, errorReporter, setNot) ?: return null
            builder.irIfThenElse(
                criteriaClassSymbol.defaultType.makeNullable(),
                predicate,
                criteriaExpr,
                builder.irNull()
            )
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

// In K2 IR, && and || are lowered to IrWhen:
//   a && b  →  when(ANDAND) { a -> b; else -> false }
//   a || b  →  when(OROR)   { a -> true; else -> b }
// We process both condition and result of each branch, skipping boolean constants.
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBlockBuilder)
private fun analyzeWhenCriteria(
    irFunction: IrFunction,
    expression: IrWhen,
    errorReporter: ErrorReporter,
    setNot: Boolean
): IrExpression? {
    // Determine logical operator from origin, applying De Morgan's when negated
    val logicalOp = when (expression.origin) {
        IrStatementOrigin.OROR -> if (setNot) "AND" else "OR"
        IrStatementOrigin.ANDAND -> if (setNot) "OR" else "AND"
        else -> "AND"
    }

    val children = mutableListOf<IrExpression>()
    for (branch in expression.branches) {
        val cond = branch.condition
        val result = branch.result
        // Skip boolean constants (true/false) that K2 inserts as lowering artifacts
        if (cond !is IrConst || cond.value !is Boolean) {
            analyzeAndBuildCriteria(irFunction, cond, errorReporter, setNot)?.let { children.add(it) }
        }
        if (result !is IrConst || result.value !is Boolean) {
            analyzeAndBuildCriteria(irFunction, result, errorReporter, setNot)?.let { children.add(it) }
        }
    }

    if (children.isEmpty()) return null
    if (children.size == 1) return children.first()
    return buildLogicalCriteria(logicalOp, children)
}

// ============================================================================
// Field extraction
// ============================================================================

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBlockBuilder)
internal fun extractFieldExpression(irFunction: IrFunction, expression: IrExpression, errorReporter: ErrorReporter): IrExpression? {
    return when (expression) {
        is IrCall -> when {
            expression.origin == IrStatementOrigin.GET_PROPERTY -> {
                // Skip `.value` property — it's a KTableForCondition helper that returns the raw value
                val propName = expression.symbol.owner.correspondingPropertySymbol?.owner?.name?.asString()
                if (propName == "value") null
                else buildFieldFromPropertyAccess(expression, errorReporter)
            }
            expression.isKronosFunction() ->
                buildFunctionField(irFunction, expression, errorReporter)
            else -> null
        }
        is IrPropertyReference -> buildFieldFromPropertyRef(expression, errorReporter)
        // Handle !! (not-null assertion) — unwrap and extract from the inner expression
        is IrTypeOperatorCall -> extractFieldExpression(irFunction, expression.argument, errorReporter)
        else -> null
    }
}

/**
 * Extracts the table name expression from a field-producing expression.
 * For property access (it.age), gets the table name from the dispatch receiver's class.
 * For Kronos function calls (f.length(it.username)), extracts from the first field argument.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBlockBuilder)
internal fun extractTableNameExpr(expression: IrExpression): IrExpression? {
    return when (expression) {
        is IrCall -> when {
            expression.origin == IrStatementOrigin.GET_PROPERTY -> {
                val receiverType = expression.dispatchReceiver?.type
                val irClass = receiverType?.classOrNull?.owner ?: return null
                if (irClass.superTypes.any { it.classFqName?.asString() == "com.kotlinorm.interfaces.KPojo" }) {
                    builder.getTableNameExpr(irClass)
                } else null
            }
            expression.isKronosFunction() -> {
                // For function calls, try to extract table name from the first field argument
                expression.valueArguments.filterNotNull().firstNotNullOfOrNull { arg ->
                    extractTableNameExpr(arg)
                }
            }
            else -> null
        }
        is IrTypeOperatorCall -> extractTableNameExpr(expression.argument)
        else -> null
    }
}

/**
 * Resolves a value expression: if it's a field expression (property access on KPojo or
 * Kronos function call), convert to Field/FunctionField; otherwise return as-is.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBlockBuilder)
private fun resolveValueExpression(irFunction: IrFunction, expression: IrExpression, errorReporter: ErrorReporter): IrExpression {
    return extractFieldExpression(irFunction, expression, errorReporter) ?: expression
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
    irFunction: IrFunction,
    left: IrExpression,
    right: IrExpression,
    not: Boolean,
    errorReporter: ErrorReporter
): IrExpression? {
    val leftField = extractFieldExpression(irFunction, left, errorReporter)
    val rightField = extractFieldExpression(irFunction, right, errorReporter)

    return when {
        leftField != null -> buildCriteriaNode(field = leftField, type = "EQUAL", not = not, value = resolveValueExpression(irFunction, right, errorReporter), tableName = extractTableNameExpr(left))
        rightField != null -> buildCriteriaNode(field = rightField, type = "EQUAL", not = not, value = resolveValueExpression(irFunction, left, errorReporter), tableName = extractTableNameExpr(right))
        left.type.isKPojoType() -> buildKPojoEqualCriteria(left, right, not, errorReporter)
        right.type.isKPojoType() -> buildKPojoEqualCriteria(right, left, not, errorReporter)
        else -> {
            // Neither side is a field or KPojo — this is a pure runtime expression (e.g., 1 == another.id.value)
            // Generate a SQL "true" / "false" criteria so it appears in the WHERE clause
            buildCriteriaNode(field = null, type = "SQL", not = false, value = builder.irBoolean(!not))
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
        buildCriteriaNode(field = fieldExpr, type = "EQUAL", not = not, value = getterCall, tableName = builder.getTableNameExpr(irClass))
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
            val leftField = extractFieldExpression(irFunction, fieldAccess, errorReporter)
            if (leftField != null) {
                return buildCriteriaNode(field = leftField, type = operator, not = not, value = resolveValueExpression(irFunction, compareValue, errorReporter), tableName = extractTableNameExpr(fieldAccess))
            }

            // Try reversed: maybe fieldAccess is actually the value and compareValue is the field
            val rightField = extractFieldExpression(irFunction, compareValue, errorReporter)
            if (rightField != null) {
                val reversedOp = when (operator) {
                    "GT" -> "LT"; "LT" -> "GT"; "GE" -> "LE"; "LE" -> "GE"
                    else -> operator
                }
                return buildCriteriaNode(field = rightField, type = reversedOp, not = not, value = resolveValueExpression(irFunction, fieldAccess, errorReporter), tableName = extractTableNameExpr(compareValue))
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
    val fieldExpr = extractFieldExpression(irFunction, receiver, errorReporter) ?: return null
    val valueExpr = getNoArgValue(irFunction, call) ?: return null
    return buildCriteriaNode(field = fieldExpr, type = "EQUAL", not = not, value = valueExpr, tableName = extractTableNameExpr(receiver))
}

/**
 * Handles no-arg `.eq` / `.neq` on either a single field or a KPojo (with optional minus).
 * - `it.age.eq` → single field equal criteria
 * - `it.eq` or `(it - it.gender).eq` → AND criteria for all (non-excluded) column properties
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBlockBuilder)
private fun buildKPojoOrFieldEq(
    irFunction: IrFunction,
    call: IrCall,
    receiver: IrExpression,
    not: Boolean,
    errorReporter: ErrorReporter
): IrExpression? {
    // Check if receiver is a KPojo or a MINUS expression on a KPojo
    val (kPojoExpr, excludes) = analyzeKPojoOrMinus(receiver)
    if (kPojoExpr != null) {
        val irClass = kPojoExpr.type.classOrNull?.owner ?: return null
        val columnProps = irClass.properties.filter { it.isColumnType() && it.name.asString() !in excludes }.toList()
        if (columnProps.isEmpty()) return null
        val children = columnProps.mapNotNull { prop ->
            val fieldExpr = buildFieldFromProperty(prop)
            val propName = prop.name.asString()
            val tableParam = irFunction.parameters.extensionReceiver ?: return@mapNotNull null
            val valueExpr = builder.irCall(getValueByFieldNameMethodSymbol).apply {
                dispatchReceiver = builder.irGet(tableParam)
                arguments[1] = builder.irString(propName)
            }
            buildCriteriaNode(field = fieldExpr, type = "EQUAL", not = not, value = valueExpr, tableName = builder.getTableNameExpr(irClass))
        }
        if (children.isEmpty()) return null
        if (children.size == 1) return children.first()
        return buildLogicalCriteria("AND", children)
    }
    // Not a KPojo — fall back to single field eq
    return buildNoArgEqualCriteria(irFunction, call, not, errorReporter)
}

/**
 * Analyzes an expression to determine if it's a KPojo or a MINUS expression on a KPojo.
 * Returns the KPojo expression and the list of excluded field names.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBlockBuilder)
private fun analyzeKPojoOrMinus(expression: IrExpression): Pair<IrExpression?, List<String>> {
    if (expression.type.isKPojoType()) {
        if (expression is IrCall && expression.origin == IrStatementOrigin.MINUS) {
            val (base, excludes) = collectMinusExcludes(expression)
            return base to excludes
        }
        return expression to emptyList()
    }
    return null to emptyList()
}

/**
 * Recursively collects excluded field names from chained minus expressions.
 * `(it - it.gender - it.age)` → base=it, excludes=["gender", "age"]
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
private fun collectMinusExcludes(call: IrCall): Pair<IrExpression, List<String>> {
    val excludes = mutableListOf<String>()
    val arg = call.getValueArgumentSafe(0)
    if (arg is IrCall && arg.origin == IrStatementOrigin.GET_PROPERTY) {
        val propName = arg.symbol.owner.correspondingPropertySymbol?.owner?.name?.asString()
        if (propName != null) excludes.add(propName)
    }
    val receiver = call.extensionReceiverArgument ?: call.dispatchReceiverArgument
    return if (receiver is IrCall && receiver.origin == IrStatementOrigin.MINUS) {
        val (base, innerExcludes) = collectMinusExcludes(receiver)
        base to (innerExcludes + excludes)
    } else {
        (receiver ?: call) to excludes
    }
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBlockBuilder)
private fun buildNoArgComparisonCriteria(irFunction: IrFunction, call: IrCall, operator: String, not: Boolean, errorReporter: ErrorReporter): IrExpression? {
    val receiver = call.extensionReceiverArgument ?: call.dispatchReceiverArgument ?: return null
    val fieldExpr = extractFieldExpression(irFunction, receiver, errorReporter) ?: return null
    val valueExpr = getNoArgValue(irFunction, call) ?: return null
    return buildCriteriaNode(field = fieldExpr, type = operator, not = not, value = valueExpr, tableName = extractTableNameExpr(receiver))
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBlockBuilder)
private fun buildNoArgLikeCriteria(irFunction: IrFunction, call: IrCall, not: Boolean, errorReporter: ErrorReporter): IrExpression? {
    val receiver = call.extensionReceiverArgument ?: call.dispatchReceiverArgument ?: return null
    val fieldExpr = extractFieldExpression(irFunction, receiver, errorReporter) ?: return null
    val rawValue = getNoArgValue(irFunction, call) ?: return null
    // Convert Any? to String via "".plus(value)
    val valueStr = concatIrString(builder.irString(""), rawValue)
    return buildCriteriaNode(field = fieldExpr, type = "LIKE", not = not, value = valueStr, tableName = extractTableNameExpr(receiver))
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBlockBuilder)
private fun buildNoArgStartsWithCriteria(irFunction: IrFunction, call: IrCall, not: Boolean, errorReporter: ErrorReporter): IrExpression? {
    val receiver = call.extensionReceiverArgument ?: call.dispatchReceiverArgument ?: return null
    val fieldExpr = extractFieldExpression(irFunction, receiver, errorReporter) ?: return null
    val rawValue = getNoArgValue(irFunction, call) ?: return null
    val valueStr = concatIrString(builder.irString(""), rawValue)
    val pattern = concatIrString(valueStr, builder.irString("%"))
    return buildCriteriaNode(field = fieldExpr, type = "LIKE", not = not, value = pattern, tableName = extractTableNameExpr(receiver))
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBlockBuilder)
private fun buildNoArgEndsWithCriteria(irFunction: IrFunction, call: IrCall, not: Boolean, errorReporter: ErrorReporter): IrExpression? {
    val receiver = call.extensionReceiverArgument ?: call.dispatchReceiverArgument ?: return null
    val fieldExpr = extractFieldExpression(irFunction, receiver, errorReporter) ?: return null
    val rawValue = getNoArgValue(irFunction, call) ?: return null
    val valueStr = concatIrString(builder.irString(""), rawValue)
    val pattern = concatIrString(builder.irString("%"), valueStr)
    return buildCriteriaNode(field = fieldExpr, type = "LIKE", not = not, value = pattern, tableName = extractTableNameExpr(receiver))
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBlockBuilder)
private fun buildNoArgContainsCriteria(irFunction: IrFunction, call: IrCall, not: Boolean, errorReporter: ErrorReporter): IrExpression? {
    val receiver = call.extensionReceiverArgument ?: call.dispatchReceiverArgument ?: return null
    val fieldExpr = extractFieldExpression(irFunction, receiver, errorReporter) ?: return null
    val rawValue = getNoArgValue(irFunction, call) ?: return null
    val valueStr = concatIrString(builder.irString(""), rawValue)
    val pattern = concatIrString(concatIrString(builder.irString("%"), valueStr), builder.irString("%"))
    return buildCriteriaNode(field = fieldExpr, type = "LIKE", not = not, value = pattern, tableName = extractTableNameExpr(receiver))
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBlockBuilder)
private fun buildNoArgRegexpCriteria(irFunction: IrFunction, call: IrCall, not: Boolean, errorReporter: ErrorReporter): IrExpression? {
    val receiver = call.extensionReceiverArgument ?: call.dispatchReceiverArgument ?: return null
    val fieldExpr = extractFieldExpression(irFunction, receiver, errorReporter) ?: return null
    val rawValue = getNoArgValue(irFunction, call) ?: return null
    return buildCriteriaNode(field = fieldExpr, type = "REGEXP", not = not, value = rawValue, tableName = extractTableNameExpr(receiver))
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
    children: List<IrExpression> = emptyList(),
    noValueStrategyType: IrExpression? = null,
    tableName: IrExpression? = null
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
            arguments[4] = tableName ?: builder.irString("") // tableName
            arguments[5] = noValueStrategyType ?: builder.irNull() // noValueStrategyType
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
