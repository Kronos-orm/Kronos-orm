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

package com.kotlinorm.compiler.backend.transformers

import com.kotlinorm.compiler.utils.KPojoFqName
import com.kotlinorm.compiler.utils.TypedQueryFunctionFqNames
import com.kotlinorm.compiler.utils.irListOf
import com.kotlinorm.compiler.utils.isSelectFromQueryFunctionFqName
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBoolean
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.superTypes
import org.jetbrains.kotlin.name.FqName

/**
 * Checks if the given FqName should be fixed by TypeParameterFixer
 */
fun FqName.shouldFix(): Boolean {
    return this in TypedQueryFunctionFqNames ||
            isSelectFromQueryFunctionFqName(this.asString())
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
        if (expression.typeArguments.none { it != null }) return false
        val fqName = expression.symbol.owner.kotlinFqName
        return fqName.shouldFix()
    }

    /**
     * Injects the KPojo marker flag and supertype FQNs into typed query runtime arguments.
     */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    fun fix(pluginContext: IrPluginContext, expression: IrCall, knownKPojoClasses: Set<IrClass> = emptySet()): IrExpression {
        val queryType = expression.queryMappingType(knownKPojoClasses) ?: return expression
        val allTypes = ([queryType] + queryType.superTypes()).mapNotNull { it.classFqName }
        val isKPojo = queryType.isKPojoMappingType(knownKPojoClasses)
        val injectedTypes = if (isKPojo && KPojoFqName !in allTypes) {
            allTypes + KPojoFqName
        } else {
            allTypes
        }

        // Last two arguments: isKPojo, superTypes
        expression.arguments[expression.arguments.size - 2] =
            DeclarationIrBuilder(pluginContext, expression.symbol).irBoolean(isKPojo)
        expression.arguments[expression.arguments.size - 1] =
            with(pluginContext) {
                DeclarationIrBuilder(pluginContext, expression.symbol).run {
                    irListOf(
                        pluginContext.irBuiltIns.stringType,
                        injectedTypes.map { irString(it.asString()) }
                    )
                }
            }

        return expression
    }

    private fun IrCall.queryMappingType(knownKPojoClasses: Set<IrClass>): IrType? {
        val candidates = (typeArguments.filterNotNull() + type)
            .flatMap { it.flattenTypeArguments() }
        return candidates.firstOrNull { candidate ->
            candidate.isKPojoMappingType(knownKPojoClasses)
        } ?: candidates.lastOrNull()
    }

    private fun IrType.isKPojoMappingType(knownKPojoClasses: Set<IrClass>): Boolean {
        if (classFqName == KPojoFqName) return true
        if (superTypes().any { it.classFqName == KPojoFqName }) return true
        val klass = getClass() ?: return false
        if (klass in knownKPojoClasses) return true
        return klass.superTypes.any { it.classFqName == KPojoFqName }
    }

    private fun IrType.flattenTypeArguments(): List<IrType> {
        if (this !is IrSimpleType) return listOf(this)
        val nested = mutableListOf<IrType>()
        for (argument in arguments) {
            val type = (argument as? IrTypeProjection)?.type ?: continue
            nested += type.flattenTypeArguments()
        }
        return listOf(this) + nested
    }
}
