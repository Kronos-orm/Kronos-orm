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

import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.typeOrFail
import org.jetbrains.kotlin.ir.types.withNullability
import org.jetbrains.kotlin.ir.util.defaultType

/**
 * Casts the given IrType to an IrSimpleType.
 *
 * @return The IrSimpleType representation of the IrType.
 */
internal fun IrType.asSimpleType() = this as IrSimpleType

/**
 * Returns the first type argument of the given IrType as an IrSimpleType.
 * 返回给定IrType的第一个类型参数
 *
 * @return The first type argument of the given IrType as an IrSimpleType.
 */
internal fun IrType.subType() = asSimpleType().arguments.firstOrNull()?.typeOrFail?.asSimpleType()

@OptIn(UnsafeDuringIrConstructionAPI::class)
internal val IrClassSymbol.nType get() = owner.defaultType.withNullability(true)