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

/**
 * Error and warning message constants for the Kronos compiler plugin.
 *
 * All compiler diagnostic messages are centralized here for consistency and maintainability.
 */
object ErrorMessages {

    // ========================================================================
    // ConditionAnalysis - Missing operands in binary expressions
    // ========================================================================
    const val MISSING_LEFT_OPERAND_EQ = "Missing left operand in == expression"
    const val MISSING_RIGHT_OPERAND_EQ = "Missing right operand in == expression"
    const val MISSING_LEFT_OPERAND_NEQ = "Missing left operand in != expression"
    const val MISSING_RIGHT_OPERAND_NEQ = "Missing right operand in != expression"
    const val MISSING_RECEIVER_NOT = "Missing receiver in ! (not) expression"

    // ========================================================================
    // ConditionAnalysis - Field extraction failures
    // ========================================================================
    const val CANNOT_EXTRACT_FIELD_ISNULL =
        "Cannot extract field from 'isNull()' receiver. Only KPojo property access is supported."
    const val CANNOT_EXTRACT_FIELD_NOTNULL =
        "Cannot extract field from 'notNull()' receiver. Only KPojo property access is supported."

    // ========================================================================
    // ConditionAnalysis - Equality
    // ========================================================================
    const val CANNOT_BUILD_EQUALITY =
        "Cannot build equality condition: neither side is a KPojo property access.\n" +
        "Suggestion: Use 'it.field == value' or 'it.field.eq(value)' syntax."
    const val CANNOT_RESOLVE_KPOJO_CLASS = "Cannot resolve KPojo class for equality comparison."

    // ========================================================================
    // ConditionAnalysis - Missing receivers for method calls
    // ========================================================================
    const val MISSING_RECEIVER_NOT_CALL = "Missing receiver for 'not()' call"
    const val MISSING_RECEIVER_ISNULL = "Missing receiver for 'isNull()' call"
    const val MISSING_RECEIVER_NOTNULL = "Missing receiver for 'notNull()' call"
    const val MISSING_RECEIVER_EQ = "Missing receiver for 'eq()' call"
    const val MISSING_RECEIVER_NEQ = "Missing receiver for 'neq()' call"
    const val MISSING_RECEIVER_STARTSWITH = "Missing receiver for 'startsWith()' call"
    const val MISSING_RECEIVER_ENDSWITH = "Missing receiver for 'endsWith()' call"
    const val MISSING_RECEIVER_CONTAINS = "Missing receiver for 'contains()' call"
    const val MISSING_RECEIVER_ASSQL = "Missing receiver for 'asSql()' call"
    const val MISSING_RECEIVER_IFNOVALUE = "Missing receiver for 'ifNoValue()' call"
    const val MISSING_RECEIVER_TAKEIF = "Missing receiver for 'takeIf()' call"

    // ========================================================================
    // ConditionAnalysis - Parameterized missing receiver/argument messages
    // ========================================================================
    fun missingReceiverFor(funcName: String) = "Missing receiver for '$funcName()' call"
    fun missingRangeArgFor(funcName: String) = "Missing range argument for '$funcName()' call"
    fun cannotExtractFieldFor(funcName: String) =
        "Cannot extract field from '$funcName()' receiver. Only KPojo property access is supported."
    fun missingOperandFor(operator: String) = "Missing operand for '$operator' comparison"

    // ========================================================================
    // ConditionAnalysis - Warnings
    // ========================================================================
    fun unsupportedConditionExprType(typeName: String?, irDump: String) =
        "Unsupported condition expression type: $typeName\n" +
        "IR dump: $irDump\n" +
        "Suggestion: Use supported condition operators (==, !=, &&, ||, isNull, between, like, etc.)"

    fun debugAnalyzeCallCriteria(origin: Any?, funcName: String) =
        "analyzeCallCriteria else: origin=$origin, funcName=$funcName"

    fun debugAnalyzeWhenCriteria(resultDump: String, condDump: String) =
        "analyzeWhenCriteria: branch.result=$resultDump branch.condition=$condDump"

    fun unrecognizedConditionFunction(funcName: String) =
        "Unrecognized condition function '$funcName'. This call will be ignored in the generated SQL.\n" +
        "Suggestion: Use supported condition functions (eq, neq, lt, gt, le, ge, isNull, notNull, between, like, contains, asSql, etc.)"

    fun kpojoNoColumnProperties(className: Any?) =
        "KPojo class '$className' has no column properties for equality comparison."

    // ========================================================================
    // FieldAnalysis - Warnings
    // ========================================================================
    fun cannotFindClassForProperty(propertyName: String, receiverType: String?) =
        "Cannot find class for property access: $propertyName. " +
        "Receiver type: ${receiverType ?: "unknown"}. " +
        "This usually means the property is accessed on a non-KPojo type."

    fun cannotFindPropertyInClass(propertyName: String, className: String, availableProps: String) =
        "Cannot find property '$propertyName' in class $className. " +
        "Available properties: $availableProps. " +
        "Make sure the property exists and is accessible."

    fun cannotFindClassForMinus(receiverType: String?) =
        "Cannot find class for minus operation. " +
        "Receiver type: ${receiverType ?: "unknown"}. " +
        "The minus operation (it - User::field) requires a KPojo instance on the left side."

    fun noColumnPropertiesForMinus(className: String) =
        "No column properties found in class $className for minus operation. " +
        "Make sure the class has properties annotated as columns."

    // ========================================================================
    // ConditionTransformer
    // ========================================================================
    fun failedToTransformCondition(message: String?) =
        "Failed to transform condition expression: $message"

    // ========================================================================
    // KClassMapGenerator
    // ========================================================================
    fun kpojoNoNoArgConstructor(fqName: Any?) =
        "KPojo class '$fqName' has no no-arg constructor and will be excluded from kClassCreator."
}
