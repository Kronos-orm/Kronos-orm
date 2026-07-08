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
import com.kotlinorm.compiler.utils.GeneratedProjectionPackageFqName
import com.kotlinorm.compiler.utils.WindowOverFunctionName
import com.kotlinorm.compiler.utils.dispatchReceiverArgument
import com.kotlinorm.compiler.utils.extensionReceiver
import com.kotlinorm.compiler.utils.extensionReceiverArgument
import com.kotlinorm.compiler.utils.funcName
import com.kotlinorm.compiler.utils.getValueArgumentSafe
import com.kotlinorm.compiler.utils.irListOf
import com.kotlinorm.compiler.utils.isKronosFunction
import com.kotlinorm.compiler.utils.valueArguments
import com.kotlinorm.compiler.backend.transformers.getTableNameExpr
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.IrBlockBuilder
import org.jetbrains.kotlin.ir.builders.irBoolean
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irIfThenElse
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrPropertyReference
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.expressions.IrWhen
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.dumpKotlinLike
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.properties

/**
 * Condition analysis and construction
 *
 * Analyzes condition expressions and directly constructs SqlExpr IR nodes.
  */

/**
 * Analyzes a condition element and builds a SqlExpr IR expression.
 */
private sealed interface ConditionBuildKind

private enum class ConditionLogicalKind : ConditionBuildKind {
    And,
    Or,
    Root
}

private enum class ConditionExpressionKind(val noValueAware: Boolean) : ConditionBuildKind {
    RawSql(noValueAware = false),
    IsNull(noValueAware = false),
    Equal(noValueAware = true),
    GreaterThan(noValueAware = true),
    GreaterThanOrEqual(noValueAware = true),
    LessThan(noValueAware = true),
    LessThanOrEqual(noValueAware = true),
    Like(noValueAware = true),
    Regexp(noValueAware = true),
    In(noValueAware = true),
    Between(noValueAware = true)
}

private enum class ConditionComparisonKind(
    val condition: ConditionExpressionKind,
    val diagnosticName: String
) {
    GreaterThan(ConditionExpressionKind.GreaterThan, "GT"),
    GreaterThanOrEqual(ConditionExpressionKind.GreaterThanOrEqual, "GE"),
    LessThan(ConditionExpressionKind.LessThan, "LT"),
    LessThanOrEqual(ConditionExpressionKind.LessThanOrEqual, "LE");

    fun reversed(): ConditionComparisonKind =
        ReversedComparisonKinds.getValue(this)
}

private val ReversedComparisonKinds = mapOf(
    ConditionComparisonKind.GreaterThan to ConditionComparisonKind.LessThan,
    ConditionComparisonKind.GreaterThanOrEqual to ConditionComparisonKind.LessThanOrEqual,
    ConditionComparisonKind.LessThan to ConditionComparisonKind.GreaterThan,
    ConditionComparisonKind.LessThanOrEqual to ConditionComparisonKind.GreaterThanOrEqual,
)

private val NoArgComparisonByFunctionName = mapOf(
    "gt" to ConditionComparisonKind.GreaterThan,
    "ge" to ConditionComparisonKind.GreaterThanOrEqual,
    "lt" to ConditionComparisonKind.LessThan,
    "le" to ConditionComparisonKind.LessThanOrEqual,
)

private val StringMatchFunctionNames = setOf("like", "notLike", "startsWith", "endsWith", "contains", "regexp", "notRegexp")

private data class ConditionOperand(
    val expression: IrExpression,
    val field: IrExpression?,
    val sqlExpr: IrExpression?
)

