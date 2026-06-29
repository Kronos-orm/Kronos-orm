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

package com.kotlinorm.compiler.transformers

import com.kotlinorm.compiler.utils.set
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irBoolean
import org.jetbrains.kotlin.ir.builders.irByte
import org.jetbrains.kotlin.ir.builders.irChar
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irLong
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irShort
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * Replaces generated projection field fake getter calls with backend-safe defaults.
 *
 * The FIR projection path gives source resolution a visible property such as
 * `KronosSelectResult_x.id`, but Fir2Ir may still materialize the getter as a
 * lazy fake override. The JVM backend cannot emit such a getter because it has
 * no overridden declaration, and lazy Fir2Ir declarations cannot be mutated from
 * an IR extension, so the first slice replaces the call expression itself.
 */
class KronosProjectionIrTransformer(
    private val pluginContext: IrPluginContext
) : IrElementTransformerVoidWithContext() {

    /**
     * Replaces projection getter calls before JVM codegen maps their fake signatures.
     */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun visitCall(expression: IrCall): IrExpression {
        val call = super.visitCall(expression) as IrCall
        val function = call.symbol.owner
        buildGeneratedSelectCall(call)?.let { return it }
        if (function.isGeneratedProjectionGetter()) {
            return DeclarationIrBuilder(pluginContext, currentScope!!.scope.scopeOwnerSymbol).defaultValue(call.type)
        }
        return call
    }

    /**
     * Rewrites bare select calls to pass the generated projection KClass at runtime.
     */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun buildGeneratedSelectCall(call: IrCall): IrCall? {
        val function = call.symbol.owner
        if (function.kotlinFqName != SELECT_FQ_NAME) return null

        val selectType = call.type as? IrSimpleType ?: return null
        val sourceType = (selectType.arguments.getOrNull(0) as? IrTypeProjection)?.type ?: return null
        val projectionType = (selectType.arguments.getOrNull(1) as? IrTypeProjection)?.type ?: return null
        val projectionClass = projectionType.classOrNull?.owner ?: return null
        if (!projectionClass.isGeneratedProjectionClass()) return null

        val selectGeneratedSymbol = pluginContext.referenceFunctions(SELECT_GENERATED_CALLABLE_ID)
            .firstOrNull { it.owner.parameters.size == 3 }
            ?: return null

        return DeclarationIrBuilder(pluginContext, currentScope!!.scope.scopeOwnerSymbol).irCall(selectGeneratedSymbol).apply {
            typeArguments[0] = sourceType
            typeArguments[1] = projectionType
            arguments[0] = call.arguments[0]
            arguments[1] = IrClassReferenceImpl(
                call.startOffset,
                call.endOffset,
                pluginContext.irBuiltIns.kClassClass.typeWith(projectionType),
                projectionClass.symbol,
                projectionType
            )
            arguments[2] = call.arguments[1]
        }
    }

    /**
     * Builds a default expression matching the projection field type.
     */
    private fun IrBuilderWithScope.defaultValue(type: IrType): IrExpression {
        if (type.isNullable()) return irNull()
        return when (type.classFqName?.asString()) {
            "kotlin.Boolean" -> irBoolean(false)
            "kotlin.Byte" -> irByte(0)
            "kotlin.Short" -> irShort(0)
            "kotlin.Int" -> irInt(0)
            "kotlin.Long" -> irLong(0L)
            "kotlin.Float" -> IrConstImpl.float(startOffset, endOffset, type, 0f)
            "kotlin.Double" -> IrConstImpl.double(startOffset, endOffset, type, 0.0)
            "kotlin.Char" -> irChar('\u0000')
            "kotlin.String" -> irString("")
            else -> irNull()
        }
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrFunction.isGeneratedProjectionGetter(): Boolean {
        if (!name.asString().startsWith("<get-")) return false
        return parentAsClass.isGeneratedProjectionClass()
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrClass.isGeneratedProjectionClass(): Boolean =
        kotlinFqName.parent() == GENERATED_PROJECTION_PACKAGE

    private companion object {
        val GENERATED_PROJECTION_PACKAGE: FqName = FqName("com.kotlinorm.generated.projection")
        val SELECT_FQ_NAME: FqName = FqName("com.kotlinorm.orm.select.select")
        val SELECT_GENERATED_CALLABLE_ID: CallableId = CallableId(
            FqName("com.kotlinorm.orm.select"),
            null,
            Name.identifier("selectGeneratedProjection")
        )
    }
}
