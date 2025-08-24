/**
 * Copyright 2022-2025 kronos-orm
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

import com.kotlinorm.compiler.helpers.IrTryBuilder.Companion.irTry
import com.kotlinorm.compiler.helpers.filterByFqName
import com.kotlinorm.compiler.helpers.invoke
import com.kotlinorm.compiler.helpers.irListOf
import com.kotlinorm.compiler.helpers.irMutableMapOf
import com.kotlinorm.compiler.helpers.mapGetterSymbol
import com.kotlinorm.compiler.helpers.referenceClass
import com.kotlinorm.compiler.helpers.referenceFunctions
import com.kotlinorm.compiler.helpers.toKClass
import com.kotlinorm.compiler.helpers.valueArguments
import com.kotlinorm.compiler.helpers.valueParameters
import com.kotlinorm.compiler.plugin.utils.kTableForCondition.ignore
import com.kotlinorm.compiler.plugin.utils.kTableForCondition.ignoreAnnotationValue
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irBoolean
import org.jetbrains.kotlin.ir.builders.irBranch
import org.jetbrains.kotlin.ir.builders.irElseBranch
import org.jetbrains.kotlin.ir.builders.irEquals
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.irWhen
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
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isGetter
import org.jetbrains.kotlin.ir.util.isSetter
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.name.FqName
import kotlin.contracts.ExperimentalContracts

context(_: IrPluginContext)
val KronosSymbol
    get() = referenceClass("com.kotlinorm.Kronos")!!

context(_: IrPluginContext)
private val getSafeValueSymbol
    get() = referenceFunctions("com.kotlinorm.utils", "getSafeValue").first()

context(_: IrPluginContext)
private val KTableIndexSymbol
    get() = referenceClass("com.kotlinorm.beans.dsl.KTableIndex")!!

context(_: IrPluginContext)
val KPojoFqName
    get() = FqName("com.kotlinorm.interfaces.KPojo")

context(_: IrPluginContext)
val KPojoSymbol
    get() = referenceClass("com.kotlinorm.interfaces.KPojo")!!

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBuilderWithScope)
fun createPropertyGetter(
    declaration: IrClass,
    irFunction: IrFunction
): IrBlockBody {
    val dispatcher = builder.irGet(irFunction.dispatchReceiverParameter!!)
    return builder.irBlockBody {
        +irReturn(
            irWhen(
                context.irBuiltIns.anyNType,
                declaration.properties.map {
                    irBranch(
                        irEquals(
                            irString(it.name.asString()),
                            irGet(irFunction.parameters.valueParameters[0])
                        ),
                        dispatcher.getValue(it)
                    )
                }.toList() + irElseBranch(
                    irNull()
                )
            )
        )
    }
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBuilderWithScope)
fun createPropertySetter(
    declaration: IrClass,
    irFunction: IrFunction
): IrBlockBody {
    val dispatcher = builder.irGet(irFunction.dispatchReceiverParameter!!)
    return builder.irBlockBody {
        +irWhen(
            context.irBuiltIns.unitType,
            declaration.properties.map {
                irBranch(
                    irEquals(
                        irString(it.name.asString()),
                        irGet(irFunction.parameters.valueParameters[0])
                    ),
                    irBlock {
                        +(dispatcher.setValue(
                            it,
                            irGet(irFunction.parameters.valueParameters[1])
                        ) ?: irNull())
                    }
                )
            }.toList() + irElseBranch(
                irNull()
            )
        )
    }
}

/**
 * Creates a new IrBlockBody that represents a function that returns the KClass of the given
 *
 * @param declaration The IrClass to be converted to a KClass.
 * @return the `IrBlockBody` that represents the function.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(_: IrPluginContext, builder: IrBuilderWithScope)
fun createKClassFunction(
    declaration: IrClass
) = builder.irBlockBody { +irReturn(declaration.symbol.toKClass()) }

/**
 * Creates a new IrBlockBody that represents a function that converts an instance of an IrClass
 * to a mutable map. The function takes in an IrClass and an IrFunction as parameters.
 *
 * @param declaration The IrClass to be converted to a map.
 * @param irFunction The IrFunction that contains the instance of the IrClass.
 * @return the `IrBlockBody` that represents the function.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(context: IrPluginContext, builder: IrBuilderWithScope)
fun createToMapFunction(
    declaration: IrClass,
    irFunction: IrFunction
) = builder.irBlockBody {
    val dispatcher = irGet(irFunction.dispatchReceiverParameter!!)
    +irReturn(
        irMutableMapOf(
            context.irBuiltIns.stringType,
            context.irBuiltIns.anyNType,
            declaration.properties.filter {
                return@filter !it.ignoreAnnotationValue().ignore("to_map") && !it.isSetter
            }.associate {
                irString(it.name.asString()) to dispatcher.getValue(it)
            }
        )
    )
}

/**
 * Creates an IrBlockBody that sets the properties of an IrClass instance using values from a map.
 *
 * @param declaration the IrClass instance whose properties will be set
 * @param irFunction the IrFunction that contains the map parameter
 * @return an `IrBlockBody` that sets the properties of the IrClass instance using values from the map
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(_: IrPluginContext, builder: IrBuilderWithScope)
fun createFromMapValueFunction(declaration: IrClass, irFunction: IrFunction): IrBlockBody {
    val map = irFunction.parameters.valueParameters.first()
    return builder.irBlockBody {
        val dispatcher = irGet(irFunction.dispatchReceiverParameter!!)
        +declaration.properties.toList().mapNotNull { property ->
            if (property.isDelegated || property.isGetter ||
                property.ignoreAnnotationValue().ignore("from_map")
            ) {
                return@mapNotNull null
            }
            dispatcher.setValue(
                property, mapGetterSymbol(
                    irGet(map),
                    irString(property.name.asString())
                )
            )
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
@OptIn(UnsafeDuringIrConstructionAPI::class, ExperimentalContracts::class)
context(context: IrPluginContext, builder: IrBuilderWithScope)
fun createSafeFromMapValueFunction(declaration: IrClass, irFunction: IrFunction): IrBlockBody {
    val map = irFunction.parameters.first()
    return builder.irBlockBody {
        val dispatcher = irGet(irFunction.dispatchReceiverParameter!!)
        +irBlock {
            declaration.properties.toList().mapNotNull { property ->
                if (property.isDelegated || property.isGetter ||
                    property.ignoreAnnotationValue().ignore("from_map")
                ) {
                    return@mapNotNull null
                }
                dispatcher.setValue(
                    property, getSafeValueSymbol(
                        irGet(irFunction.dispatchReceiverParameter!!),
                        property.backingField!!.type.classOrFail.toKClass(),
                        irListOf(
                            context.irBuiltIns.stringType,
                            property.backingField!!.type.getClass()!!.superTypes.map { type ->
                                irString(type.getClass()!!.kotlinFqName.asString())
                            }
                        ),
                        irGet(map),
                        irString(property.name.asString()),
                        irBoolean(property.hasAnnotation(SerializeAnnotationsFqName))
                    )
                )?.let {
                    +irTry(it, context.irBuiltIns.unitType).catch().build()
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
context(_: IrPluginContext, builder: IrBuilderWithScope)
fun createKronosTableName(declaration: IrClass) = builder.irBlockBody {
    +irReturn(
        getTableName(declaration)
    )
}

context(_: IrPluginContext, builder: IrBuilderWithScope)
fun createKronosComment(declaration: IrClass) = builder.irBlockBody {
    +irReturn(
        declaration.getKDocString()
    )
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
context(_: IrPluginContext, builder: IrBuilderWithScope)
fun createKronosTableIndex(declaration: IrClass) = builder.irBlockBody {
    val indexesAnnotations = declaration.annotations.filterByFqName(TableIndexAnnotationsFqName)
    +irReturn(
        irListOf(
            KTableIndexSymbol.defaultType,
            indexesAnnotations.map {
                KTableIndexSymbol.constructors.first()(
                    *it.valueArguments.toTypedArray()
                )
            }
        )
    )
}

/**
 * Creates a safe from map value function for the given declaration and irFunction.
 *
 * @param declaration The IrClass declaration.
 * @return an `IrBlockBody` containing the generated code.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
context(_: IrPluginContext, builder: IrBuilderWithScope)
fun createGetFieldsFunction(declaration: IrClass) = builder.irBlockBody {
    +irReturn(
        irListOf(
            fieldSymbol.owner.defaultType,
            declaration.properties.map {
                getColumnName(it)
            }.toList()
        )
    )
}

/**
 * Creates an IrBlockBody that contains an IrReturn statement with the result of calling the
 * getValidStrategy function with the given IrClass declaration, globalCreateTimeSymbol, and
 * CreateTimeFqName as arguments.
 *
 * @param declaration The IrClass declaration to generate the IrBlockBody for.
 * @return an `IrBlockBody` containing the generated code.
 */
