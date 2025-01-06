/**
 * Copyright 2022-2024 kronos-orm
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

package com.kotlinorm.compiler.plugin.transformer

import com.kotlinorm.compiler.plugin.utils.KClassCreatorUtil.buildKClassMapper
import com.kotlinorm.compiler.plugin.utils.context.withBuilder
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.name.FqName

/**
 * Kronos KClassMapper Transformer
 *
 * @author OUSC
 */
class KronosKClassMapperTransformer(
    private val pluginContext: IrPluginContext
) : IrElementTransformerVoidWithContext() {
    private var initAnnotationFqName = FqName("com.kotlinorm.annotations.KronosInit")

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun visitCall(expression: IrCall): IrExpression {
        with(pluginContext){
            if (expression.symbol.owner.hasAnnotation(initAnnotationFqName)) {
                val initializer = (expression.getValueArgument(0) as IrFunctionExpressionImpl).function
                with(DeclarationIrBuilder(pluginContext, initializer.symbol) as IrBuilderWithScope) {
                    withBuilder(pluginContext){
                        buildKClassMapper(initializer)
                    }
                }
            }
            return super.visitCall(expression)
        }
    }
}