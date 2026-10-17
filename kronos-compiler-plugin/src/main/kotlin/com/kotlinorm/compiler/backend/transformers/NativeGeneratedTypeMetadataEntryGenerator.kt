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

import com.kotlinorm.compiler.core.buildStaticTypeInfo
import com.kotlinorm.compiler.core.collectStaticTypeMetadataTypes
import com.kotlinorm.compiler.core.registerGeneratedTypeMetadataSymbol
import com.kotlinorm.compiler.core.staticTypeInfoClassSymbol
import com.kotlinorm.compiler.utils.irListOf
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
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
        kPojoClasses: Collection<IrClass>,
        enumClasses: Collection<IrClass>,
        directQueryTaskTypeRoots: Collection<IrType>
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

        val registry = entry.parameters.firstOrNull { it.kind == IrParameterKind.Regular }
            ?: error("Native generated metadata entry '$entryFqName' must accept a registry parameter")
        val builder = DeclarationIrBuilder(pluginContext, entry.symbol)
        val typeInfos = with(pluginContext) { collectStaticTypeMetadataTypes(roots) }
            .mapNotNull { type ->
                with(pluginContext) { with(builder) { buildStaticTypeInfo(type) } }
            }
        val register = with(pluginContext) { registerGeneratedTypeMetadataSymbol }
        entry.body = with(pluginContext) {
            builder.irBlockBody {
                +irCall(register).apply {
                    arguments[0] = irGet(registry)
                    arguments[1] = irString(providerId)
                    arguments[2] = irListOf(staticTypeInfoClassSymbol.defaultType, typeInfos)
                }
            }
        }
    }
}