context(_: IrPluginContext, builder: IrBuilderWithScope)
fun createKronosCreateTime(declaration: IrClass) = builder.irBlockBody {
    +irReturn(
        getValidStrategy(declaration, createTimeStrategySymbol, CreateTimeFqName)
    )
}

/**
 * Creates an IrBlockBody that contains an IrReturn statement with the result of calling the
 * getValidStrategy function with the given IrClass declaration, globalUpdateTimeSymbol, and
 * UpdateTimeFqName as arguments.
 *
 * @param declaration The IrClass declaration to generate the IrBlockBody for.
 * @return an `IrBlockBody` containing the generated code.
 */
context(_: IrPluginContext, builder: IrBuilderWithScope)
fun createKronosUpdateTime(declaration: IrClass) = builder.irBlockBody {
    +irReturn(
        getValidStrategy(declaration, updateTimeStrategySymbol, UpdateTimeFqName)
    )
}

/**
 * Creates a function that returns a block body containing an irCall to createFieldListSymbol
 * with the properties of the given IrClass as arguments.
 *
 * @param declaration the IrClass whose properties will be used as arguments for createFieldListSymbol
 * @return an `IrBlockBody` containing an irCall to createFieldListSymbol
 */
context(_: IrPluginContext, builder: IrBuilderWithScope)
fun createKronosLogicDelete(declaration: IrClass) = builder.irBlockBody {
    +irReturn(
        getValidStrategy(declaration, logicDeleteStrategySymbol, LogicDeleteFqName)
    )
}

context(_: IrPluginContext, builder: IrBuilderWithScope)
fun createKronosOptimisticLock(declaration: IrClass) = builder.irBlockBody {
    +irReturn(
        getValidStrategy(declaration, optimisticLockStrategySymbol, OptimisticLockFqName)
    )
}

context(builder: IrBuilderWithScope)
fun IrExpression.getValue(property: IrProperty): IrExpression {
    return if (property.getter != null) {
        property.getter!!.symbol(this@getValue)
    } else {
        builder.irGetField(
            this@getValue, property.backingField!!
        )
    }
}

context(builder: IrBuilderWithScope)
fun IrExpression.setValue(property: IrProperty, value: IrExpression): IrExpression? {
    with(builder) {
        if (property.isDelegated) return null
        return if (property.setter != null) {
            property.setter!!.symbol(this@setValue, value)
        } else {
            irSetField(
                this@setValue, property.backingField!!, value
            )
        }
    }
}