context(context: IrPluginContext, builder: IrBlockBuilder)
fun analyzeAndBuildSqlExpr(
    irFunction: IrFunction,
    element: IrElement,
    errorReporter: ErrorReporter,
    setNot: Boolean = false
): IrExpression? {
    return when (element) {
        is IrCall -> analyzeCallSqlExpr(irFunction, element, errorReporter, setNot)
        is IrWhen -> analyzeWhenSqlExpr(irFunction, element, errorReporter, setNot)
        is IrReturn -> analyzeAndBuildSqlExpr(irFunction, element.value, errorReporter, setNot)
        is IrBlock -> buildBlockSqlExpr(irFunction, element.statements, errorReporter, setNot)
        is IrBlockBody -> buildBlockSqlExpr(irFunction, element.statements, errorReporter, setNot)
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
private fun analyzeCallSqlExpr(
    irFunction: IrFunction,
    call: IrCall,
    errorReporter: ErrorReporter,
    setNot: Boolean
): IrExpression? {
    return when (call.origin) {
        IrStatementOrigin.EQEQ -> {
            val left = call.getValueArgumentSafe(0) ?: return null
            val right = call.getValueArgumentSafe(1) ?: return null
            buildEqualSqlExpr(irFunction, left, right, not = setNot, errorReporter = errorReporter)
        }
        IrStatementOrigin.EXCLEQ -> {
            // In K2 IR, `a != b` is lowered to `EQEQ(a, b).not()` with EXCLEQ origin on BOTH calls.
            // The outer not() has 1 arg (the inner EQEQ), the inner EQEQ has 2 args.
            val funcName = call.symbol.owner.name.asString()
            if (funcName == "EQEQ" || funcName == "ieee754equals") {
                // This is the inner equality call — extract operands directly
                val left = call.getValueArgumentSafe(0) ?: return null
                val right = call.getValueArgumentSafe(1) ?: return null
                buildEqualSqlExpr(irFunction, left, right, not = !setNot, errorReporter = errorReporter)
            } else {
                // This is the outer not() call — delegate to the inner EQEQ without flipping not
                // (the inner EQEQ branch above handles the negation)
                val inner = call.arguments.firstOrNull { it != null } as? IrCall
                if (inner != null) {
                    analyzeCallSqlExpr(irFunction, inner, errorReporter, setNot)
                } else {
                    errorReporter.reportError(call, ErrorMessages.MISSING_LEFT_OPERAND_NEQ)
                    null
                }
            }
        }
        IrStatementOrigin.ANDAND -> buildLogicalCallSqlExpr(irFunction, call, errorReporter, setNot, ConditionLogicalKind.And)
        IrStatementOrigin.OROR -> buildLogicalCallSqlExpr(irFunction, call, errorReporter, setNot, ConditionLogicalKind.Or)
        IrStatementOrigin.EXCL -> {
            val arg = call.conditionReceiver()
            analyzeAndBuildSqlExpr(irFunction, arg, errorReporter, !setNot)
        }
        IrStatementOrigin.GT -> buildBinaryComparisonSqlExpr(irFunction, call, ConditionComparisonKind.GreaterThan, setNot, errorReporter)
        IrStatementOrigin.LT -> buildBinaryComparisonSqlExpr(irFunction, call, ConditionComparisonKind.LessThan, setNot, errorReporter)
        IrStatementOrigin.GTEQ -> buildBinaryComparisonSqlExpr(irFunction, call, ConditionComparisonKind.GreaterThanOrEqual, setNot, errorReporter)
        IrStatementOrigin.LTEQ -> buildBinaryComparisonSqlExpr(irFunction, call, ConditionComparisonKind.LessThanOrEqual, setNot, errorReporter)
        else -> {
            analyzeMethodSqlExpr(irFunction, call, errorReporter, setNot)
        }
    }
}

context(context: IrPluginContext, builder: IrBlockBuilder)
private fun buildBlockSqlExpr(
    irFunction: IrFunction,
    statements: List<IrStatement>,
    errorReporter: ErrorReporter,
    setNot: Boolean
): IrExpression? {
    val children = statements.mapNotNull {
        analyzeAndBuildSqlExpr(irFunction, it, errorReporter, setNot)
    }
    if (children.isEmpty()) return null
    return buildConditionSqlExpr(irFunction, kind = ConditionLogicalKind.Root, not = false, children = children)
}

context(context: IrPluginContext, builder: IrBlockBuilder)
private fun buildLogicalCallSqlExpr(
    irFunction: IrFunction,
    call: IrCall,
    errorReporter: ErrorReporter,
    setNot: Boolean,
    kind: ConditionLogicalKind
): IrExpression {
    val children = call.valueArguments
        .take(2)
        .filterNotNull()
        .mapNotNull { analyzeAndBuildSqlExpr(irFunction, it, errorReporter, setNot) }
    val actualKind = when {
        !setNot -> kind
        kind == ConditionLogicalKind.And -> ConditionLogicalKind.Or
        else -> ConditionLogicalKind.And
    }
    return buildConditionSqlExpr(irFunction, kind = actualKind, not = false, children = children)
}

context(context: IrPluginContext, builder: IrBlockBuilder)
private fun analyzeMethodSqlExpr(
    irFunction: IrFunction,
    call: IrCall,
    errorReporter: ErrorReporter,
    setNot: Boolean
): IrExpression? {
    val funcName = call.funcName()
    val extensionReceiver = call.extensionReceiverArgument
    val dispatchReceiver = call.dispatchReceiverArgument

    return when (funcName) {
        "run" -> {
            val lambda = call.valueArguments.firstNotNullOfOrNull { it as? IrFunctionExpression } ?: return null
            analyzeAndBuildSqlExpr(irFunction, lambda.function.body ?: return null, errorReporter, setNot)
        }
        "not" -> {
            val receiver = call.conditionReceiver()
            analyzeAndBuildSqlExpr(irFunction, receiver, errorReporter, !setNot)
        }
        "isNull" -> {
            val receiver = call.conditionReceiver()
            val fieldExpr = extractFieldExpression(irFunction, receiver, errorReporter) ?: return null
            buildConditionSqlExpr(
                irFunction,
                field = fieldExpr,
                kind = ConditionExpressionKind.IsNull,
                not = setNot,
                tableName = extractTableNameExpr(receiver)
            )
        }
        "notNull" -> {
            val receiver = call.conditionReceiver()
            val fieldExpr = extractFieldExpression(irFunction, receiver, errorReporter) ?: return null
            buildConditionSqlExpr(
                irFunction,
                field = fieldExpr,
                kind = ConditionExpressionKind.IsNull,
                not = !setNot,
                tableName = extractTableNameExpr(receiver)
            )
        }
        in NoArgComparisonByFunctionName -> {
            if (call.getValueArgumentSafe(0) != null) {
                errorReporter.reportWarning(call, ErrorMessages.parameterizedConditionFunctionUnsupported(funcName))
                return null
            }
            val comparisonKind = NoArgComparisonByFunctionName.getValue(funcName)
            buildNoArgComparisonSqlExpr(irFunction, call, comparisonKind, setNot, errorReporter)
        }
        "eq" -> {
            val receiver = extensionReceiver ?: call.conditionReceiver()
            val arg = call.getValueArgumentSafe(0)
            if (arg != null) {
                errorReporter.reportWarning(call, ErrorMessages.parameterizedConditionFunctionUnsupported(funcName))
                return null
            }
            buildKPojoOrFieldEq(irFunction, call, receiver, not = setNot, errorReporter = errorReporter)
        }
        "neq" -> {
            val receiver = extensionReceiver ?: call.conditionReceiver()
            val arg = call.getValueArgumentSafe(0)
            if (arg != null) {
                errorReporter.reportWarning(call, ErrorMessages.parameterizedConditionFunctionUnsupported(funcName))
                return null
            }
            buildKPojoOrFieldEq(irFunction, call, receiver, not = !setNot, errorReporter = errorReporter)
        }
        "between", "notBetween" -> {
            val withNot = setNot xor (funcName == "notBetween")
            val receiver = call.conditionReceiver()
            val range = call.getValueArgumentSafe(0) ?: return null
            val fieldExpr = extractFieldExpression(irFunction, receiver, errorReporter) ?: return null
            buildConditionSqlExpr(
                irFunction,
                field = fieldExpr,
                kind = ConditionExpressionKind.Between,
                not = withNot,
                value = range,
                tableName = extractTableNameExpr(receiver)
            )
        }
        "exists" -> {
            val query = call.getValueArgumentSafe(0) ?: return null
            buildExistsSqlExpr(irFunction, query, not = setNot)
        }
        in StringMatchFunctionNames ->
            analyzeStringMatchSqlExpr(irFunction, call, funcName, errorReporter, setNot)
        "asSql" -> {
            val receiver = call.conditionReceiver()
            buildConditionSqlExpr(
                irFunction,
                field = null,
                kind = ConditionExpressionKind.RawSql,
                not = false,
                value = receiver
            )
        }
        "ifNoValue" -> {
            val receiver = call.conditionReceiver()
            val strategy = call.getValueArgumentSafe(0) ?: return analyzeAndBuildSqlExpr(irFunction, receiver, errorReporter, setNot)
            val expr = analyzeAndBuildSqlExpr(irFunction, receiver, errorReporter, setNot) ?: return null
            if (expr is IrCall && expr.symbol in noValueAwareConditionExprSymbols()) {
                expr.arguments[6] = strategy
            }
            expr
        }
        "takeIf" -> {
            val receiver = call.conditionReceiver()
            val predicate = call.getValueArgumentSafe(0) ?: return null
            // Generate runtime: if (predicate) expr else null
            val sqlExpr = analyzeAndBuildSqlExpr(irFunction, receiver, errorReporter, setNot) ?: return null
            builder.irIfThenElse(
                sqlExpr.type.makeNullable(),
                predicate,
                sqlExpr,
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

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBlockBuilder)
private fun buildExistsSqlExpr( 
    irFunction: IrFunction,
    query: IrExpression,
    not: Boolean
): IrExpression {
    val receiver = irFunction.parameters.extensionReceiver
        ?: error("KTableForCondition extension receiver not found for EXISTS generation.")
    return builder.irCall(existsExprMethodSymbol).apply {
        dispatchReceiver = builder.irGet(receiver)
        arguments[1] = query
        arguments[2] = builder.irBoolean(not)
    }
}

/**
 * Handles string-matching condition functions: like, notLike, startsWith, endsWith, contains, regexp, notRegexp.
 */
context(context: IrPluginContext, builder: IrBlockBuilder)
private fun analyzeStringMatchSqlExpr(
    irFunction: IrFunction,
    call: IrCall,
    funcName: String,
    errorReporter: ErrorReporter,
    setNot: Boolean
): IrExpression? {
    return when (funcName) {
        "like", "notLike" -> {
            val withNot = setNot xor (funcName == "notLike")
            val receiver = call.conditionReceiver()
            val pattern = call.getValueArgumentSafe(0)
            if (pattern != null) {
                buildFieldConditionWithValue(irFunction, receiver, ConditionExpressionKind.Like, withNot, errorReporter) {
                    resolveValueExpression(irFunction, pattern, errorReporter)
                }
            } else {
                buildNoArgLikeSqlExpr(irFunction, call, not = withNot, errorReporter = errorReporter)
            }
        }
        "startsWith" -> {
            val receiver = call.conditionReceiver()
            val prefix = call.getValueArgumentSafe(0)
            if (prefix != null) {
                buildFieldConditionWithValue(irFunction, receiver, ConditionExpressionKind.Like, setNot, errorReporter) {
                    concatIrString(prefix, builder.irString("%"))
                }
            } else {
                buildNoArgStartsWithSqlExpr(irFunction, call, setNot, errorReporter)
            }
        }
        "endsWith" -> {
            val receiver = call.conditionReceiver()
            val suffix = call.getValueArgumentSafe(0)
            if (suffix != null) {
                buildFieldConditionWithValue(irFunction, receiver, ConditionExpressionKind.Like, setNot, errorReporter) {
                    concatIrString(builder.irString("%"), suffix)
                }
            } else {
                buildNoArgEndsWithSqlExpr(irFunction, call, setNot, errorReporter)
            }
        }
        "contains" -> {
            val receiver = call.conditionReceiver()
            val arg = call.getValueArgumentSafe(0)
            if (arg != null) {
                if (receiver.type.isKSelectableType()) {
                    buildInSubquerySqlExpr(irFunction, query = receiver, value = arg, not = setNot, errorReporter = errorReporter)
                } else if (receiver.type.classFqName?.asString() == "kotlin.String") {
                    val fieldExpr = extractFieldExpression(irFunction, receiver, errorReporter) ?: return null
                    val pattern = concatIrString(concatIrString(builder.irString("%"), arg), builder.irString("%"))
                    buildConditionSqlExpr(
                        irFunction,
                        field = fieldExpr,
                        kind = ConditionExpressionKind.Like,
                        not = setNot,
                        value = pattern,
                        tableName = extractTableNameExpr(receiver)
                    )
                } else {
                    val fieldExpr = extractFieldExpression(irFunction, arg, errorReporter) ?: return null
                    buildConditionSqlExpr(
                        irFunction,
                        field = fieldExpr,
                        kind = ConditionExpressionKind.In,
                        not = setNot,
                        value = receiver,
                        tableName = extractTableNameExpr(arg)
                    )
                }
            } else {
                buildNoArgContainsSqlExpr(irFunction, call, setNot, errorReporter)
            }
        }
        "regexp", "notRegexp" -> {
            val withNot = setNot xor (funcName == "notRegexp")
            val receiver = call.conditionReceiver()
            val arg = call.getValueArgumentSafe(0)
            if (arg != null) {
                buildFieldConditionWithValue(irFunction, receiver, ConditionExpressionKind.Regexp, withNot, errorReporter) { arg }
            } else {
                buildNoArgRegexpSqlExpr(irFunction, call, not = withNot, errorReporter = errorReporter)
            }
        }
        else -> null
    }
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBlockBuilder)
private fun buildInSubquerySqlExpr(
    irFunction: IrFunction,
    query: IrExpression,
    value: IrExpression,
    not: Boolean,
    errorReporter: ErrorReporter
): IrExpression? {
    val tupleFields = buildTupleFieldList(irFunction, value, errorReporter)
    val fieldExpr = if (tupleFields == null) {
        extractFieldExpression(irFunction, value, errorReporter) ?: return null
    } else {
        null
    }
    val tupleExpr = tupleFields?.let {
        val receiver = irFunction.parameters.extensionReceiver
            ?: error("KTableForCondition extension receiver not found for tuple IN generation.")
        builder.irCall(tupleExprMethodSymbol).apply {
            dispatchReceiver = builder.irGet(receiver)
            arguments[1] = it
        }
    }
    return buildConditionSqlExpr(irFunction, 
        field = fieldExpr,
        sqlExpr = tupleExpr,
        kind = ConditionExpressionKind.In,
        not = not,
        value = query,
        tableName = if (fieldExpr != null) extractTableNameExpr(value) else null
    )
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBlockBuilder)
private fun buildTupleFieldList(
    irFunction: IrFunction,
    expression: IrExpression,
    errorReporter: ErrorReporter
): IrExpression? {
    val fields = analyzeAndBuildFields(irFunction, expression, errorReporter)
    if (fields.size <= 1) {
        if (fields.size == 1 && expression is IrCall && expression.symbol.owner.name.asString() in setOf("get", "of", "listOf", "mutableListOf", "setOf", "arrayOf")) {
        errorReporter.reportError(
            expression,
            "Tuple IN requires at least two fields. Use `field in query` for a single-field subquery."
        )
        }
        return null
    }

    return com.kotlinorm.compiler.utils.irListOf(fieldClassSymbol.defaultType, fields)
}

// In K2 IR, && and || are lowered to IrWhen:
//   a && b  →  when(ANDAND) { a -> b; else -> false }
//   a || b  →  when(OROR)   { a -> true; else -> b }
// We process both condition and result of each branch, skipping boolean constants.
context(context: IrPluginContext, builder: IrBlockBuilder)
private fun analyzeWhenSqlExpr(
    irFunction: IrFunction,
    expression: IrWhen,
    errorReporter: ErrorReporter,
    setNot: Boolean
): IrExpression? {
    // Determine logical operator from origin, applying De Morgan's when negated
    val logicalOp = when (expression.origin) {
        IrStatementOrigin.OROR -> if (setNot) ConditionLogicalKind.And else ConditionLogicalKind.Or
        IrStatementOrigin.ANDAND -> if (setNot) ConditionLogicalKind.Or else ConditionLogicalKind.And
        else -> ConditionLogicalKind.And
    }

    val children = mutableListOf<IrExpression>()
    for (branch in expression.branches) {
        val cond = branch.condition
        val result = branch.result
        // Skip boolean constants (true/false) that K2 inserts as lowering artifacts
        if (cond !is IrConst || cond.value !is Boolean) {
            analyzeAndBuildSqlExpr(irFunction, cond, errorReporter, setNot)?.let { children.add(it) }
        }
        if (result !is IrConst || result.value !is Boolean) {
            analyzeAndBuildSqlExpr(irFunction, result, errorReporter, setNot)?.let { children.add(it) }
        }
    }

    if (children.isEmpty()) return null
    if (children.size == 1) return children.first()
    return buildConditionSqlExpr(irFunction, kind = logicalOp, not = false, children = children)
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
            else -> null
        }
        is IrPropertyReference -> buildFieldFromPropertyRef(expression, errorReporter)
        // Handle !! (not-null assertion) — unwrap and extract from the inner expression
        is IrTypeOperatorCall -> extractFieldExpression(irFunction, expression.argument, errorReporter)
        else -> null
    }
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBlockBuilder)
private fun extractSqlExpression(irFunction: IrFunction, expression: IrExpression, errorReporter: ErrorReporter): IrExpression? {
    return when (expression) {
        is IrCall -> when {
            expression.isKronosFunction() || expression.operatorFunctionName() != null -> {
                builder.irCall(kronosFunctionExprGetterSymbol).apply {
                    dispatchReceiver = buildKronosFunctionExpr(irFunction, expression, errorReporter)
                }
            }
            else -> null
        }
        is IrTypeOperatorCall -> extractSqlExpression(irFunction, expression.argument, errorReporter)
        else -> null
    }
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBlockBuilder)
private fun functionMetadataField(expression: IrExpression): IrExpression? {
    val call = expression as? IrCall ?: return null
    val receiverCall = (call.extensionReceiverArgument ?: call.dispatchReceiverArgument) as? IrCall
    val sourceCall = if (call.funcName() == WindowOverFunctionName && receiverCall != null) receiverCall else call
    val functionName = sourceCall.operatorFunctionName() ?: sourceCall.funcName()
    return builder.irCall(fieldConstructorSymbol).apply {
        arguments[0] = builder.irString(functionName)
        arguments[1] = builder.irString(functionName)
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
                val irClass = expression.dispatchReceiver!!.type.classOrNull!!.owner
                if (irClass.isGeneratedProjectionClass()) {
                    return builder.irString("")
                }
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
            expression.operatorFunctionName() != null -> {
                expression.operatorOperands().firstNotNullOfOrNull { arg ->
                    extractTableNameExpr(arg)
                }
            }
            else -> null
        }
        is IrTypeOperatorCall -> extractTableNameExpr(expression.argument)
        else -> null
    }
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
private fun IrClass.isGeneratedProjectionClass(): Boolean =
    kotlinFqName.parent() == GeneratedProjectionPackageFqName

/**
 * Resolves a value expression: property access becomes Field and Kronos function calls
 * become KronosFunctionExpr; otherwise the original expression is preserved.
 */
context(context: IrPluginContext, builder: IrBlockBuilder)
private fun resolveValueExpression(irFunction: IrFunction, expression: IrExpression, errorReporter: ErrorReporter): IrExpression {
    val valueExpression = expression.safeSelectableValueExpression()
    if (valueExpression is IrCall && (valueExpression.isKronosFunction() || valueExpression.operatorFunctionName() != null)) {
        return buildKronosFunctionExpr(irFunction, valueExpression, errorReporter)
    }
    return extractFieldExpression(irFunction, valueExpression, errorReporter) ?: valueExpression
}

// ============================================================================
// Equal SQL expressions (handles KPojo object comparison too)
// ============================================================================

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBlockBuilder)
private fun buildEqualSqlExpr(
    irFunction: IrFunction,
    left: IrExpression,
    right: IrExpression,
    not: Boolean,
    errorReporter: ErrorReporter
): IrExpression? {
    val leftField = extractFieldExpression(irFunction, left, errorReporter)
    val rightField = extractFieldExpression(irFunction, right, errorReporter)
    val leftSql = extractSqlExpression(irFunction, left, errorReporter)
    val rightSql = extractSqlExpression(irFunction, right, errorReporter)

    return when {
        leftSql != null -> buildConditionSqlExpr(
            irFunction,
            field = functionMetadataField(left),
            sqlExpr = leftSql,
            kind = ConditionExpressionKind.Equal,
            not = not,
            value = resolveValueExpression(irFunction, right, errorReporter),
            tableName = extractTableNameExpr(left)
        )
        rightSql != null -> buildConditionSqlExpr(
            irFunction,
            field = functionMetadataField(right),
            sqlExpr = rightSql,
            kind = ConditionExpressionKind.Equal,
            not = not,
            value = resolveValueExpression(irFunction, left, errorReporter),
            tableName = extractTableNameExpr(right)
        )
        leftField != null -> buildConditionSqlExpr(
            irFunction,
            field = leftField,
            kind = ConditionExpressionKind.Equal,
            not = not,
            value = resolveValueExpression(irFunction, right, errorReporter),
            tableName = extractTableNameExpr(left)
        )
        rightField != null -> buildConditionSqlExpr(
            irFunction,
            field = rightField,
            kind = ConditionExpressionKind.Equal,
            not = not,
            value = resolveValueExpression(irFunction, left, errorReporter),
            tableName = extractTableNameExpr(right)
        )
        left.type.isKPojoType() -> buildKPojoEqualSqlExpr(irFunction, left, right, not, errorReporter)
        right.type.isKPojoType() -> buildKPojoEqualSqlExpr(irFunction, right, left, not, errorReporter)
        else -> {
            // Neither side is a field or KPojo — this is a pure runtime expression (e.g., 1 == another.id.value)
            // Generate a SQL "true" / "false" expression so it appears in the WHERE clause
            buildConditionSqlExpr(
                irFunction,
                field = null,
                kind = ConditionExpressionKind.RawSql,
                not = false,
                value = builder.irBoolean(!not)
            )
        }
    }
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBlockBuilder)
private fun buildKPojoEqualSqlExpr(
    irFunction: IrFunction,
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
        val getterCall = builder.irCall(getter.symbol).apply { dispatchReceiver = valueExpr.freshValueExpression() }
        buildConditionSqlExpr(
            irFunction,
            field = fieldExpr,
            kind = ConditionExpressionKind.Equal,
            not = not,
            value = getterCall,
            tableName = builder.getTableNameExpr(irClass)
        )
    }
    return buildConditionSqlExpr(irFunction, kind = ConditionLogicalKind.And, not = false, children = children)
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBlockBuilder)
private fun IrExpression.freshValueExpression(): IrExpression =
    if (this is IrGetValue) builder.irGet(symbol.owner) else deepCopyWithSymbols()

// ============================================================================
// Binary comparison SQL expressions (for `it.age < 18` and `18 > it.age` forms)
// ============================================================================

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBlockBuilder)
private fun buildBinaryComparisonSqlExpr(
    irFunction: IrFunction,
    call: IrCall,
    operator: ConditionComparisonKind,
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

    val directLeft = call.getValueArgumentSafe(0)
    val directRight = call.getValueArgumentSafe(1)
    if (directLeft != null && directRight != null) {
        buildComparisonSqlExpr(irFunction, directLeft, directRight, operator, not, errorReporter)
            ?.let { return it }
    }

    // Try to find compareTo call in valueArguments
    val compareToCall = call.valueArguments.filterIsInstance<IrCall>().firstOrNull { it.symbol.owner.name.asString() == "compareTo" }
    if (compareToCall != null) {
        // In K2, the field access is typically in extensionReceiver for Comparable.compareTo
        val compareValue = compareToCall.getValueArgumentSafe(0)

        // Try extensionReceiver as field access first, then dispatchReceiver
        val fieldAccess = compareToCall.extensionReceiverArgument ?: compareToCall.dispatchReceiverArgument
        if (fieldAccess != null && compareValue != null) {
            buildComparisonSqlExpr(irFunction, fieldAccess, compareValue, operator, not, errorReporter)
                ?.let { return it }
        }
    }

    errorReporter.reportError(call, ErrorMessages.missingOperandFor(operator.diagnosticName))
    return null
}

