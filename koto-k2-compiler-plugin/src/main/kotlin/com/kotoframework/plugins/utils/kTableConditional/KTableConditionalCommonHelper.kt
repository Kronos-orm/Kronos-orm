package com.kotoframework.plugins.utils.kTableConditional

import com.kotoframework.plugins.scopes.KotoBuildScope
import com.kotoframework.plugins.scopes.KotoBuildScope.Companion.dispatchBy
import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.ir.builders.irBoolean
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.getPropertySetter
import org.jetbrains.kotlin.ir.util.getSimpleFunction
import org.jetbrains.kotlin.name.FqName

/**
 * Defines various Kotlin IR extensions to handle specific methods of `KTable` during the IR transformation process in Kotlin compiler plugins.
 * 定义多个 Kotlin IR 扩展，用于在 Kotlin 编译器插件的 IR 转换过程中处理 `KTable` 类的特定方法。
 */
//KTableConditional类的setCriteria函数
@OptIn(FirIncompatiblePluginAPI::class)
internal val KotoBuildScope.criteriaSetterSymbol
    get() = pluginContext.referenceClass(FqName("com.kotoframework.beans.dsl.KTableConditional"))!!.getPropertySetter("criteria")!!

@OptIn(FirIncompatiblePluginAPI::class)
private val KotoBuildScope.criteriaClassSymbol
    get() =
        pluginContext.referenceClass(FqName("com.kotoframework.beans.dsl.Criteria"))!!

//Criteria类的addi函数
private val KotoBuildScope.addCriteriaChild
    get() = criteriaClassSymbol.getSimpleFunction("addChild")!!

@OptIn(FirIncompatiblePluginAPI::class)
fun KotoBuildScope.string2ConditionTypeSymbol() =
    pluginContext.referenceFunctions(FqName("com.kotoframework.enums.toConditionType")).first()

// 获取koto函数名
fun IrCall.funcName(): String {
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

    val name = this.symbol.owner.name.asString()
    return map[name] ?: name
}

// 创建Criteria语句
fun KotoBuildScope.createCriteria(
    parameterName: IrExpression? = null,
    type: String,
    not: Boolean,
    value: IrExpression? = null,
    children: List<IrVariable> = listOf(),
    tableName: IrExpression? = null
): IrVariable {
    //创建Criteria
    val irVariable = builder.irTemporary(
        applyIrCall(
            criteriaClassSymbol.constructors.first(),
            parameterName,
            string2ConditionType(type),
            builder.irBoolean(not),
            value,
            tableName
        )
    )
    builder.apply {
        //添加子条件
        children.forEach {
            +applyIrCall(
                addCriteriaChild,
                builder.irGet(it)){
                dispatchBy(irGet(irVariable))
            }
        }
    }
    return irVariable
}

fun KotoBuildScope.string2ConditionType(str: String): IrFunctionAccessExpression {
    return applyIrCall(string2ConditionTypeSymbol(), builder.irString(str))
}
