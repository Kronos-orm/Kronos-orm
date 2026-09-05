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

package com.kotlinorm.compiler.utils

import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.name.FqName

/**
 * Annotation utility functions for Kronos compiler plugin
 *
 * Provides helper functions for reading and processing annotations on IR elements
 */

/**
 * Extension property to get value arguments from IrFunctionAccessExpression
 * Uses the arguments property which is the stable K2 API
 * 
 * This follows the same pattern as IrMemberAccessExpressionHelper from the old plugin
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
private val IrFunctionAccessExpression.valueArgs: List<IrExpression?>
    get() = arguments.filterIndexed { index, _ ->
        symbol.owner.parameters[index].kind == IrParameterKind.Regular
    }

/**
 * Finds an annotation by its fully qualified name
 *
 * @param fqName The fully qualified name of the annotation to find
 * @return The IrConstructorCall representing the annotation, or null if not found
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
fun IrAnnotationContainer.findAnnotation(fqName: FqName): IrConstructorCall? {
    return annotations.find { 
        it.symbol.owner.returnType.getClass()?.fqNameWhenAvailable == fqName 
    }
}

/**
 * Gets the value argument from an annotation at the specified index
 *
 * @param annotation The annotation to read from
 * @param index The parameter index (default: 0)
 * @return The IrExpression representing the value, or null if not found
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
fun getAnnotationValueArgument(annotation: IrConstructorCall?, index: Int = 0): IrExpression? {
    if (annotation == null) return null
    return annotation.valueArgs.getOrNull(index)
}

/**
 * Gets the value of a string parameter from an annotation
 *
 * @param annotation The annotation to read from
 * @param index The parameter index (default: 0)
 * @return The string value, or null if not found or not a string
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
fun getAnnotationStringValue(annotation: IrConstructorCall?, index: Int = 0): String? {
    val arg = getAnnotationValueArgument(annotation, index) ?: return null
    return when (arg) {
        is IrConstImpl -> arg.value as? String
        else -> null
    }
}
