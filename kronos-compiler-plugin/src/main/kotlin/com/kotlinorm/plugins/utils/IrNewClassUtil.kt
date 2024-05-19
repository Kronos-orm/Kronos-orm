package com.kotlinorm.plugins.utils

import com.kotlinorm.plugins.helpers.applyIrCall
import com.kotlinorm.plugins.helpers.dispatchBy
import com.kotlinorm.plugins.helpers.referenceClass
import com.kotlinorm.plugins.helpers.referenceFunctions
import com.kotlinorm.plugins.utils.kTable.getColumnName
import com.kotlinorm.plugins.utils.kTable.getTableName
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.getPropertyGetter
import org.jetbrains.kotlin.ir.util.getSimpleFunction
import org.jetbrains.kotlin.ir.util.properties

context(IrPluginContext)
val createPairSymbol
    get() = referenceFunctions("com.kotlinorm.utils", "createPair")
        .first()
context(IrPluginContext)
val createMutableMapSymbol
    get() = referenceFunctions("com.kotlinorm.utils", "createMutableMap")
        .first()
context(IrPluginContext)
val createFieldListSymbol
    get() = referenceFunctions("com.kotlinorm.utils", "createFieldList")
        .first()

context(IrPluginContext)
private val fieldSymbol
    get() = referenceClass("com.kotlinorm.beans.dsl.Field")!!

context(IrPluginContext)
private val mapGetterSymbol
    get() = referenceClass("kotlin.collections.Map")!!.getSimpleFunction("get")

context(IrBuilderWithScope, IrPluginContext)
fun createToMapFunction(declaration: IrClass, irFunction: IrFunction): IrBlockBody {
    return irBlockBody {
        val pairs = declaration.properties.map {
            applyIrCall(
                createPairSymbol,
                irString(it.name.asString()),
                irGetField(
                    irGet(irFunction.dispatchReceiverParameter!!),
                    it.backingField!!
                )
            )
        }.toList()
        +irReturn(
            applyIrCall(
                createMutableMapSymbol, irVararg(
                    createPairSymbol.owner.returnType,
                    pairs
                )
            )
        )
    }
}

context(IrBuilderWithScope, IrPluginContext)
fun createFromMapValueFunction(declaration: IrClass, irFunction: IrFunction): IrBlockBody {
    val map = irFunction.valueParameters.first()
    return irBlockBody {
        declaration.properties.forEach {
            +irSetField(
                irGet(irFunction.dispatchReceiverParameter!!),
                it.backingField!!,
                applyIrCall(
                    mapGetterSymbol!!,
                    irString(it.name.asString())
                ){
                    dispatchBy(irGet(map))
                }
            )
        }

        +irReturn(
            irGet(irFunction.dispatchReceiverParameter!!)
        )
    }
}

context(IrBuilderWithScope, IrPluginContext)
fun createKronosTableName(declaration: IrClass): IrBlockBody {
    return irBlockBody {
        +irReturn(
            getTableName(declaration)
        )
    }
}

context(IrBuilderWithScope, IrPluginContext)
fun createGetFieldsFunction(declaration: IrClass): IrBlockBody {
    return irBlockBody {
        +irReturn(
            applyIrCall(
                createFieldListSymbol, irVararg(
                    fieldSymbol.owner.defaultType,
                    declaration.properties.map {
                        getColumnName(it)
                    }.toList()
                )
            )
        )
    }
}

context(IrBuilderWithScope, IrPluginContext)
fun createKronosCreateTime(declaration: IrClass): IrBlockBody {
    return irBlockBody {
        +irReturn(
            getValidStrategy(declaration, globalCreateTimeSymbol, CreateTimeFqName)
        )
    }
}

context(IrBuilderWithScope, IrPluginContext)
fun createKronosUpdateTime(declaration: IrClass): IrBlockBody {
    return irBlockBody {
        +irReturn(
            getValidStrategy(declaration, globalUpdateTimeSymbol, UpdateTimeFqName)
        )
    }
}

context(IrBuilderWithScope, IrPluginContext)
fun createKronosLogicDelete(declaration: IrClass): IrBlockBody {
    return irBlockBody {
        +irReturn(
            getValidStrategy(declaration, globalLogicDeleteSymbol, LogicDeleteFqName)
        )
    }
}