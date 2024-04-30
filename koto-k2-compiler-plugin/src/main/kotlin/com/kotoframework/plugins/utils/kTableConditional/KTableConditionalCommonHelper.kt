package com.kotoframework.plugins.utils.kTableConditional

import com.kotoframework.plugins.utils.applyIrCall
import com.kotoframework.plugins.utils.dispatchBy
import com.kotoframework.plugins.utils.kTable.correspondingName
import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.getPropertySetter
import org.jetbrains.kotlin.ir.util.getSimpleFunction
import org.jetbrains.kotlin.name.FqName

/**
 * Defines various Kotlin IR extensions to handle specific methods of `KTable` during the IR transformation process in Kotlin compiler plugins.
 * 定义多个 Kotlin IR 扩展，用于在 Kotlin 编译器插件的 IR 转换过程中处理 `KTable` 类的特定方法。
 */
//KTableConditional类的setCriteria函数
context(IrPluginContext)
@OptIn(FirIncompatiblePluginAPI::class)
internal val criteriaSetterSymbol
    get() = referenceClass(FqName("com.kotoframework.beans.dsl.KTableConditional"))!!.getPropertySetter("criteria")!!

context(IrPluginContext)
@OptIn(FirIncompatiblePluginAPI::class)
private val criteriaClassSymbol
    get() = referenceClass(FqName("com.kotoframework.beans.dsl.Criteria"))!!

//Criteria类的add函数
context(IrBuilderWithScope, IrPluginContext)
private val addCriteriaChild
    get() = criteriaClassSymbol.getSimpleFunction("addChild")!!

context(IrPluginContext)
@OptIn(FirIncompatiblePluginAPI::class)
private val string2ConditionTypeSymbol
    get() = referenceFunctions(FqName("com.kotoframework.enums.toConditionType")).first()

context(IrPluginContext)
@OptIn(FirIncompatiblePluginAPI::class)
internal val stringPlusSymbol
    get() = referenceFunctions(FqName("kotlin.String.plus")).first()

// 获取koto函数名
context(IrPluginContext)
fun IrExpression.funcName(setNot: Boolean = false): String {
    return when (this) {
        is IrCall -> when (origin) {
            IrStatementOrigin.EQEQ , IrStatementOrigin.EXCLEQ -> "equal"
            IrStatementOrigin.GT -> "gt"
            IrStatementOrigin.LT -> "lt"
            IrStatementOrigin.GTEQ -> "ge"
            IrStatementOrigin.LTEQ -> "le"
            else -> correspondingName?.asString() ?: symbol.owner.name.asString()
        }

        is IrWhen -> when {
            origin == IrStatementOrigin.OROR && !setNot -> "OR"
            origin == IrStatementOrigin.ANDAND && !setNot -> "AND"
            origin == IrStatementOrigin.OROR && setNot -> "AND"
            origin == IrStatementOrigin.ANDAND && setNot -> "OR"
            else -> origin.toString()
        }

        else -> ""
    }

}

context(IrPluginContext)
fun parseConditionType(funcName: String): Pair<String, Boolean> {
    return when (funcName) {
        "isNull" -> funcName to false
        "notNull" -> "isNull" to true
        "lt", "gt", "le", "ge" -> funcName to false
        "equal" -> "equal" to false
        "eq" -> "equal" to false
        "neq" -> "equal" to true
        "between" -> "between" to false
        "notBetween" -> "between" to true
        "like", "matchLeft", "matchRight", "matchBoth" -> "like" to false
        "notLike" -> "like" to true
        "contains" -> "in" to false
        "asSql" -> "sql" to false
        else -> throw IllegalArgumentException("Unknown condition type: $funcName")
    }
}

// 创建Criteria语句
context(IrBlockBuilder, IrPluginContext)
fun createCriteria(
    parameterName: IrExpression? = null,
    type: String,
    not: Boolean,
    value: IrExpression? = null,
    children: List<IrVariable> = listOf(),
    tableName: IrExpression? = null
): IrVariable {
    //创建Criteria
    val irVariable = irTemporary(
        applyIrCall(
            criteriaClassSymbol.constructors.first(),
            parameterName,
            string2ConditionType(type),
            irBoolean(not),
            value,
            tableName
        )
    )
    //添加子条件
    children.forEach {
        +applyIrCall(
            addCriteriaChild,
            irGet(it)
        ) {
            dispatchBy(irGet(irVariable))
        }
    }
    return irVariable
}

context(IrBuilderWithScope, IrPluginContext)
fun string2ConditionType(str: String): IrFunctionAccessExpression {
    return applyIrCall(string2ConditionTypeSymbol, irString(str))
}
