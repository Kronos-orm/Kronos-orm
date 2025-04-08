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

package com.kotlinorm.compiler.plugin.transformer

import com.kotlinorm.compiler.plugin.utils.context.withBuilder
import com.kotlinorm.compiler.plugin.utils.createFromMapValueFunction
import com.kotlinorm.compiler.plugin.utils.createGetFieldsFunction
import com.kotlinorm.compiler.plugin.utils.createKronosComment
import com.kotlinorm.compiler.plugin.utils.createKronosCreateTime
import com.kotlinorm.compiler.plugin.utils.createKronosLogicDelete
import com.kotlinorm.compiler.plugin.utils.createKronosOptimisticLock
import com.kotlinorm.compiler.plugin.utils.createKronosTableIndex
import com.kotlinorm.compiler.plugin.utils.createKronosTableName
import com.kotlinorm.compiler.plugin.utils.createKronosUpdateTime
import com.kotlinorm.compiler.plugin.utils.createPropertyGetter
import com.kotlinorm.compiler.plugin.utils.createPropertySetter
import com.kotlinorm.compiler.plugin.utils.createSafeFromMapValueFunction
import com.kotlinorm.compiler.plugin.utils.createToMapFunction
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.properties

/**
 * Kronos Parser Transformer
 *
 * @author OUSC, Jieyao Lu
 *
 * Roughly speaking, the transform will turn the following:
 *
 *     // file: Foo.kt
 *     class Foo : KPojo {
 *         var username: String {get set}
 *         var password: String {get set}
 *
 *         fake override fun toDataMap(): MutableMap<String, Any?>
 *         fake override fun safeFromMapData(data: Map<String, Any?>): Foo
 *         fake override fun fromMapData(data: Map<String, Any?>): Foo
 *         fake override fun kronosTableName(): String
 *         fake override fun kronosTableComment(): String
 *         fake override fun kronosTableIndex(): List<KTableIndex>
 *         fake override fun kronosColumns(): List<Field>
 *         fake override fun kronosCreateTime(): KronosCommonStrategy
 *         fake override fun kronosUpdateTime(): KronosCommonStrategy
 *         fake override fun kronosLogicDelete(): KronosCommonStrategy
 *         fake override fun kronosOptimisticLock(): KronosCommonStrategy
 *     }
 *
 * into the following equivalent representation:
 *
 *    // file: Foo.kt
 *     class Foo : KPojo {
 *         var username: String {get set}
 *         var password: String {get set}
 *
 *         override fun toDataMap(): MutableMap<String, Any?> {
 *             return mutableMapOf(
 *                 "username" to username,
 *                 "password" to password
 *             )
 *         }
 *
 *         override fun safeFromMapData(data: Map<String, Any?>): Foo {
 *              try this.username = getSafeValue(data, "username") catch (e: Exception) e.printStackTrace()
 *              try this.password = getSafeValue(data, "password") catch (e: Exception) e.printStackTrace()
 *              return this
 *         }
 *
 *         override fun fromMapData(data: Map<String, Any?>): Foo {
 *              this.username = data["username"]
 *              this.password = data["password"]
 *              return this
 *         }
 *
 *         override fun kronosTableName(): String {
 *               return "foo"
 *         }
 *
 *         override fun kronosTableComment(): String {
 *              return "file: Foo.kt"
 *         }
 *
 *         override fun kronosTableIndex(): List<KTableIndex> {
 *              return listOf()
 *         }
 *
 *         override fun kronosColumns(): List<Field> {
 *              return listOf(Field("username"), Field("password"))
 *         }
 *
 *         override fun kronosCreateTime(): KronosCommonStrategy {
 *              return KronosCommonStrategy("create_time")
 *         }
 *
 *         override fun kronosUpdateTime(): KronosCommonStrategy {
 *              return KronosCommonStrategy("update_time")
 *         }
 *
 *         override fun kronosLogicDelete(): KronosCommonStrategy {
 *              return KronosCommonStrategy("deleted")
 *         }
 *
 *         override fun kronosOptimisticLock(): KronosCommonStrategy {
 *              return KronosCommonStrategy("version")
 *         }
 */
class KronosIrClassNewTransformer(
    private val pluginContext: IrPluginContext, private val irClass: IrClass
) : IrElementTransformerVoidWithContext() {
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun visitFunctionNew(declaration: IrFunction): IrStatement {
        if (declaration is IrSimpleFunction && declaration.isFakeOverride) {
            irClass.properties.forEach {
                it.isVar = true
                it.isConst = false
            }
            fun replaceFakeBody(functionBodyFactory: () -> IrBlockBody) {
                declaration.isFakeOverride = false
                declaration.dispatchReceiverParameter = irClass.thisReceiver
                declaration.body = functionBodyFactory()
            }
            with(DeclarationIrBuilder(pluginContext, declaration.symbol) as IrBuilderWithScope) {
                withBuilder(pluginContext) {
                    when (declaration.name.asString()) {
                        "toDataMap" -> replaceFakeBody { createToMapFunction(irClass, declaration) }
                        "get" -> replaceFakeBody { createPropertyGetter(irClass, declaration) }
                        "set" -> replaceFakeBody { createPropertySetter(irClass, declaration) }
                        "safeFromMapData" -> replaceFakeBody { createSafeFromMapValueFunction(irClass, declaration) }
                        "fromMapData" -> replaceFakeBody { createFromMapValueFunction(irClass, declaration) }
                        "kronosTableName" -> replaceFakeBody { createKronosTableName(irClass) }
                        "kronosTableComment" -> replaceFakeBody { createKronosComment(irClass) }
                        "kronosTableIndex" -> replaceFakeBody { createKronosTableIndex(irClass) }
                        "kronosColumns" -> replaceFakeBody { createGetFieldsFunction(irClass) }
                        "kronosCreateTime" -> replaceFakeBody { createKronosCreateTime(irClass) }
                        "kronosUpdateTime" -> replaceFakeBody { createKronosUpdateTime(irClass) }
                        "kronosLogicDelete" -> replaceFakeBody { createKronosLogicDelete(irClass) }
                        "kronosOptimisticLock" -> replaceFakeBody { createKronosOptimisticLock(irClass) }
                    }
                }
            }
        }
        return super.visitFunctionNew(declaration)
    }
}