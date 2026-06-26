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

/**
 * Provides utilities to build `try-catch-finally` blocks in Kotlin IR.
 */

/**
 * Gets the symbol for the `java.lang.Exception` class.
 *
 * @return The symbol for the `java.lang.Exception` class.
 */
context(_: IrPluginContext)
val JavaLangExceptionSymbol
    get() = referenceClass("java.lang.Exception")!!

/**
 * Gets the symbol for the `printStackTrace` function of the `java.lang.Throwable` class.
 *
 * @return The symbol for the `printStackTrace` function.
 */
@UnsafeDuringIrConstructionAPI
context(_: IrPluginContext)
val printStackTraceSymbol
    get() = referenceClass("java.lang.Throwable")!!.getSimpleFunction("printStackTrace")!!

/**
 * A builder class for constructing `IrTry` expressions with multiple `catch` blocks and an optional `finally` block.
 *
 * @property result The main expression to be executed within the `try` block.
 * @property type The type of the `try` expression, defaulting to the type of the `result`.
 */
class IrTryBuilder(
    val result: IrExpression,
    val type: IrType = result.type
) {
    private val catches = mutableListOf<IrCatch>()
    private val caughtTypes = mutableSetOf<IrType>()
    private var finallyExpression: IrExpression? = null
        set(value) {
            if (field != null) error("Finally block already set")
            field = value
        }


    /**
     * Adds a `catch` block to the `try` expression for the specified throwable type.
     *
     * @param throwableType The type of exception to catch. Defaults to `java.lang.Exception`.
     * @param body A lambda that defines the body of the `catch` block, receiving the caught exception variable.
     * @throws IllegalArgumentException if the `throwableType` does not inherit from `kotlin.Throwable`
     */
    @ExperimentalContracts
    @UnsafeDuringIrConstructionAPI
    context(builder: IrBuilderWithScope, context: IrPluginContext)
    private fun irCatch(
        throwableType: IrType = JavaLangExceptionSymbol.defaultType,
        body: IrBuilderWithScope.(IrVariable) -> IrExpression = { printStackTraceSymbol(irGet(it)) }
    ) {
        contract { callsInPlace(body, InvocationKind.EXACTLY_ONCE) }
        if (throwableType.sub() == builder.context.irBuiltIns.throwableType && throwableType != builder.context.irBuiltIns.throwableType) error(
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

    /**
     * Adds a `catch` block to the `try` expression that catches all exceptions.
     * The body of the `catch` block is defined by the provided lambda.
     *
     * @param expression A lambda that defines the body of the `catch` block.
     * @return The current `IrTryBuilder` instance for chaining.
     */
    @ExperimentalContracts
    @UnsafeDuringIrConstructionAPI
    context(builder: IrBuilderWithScope, _: IrPluginContext)
    fun catch(expression: IrTryBuilder.() -> Unit = {}): IrTryBuilder {
        irCatch(builder.context.irBuiltIns.throwableType) {
            builder.irBlock {
                expression()
            }
        }
        return this
    }

    /**
     * Sets the `finally` block for the `try` expression.
     *
     * @param expression The expression to be executed in the `finally` block.
     * @return The current `IrTryBuilder` instance for chaining.
     * @throws IllegalStateException if a `finally` block has already been set.
     */
    @ExperimentalContracts
    @UnsafeDuringIrConstructionAPI
    fun finally(expression: IrExpression): IrTryBuilder {
        finallyExpression = expression
        return this
    }

    /**
     * Builds the `IrTry` expression using the configured `try`, `catch`, and `finally` blocks.
     *
     * @return The constructed `IrTry` expression.
     */
    context(builder: IrBuilderWithScope, _: IrPluginContext)
    fun build(): IrTry =
        builder.irTry(type, result, catches, finallyExpression)

    companion object {
        /**
         * Creates a new `IrTryBuilder` instance with the specified result expression and type.
         *
         * @param result The main expression to be executed within the `try` block. Defaults to an empty block.
         * @param type The type of the `try` expression, defaulting to the type of the `result`.
         * @return A new `IrTryBuilder` instance.
         */
        context(builder: IrBuilderWithScope, _: IrPluginContext)
        fun irTry(
            result: IrExpression = builder.irBlock {},
            type: IrType = result.type,
        ): IrTryBuilder = IrTryBuilder(result, type)
    }
}