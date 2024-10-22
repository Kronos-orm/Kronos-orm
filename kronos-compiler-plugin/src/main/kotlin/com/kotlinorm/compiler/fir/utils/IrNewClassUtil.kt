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

package com.kotlinorm.compiler.fir.utils

import com.kotlinorm.compiler.helpers.applyIrCall
import com.kotlinorm.compiler.helpers.createKClassExpr
import com.kotlinorm.compiler.helpers.dispatchBy
import com.kotlinorm.compiler.helpers.filterByFqName
import com.kotlinorm.compiler.helpers.irListOf
import com.kotlinorm.compiler.helpers.irMutableMapOf
import com.kotlinorm.compiler.helpers.irTry
import com.kotlinorm.compiler.helpers.referenceClass
import com.kotlinorm.compiler.helpers.referenceFunctions
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.backend.js.utils.valueArguments
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irBoolean
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.classOrFail
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.getSimpleFunction
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.name.FqName

context(IrPluginContext)
val KronosSymbol
    get() = referenceClass("com.kotlinorm.Kronos")!!

context(IrPluginContext)
private val getSafeValueSymbol
    get() = referenceFunctions("com.kotlinorm.utils", "getSafeValue").first()

context(IrPluginContext)
@OptIn(UnsafeDuringIrConstructionAPI::class)
val mapGetterSymbol
    get() = referenceClass("kotlin.collections.Map")!!.getSimpleFunction("get")!!

context(IrPluginContext)
private val KTableIndexSymbol
    get() = referenceClass("com.kotlinorm.beans.dsl.KTableIndex")!!

context(IrPluginContext)
val KPojoFqName
    get() = FqName("com.kotlinorm.interfaces.KPojo")

/**
 * Creates a new IrBlockBody that represents a function that converts an instance of an IrClass
 * to a mutable map. The function takes in an IrClass and an IrFunction as parameters.
 *
 * @param declaration The IrClass to be converted to a map.
 * @param irFunction The IrFunction that contains the instance of the IrClass.
 * @return the `IrBlockBody` that represents the function.
 */
context(IrBuilderWithScope, IrPluginContext)
@OptIn(UnsafeDuringIrConstructionAPI::class)
fun createToMapFunction(declaration: IrClass, irFunction: IrFunction): IrBlockBody {
    return irBlockBody {
        val dispatcher = irGet(irFunction.dispatchReceiverParameter!!)
        +irReturn(
            irMutableMapOf(
                irBuiltIns.stringType,
                irBuiltIns.anyNType,
                declaration.properties.associate {
                    irString(it.name.asString()) to dispatcher.getValue(it)
                }
            )
        )
    }
}

/**
 * Creates an IrBlockBody that sets the properties of an IrClass instance using values from a map.
 *
 * @param declaration the IrClass instance whose properties will be set
 * @param irFunction the IrFunction that contains the map parameter
 * @return an `IrBlockBody` that sets the properties of the IrClass instance using values from the map
 */
context(IrBuilderWithScope, IrPluginContext)
@OptIn(UnsafeDuringIrConstructionAPI::class)
fun createFromMapValueFunction(declaration: IrClass, irFunction: IrFunction): IrBlockBody {
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

/**
 * Creates a safe from map value function.
 *
 * @param declaration The IrClass declaration.
 * @param irFunction The IrFunction to create the safe from map value function for.
 * @return an `IrBlockBody` containing the generated code.
 */
context(IrBuilderWithScope, IrPluginContext)
@OptIn(UnsafeDuringIrConstructionAPI::class)
fun createSafeFromMapValueFunction(declaration: IrClass, irFunction: IrFunction): IrBlockBody {
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

/**
 * Creates an IrBlockBody that contains an IrReturn statement with the result of calling the
 * getTableName function with the given IrClass declaration as an argument.
 *
 * @param declaration the IrClass declaration to generate the table name for
 * @return an `IrBlockBody` containing an IrReturn statement with the generated table name
 */
context(IrBuilderWithScope, IrPluginContext)
fun createKronosTableName(declaration: IrClass): IrBlockBody {
    return irBlockBody {
        +irReturn(
            getTableName(declaration)
        )
    }
}

context(IrBuilderWithScope, IrPluginContext)
fun createKronosComment(declaration: IrClass): IrBlockBody {
    return irBlockBody {
        +irReturn(
            declaration.getKDocString()
        )
    }
}

context(IrBuilderWithScope, IrPluginContext)
@OptIn(UnsafeDuringIrConstructionAPI::class)
fun createKronosTableIndex(declaration: IrClass): IrBlockBody {
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

/**
 * Creates a safe from map value function for the given declaration and irFunction.
 *
 * @param declaration The IrClass declaration.
 * @return an `IrBlockBody` containing the generated code.
 */
context(IrBuilderWithScope, IrPluginContext)
@OptIn(UnsafeDuringIrConstructionAPI::class)
fun createGetFieldsFunction(declaration: IrClass): IrBlockBody {
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

/**
 * Creates an IrBlockBody that contains an IrReturn statement with the result of calling the
 * getValidStrategy function with the given IrClass declaration, globalCreateTimeSymbol, and
 * CreateTimeFqName as arguments.
 *
 * @param declaration The IrClass declaration to generate the IrBlockBody for.
 * @return an `IrBlockBody` containing the generated code.
 */
context(IrBuilderWithScope, IrPluginContext)
fun createKronosCreateTime(declaration: IrClass): IrBlockBody {
    return irBlockBody {
        +irReturn(
            getValidStrategy(declaration, globalCreateTimeSymbol, CreateTimeFqName)
        )
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
context(IrBuilderWithScope, IrPluginContext)
fun createKronosUpdateTime(declaration: IrClass): IrBlockBody {
    return irBlockBody {
        +irReturn(
            getValidStrategy(declaration, globalUpdateTimeSymbol, UpdateTimeFqName)
        )
    }
}

/**
 * Creates a function that returns a block body containing an irCall to createFieldListSymbol
 * with the properties of the given IrClass as arguments.
 *
 * @param declaration the IrClass whose properties will be used as arguments for createFieldListSymbol
 * @return an `IrBlockBody` containing an irCall to createFieldListSymbol
 */
context(IrBuilderWithScope, IrPluginContext)
fun createKronosLogicDelete(declaration: IrClass): IrBlockBody {
    return irBlockBody {
        +irReturn(
            getValidStrategy(declaration, globalLogicDeleteSymbol, LogicDeleteFqName)
        )
    }
}

context(IrBuilderWithScope, IrPluginContext)
fun createKronosOptimisticLock(declaration: IrClass): IrBlockBody {
    return irBlockBody {
        +irReturn(
            getValidStrategy(declaration, globalOptimisticLockSymbol, OptimisticLockFqName)
        )
    }
}

context(IrBuilderWithScope, IrPluginContext)
fun IrExpression.setValue(property: IrProperty, value: IrExpression): IrExpression? {
    if (property.isDelegated) return null
    return if (property.setter != null) {
        applyIrCall(
            property.setter!!.symbol, value
        ) {
            dispatchReceiver = this@setValue
        }
    } else {
        irSetField(
            this@setValue, property.backingField!!, value
        )
    }
}

context(IrBuilderWithScope, IrPluginContext)
fun IrExpression.getValue(property: IrProperty): IrExpression {
    return if (property.getter != null) {
        applyIrCall(
            property.getter!!.symbol
        ) {
            dispatchReceiver = this@getValue
        }
    } else {
        irGetField(
            this@getValue, property.backingField!!
        )
    }
}