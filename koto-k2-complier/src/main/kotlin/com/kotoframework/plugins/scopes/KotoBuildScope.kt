package com.kotoframework.plugins.scopes

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.builders.IrBlockBuilder
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrExpression

class KotoBuildScope {
    lateinit var builder: IrBuilderWithScope
    lateinit var blockBody: IrBlockBuilder
    lateinit var function: IrFunction
    lateinit var pluginContext: IrPluginContext

    class CriteriaIR(
        var parameterName: IrExpression,
        var type: String,
        var not: Boolean,
        val value: IrExpression? = null,
        val children: List<IrVariable> = listOf(),
        var tableName: IrExpression? = null,
    )
}