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

package com.kotlinorm.compiler.utils

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.name.FqName

/**
 * Maps an exact Kotlin callable to the SQL function used when a DSL-specific lowering allows it.
 */
internal data class KotlinSqlFunctionRule(
    val callableFqName: FqName,
    val sqlFunctionName: String,
    val extensionReceiverFqName: FqName,
    val valueParameterCount: Int = 0
)

private val KotlinSqlFunctionRules = listOf(
    KotlinSqlFunctionRule(FqName("kotlin.text.uppercase"), "upper", StringFqName),
    KotlinSqlFunctionRule(FqName("kotlin.text.lowercase"), "lower", StringFqName)
)

@OptIn(UnsafeDuringIrConstructionAPI::class)
internal fun IrCall.kotlinSqlFunctionRuleOrNull(): KotlinSqlFunctionRule? {
    val function = symbol.owner
    return KotlinSqlFunctionRules.firstOrNull {
        function.kotlinFqName == it.callableFqName &&
            function.parameters.extensionReceiver?.type?.classFqName == it.extensionReceiverFqName &&
            function.parameters.valueParameters.size == it.valueParameterCount
    }
}

/**
 * Identifies the condition DSL's explicit runtime-value escape hatch by its resolved owner.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
internal fun IrCall.isKronosConditionValueAccess(): Boolean {
    val property = symbol.owner.correspondingPropertySymbol?.owner ?: return false
    val owner = property.parent as? IrClass ?: return false
    return property.name.asString() == "value" && owner.kotlinFqName == KTableForConditionFqName
}
