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
import com.kotlinorm.compiler.utils.AnnotationFqNames
import com.kotlinorm.compiler.utils.set
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.declarations.addBackingField
import org.jetbrains.kotlin.ir.builders.declarations.addDefaultGetter
import org.jetbrains.kotlin.ir.builders.declarations.addDefaultSetter
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.parentAsClass

/**
 * Kronos IR Class Transformer
 *
 * Transforms classes that implement the `KPojo` interface by replacing their
 * fake-override functions and properties with real, compiler-generated implementations.
 *
 * The following methods/properties are generated for each KPojo class:
 * - `__kType` — property holding the concrete KType for static KPojo classes.
 * - `__columns` — property holding the list of [Field] descriptors for all column properties.
 * - `__tableName` — property holding the table name (from `@Table` annotation or class name).
 * - `__tableComment` — property holding the table comment.
 * - `__tableIndexes` — property holding the list of table indexes.
 * - `__createTime` / `__updateTime` / `__logicDelete` / `__optimisticLock`
 *   — properties holding the special-purpose strategies annotated on the class.
 * - `toDataMap()` — serializes the instance to a `MutableMap<String, Any?>`.
 * - `get(name)` / `set(name, value)` — dynamic property access by column name.
 * - `fromMapData()` / `safeFromMapData()` — populates the instance from a map.
 */
class KronosIrClassTransformer(
    private val pluginContext: IrPluginContext,
    private val irClass: IrClass,
    @Suppress("unused") private val errorReporter: ErrorReporter,
    private val metadataClass: IrClass = irClass
) : IrElementTransformerVoidWithContext() {
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun visitFunctionNew(declaration: IrFunction): IrStatement {
        if (declaration !is IrSimpleFunction || !declaration.isFakeOverride) {
            return super.visitFunctionNew(declaration)
        }

        fun replaceFakeBody(bodyFactory: DeclarationIrBuilder.() -> IrBlockBody) {
            declaration.isFakeOverride = false
            declaration[IrParameterKind.DispatchReceiver] = irClass.thisReceiver
            declaration.body = DeclarationIrBuilder(pluginContext, declaration.symbol).bodyFactory()
        }

        with(pluginContext) {
            when (declaration.name.asString()) {
                "toDataMap" -> replaceFakeBody { createToDataMap(irClass, declaration) }
                "get" -> replaceFakeBody { createPropertyGetter(irClass, declaration) }
                "set" -> replaceFakeBody { createPropertySetter(irClass, declaration) }
                "fromMapData" -> replaceFakeBody { createFromMapData(irClass, declaration) }
                "safeFromMapData" -> replaceFakeBody { createSafeFromMapData(irClass, declaration) }
            }
        }

        return super.visitFunctionNew(declaration)
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun visitPropertyNew(declaration: IrProperty): IrStatement {
        if (declaration.isFakeOverride && declaration.backingField == null) {
            fun replaceFakeProp(initializer: () -> IrExpressionBody) {
                declaration.isFakeOverride = false
                declaration.addBackingField { type = declaration.getter!!.returnType }
                declaration.backingField!!.initializer = initializer()
                declaration.getter = null
                declaration.setter = null
                declaration.addDefaultGetter(declaration.parentAsClass, pluginContext.irBuiltIns)
                declaration.addDefaultSetter(declaration.parentAsClass, pluginContext.irBuiltIns)
            }
            with(DeclarationIrBuilder(pluginContext, declaration.symbol)) {
                with(pluginContext) {
                    when (declaration.name.asString()) {
                        "__kType" -> replaceFakeProp { createKTypeProperty(irClass) }
                        "__columns" -> replaceFakeProp { createColumns(irClass, metadataClass) }
                        "__tableName" -> replaceFakeProp { createTableName(metadataClass) }
                        "__tableComment" -> replaceFakeProp { createTableComment(metadataClass) }
                        "__tableIndexes" -> replaceFakeProp { createTableIndexes(irClass) }
                        "__createTime" -> replaceFakeProp { createKronosSpecialField(irClass, AnnotationFqNames.CreateTime) }
                        "__updateTime" -> replaceFakeProp { createKronosSpecialField(irClass, AnnotationFqNames.UpdateTime) }
                        "__logicDelete" -> replaceFakeProp { createKronosSpecialField(irClass, AnnotationFqNames.LogicDelete) }
                        "__optimisticLock" -> replaceFakeProp { createKronosSpecialField(irClass, AnnotationFqNames.Version) }
                    }
                }
            }
        }
        return super.visitPropertyNew(declaration)
    }
}
