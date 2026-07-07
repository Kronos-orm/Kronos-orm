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

package com.kotlinorm.compiler.fir

import com.kotlinorm.compiler.utils.KotlinListOfFunctionName
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirCollectionLiteral
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.expressions.FirVarargArgumentsExpression
import org.jetbrains.kotlin.fir.expressions.FirWrappedExpression

internal fun FirBlock.lastExpression(): FirStatement? {
    val statement = statements.lastOrNull() ?: return null
    return (statement as? FirReturnExpression)?.result ?: statement
}

internal fun FirStatement.projectionStatementOrNull(): FirStatement? {
    var statement = this
    while (true) {
        statement = when (statement) {
            is FirReturnExpression -> statement.result
            is FirBlock -> statement.lastExpression() ?: return null
            is FirWrappedExpression -> statement.expression
            else -> return statement
        }
    }
}

internal fun FirStatement.projectionItems(includeSingle: Boolean = false): List<FirStatement> {
    val statement = projectionStatementOrNull() ?: return emptyList()
    statement.projectionCollectionItemsOrNull()?.let { return it }
    return if (includeSingle) listOf(statement) else emptyList()
}

internal fun FirStatement.collectionLiteralItemsOrNull(): List<FirStatement>? {
    val statement = projectionStatementOrNull() ?: return null
    return statement.projectionCollectionItemsOrNull()
}

private fun FirStatement.projectionCollectionItemsOrNull(): List<FirStatement>? =
    when (this) {
        is FirCollectionLiteral -> argumentList.arguments
        is FirFunctionCall -> {
            val arguments = argumentList.arguments
            arguments.singleVarargArgumentsOrNull()
                ?: arguments.takeIf { calleeReference.name == KotlinListOfFunctionName }
        }
        else -> null
    }

internal fun FirStatement.stringLiteralValue(): String? {
    val literal = projectionStatementOrNull() as? FirLiteralExpression ?: return null
    return literal.value as? String
}

private fun List<FirStatement>.singleVarargArgumentsOrNull(): List<FirStatement>? =
    (singleOrNull() as? FirVarargArgumentsExpression)?.arguments
