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

package com.kotlinorm.compiler.fir.transformer

import com.kotlinorm.compiler.fir.utils.KClassCreatorUtil.buildKClassMapper
import com.kotlinorm.compiler.fir.utils.KronosSymbol
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable

/**
 * Kronos KClassMapper Transformer
 *
 * @author OUSC
 */
class KronosKClassMapperTransformer(
    private val pluginContext: IrPluginContext
) : IrElementTransformerVoidWithContext() {
    private var transformed = false

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun visitFunctionNew(declaration: IrFunction): IrStatement {
        if (!transformed) {
            with(pluginContext) {
                if (declaration.extensionReceiverParameter != null) {
                    if (
                        declaration.extensionReceiverParameter!!.type.classFqName == KronosSymbol.owner.fqNameWhenAvailable
                    ) {
                        transformed = true
                        with(DeclarationIrBuilder(pluginContext, declaration.symbol)) {
                            buildKClassMapper(declaration)
                        }
                    }
                }
            }
        }
        return super.visitFunctionNew(declaration)
    }
}