context(context: IrPluginContext, builder: IrBlockBuilder)
private fun buildComparisonSqlExpr(
    irFunction: IrFunction,
    left: IrExpression,
    right: IrExpression,
    operator: ConditionComparisonKind,
    not: Boolean,
    errorReporter: ErrorReporter
): IrExpression? {
    comparisonOperand(irFunction, left, errorReporter)?.let { operand ->
        return operand.buildComparison(irFunction, operator, not, right, errorReporter)
    }
    comparisonOperand(irFunction, right, errorReporter)?.let { operand ->
        return operand.buildComparison(irFunction, operator.reversed(), not, left, errorReporter)
    }
    return null
}

context(context: IrPluginContext, builder: IrBlockBuilder)
private fun comparisonOperand(
    irFunction: IrFunction,
    expression: IrExpression,
    errorReporter: ErrorReporter
): ConditionOperand? {
    val sqlExpr = extractSqlExpression(irFunction, expression, errorReporter)
    if (sqlExpr != null) {
        return ConditionOperand(expression, functionMetadataField(expression), sqlExpr)
    }
    val field = extractFieldExpression(irFunction, expression, errorReporter) ?: return null
    return ConditionOperand(expression, field, null)
}

context(context: IrPluginContext, builder: IrBlockBuilder)
private fun ConditionOperand.buildComparison(
    irFunction: IrFunction,
    operator: ConditionComparisonKind,
    not: Boolean,
    value: IrExpression,
    errorReporter: ErrorReporter
): IrExpression {
    return buildConditionSqlExpr(
        irFunction,
        field = field,
        sqlExpr = sqlExpr,
        kind = operator.condition,
        not = not,
        value = resolveValueExpression(irFunction, value, errorReporter),
        tableName = extractTableNameExpr(expression)
    )
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
    val receiver = (call.extensionReceiverArgument ?: call.dispatchReceiverArgument) as? IrCall ?: return null
    val propName = receiver.symbol.owner.correspondingPropertySymbol!!.owner.name.asString()
    // The KTableForCondition instance is the extension receiver of the lambda, not the User instance
    val tableParam = irFunction.parameters.extensionReceiver!!
    return builder.irCall(sourceValueByFieldNameMethodSymbol).apply {
        dispatchReceiver = builder.irGet(tableParam)
        arguments[1] = builder.irString(propName)
    }
}

