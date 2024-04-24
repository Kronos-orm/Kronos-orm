package com.kotoframework.plugins.utils.kTableConditional

import com.kotoframework.plugins.scopes.KotoBuildScope
import com.kotoframework.plugins.utils.kTable.correspondingName
import com.kotoframework.plugins.utils.kTable.fieldK2dbSymbol
import com.kotoframework.plugins.utils.kTable.getColumnName
import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrIfThenElseImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.classId

val map = mapOf(
    "EQEQ" to "equal",
    "ANDAND" to "and",
    "OROR" to "or",
    "greater" to "gt",
    "less" to "lt",
    "greaterOrEqual" to "ge",
    "lessOrEqual" to "le",
    "contains" to "isIn",
)

/**
 * Defines various Kotlin IR extensions to handle specific methods of `KTable` during the IR transformation process in Kotlin compiler plugins.
 * 定义多个 Kotlin IR 扩展，用于在 Kotlin 编译器插件的 IR 转换过程中处理 `KTable` 类的特定方法。
 */
//KTableConditional类的setCriteria函数
@OptIn(FirIncompatiblePluginAPI::class)
private val KotoBuildScope.criteriaSetterSymbol
    get() = pluginContext.referenceClass(FqName("com.kotoframework.beans.dsl.KTableConditional"))!!.getPropertySetter("criteria")!!

@OptIn(FirIncompatiblePluginAPI::class)
fun KotoBuildScope.criteriaClassSymbol() =
    pluginContext.referenceClass(FqName("com.kotoframework.beans.dsl.Criteria"))!!

@OptIn(FirIncompatiblePluginAPI::class)
fun KotoBuildScope.string2ConditionTypeSymbol() =
    pluginContext.referenceFunctions(FqName("com.kotoframework.enums.toConditionType")).first()

fun KotoBuildScope.buildCritercia(element: IrElement , setNot: Boolean = false): IrVariable? {
    var paramName: IrExpression = builder.irString("")
    var type = "ROOT"
    var not = setNot
    var value:IrExpression? = null
    var children: MutableList<IrVariable?> = mutableListOf()
    var tableName: IrExpression? = null

    when(element) {
        is IrBlockBody -> {
            element.statements.forEach { statement ->
                children.add(buildCritercia(statement))
            }
        }

        is IrIfThenElseImpl -> {
            val origin = element.origin.toString()
            type = map[origin] ?: origin
            element.branches.forEach {
                children.add(buildCritercia(it.condition))
                children.add(buildCritercia(it.result))
            }
        }

        is IrCall -> {
            val funcName = element.funcName
            type = funcName
            val args = element.argumentsNot("KTableConditional")
            if ("not" == funcName) {
                if (args.size == 1) {
                    return buildCritercia(args[0] , true)
                }
                args.forEach {
                    children.add(buildCritercia(it))
                }
            } else {
                when (funcName) {
                    "isIn" -> {
                        value = args[0]
                        paramName = getColumnName(args[1])
                        tableName = getTableName(args[1])
                    }

                    "isNull" , "notNull" -> {
                        paramName = getColumnName(args[0])
                        tableName = getTableName(args[0])
                    }

                    "lt" , "gt" , "le" , "ge" -> {
                        val compareToIrCall = args[0]
                        val compareToArgs = (compareToIrCall as IrCallImpl).argumentsNot("KTableConditional")
                        paramName = getColumnName(compareToArgs[0])
                        value = compareToArgs[1]
                        tableName = getTableName(compareToArgs[0])
                    }

                    "equal" , "like" , "between" -> {
                        paramName = getColumnName(args[0])
                        value = args[1]
                        tableName = getTableName(args[0])
                    }

                    "notLike" , "notBetween" -> {
                        type = funcName.replaceFirst("not" , "").replaceFirstChar { it.lowercase() }
                        not = true
                        paramName = getColumnName(args[0])
                        value = args[1]
                        tableName = getTableName(args[0])
                    }
                }
            }
        }

        is IrReturn -> {
            return buildCritercia(element.value)
        }

        is IrConstImpl<*> -> {
            return null
        }

    }

    return KotoBuildScope.CriteriaIR(paramName , type , not , value , children.filterNotNull() , tableName).toIrVariable()
}

