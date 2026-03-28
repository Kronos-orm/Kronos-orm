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

package com.kotlinorm.compiler.plugin.utils

import com.kotlinorm.compiler.helpers.findByFqName
import com.kotlinorm.compiler.helpers.invoke
import com.kotlinorm.compiler.helpers.irCast
import com.kotlinorm.compiler.helpers.referenceClass
import com.kotlinorm.compiler.helpers.valueArguments
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.builders.IrBlockBuilder
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irBoolean
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.getPropertyGetter
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.name.FqName

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(_: IrPluginContext)
internal val updateTimeStrategySymbol
    get() =
        KronosSymbol.getPropertyGetter("updateTimeStrategy")!!

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(_: IrPluginContext)
internal val createTimeStrategySymbol
    get() =
        KronosSymbol.getPropertyGetter("createTimeStrategy")!!

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(_: IrPluginContext)
internal val logicDeleteStrategySymbol
    get() =
        KronosSymbol.getPropertyGetter("logicDeleteStrategy")!!

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(_: IrPluginContext)
internal val optimisticLockStrategySymbol
    get() =
        KronosSymbol.getPropertyGetter("optimisticLockStrategy")!!

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(_: IrPluginContext)
internal val commonStrategySymbol
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
context(_: IrPluginContext, builder: IrBuilderWithScope)
internal fun getValidStrategy(irClass: IrClass, globalSymbol: IrFunctionSymbol, fqName: FqName): IrExpression {
    var strategy = globalSymbol(builder.irGetObject(KronosSymbol))
    val tableAnno = irClass.annotations.findByFqName(fqName)?.irCast<IrConstructorCall>()
    fun isAnnoDisabled() =
        // 找到为IrBoolean且为false
        tableAnno?.valueArguments?.any { arg -> arg is IrConst && arg.value == false } == true
    if (isAnnoDisabled()) {
        strategy = commonStrategySymbol(
            builder.irBoolean(false),
            fieldSymbol.constructors.first()(
                builder.irString("")
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
                enabled = anno.valueArguments[0] as IrConst?
                strategy = when (enabled?.value) {
                    true, null -> commonStrategySymbol(builder.irBoolean(true), getColumnName(col))
                    else -> commonStrategySymbol(builder.irBoolean(false), getColumnName(col))
                }
            }
        }
    }
    return strategy
}