context(context: IrPluginContext, builder: IrBlockBuilder)
private fun buildNoArgEqualSqlExpr(irFunction: IrFunction, call: IrCall, not: Boolean, errorReporter: ErrorReporter): IrExpression? {
    return buildNoArgFieldConditionSqlExpr(irFunction, call, ConditionExpressionKind.Equal, not, errorReporter)
}

/**
 * Handles no-arg `.eq` / `.neq` on either a single field or a KPojo (with optional minus).
 * - `it.age.eq` → single field equal SQL expression
 * - `it.eq` or `(it - it.gender).eq` → AND SQL expression for all (non-excluded) column properties
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
        val tableParam = irFunction.parameters.extensionReceiver
            ?: error("KTableForCondition extension receiver not found for KPojo equality generation.")
        val children = columnProps.map { prop ->
            val fieldExpr = buildFieldFromProperty(prop)
            val propName = prop.name.asString()
            val valueExpr = builder.irCall(sourceValueByFieldNameMethodSymbol).apply {
                dispatchReceiver = builder.irGet(tableParam)
                arguments[1] = builder.irString(propName)
            }
            buildConditionSqlExpr(
                irFunction,
                field = fieldExpr,
                kind = ConditionExpressionKind.Equal,
                not = not,
                value = valueExpr,
                tableName = builder.getTableNameExpr(irClass)
            )
        }
        if (children.size == 1) return children.first()
        return buildConditionSqlExpr(irFunction, kind = ConditionLogicalKind.And, not = false, children = children)
    }
    // Not a KPojo — fall back to single field eq
    return buildNoArgEqualSqlExpr(irFunction, call, not, errorReporter)
}

/**
 * Analyzes an expression to determine if it's a KPojo or a MINUS expression on a KPojo.
 * Returns the KPojo expression and the list of excluded field names.
 */
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
        excludes.add(arg.symbol.owner.correspondingPropertySymbol!!.owner.name.asString())
    }
    val receiver = call.extensionReceiverArgument ?: call.dispatchReceiverArgument
    return if (receiver is IrCall && receiver.origin == IrStatementOrigin.MINUS) {
        val (base, innerExcludes) = collectMinusExcludes(receiver)
        base to (innerExcludes + excludes)
    } else {
        receiver!! to excludes
    }
}

