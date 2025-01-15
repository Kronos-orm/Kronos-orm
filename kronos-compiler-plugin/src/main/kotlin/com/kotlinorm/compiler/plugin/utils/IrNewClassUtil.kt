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

package com.kotlinorm.compiler.plugin.utils

import com.kotlinorm.compiler.helpers.applyIrCall
import com.kotlinorm.compiler.helpers.createKClassExpr
import com.kotlinorm.compiler.helpers.dispatchBy
import com.kotlinorm.compiler.helpers.filterByFqName
import com.kotlinorm.compiler.helpers.irListOf
import com.kotlinorm.compiler.helpers.irMutableMapOf
import com.kotlinorm.compiler.helpers.irTry
import com.kotlinorm.compiler.helpers.mapGetterSymbol
import com.kotlinorm.compiler.helpers.referenceClass
import com.kotlinorm.compiler.helpers.referenceFunctions
import com.kotlinorm.compiler.plugin.utils.context.KotlinBuilderContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import com.kotlinorm.compiler.helpers.valueArguments
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irBoolean
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.classOrFail
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.name.FqName


val IrPluginContext.KronosSymbol
    get() = referenceClass("com.kotlinorm.Kronos")!!

private val IrPluginContext.getSafeValueSymbol
    get() = referenceFunctions("com.kotlinorm.utils", "getSafeValue").first()

private val IrPluginContext.KTableIndexSymbol
    get() = referenceClass("com.kotlinorm.beans.dsl.KTableIndex")!!

val IrPluginContext.KPojoFqName
    get() = FqName("com.kotlinorm.interfaces.KPojo")

val IrPluginContext.KPojoSymbol
    get() = referenceClass("com.kotlinorm.interfaces.KPojo")!!

/**
 * Creates a new IrBlockBody that represents a function that converts an instance of an IrClass
 * to a mutable map. The function takes in an IrClass and an IrFunction as parameters.
 *
 * @param declaration The IrClass to be converted to a map.
 * @param irFunction The IrFunction that contains the instance of the IrClass.
 * @return the `IrBlockBody` that represents the function.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
fun KotlinBuilderContext.createToMapFunction(
    declaration: IrClass,
    irFunction: IrFunction,
    ignoreDelegate: Boolean = false
): IrBlockBody {
    with(pluginContext) {
        with(builder) {
            return irBlockBody {
                val dispatcher = irGet(irFunction.dispatchReceiverParameter!!)
                +irReturn(
                    irMutableMapOf(
                        irBuiltIns.stringType,
                        irBuiltIns.anyNType,
                        if (ignoreDelegate) {
                            declaration.properties.filter { !it.isDelegated }.associate {
                                irString(it.name.asString()) to dispatcher.getValue(it)
                            }
                        } else {
                            declaration.properties.associate {
                                irString(it.name.asString()) to dispatcher.getValue(it)
                            }
                        }
                    )
                )
            }
        }
    }
}

/**
 * Creates an IrBlockBody that sets the properties of an IrClass instance using values from a map.
 *
 * @param declaration the IrClass instance whose properties will be set
 * @param irFunction the IrFunction that contains the map parameter
 * @return an `IrBlockBody` that sets the properties of the IrClass instance using values from the map
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
fun KotlinBuilderContext.createFromMapValueFunction(declaration: IrClass, irFunction: IrFunction): IrBlockBody {
    with(pluginContext) {
        with(builder) {
            val map = irFunction.valueParameters.first()
            return irBlockBody {
                val dispatcher = irGet(irFunction.dispatchReceiverParameter!!)
                +declaration.properties.toList().mapNotNull { property ->
                    dispatcher.setValue(property, applyIrCall(
                        mapGetterSymbol, irString(property.name.asString())
                    ) {
                        dispatchBy(irGet(map))
                    })
                }

                +irReturn(
                    irGet(irFunction.dispatchReceiverParameter!!)
                )
            }
        }
    }
}

/**
 * Creates a safe from map value function.
 *
 * @param declaration The IrClass declaration.
 * @param irFunction The IrFunction to create the safe from map value function for.
 * @return an `IrBlockBody` containing the generated code.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
fun KotlinBuilderContext.createSafeFromMapValueFunction(declaration: IrClass, irFunction: IrFunction): IrBlockBody {
    with(pluginContext) {
        with(builder) {
            val map = irFunction.valueParameters.first()
            return irBlockBody {
                val dispatcher = irGet(irFunction.dispatchReceiverParameter!!)
                +irBlock {
                    declaration.properties.toList().mapNotNull { property ->
                        dispatcher.setValue(
                            property, applyIrCall(
                                getSafeValueSymbol,
                                irGet(irFunction.dispatchReceiverParameter!!),
                                createKClassExpr(property.backingField!!.type.classOrFail),
                                irListOf(
                                    irBuiltIns.stringType,
                                    property.backingField!!.type.getClass()!!.superTypes.map { type ->
                                        irString(type.getClass()!!.kotlinFqName.asString())
                                    }
                                ),
                                irGet(map),
                                irString(property.name.asString()),
                                irBoolean(property.hasAnnotation(SerializableAnnotationsFqName))
                            )
                        )?.let {
                            +irTry(
                                it,
                                irBuiltIns.unitType,
                            ) { irCatch() }
                        }
                    }
                }

                +irReturn(dispatcher)
            }
        }
    }
}

/**
 * Creates an IrBlockBody that contains an IrReturn statement with the result of calling the
 * getTableName function with the given IrClass declaration as an argument.
 *
 * @param declaration the IrClass declaration to generate the table name for
 * @return an `IrBlockBody` containing an IrReturn statement with the generated table name
 */
