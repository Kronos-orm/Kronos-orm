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

package com.kotlinorm.plugins.transformer

import com.kotlinorm.plugins.utils.*
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction

/**
 * Kronos Parser Transformer
 *
 * @author OUSC, Jieyao Lu
 */
class KronosIrClassNewTransformer(
    private val pluginContext: IrPluginContext, private val irClass: IrClass
) : IrElementTransformerVoidWithContext() {
    override fun visitFunctionNew(declaration: IrFunction): IrStatement {
        if (declaration is IrSimpleFunction && declaration.isFakeOverride) {
            declaration.isFakeOverride = false
            declaration.dispatchReceiverParameter = irClass.thisReceiver
            with(pluginContext) {
                with(DeclarationIrBuilder(pluginContext, declaration.symbol)) {
                    when (declaration.name.asString()) {
                        "toDataMap" -> declaration.body = createToMapFunction(irClass, declaration)
                        "safeFromMapData" -> declaration.body = createSafeFromMapValueFunction(irClass, declaration)
                        "fromMapData" -> declaration.body = createFromMapValueFunction(irClass, declaration)
                        "kronosTableName" -> declaration.body = createKronosTableName(irClass)
                        "kronosColumns" -> declaration.body = createGetFieldsFunction(irClass)
                        "kronosCreateTime" -> declaration.body = createKronosCreateTime(irClass)
                        "kronosUpdateTime" -> declaration.body = createKronosUpdateTime(irClass)
                        "kronosLogicDelete" -> declaration.body = createKronosLogicDelete(irClass)
                    }
                }
            }
        }
        return super.visitFunctionNew(declaration)
    }
}