context(context: IrPluginContext, builder: IrBlockBuilder)
private fun buildNoArgComparisonSqlExpr(
    irFunction: IrFunction,
    call: IrCall,
    operator: ConditionComparisonKind,
    not: Boolean,
    errorReporter: ErrorReporter
): IrExpression? {
    return buildNoArgFieldConditionSqlExpr(irFunction, call, operator.condition, not, errorReporter)
}

context(context: IrPluginContext, builder: IrBlockBuilder)
private fun buildNoArgFieldConditionSqlExpr(
    irFunction: IrFunction,
    call: IrCall,
    kind: ConditionExpressionKind,
    not: Boolean,
    errorReporter: ErrorReporter,
    valueTransform: (IrExpression) -> IrExpression = { it }
): IrExpression? {
    val receiver = call.conditionReceiver()
    val fieldExpr = extractFieldExpression(irFunction, receiver, errorReporter) ?: return null
    val valueExpr = valueTransform(getNoArgValue(irFunction, call)!!)
    return buildConditionSqlExpr(
        irFunction,
        field = fieldExpr,
        kind = kind,
        not = not,
        value = valueExpr,
        tableName = extractTableNameExpr(receiver)
    )
}

context(context: IrPluginContext, builder: IrBlockBuilder)
private fun buildNoArgLikeSqlExpr(irFunction: IrFunction, call: IrCall, not: Boolean, errorReporter: ErrorReporter): IrExpression? {
    return buildNoArgFieldConditionSqlExpr(irFunction, call, ConditionExpressionKind.Like, not, errorReporter) { rawValue ->
        concatIrString(builder.irString(""), rawValue)
    }
}

