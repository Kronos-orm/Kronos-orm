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
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.name.FqName

internal enum class KotlinSqlFunctionReceiverKind {
    Extension,
    Dispatch
}

internal enum class KotlinSqlFunctionKind {
    Direct,
    SubstringFrom,
    SubstringRange
}

/**
 * Maps an exact Kotlin callable to the SQL function used when a DSL-specific lowering allows it.
 */
internal data class KotlinSqlFunctionRule(
    val callableFqName: FqName,
    val sqlFunctionName: String,
    val receiverFqName: FqName,
    val receiverKind: KotlinSqlFunctionReceiverKind = KotlinSqlFunctionReceiverKind.Extension,
    val valueParameterCount: Int = 0,
    val sqlValueArgumentCount: Int = valueParameterCount,
    val kind: KotlinSqlFunctionKind = KotlinSqlFunctionKind.Direct,
    val propertyGetter: Boolean = false,
    val requiresFalseTrailingArgument: Boolean = false
)

private val CharSequenceFqName = FqName("kotlin.CharSequence")

private val KotlinSqlFunctionRules = listOf(
    KotlinSqlFunctionRule(FqName("kotlin.text.uppercase"), "upper", StringFqName),
    KotlinSqlFunctionRule(FqName("kotlin.text.lowercase"), "lower", StringFqName),
    KotlinSqlFunctionRule(
        callableFqName = FqName("kotlin.String.length"),
        sqlFunctionName = "length",
        receiverFqName = StringFqName,
        receiverKind = KotlinSqlFunctionReceiverKind.Dispatch,
        propertyGetter = true
    ),
    KotlinSqlFunctionRule(FqName("kotlin.text.count"), "length", CharSequenceFqName),
    KotlinSqlFunctionRule(
        callableFqName = FqName("kotlin.text.replace"),
        sqlFunctionName = "replace",
        receiverFqName = StringFqName,
        valueParameterCount = 3,
        sqlValueArgumentCount = 2,
        requiresFalseTrailingArgument = true
    ),
    KotlinSqlFunctionRule(
        callableFqName = FqName("kotlin.text.substring"),
        sqlFunctionName = "substr",
        receiverFqName = StringFqName,
        valueParameterCount = 1,
        kind = KotlinSqlFunctionKind.SubstringFrom
    ),
    KotlinSqlFunctionRule(
        callableFqName = FqName("kotlin.text.substring"),
        sqlFunctionName = "substr",
        receiverFqName = StringFqName,
        valueParameterCount = 2,
        kind = KotlinSqlFunctionKind.SubstringRange
    ),
    KotlinSqlFunctionRule(
        callableFqName = FqName("kotlin.String.subSequence"),
        sqlFunctionName = "substr",
        receiverFqName = StringFqName,
        receiverKind = KotlinSqlFunctionReceiverKind.Dispatch,
        valueParameterCount = 2,
        kind = KotlinSqlFunctionKind.SubstringRange
    ),
    KotlinSqlFunctionRule(FqName("kotlin.text.take"), "left", StringFqName, valueParameterCount = 1),
    KotlinSqlFunctionRule(FqName("kotlin.text.takeLast"), "right", StringFqName, valueParameterCount = 1)
)

@OptIn(UnsafeDuringIrConstructionAPI::class)
internal fun IrCall.kotlinSqlFunctionRuleOrNull(): KotlinSqlFunctionRule? {
    val function = symbol.owner
    return KotlinSqlFunctionRules.firstOrNull {
        val callableFqName = if (it.propertyGetter) {
            function.correspondingPropertySymbol?.owner?.let { property ->
                (property.parent as? IrClass)?.kotlinFqName?.child(property.name)
            }
        } else {
            function.kotlinFqName
        }
        val receiverType = when (it.receiverKind) {
            KotlinSqlFunctionReceiverKind.Extension -> function.parameters.extensionReceiver?.type
            KotlinSqlFunctionReceiverKind.Dispatch -> function.parameters.dispatchReceiver?.type
        }
        callableFqName == it.callableFqName &&
            receiverType?.classFqName == it.receiverFqName &&
            function.parameters.valueParameters.size == it.valueParameterCount &&
            (!it.requiresFalseTrailingArgument || getValueArgumentSafe(it.sqlValueArgumentCount).isOmittedOrFalse())
    }
}

internal fun IrCall.kotlinSqlFunctionReceiverArgument(rule: KotlinSqlFunctionRule): IrExpression? =
    when (rule.receiverKind) {
        KotlinSqlFunctionReceiverKind.Extension -> extensionReceiverArgument
        KotlinSqlFunctionReceiverKind.Dispatch -> dispatchReceiverArgument
    }

private fun IrExpression?.isOmittedOrFalse(): Boolean =
    this == null || (this is IrConst && value == false)

/**
 * Identifies the condition DSL's explicit runtime-value escape hatch by its resolved owner.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
internal fun IrCall.isKronosConditionValueAccess(): Boolean {
    val property = symbol.owner.correspondingPropertySymbol?.owner ?: return false
    val owner = property.parent as? IrClass ?: return false
    return property.name.asString() == "value" && owner.kotlinFqName == KTableForConditionFqName
}
