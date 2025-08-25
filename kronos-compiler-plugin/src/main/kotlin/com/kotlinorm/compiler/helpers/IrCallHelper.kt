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

import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.name.FqName

typealias IrTypes = List<IrType>

/**
 * Creates an IR function access expression using the given function symbol and arguments.
 *
 * @param args The arguments to pass to the function call.
 * @param types The types of the arguments.
 * @param operator An optional origin for the function call, such as an operator like `invoke`.
 * @return An IR function access expression representing the function call.
 */
context(builder: IrBuilderWithScope)
internal operator fun IrFunctionSymbol.invoke(
    vararg args: IrExpression?,
    types: IrTypes = emptyList(),
    operator: IrStatementOrigin? = null,
): IrFunctionAccessExpression {
    return builder.irCall(this).apply {
        origin = operator
        args.forEachIndexed { i, arg -> arguments[i] = arg }
        types.forEachIndexed { i, type -> typeArguments[i] = type }
    }
}

/**
 * Casts the current IrExpression to the specified type T.
 *
 * @param T The target type to cast to, which must be a subtype of IrExpression.
 * @return The current IrExpression cast to type T.
 */
internal inline fun <reified T : IrExpression> IrExpression.irCast(): T {
    return try {
        this as T
    } catch (e: ClassCastException) {
        throw IllegalArgumentException("Expected IrExpression to be of type ${T::class.java}, but was ${this::class.java}", e)
    }
}

/**
 * Finds the first IrConstructorCall in the iterable that has a containing descriptor with the given fqName.
 *
 * @param fqName The fully qualified name of the containing descriptor to search for.
 * @return The first IrConstructorCall that matches the given fqName, or null if none is found.
 */
internal fun <T : IrFunctionAccessExpression> Iterable<T>.findByFqName(fqName: FqName): T? =
    find { it.type.classFqName == fqName }

/**
 * Finds the first IrConstructorCall in the iterable that has a containing descriptor with the given fqName.
 *
 * @param fqName The fully qualified name of the containing descriptor to search for.
 * @return The first IrConstructorCall that matches the given fqName, or null if none is found.
 */
internal fun <T : IrFunctionAccessExpression> Iterable<T>.filterByFqName(fqName: FqName): List<T> =
    filter { it.type.classFqName == fqName }