fun KotlinBuilderContext.createKronosTableName(declaration: IrClass): IrBlockBody {
    with(pluginContext){
        with(builder){
            return irBlockBody {
                +irReturn(
                    getTableName(declaration)
                )
            }
        }
    }
}

fun KotlinBuilderContext.createKronosComment(declaration: IrClass): IrBlockBody {
    with(pluginContext){
        with(builder) {
            return irBlockBody {
                +irReturn(
                    declaration.getKDocString()
                )
            }
        }
    }
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
fun KotlinBuilderContext.createKronosTableIndex(declaration: IrClass): IrBlockBody {
    with(pluginContext){
        with(builder) {
            return irBlockBody {
                val indexesAnnotations = declaration.annotations.filterByFqName(TableIndexAnnotationsFqName)
                +irReturn(
                    irListOf(
                        KTableIndexSymbol.defaultType,
                        indexesAnnotations.map {
                            applyIrCall(
                                KTableIndexSymbol.constructors.first(), *it.valueArguments.toTypedArray()
                            )
                        }
                    )
                )
            }
        }
    }
}

/**
 * Creates a safe from map value function for the given declaration and irFunction.
 *
 * @param declaration The IrClass declaration.
 * @return an `IrBlockBody` containing the generated code.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
fun KotlinBuilderContext.createGetFieldsFunction(declaration: IrClass): IrBlockBody {
    with(pluginContext) {
        with(builder) {
            return irBlockBody {
                +irReturn(
                    irListOf(
                        fieldSymbol.owner.defaultType,
                        declaration.properties.map {
                            getColumnName(it)
                        }.toList()
                    )
                )
            }
        }
    }
}

/**
 * Creates an IrBlockBody that contains an IrReturn statement with the result of calling the
 * getValidStrategy function with the given IrClass declaration, globalCreateTimeSymbol, and
 * CreateTimeFqName as arguments.
 *
 * @param declaration The IrClass declaration to generate the IrBlockBody for.
 * @return an `IrBlockBody` containing the generated code.
 */
fun KotlinBuilderContext.createKronosCreateTime(declaration: IrClass): IrBlockBody {
    with(pluginContext){
        with(builder) {
            return irBlockBody {
                +irReturn(
                    getValidStrategy(declaration, createTimeStrategySymbol, CreateTimeFqName)
                )
            }
        }
    }
}

/**
 * Creates an IrBlockBody that contains an IrReturn statement with the result of calling the
 * getValidStrategy function with the given IrClass declaration, globalUpdateTimeSymbol, and
 * UpdateTimeFqName as arguments.
 *
 * @param declaration The IrClass declaration to generate the IrBlockBody for.
 * @return an `IrBlockBody` containing the generated code.
 */
fun KotlinBuilderContext.createKronosUpdateTime(declaration: IrClass): IrBlockBody {
    with(pluginContext){
        with(builder) {
            return irBlockBody {
                +irReturn(
                    getValidStrategy(declaration, updateTimeStrategySymbol, UpdateTimeFqName)
                )
            }
        }
    }
}

/**
 * Creates a function that returns a block body containing an irCall to createFieldListSymbol
 * with the properties of the given IrClass as arguments.
 *
 * @param declaration the IrClass whose properties will be used as arguments for createFieldListSymbol
 * @return an `IrBlockBody` containing an irCall to createFieldListSymbol
 */
fun KotlinBuilderContext.createKronosLogicDelete(declaration: IrClass): IrBlockBody {
    with(pluginContext){
        with(builder) {
            return irBlockBody {
                +irReturn(
                    getValidStrategy(declaration, logicDeleteStrategySymbol, LogicDeleteFqName)
                )
            }
        }
    }
}

fun KotlinBuilderContext.createKronosOptimisticLock(declaration: IrClass): IrBlockBody {
    with(pluginContext){
        with(builder) {
            return irBlockBody {
                +irReturn(
                    getValidStrategy(declaration, optimisticLockStrategySymbol, OptimisticLockFqName)
                )
            }
        }
        }
}