private fun IrCall.conditionReceiver(): IrExpression =
    extensionReceiverArgument ?: dispatchReceiverArgument ?: error("Kronos condition DSL receiver is missing.")

context(context: IrPluginContext, builder: IrBlockBuilder)
private fun buildFieldConditionWithValue(
    irFunction: IrFunction,
    receiver: IrExpression,
    kind: ConditionExpressionKind,
    not: Boolean,
    errorReporter: ErrorReporter,
    value: () -> IrExpression
): IrExpression? {
    val fieldExpr = extractFieldExpression(irFunction, receiver, errorReporter) ?: return null
    return buildConditionSqlExpr(
        irFunction,
        field = fieldExpr,
        kind = kind,
        not = not,
        value = value(),
        tableName = extractTableNameExpr(receiver)
    )
}

context(context: IrPluginContext, builder: IrBlockBuilder)
private fun buildNoArgStartsWithSqlExpr(irFunction: IrFunction, call: IrCall, not: Boolean, errorReporter: ErrorReporter): IrExpression? {
    return buildNoArgFieldConditionSqlExpr(irFunction, call, ConditionExpressionKind.Like, not, errorReporter) { rawValue ->
        concatIrString(concatIrString(builder.irString(""), rawValue), builder.irString("%"))
    }
}

