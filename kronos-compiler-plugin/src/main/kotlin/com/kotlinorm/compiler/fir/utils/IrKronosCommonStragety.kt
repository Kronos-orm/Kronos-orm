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

package com.kotlinorm.compiler.fir.utils

import com.kotlinorm.compiler.helpers.referenceClass
import com.kotlinorm.compiler.helpers.applyIrCall
import com.kotlinorm.compiler.helpers.asIrCall
import com.kotlinorm.compiler.helpers.dispatchBy
import com.kotlinorm.compiler.helpers.findByFqName
import com.kotlinorm.compiler.helpers.subType
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irBoolean
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.getPropertyGetter
import org.jetbrains.kotlin.ir.util.getValueArgument
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

context(IrBuilderWithScope, IrPluginContext)
@OptIn(UnsafeDuringIrConstructionAPI::class)
internal val updateTimeStrategySymbol
    get() =
        KronosSymbol.getPropertyGetter("updateTimeStrategy")!!

context(IrBuilderWithScope, IrPluginContext)
@OptIn(UnsafeDuringIrConstructionAPI::class)
internal val createTimeStrategySymbol
    get() =
        KronosSymbol.getPropertyGetter("createTimeStrategy")!!

context(IrBuilderWithScope, IrPluginContext)
@OptIn(UnsafeDuringIrConstructionAPI::class)
internal val logicDeleteStrategySymbol
    get() =
        KronosSymbol.getPropertyGetter("logicDeleteStrategy")!!

context(IrBuilderWithScope, IrPluginContext)
@OptIn(UnsafeDuringIrConstructionAPI::class)
internal val optimisticLockStrategySymbol
    get() =
        KronosSymbol.getPropertyGetter("optimisticLockStrategy")!!

context(IrPluginContext)
@OptIn(UnsafeDuringIrConstructionAPI::class)
internal val commonStrategySymbol
    get() = referenceClass("com.kotlinorm.beans.config.KronosCommonStrategy")!!.constructors.first()

val UpdateTimeFqName = FqName("com.kotlinorm.annotations.UpdateTime")

val LogicDeleteFqName = FqName("com.kotlinorm.annotations.LogicDelete")

val CreateTimeFqName = FqName("com.kotlinorm.annotations.CreateTime")

val OptimisticLockFqName = FqName("com.kotlinorm.annotations.Version")

/**
 * Returns the `num`-th subtype of the class represented by the given `IrCall`.
 *
 * @param depth the number of subtypes to traverse (default: 1)
 * @return the `IrClass` representing the `num`-th subtype of the class represented by the `IrCall`
 * @throws IllegalArgumentException if `num` is negative
 */
internal fun IrCall.subTypeClass(depth: Int = 1): IrClass {
    var type = this.type
    for (i in 1..depth) {
        type = type.subType()!!
    }
    return type.getClass()!!
}

/**
 * Retrieves a valid strategy for the given IrClass, global symbol, and FqName.
 *
 * @param irClass The IrClass for which the strategy is being retrieved.
 * @param globalSymbol The global symbol representing the strategy.
 * @param fqName The fully qualified name of the strategy.
 * @return The valid strategy as an IrExpression, or null if no valid strategy is found.
 */
context(IrBuilderWithScope, IrPluginContext)
@OptIn(UnsafeDuringIrConstructionAPI::class)
internal fun getValidStrategy(irClass: IrClass, globalSymbol: IrFunctionSymbol, fqName: FqName): IrExpression {
    var strategy = applyIrCall(globalSymbol){ dispatchBy(irGetObject(KronosSymbol)) }
    val tableSetting = irClass.annotations.findByFqName(fqName)?.asIrCall()?.getValueArgument(1)
    if (tableSetting == null || (tableSetting is IrConst<*> && tableSetting.value == true)) {
        var annotation: IrConstructorCall?
        var enabled: IrConst<*>?
        val column = irClass.properties.find {
            annotation = it.annotations.findByFqName(fqName)
            enabled = annotation?.getValueArgument(Name.identifier("enabled")) as IrConst<*>?
            annotation != null && enabled?.value != false
        }
        if (column != null) {
            strategy = applyIrCall(commonStrategySymbol, irBoolean(true), getColumnName(column))
        }
    }
    return strategy
}