// 获取koto函数名
val IrCall.funcName get(): String {
        val name = this.symbol.owner.name.asString()
        return map[name] ?: name
    }

// 获取函数参数列表（去除reciver）
@OptIn(ObsoleteDescriptorBasedAPI::class)
fun IrCall.argumentsNot(simpleName: String): List<IrExpression> {
    return getArguments().filter {
        (_ , expression) -> expression.type.toKotlinType().toString() != simpleName
    }.map { it.second }
}

////返回KPojo或字符串的参数名，若有注解则读取注解信息
//@OptIn(ObsoleteDescriptorBasedAPI::class)
//fun KotoBuildScope.getParamName(expression: IrExpression): IrExpression {
//    return when (expression) {
//        is IrCall -> {
//            val propName = expression.correspondingName
//            val  annotations = expression.dispatchReceiver!!.type.getClass()!!.properties.first {
//                it.name == propName
//            }.annotations
//            val columnAnnotation = annotations.firstOrNull {
//                it.symbol.descriptor.containingDeclaration.classId == ClassId.fromString("com/kotoframework/annotations/Column")
//            }
//            if (null != columnAnnotation) {
//                line2Hump(columnAnnotation.getValueArgument(0))
//            } else {
//                builder.irString(propName.toString())
//            }
//        }
//        is IrConst<*> -> builder.irString(expression.value.toString())
//        else -> builder.irString("")
//    }
//}

@OptIn(ObsoleteDescriptorBasedAPI::class)
fun KotoBuildScope.getTableName(expression: IrExpression): IrExpression? {
    return when (expression) {
        is IrCall -> {
            val kClass = expression.dispatchReceiver!!.type.getClass()
            val annotations = kClass!!.annotations
            val tableAnnotation = annotations.firstOrNull {
                it.symbol.descriptor.containingDeclaration.classId == ClassId.fromString("com/kotoframework/annotations/Table")
            }
            if (null != tableAnnotation) {
                return tableAnnotation.getValueArgument(0)
            } else {
                hump2line(builder.irString(kClass.name.asString()))
            }
        }
        else -> null
    }
}

// 下划线转驼峰
fun KotoBuildScope.line2Hump(irExpression: IrExpression?): IrFunctionAccessExpression {
    return applyIrCall(fieldK2dbSymbol, irExpression)
}

// 驼峰转下划线
fun KotoBuildScope.hump2line(irExpression: IrExpression): IrFunctionAccessExpression {
    return applyIrCall(fieldK2dbSymbol , irExpression)
}

// 创建SimpleCriteria语句
fun KotoBuildScope.createCriteria(
    parameterName: IrExpression,
    type: String,
    not: Boolean,
    value: IrExpression? = null,
    children: List<IrVariable> = listOf(),
    tableName: IrExpression? = null
): IrVariable {
    val irCall = builder.irCall(criteriaClassSymbol().constructors.first())
    irCall.putValueArgument(0 , parameterName)
    irCall.putValueArgument(1 , string2ConditionType(type))
    irCall.putValueArgument(2 , builder.irBoolean(not))

    if (null != value) {
        irCall.putValueArgument(3 , value)
    }
    if (null != tableName) {
        irCall.putValueArgument(4 , tableName)
    }
    val irVariable = builder.irTemporary(irCall)
    builder.apply {
        children.forEach {
            +applyIrCall(criteriaSetterSymbol , irGet(irVariable)).apply {
                putValueArgument(0 , builder.irGet(it))
            }
        }
    }
    return irVariable
}

fun KotoBuildScope.string2ConditionType(str: String): IrFunctionAccessExpression {
    return applyIrCall(string2ConditionTypeSymbol(), builder.irString(str))
}
