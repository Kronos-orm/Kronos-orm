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
import com.kotlinorm.compiler.core.buildFieldFromProperty
import com.kotlinorm.compiler.core.isColumnType
import com.kotlinorm.compiler.utils.AnnotationFqNames
import com.kotlinorm.compiler.utils.KPojoFqName
import com.kotlinorm.compiler.utils.set
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.IrBlockBuilder
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.addBackingField
import org.jetbrains.kotlin.ir.builders.declarations.addDefaultGetter
import org.jetbrains.kotlin.ir.builders.declarations.addDefaultSetter
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irBoolean
import org.jetbrains.kotlin.ir.builders.irBranch
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irElseBranch
import org.jetbrains.kotlin.ir.builders.irEquals
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.irWhen
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.superTypes
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.getSimpleFunction
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.name.FqName

/**
 * Kronos IR Class Transformer
 *
 * Transforms classes that implement the `KPojo` interface by replacing their
 * fake-override functions with real, compiler-generated implementations.
 *
 * The following methods/properties are generated for each KPojo class:
 * - `kronosColumns()` — returns the list of [Field] descriptors for all column properties.
 * - `__tableName` — property holding the table name (from `@Table` annotation or class name).
 * - `__tableComment` — property holding the table comment.
 * - `kronosTableIndex()` — returns the list of table indexes.
 * - `kronosCreateTime()` / `kronosUpdateTime()` / `kronosLogicDelete()` / `kronosOptimisticLock()`
 *   — returns the special-purpose field annotated with the corresponding annotation, or null.
 * - `kClass()` — returns the KClass reference of the class.
 * - `toDataMap()` — serializes the instance to a `MutableMap<String, Any?>`.
 * - `get(name)` / `set(name, value)` — dynamic property access by column name.
 * - `fromMapData()` / `safeFromMapData()` — populates the instance from a map.
 */
class KronosIrClassTransformer(
    private val pluginContext: IrPluginContext,
    private val irClass: IrClass,
    @Suppress("unused") private val errorReporter: ErrorReporter
) : IrElementTransformerVoidWithContext() {

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun visitFunctionNew(declaration: IrFunction): IrStatement {
        if (declaration !is IrSimpleFunction || !declaration.isFakeOverride) {
            return super.visitFunctionNew(declaration)
        }

        // Make all properties mutable (needed for set/fromMapData)
        irClass.properties.forEach {
            it.isVar = true
            it.isConst = false
        }

        fun replaceFakeBody(bodyFactory: DeclarationIrBuilder.() -> IrBlockBody) {
            declaration.isFakeOverride = false
            declaration[IrParameterKind.DispatchReceiver] = irClass.thisReceiver
            declaration.body = DeclarationIrBuilder(pluginContext, declaration.symbol).bodyFactory()
        }

        with(pluginContext) {
            when (declaration.name.asString()) {
                "kronosColumns" -> replaceFakeBody { createKronosColumns(irClass) }
                "kronosTableIndex" -> replaceFakeBody { createKronosTableIndex(irClass) }
                "kronosCreateTime" -> replaceFakeBody { createKronosSpecialField(irClass, AnnotationFqNames.CreateTime) }
                "kronosUpdateTime" -> replaceFakeBody { createKronosSpecialField(irClass, AnnotationFqNames.UpdateTime) }
                "kronosLogicDelete" -> replaceFakeBody { createKronosSpecialField(irClass, AnnotationFqNames.LogicDelete) }
                "kronosOptimisticLock" -> replaceFakeBody { createKronosSpecialField(irClass, AnnotationFqNames.Version) }
                "kClass" -> replaceFakeBody { createKClassFunction(irClass) }
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
        if (declaration.backingField == null) {
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
                        "__tableName" -> replaceFakeProp { createTableName(irClass) }
                        "__tableComment" -> replaceFakeProp { createTableComment(irClass) }
                    }
                }
            }
        }
        return super.visitPropertyNew(declaration)
    }
}
