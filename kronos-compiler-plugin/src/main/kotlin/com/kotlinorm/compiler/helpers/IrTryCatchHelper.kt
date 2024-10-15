package com.kotlinorm.compiler.helpers

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.irCatch
import org.jetbrains.kotlin.fir.types.isSubtypeOf
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildVariable
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

context(IrPluginContext)
val JavaLangExceptionSymbol
    get() = referenceClass("java.lang.Exception")!!

context(IrPluginContext)
@OptIn(UnsafeDuringIrConstructionAPI::class)
val printStackTraceSymbol
    get() = referenceClass("java.lang.Throwable")!!.getSimpleFunction("printStackTrace")!!

context(IrPluginContext)
class IrTryBuilder(private val builder: IrBuilderWithScope) {
    private val catches = mutableListOf<IrCatch>()
    private val caughtTypes = mutableSetOf<IrType>()
    private var finallyExpression: IrExpression? = null

    @OptIn(ExperimentalContracts::class)
    fun irCatch(
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
            Name.identifier("t_${catches.size}"),
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

context(IrPluginContext)
inline fun IrBuilderWithScope.irTry(
    result: IrExpression = irBlock {},
    type: IrType = result.type,
    catches: IrTryBuilder.() -> Unit = {},
): IrTry = IrTryBuilder(
    this
).apply(catches).build(result, type)