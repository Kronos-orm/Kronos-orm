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

import com.kotlinorm.compiler.utils.irListOf
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBoolean
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.superTypes
import org.jetbrains.kotlin.name.FqName

private val fqNameOfTypedQuery = listOf(
    FqName("com.kotlinorm.beans.task.KronosQueryTask.queryList"),
    FqName("com.kotlinorm.beans.task.KronosQueryTask.queryOne"),
    FqName("com.kotlinorm.beans.task.KronosQueryTask.queryOneOrNull"),
    FqName("com.kotlinorm.orm.select.SelectClause.queryList"),
    FqName("com.kotlinorm.orm.select.SelectClause.queryOne"),
    FqName("com.kotlinorm.orm.select.SelectClause.queryOneOrNull"),
    FqName("com.kotlinorm.database.SqlHandler.queryList"),
    FqName("com.kotlinorm.database.SqlHandler.queryOne"),
    FqName("com.kotlinorm.database.SqlHandler.queryOneOrNull")
)

private val fqNameOfSelectFromsRegexes = listOf(
    "com.kotlinorm.orm.join.SelectFrom\\d.queryList",
    "com.kotlinorm.orm.join.SelectFrom\\d.queryOne",
    "com.kotlinorm.orm.join.SelectFrom\\d.queryOneOrNull"
)

private val KPojoFqName = FqName("com.kotlinorm.interfaces.KPojo")

/**
 * Checks if the given FqName should be fixed by TypeParameterFixer
 */
fun FqName.shouldFix(): Boolean {
    return this in fqNameOfTypedQuery ||
            fqNameOfSelectFromsRegexes.any { Regex(it).matches(this.asString()) }
}

/**
 * Type Parameter Fixer
 *
 * Fixes type parameters for typed query calls (`queryList`, `queryOne`, `queryOneOrNull`)
 * on `KronosQueryTask`, `SelectClause`, `SelectFrom*`, and `SqlHandler` by injecting
 * the `isKPojo` flag and the `superTypes` list at compile time.
 *
 * This allows the runtime to determine whether the reified type argument implements
 * `KPojo` and to access its full type hierarchy without reflection.
 */
object TypeParameterFixer {

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    fun shouldFix(expression: IrCall): Boolean {
        if (expression.typeArguments.size != 1) return false
        val fqName = expression.symbol.owner.kotlinFqName
        return fqName in fqNameOfTypedQuery ||
                fqNameOfSelectFromsRegexes.any { Regex(it).matches(fqName.asString()) }
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    fun fix(pluginContext: IrPluginContext, expression: IrCall): IrExpression {
        val queryType = expression.typeArguments[0] ?: return expression
        val allTypes = (listOf(queryType) + queryType.superTypes()).mapNotNull { it.classFqName }
        val isKPojo = KPojoFqName in allTypes

        val irSuperTypes = with(pluginContext) {
            DeclarationIrBuilder(pluginContext, expression.symbol).irBlock {
                irListOf(
                    pluginContext.irBuiltIns.stringType,
                    allTypes.map { irString(it.asString()) }
                )
            }
        }

        // Last two arguments: isKPojo, superTypes
        expression.arguments[expression.arguments.size - 2] =
            DeclarationIrBuilder(pluginContext, expression.symbol).irBoolean(isKPojo)
        expression.arguments[expression.arguments.size - 1] =
            with(pluginContext) {
                DeclarationIrBuilder(pluginContext, expression.symbol).run {
                    irListOf(
                        pluginContext.irBuiltIns.stringType,
                        allTypes.map { irString(it.asString()) }
                    )
                }
            }

        return expression
    }
}