context(context: IrPluginContext, builder: IrBlockBuilder)
private fun buildNoArgEndsWithSqlExpr(irFunction: IrFunction, call: IrCall, not: Boolean, errorReporter: ErrorReporter): IrExpression? {
    return buildNoArgFieldConditionSqlExpr(irFunction, call, ConditionExpressionKind.Like, not, errorReporter) { rawValue ->
        concatIrString(builder.irString("%"), concatIrString(builder.irString(""), rawValue))
    }
}

context(context: IrPluginContext, builder: IrBlockBuilder)
private fun buildNoArgContainsSqlExpr(irFunction: IrFunction, call: IrCall, not: Boolean, errorReporter: ErrorReporter): IrExpression? {
    return buildNoArgFieldConditionSqlExpr(irFunction, call, ConditionExpressionKind.Like, not, errorReporter) { rawValue ->
        val valueStr = concatIrString(builder.irString(""), rawValue)
        concatIrString(concatIrString(builder.irString("%"), valueStr), builder.irString("%"))
    }
}

context(context: IrPluginContext, builder: IrBlockBuilder)
private fun buildNoArgRegexpSqlExpr(irFunction: IrFunction, call: IrCall, not: Boolean, errorReporter: ErrorReporter): IrExpression? {
    return buildNoArgFieldConditionSqlExpr(irFunction, call, ConditionExpressionKind.Regexp, not, errorReporter)
}

