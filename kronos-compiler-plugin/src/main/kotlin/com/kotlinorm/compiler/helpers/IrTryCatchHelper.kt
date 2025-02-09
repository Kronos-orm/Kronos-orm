package com.kotlinorm.compiler.helpers

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.irCatch
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.buildVariable
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irTry
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrCatch
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrTry
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.getSimpleFunction
import org.jetbrains.kotlin.name.Name
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

val IrPluginContext.JavaLangExceptionSymbol
    get() = referenceClass("java.lang.Exception")!!

@OptIn(UnsafeDuringIrConstructionAPI::class)
val IrPluginContext.printStackTraceSymbol
    get() = referenceClass("java.lang.Throwable")!!.getSimpleFunction("printStackTrace")!!

class IrTryBuilder(private val builder: IrBuilderWithScope) {
    private val catches = mutableListOf<IrCatch>()
    private val caughtTypes = mutableSetOf<IrType>()
    private var finallyExpression: IrExpression? = null

    @OptIn(ExperimentalContracts::class)
    fun IrPluginContext.irCatch(
        throwableType: IrType = JavaLangExceptionSymbol.defaultType,
        body: IrBuilderWithScope.(IrVariable) -> IrExpression = {
            applyIrCall(printStackTraceSymbol) {
                dispatchReceiver = irGet(it)
            }
        }
    ) {
        contract { callsInPlace(body, InvocationKind.EXACTLY_ONCE) }
        if (throwableType.subType() == builder.context.irBuiltIns.throwableType && throwableType != builder.context.irBuiltIns.throwableType) error(
            "Can only catch types that inherit from kotlin.Throwable"
        )

        if (!caughtTypes.add(throwableType)) error("Already caught type $throwableType")

        val catchVariable = buildVariable(
            builder.scope.getLocalDeclarationParent(),
            builder.startOffset,
            builder.endOffset,
            IrDeclarationOrigin.CATCH_PARAMETER,
            Name.identifier("e_${catches.size}"),
            throwableType
        )

        catches += builder.irCatch(catchVariable, builder.body(catchVariable))
    }

    fun irFinally(expression: IrExpression) {
        if (finallyExpression != null) error("finally expression already set")
        finallyExpression = expression
    }

    fun build(result: IrExpression, type: IrType): IrTry = builder.irTry(type, result, catches, finallyExpression)
}

inline fun IrBuilderWithScope.irTry(
    result: IrExpression = irBlock {},
    type: IrType = result.type,
    catches: IrTryBuilder.() -> Unit = {},
): IrTry = IrTryBuilder(
    this
).apply(catches).build(result, type)