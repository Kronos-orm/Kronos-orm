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

import com.kotlinorm.compiler.core.ErrorReporter
import com.kotlinorm.compiler.core.kTableForConditionSymbol
import com.kotlinorm.compiler.core.kTableForInsertSelectSymbol
import com.kotlinorm.compiler.core.kTableForReferenceSymbol
import com.kotlinorm.compiler.core.kTableForSelectSymbol
import com.kotlinorm.compiler.core.kTableForSetSymbol
import com.kotlinorm.compiler.core.kTableForSortSymbol
import com.kotlinorm.compiler.core.firstTypeArgument
import com.kotlinorm.compiler.core.isKPojoType
import com.kotlinorm.compiler.utils.GeneratedProjectionPackageFqName
import com.kotlinorm.compiler.utils.extensionReceiver
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.ir.util.statements

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
 */
class KronosParserTransformer(
    private val pluginContext: IrPluginContext,
    messageCollector: MessageCollector
) : IrElementTransformerVoidWithContext() {

    private val errorReporter = ErrorReporter(messageCollector)
    val kPojoClasses = mutableSetOf<IrClass>()
    val enumClasses = mutableSetOf<IrClass>()
    private val transformedKPojoClasses = mutableSetOf<IrClass>()

    override fun visitFileNew(declaration: IrFile): IrFile {
        errorReporter.currentFileEntry = declaration.fileEntry
        return super.visitFileNew(declaration)
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun visitClassNew(declaration: IrClass): IrStatement =
        if (declaration.isGeneratedProjectionClass()) declaration else {
            if (with(pluginContext) { declaration.defaultType.isKPojoType() }) {
                processKPojoClass(declaration)
            }
            super.visitClassNew(declaration)
        }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun visitCall(expression: IrCall): IrExpression {
        for (i in 0 until expression.typeArguments.size) {
            collectKPojoFactoryCandidate(expression.typeArguments[i])
        }
        if (expression.isStaticTypedResultCall()) {
            expression.type.collectConcreteEnumLeaves()
            for (i in 0 until expression.typeArguments.size) {
                expression.typeArguments[i]?.collectConcreteEnumLeaves()
            }
        }
        return super.visitCall(expression) as IrCall
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun visitClassReference(expression: IrClassReference): IrExpression {
        collectKPojoFactoryCandidate(expression.classType)
        return super.visitClassReference(expression)
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
        collectKPojoFactoryCandidate(expression.type)
        return super.visitConstructorCall(expression)
    }

    /**
     * Collects and enhances each source KPojo class once.
     */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun processKPojoClass(irClass: IrClass) {
        if (irClass.kind == ClassKind.INTERFACE || irClass.isGeneratedProjectionClass()) {
            return
        }
        kPojoClasses.add(irClass)
        collectCascadeTargetKPojoClasses(irClass)
        if (transformedKPojoClasses.add(irClass)) {
            irClass.properties.forEach { property ->
                property.isVar = true
                property.isConst = false
            }
            irClass.transform(KronosIrClassTransformer(pluginContext, irClass, errorReporter), null)
        }
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun collectCascadeTargetKPojoClasses(irClass: IrClass) {
        irClass.properties.forEach { property ->
            val propertyType = property.getter?.returnType ?: property.backingField?.type ?: return@forEach
            val targets = sequenceOf(
                propertyType.toKPojoFactoryCandidate(),
                propertyType.firstTypeArgument()?.toKPojoFactoryCandidate()
            )
            targets
                .filterNotNull()
                .forEach { target -> kPojoClasses.add(target) }
        }
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun collectKPojoFactoryCandidate(type: IrType?) {
        type?.toKPojoFactoryCandidate()?.let { kPojoClasses.add(it) }
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrCall.isStaticTypedResultCall(): Boolean {
        if (symbol.owner.name.asString() !in StaticTypedResultFunctionNames) return false
        return symbol.owner.fqNameWhenAvailable?.asString()?.startsWith("com.kotlinorm.") == true
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrType.collectConcreteEnumLeaves() {
        val simpleType = this as? IrSimpleType ?: return
        simpleType.classOrNull?.owner
            ?.takeIf { it.kind == ClassKind.ENUM_CLASS }
            ?.let(enumClasses::add)
        simpleType.arguments
            .filterIsInstance<IrTypeProjection>()
            .forEach { it.type.collectConcreteEnumLeaves() }
    }

    /**
     * Synthetic projection classes are FIR-backed lazy classes. Expanding their members while
     * visiting a call type argument can ask FIR2IR for property symbols before they are bound.
     */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrClass.isGeneratedProjectionClass(): Boolean =
        kotlinFqName.parent() == GeneratedProjectionPackageFqName

    private fun IrType.isGeneratedProjectionClassType(): Boolean =
        classFqName?.parent() == GeneratedProjectionPackageFqName

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrType.toKPojoFactoryCandidate(): IrClass? {
        if (isGeneratedProjectionClassType()) return null
        val irClass = getClass() ?: classOrNull?.owner ?: return null
        if (irClass.kind == ClassKind.INTERFACE || irClass.isGeneratedProjectionClass()) return null
        return irClass.takeIf { with(pluginContext) { it.defaultType.isKPojoType() } }
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun visitFunctionNew(declaration: IrFunction): IrStatement {
        with(pluginContext) {
            val extensionReceiverFqName = declaration.parameters.extensionReceiver?.type?.classFqName?.asString()
            when (extensionReceiverFqName) {
                kTableForSelectSymbol.owner.kotlinFqName.asString() ->
                    declaration.body = transformWith(declaration) { SelectTransformer(it, errorReporter) }
                kTableForInsertSelectSymbol.owner.kotlinFqName.asString() ->
                    declaration.body = transformWith(declaration) { InsertSelectTransformer(pluginContext, it, errorReporter) }
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

private val StaticTypedResultFunctionNames = setOf(
    "queryList",
    "queryOne",
    "queryOneOrNull",
    "toList",
    "first",
    "firstOrNull"
)
