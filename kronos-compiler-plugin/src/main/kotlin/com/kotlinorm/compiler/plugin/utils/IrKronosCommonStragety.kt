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

package com.kotlinorm.compiler.plugin.utils

import com.kotlinorm.compiler.helpers.applyIrCall
import com.kotlinorm.compiler.helpers.asIrConstructorCall
import com.kotlinorm.compiler.helpers.dispatchBy
import com.kotlinorm.compiler.helpers.findByFqName
import com.kotlinorm.compiler.helpers.referenceClass
import com.kotlinorm.compiler.helpers.valueArguments
import com.kotlinorm.compiler.plugin.utils.context.KotlinBuilderContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.builders.irBoolean
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.getPropertyGetter
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.name.FqName

@OptIn(UnsafeDuringIrConstructionAPI::class)
internal val IrPluginContext.updateTimeStrategySymbol
    get() =
        KronosSymbol.getPropertyGetter("updateTimeStrategy")!!

@OptIn(UnsafeDuringIrConstructionAPI::class)
internal val IrPluginContext.createTimeStrategySymbol
    get() =
        KronosSymbol.getPropertyGetter("createTimeStrategy")!!

@OptIn(UnsafeDuringIrConstructionAPI::class)
internal val IrPluginContext.logicDeleteStrategySymbol
    get() =
        KronosSymbol.getPropertyGetter("logicDeleteStrategy")!!

@OptIn(UnsafeDuringIrConstructionAPI::class)
internal val IrPluginContext.optimisticLockStrategySymbol
    get() =
        KronosSymbol.getPropertyGetter("optimisticLockStrategy")!!

@OptIn(UnsafeDuringIrConstructionAPI::class)
internal val IrPluginContext.commonStrategySymbol
    get() = referenceClass("com.kotlinorm.beans.config.KronosCommonStrategy")!!.constructors.first()

val UpdateTimeFqName = FqName("com.kotlinorm.annotations.UpdateTime")

val LogicDeleteFqName = FqName("com.kotlinorm.annotations.LogicDelete")

val CreateTimeFqName = FqName("com.kotlinorm.annotations.CreateTime")

val OptimisticLockFqName = FqName("com.kotlinorm.annotations.Version")

/**
 * Retrieves a valid strategy for the given IrClass, global symbol, and FqName.
 *
 * @param irClass The IrClass for which the strategy is being retrieved.
 * @param globalSymbol The global symbol representing the strategy.
 * @param fqName The fully qualified name of the strategy.
 * @return The valid strategy as an IrExpression, or null if no valid strategy is found.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
internal fun KotlinBuilderContext.getValidStrategy(irClass: IrClass, globalSymbol: IrFunctionSymbol, fqName: FqName): IrExpression {
    with(pluginContext){
        with(builder){
            var strategy = applyIrCall(globalSymbol) { dispatchBy(irGetObject(KronosSymbol)) }
            val tableAnno = irClass.annotations.findByFqName(fqName)?.asIrConstructorCall()
            fun isAnnoDisabled() =
               // 找到为IrBoolean且为false
                tableAnno?.valueArguments?.any { arg -> arg is IrConst && arg.value == false } == true
            if (isAnnoDisabled()) {
                strategy = applyIrCall(
                    commonStrategySymbol, irBoolean(false), applyIrCall(
                        fieldSymbol.constructors.first(),
                        irString("")
                    )
                )
            } else {
                var enabled: IrConst?
                val search = irClass.properties.map {
                    it to it.annotations.findByFqName(fqName)
                }.find { it.second != null }
                if (search != null) {
                    val (col, anno) = search
                    if (anno != null) {
                        enabled = anno.getValueArgument(0) as IrConst?
                        strategy = when (enabled?.value) {
                            true, null -> applyIrCall(commonStrategySymbol, irBoolean(true), getColumnName(col))
                            else -> applyIrCall(commonStrategySymbol, irBoolean(false), getColumnName(col))
                        }
                    }
                }
            }
            return strategy
        }
    }
}