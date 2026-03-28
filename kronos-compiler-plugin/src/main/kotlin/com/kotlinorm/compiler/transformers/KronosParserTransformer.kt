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

import com.kotlinorm.compiler.core.ErrorReporter
import com.kotlinorm.compiler.core.kTableForConditionSymbol
import com.kotlinorm.compiler.core.kTableForReferenceSymbol
import com.kotlinorm.compiler.core.kTableForSelectSymbol
import com.kotlinorm.compiler.core.kTableForSetSymbol
import com.kotlinorm.compiler.core.kTableForSortSymbol
import com.kotlinorm.compiler.utils.KPojoFqName
import com.kotlinorm.compiler.utils.extensionReceiver
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.util.superTypes

/**
 * Kronos Parser Transformer
 *
 * The main entry-point IR transformer for the Kronos compiler plugin (K2).
 * It traverses the entire module and performs the following tasks:
 *
 * 1. Collects all classes that implement `KPojo` (from class declarations,
 *    class references, constructor calls, and type arguments).
 * 2. Dispatches each collected KPojo class to [KronosIrClassTransformer] to
 *    generate the required interface method implementations.
 * 3. Identifies DSL extension functions (KTableForSelect, KTableForSet,
 *    KTableForCondition, KTableForSort, KTableForReference) and delegates
 *    to the corresponding transformer ([SelectTransformer], [SetTransformer],
 *    [ConditionTransformer], [SortTransformer], [ReferenceTransformer]).
 * 4. Handles `@KronosInit`-annotated functions via [KClassMapGenerator].
 * 5. Fixes typed query parameters (isKPojo + superTypes injection) via
 *    [TypeParameterFixer].
 */
class KronosParserTransformer(
    private val pluginContext: IrPluginContext,
    messageCollector: MessageCollector
) : IrElementTransformerVoidWithContext() {

    private val errorReporter = ErrorReporter(messageCollector)
    val kPojoClasses = mutableSetOf<IrClass>()

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun visitClassNew(declaration: IrClass): IrStatement {
        if (declaration.superTypes.any { it.classFqName == KPojoFqName }) {
            kPojoClasses.add(declaration)
            // Apply class transformer to generate KPojo interface implementations
            declaration.transform(KronosIrClassTransformer(pluginContext, declaration, errorReporter), null)
        }
        return super.visitClassNew(declaration)
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun visitClassReference(expression: IrClassReference): IrExpression {
        val cls = expression.classType.classOrNull?.owner
        if (cls != null && cls.superTypes.any { it.classFqName == KPojoFqName }) {
            kPojoClasses.add(cls)
        }
        return super.visitClassReference(expression)
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
        val cls = expression.type.classOrNull?.owner
        if (cls != null && cls.superTypes.any { it.classFqName == KPojoFqName }) {
            kPojoClasses.add(cls)
        }
        return super.visitConstructorCall(expression)
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun visitCall(expression: IrCall): IrExpression {
        // Collect KPojo types from type arguments
        for (i in 0 until expression.typeArguments.size) {
            val cls = expression.typeArguments[i]?.getClass()
            if (cls != null && cls.superTypes.any { it.classFqName == KPojoFqName }) {
                kPojoClasses.add(cls)
            }
        }
        // Fix typed query parameters (isKPojo + superTypes injection)
        val result = super.visitCall(expression) as IrCall
        if (TypeParameterFixer.shouldFix(result)) {
            return TypeParameterFixer.fix(pluginContext, result)
        }
        return result
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun visitFunctionNew(declaration: IrFunction): IrStatement {
        with(pluginContext) {
            // Handle @KronosInit functions
            if (KClassMapGenerator.isKronosInitFunction(declaration)) {
                KClassMapGenerator.generate(pluginContext, declaration, kPojoClasses, errorReporter)
                return super.visitFunctionNew(declaration)
            }

            val extensionReceiverFqName = declaration.parameters.extensionReceiver?.type?.classFqName?.asString()
            when (extensionReceiverFqName) {
                kTableForSelectSymbol.owner.kotlinFqName.asString() ->
                    declaration.body = transformWith(declaration) { SelectTransformer(pluginContext, it, errorReporter) }
                kTableForSetSymbol.owner.kotlinFqName.asString() ->
                    declaration.body = transformWith(declaration) { SetTransformer(pluginContext, it, errorReporter) }
                kTableForConditionSymbol.owner.kotlinFqName.asString() ->
                    declaration.body = transformWith(declaration) { ConditionTransformer(pluginContext, it, errorReporter) }
                kTableForSortSymbol.owner.kotlinFqName.asString() ->
                    declaration.body = transformWith(declaration) { SortTransformer(pluginContext, it, errorReporter) }
                kTableForReferenceSymbol.owner.kotlinFqName.asString() ->
                    declaration.body = transformWith(declaration) { ReferenceTransformer(pluginContext, it, errorReporter) }
            }
        }
        return super.visitFunctionNew(declaration)
    }

    private fun transformWith(
        declaration: IrFunction,
        transformerFactory: (IrFunction) -> IrElementTransformerVoidWithContext
    ) = DeclarationIrBuilder(pluginContext, declaration.symbol).irBlockBody {
        +irBlock {
            +declaration.body!!.statements
        }.transform(transformerFactory(declaration), null)
    }
}
