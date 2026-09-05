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

package com.kotlinorm.compiler.core

import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrReturn

/**
 * Base class for KTable* transformers
 *
 * Provides unified traversal strategy and error handling for all KTable* DSL transformers.
 * This base class ensures consistent behavior across different transformer implementations.
 *
 * @property irFunction The IR function being transformed (the extension function on KTable*)
 * @property errorReporter The error reporter for reporting compilation errors
 */
abstract class KTableTransformer(
    protected val irFunction: IrFunction,
    protected val errorReporter: ErrorReporter
) : IrElementTransformerVoidWithContext() {

    /**
     * Determines whether a return expression should be processed by this transformer
     *
     * This method ensures that only return statements from the current function are processed,
     * avoiding the processing of return statements from nested lambdas or inner functions.
     *
     * The check is based on comparing the return target symbol with the current function's symbol.
     * If they match, the return is from the current function and should be processed.
     *
     * @param expression The return expression to check
     * @return true if this return expression should be processed, false otherwise
     */
    protected fun shouldProcessReturn(expression: IrReturn): Boolean {
        return expression.returnTargetSymbol == irFunction.symbol
    }

    /**
     * Reports a compilation error
     *
     * This is a convenience method that delegates to the error reporter.
     * It provides a simpler API for transformer implementations to report errors.
     *
     * @param element The IR element where the error occurred
     * @param message The error message
     * @param suggestion Optional suggestion for fixing the error
     */
    protected fun reportError(
        element: IrElement,
        message: String,
        suggestion: String? = null
    ) {
        errorReporter.reportError(element, message, suggestion)
    }
}
