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
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.name.FqName

/**
 * Fills a common-source Native registration skeleton with compiler-owned type
 * descriptors. The skeleton is source-visible to downstream KMP modules, so
 * an application can call it explicitly and retain the generated graph at link
 * time without ServiceLoader or resource discovery.
 */
internal object NativeGeneratedTypeMetadataEntryGenerator {
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    fun generate(
        pluginContext: IrPluginContext,
        moduleFragment: IrModuleFragment,
        entryFqName: FqName,
        providerId: String,
        dependencyEntries: List<FqName> = emptyList(),
        kPojoClasses: Collection<IrClass>,
        enumClasses: Collection<IrClass>,
        directQueryTaskTypeRoots: Collection<IrType>,
        errorReporter: ErrorReporter
    ) {
        val roots = buildList {
            addAll(directQueryTaskTypeRoots)
            addAll(kPojoClasses.map { it.symbol.defaultType })
            addAll(enumClasses.map { it.symbol.defaultType })
            kPojoClasses.forEach { kPojo ->
                kPojo.properties.mapNotNullTo(this) { property ->
                    property.getter?.returnType ?: property.backingField?.type
                }
            }
        }
        val entry = moduleFragment.files
            .asSequence()
            .flatMap { it.declarations.asSequence() }
            .filterIsInstance<IrFunction>()
            .firstOrNull { it.fqNameWhenAvailable == entryFqName }
        if (entry == null) {
            check(roots.isEmpty()) {
                "Native generated metadata entry '$entryFqName' was not found. " +
                    "The Gradle plugin must add its common-source skeleton to the compilation."
            }
            return
        }

        val providerName = FqName(entryFqName.asString() + "Provider")
        val provider = moduleFragment.files.asSequence().flatMap { it.declarations.asSequence() }
            .filterIsInstance<IrClass>().singleOrNull { it.fqNameWhenAvailable == providerName }
            ?: error("Native source provider '$providerName' was not found; regenerate its Gradle source entry.")
        GeneratedTypeProviderGenerator.fillNativeProvider(
            pluginContext, moduleFragment, provider, providerId, kPojoClasses, enumClasses, directQueryTaskTypeRoots, errorReporter
        )
        addTargetDependencyEntries(pluginContext, moduleFragment, entryFqName, dependencyEntries)
        bindApplicationInitialization(pluginContext, moduleFragment, entryFqName)
    }

    /** Target-only dependencies cannot be referenced from common metadata source. Link them in target IR. */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun addTargetDependencyEntries(
        context: IrPluginContext,
        module: IrModuleFragment,
        entryFqName: FqName,
        dependencyEntries: List<FqName>
    ) {
        if (dependencyEntries.isEmpty()) return
        val aggregateName = FqName(entryFqName.asString() + "Dependencies")
        val aggregate = module.files.asSequence().flatMap { it.declarations.asSequence() }
            .filterIsInstance<IrFunction>().singleOrNull { it.fqNameWhenAvailable == aggregateName }
            ?: error("Missing generated Native dependency entry '$aggregateName'")
        val body = aggregate.body as? IrBlockBody ?: error("Native aggregate '$aggregateName' has no source body")
        val existing = body.statements.filterIsInstance<IrCall>().mapNotNull { it.symbol.owner.fqNameWhenAvailable }.toSet()
        val builder = DeclarationIrBuilder(context, aggregate.symbol)
        val calls = dependencyEntries.distinct().filter { it != aggregateName && it !in existing }.map { entry ->
            val symbol = module.files.asSequence().flatMap { it.declarations.asSequence() }
                .filterIsInstance<IrFunction>().singleOrNull { it.fqNameWhenAvailable == entry }?.symbol
                ?: context.finderForBuiltins().findFunctions(CallableId(entry.parent(), entry.shortName()))
                    .singleOrNull { it.owner.parameters.size == 1 }
                ?: error("Native dependency metadata entry '$entry' was not found in this target compilation")
            builder.irCall(symbol).apply { arguments[0] = builder.irGet(aggregate.parameters.single()) }
        }
        body.statements.addAll(0, calls)
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun bindApplicationInitialization(
        context: IrPluginContext,
        module: IrModuleFragment,
        entryFqName: FqName
    ) {
        val aggregateName = FqName(entryFqName.asString() + "Dependencies")
        val aggregate = module.files.asSequence().flatMap { it.declarations.asSequence() }
            .filterIsInstance<IrFunction>().firstOrNull { it.fqNameWhenAvailable == aggregateName }
        module.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                val call = super.visitCall(expression) as IrCall
                if (call.symbol.owner.fqNameWhenAvailable != InitializeMetadataFqName) return call
                val target = checkNotNull(aggregate) {
                    "Missing generated Native dependency entry '$aggregateName'; apply the Kronos Gradle plugin."
                }
                return DeclarationIrBuilder(context, target.symbol, call.startOffset, call.endOffset)
                    .irCall(target.symbol)
            }
        })
    }

}

private val InitializeMetadataFqName = FqName("com.kotlinorm.utils.initializeKronosMetadata")
