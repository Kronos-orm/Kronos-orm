/**
 * Copyright 2022-2026 kronos-orm
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

package com.kotlinorm.compiler.backend.transformers

import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irBoolean
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irImplicitCast
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isBoolean
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.types.makeNotNull
import org.jetbrains.kotlin.ir.util.defaultValueForType

/** Retains the original return target after its value has become DSL side effects. */
internal fun IrBuilderWithScope.neutralDslReturn(expression: IrReturn, returnType: IrType): IrReturn =
    expression.apply {
        value = when {
            returnType.isUnit() -> irGetObject(context.irBuiltIns.unitClass)
            returnType.makeNotNull().isBoolean() -> irBoolean(false)
            // R is unconstrained in the public DSL aliases. Keep its calling convention
            // even when callers explicitly choose a primitive or reference return type.
            // The DSL consumes the collected side effects and discards this value.
            else -> irImplicitCast(
                IrConstImpl.defaultValueForType(startOffset, endOffset, returnType),
                returnType
            )
        }
    }