// ============================================================================
// Core condition SQL expression builder.
// ============================================================================

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBlockBuilder)
private fun buildConditionSqlExpr(
    irFunction: IrFunction,
    field: IrExpression? = null,
    sqlExpr: IrExpression? = null,
    kind: ConditionBuildKind,
    not: Boolean,
    value: IrExpression? = null,
    children: List<IrExpression> = emptyList(),
    noValueStrategyType: IrExpression? = null,
    tableName: IrExpression? = null
): IrExpression {
    val receiver = irFunction.parameters.extensionReceiver
        ?: error("KTableForCondition extension receiver not found for condition SQL expression generation.")

    if (children.isNotEmpty()) {
        val method = when (kind) {
            ConditionLogicalKind.Or -> orExprMethodSymbol
            ConditionLogicalKind.And,
            ConditionLogicalKind.Root -> andExprMethodSymbol
            is ConditionExpressionKind -> error("Condition '$kind' cannot contain child expressions.")
        }
        return builder.irCall(method).apply {
            dispatchReceiver = builder.irGet(receiver)
            arguments[1] = irListOf(children.first().type, children)
        }
    }

    if (kind is ConditionLogicalKind) {
        return builder.irNull()
    }

    val expressionKind = kind as ConditionExpressionKind
    val method = conditionExprMethodSymbol(expressionKind)
    return builder.irCall(method).apply {
        dispatchReceiver = builder.irGet(receiver)
        arguments[1] = field ?: builder.irNull()
        arguments[2] = sqlExpr ?: builder.irNull()
        arguments[3] = builder.irBoolean(not)
        arguments[4] = value ?: builder.irNull()
        arguments[5] = tableName ?: builder.irNull()
        arguments[6] = noValueStrategyType ?: builder.irNull()
    }
}

context(context: IrPluginContext)
private fun noValueAwareConditionExprSymbols(): Set<IrSimpleFunctionSymbol> =
    ConditionExpressionKind.entries
        .filter { it.noValueAware }
        .mapTo(mutableSetOf()) { conditionExprMethodSymbol(it) }

context(context: IrPluginContext)
private fun conditionExprMethodSymbol(kind: ConditionExpressionKind): IrSimpleFunctionSymbol =
    when (kind) {
        ConditionExpressionKind.RawSql -> rawConditionExprMethodSymbol
        ConditionExpressionKind.IsNull -> isNullConditionExprMethodSymbol
        ConditionExpressionKind.Equal -> equalConditionExprMethodSymbol
        ConditionExpressionKind.GreaterThan -> greaterThanConditionExprMethodSymbol
        ConditionExpressionKind.GreaterThanOrEqual -> greaterThanOrEqualConditionExprMethodSymbol
        ConditionExpressionKind.LessThan -> lessThanConditionExprMethodSymbol
        ConditionExpressionKind.LessThanOrEqual -> lessThanOrEqualConditionExprMethodSymbol
        ConditionExpressionKind.Like -> likeConditionExprMethodSymbol
        ConditionExpressionKind.Regexp -> regexpConditionExprMethodSymbol
        ConditionExpressionKind.In -> inConditionExprMethodSymbol
        ConditionExpressionKind.Between -> betweenConditionExprMethodSymbol
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
