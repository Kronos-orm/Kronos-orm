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

import kotlin.test.Test
import kotlin.test.assertEquals

class ErrorMessagesTest {

    // ========================================================================
    // Constant message tests
    // ========================================================================

    @Test
    fun `MISSING_LEFT_OPERAND_EQ contains eq context`() {
        assertEquals("Missing left operand in == expression", ErrorMessages.MISSING_LEFT_OPERAND_EQ)
    }

    @Test
    fun `MISSING_RIGHT_OPERAND_EQ contains eq context`() {
        assertEquals("Missing right operand in == expression", ErrorMessages.MISSING_RIGHT_OPERAND_EQ)
    }

    @Test
    fun `MISSING_LEFT_OPERAND_NEQ contains neq context`() {
        assertEquals("Missing left operand in != expression", ErrorMessages.MISSING_LEFT_OPERAND_NEQ)
    }

    @Test
    fun `MISSING_RIGHT_OPERAND_NEQ contains neq context`() {
        assertEquals("Missing right operand in != expression", ErrorMessages.MISSING_RIGHT_OPERAND_NEQ)
    }

    @Test
    fun `MISSING_RECEIVER_NOT contains not context`() {
        assertEquals("Missing receiver in ! (not) expression", ErrorMessages.MISSING_RECEIVER_NOT)
    }

    @Test
    fun `CANNOT_EXTRACT_FIELD_ISNULL mentions isNull`() {
        assertEquals(
            "Cannot extract field from 'isNull()' receiver. Only KPojo property access is supported.",
            ErrorMessages.CANNOT_EXTRACT_FIELD_ISNULL
        )
    }

    @Test
    fun `CANNOT_EXTRACT_FIELD_NOTNULL mentions notNull`() {
        assertEquals(
            "Cannot extract field from 'notNull()' receiver. Only KPojo property access is supported.",
            ErrorMessages.CANNOT_EXTRACT_FIELD_NOTNULL
        )
    }

    @Test
    fun `CANNOT_BUILD_EQUALITY includes suggestion`() {
        assertEquals(
            "Cannot build equality condition: neither side is a KPojo property access.\n" +
                "Suggestion: Use 'it.field == value' for explicit values, or 'it.field.eq' / 'it.eq' for current KPojo values.",
            ErrorMessages.CANNOT_BUILD_EQUALITY
        )
    }

    @Test
    fun `CANNOT_RESOLVE_KPOJO_CLASS contains equality context`() {
        assertEquals("Cannot resolve KPojo class for equality comparison.", ErrorMessages.CANNOT_RESOLVE_KPOJO_CLASS)
    }

    @Test
    fun `constant receiver messages contain function names`() {
        assertEquals(
            listOf(
                "Missing receiver for 'not()' call",
                "Missing receiver for 'isNull()' call",
                "Missing receiver for 'notNull()' call",
                "Missing receiver for 'eq' condition",
                "Missing receiver for 'neq' condition",
                "Missing receiver for 'startsWith()' call",
                "Missing receiver for 'endsWith()' call",
                "Missing receiver for 'contains()' call",
                "Missing receiver for 'asSql()' call",
                "Missing receiver for 'ifNoValue()' call",
                "Missing receiver for 'takeIf()' call"
            ),
            listOf(
                ErrorMessages.MISSING_RECEIVER_NOT_CALL,
                ErrorMessages.MISSING_RECEIVER_ISNULL,
                ErrorMessages.MISSING_RECEIVER_NOTNULL,
                ErrorMessages.MISSING_RECEIVER_EQ,
                ErrorMessages.MISSING_RECEIVER_NEQ,
                ErrorMessages.MISSING_RECEIVER_STARTSWITH,
                ErrorMessages.MISSING_RECEIVER_ENDSWITH,
                ErrorMessages.MISSING_RECEIVER_CONTAINS,
                ErrorMessages.MISSING_RECEIVER_ASSQL,
                ErrorMessages.MISSING_RECEIVER_IFNOVALUE,
                ErrorMessages.MISSING_RECEIVER_TAKEIF
            )
        )
    }

    // ========================================================================
    // Parameterized function tests
    // ========================================================================

    @Test
    fun `missingReceiverFor formats function name`() {
        val msg = ErrorMessages.missingReceiverFor("between")
        assertEquals("Missing receiver for 'between()' call", msg)
    }

    @Test
    fun `missingRangeArgFor formats function name`() {
        val msg = ErrorMessages.missingRangeArgFor("between")
        assertEquals("Missing range argument for 'between()' call", msg)
    }

    @Test
    fun `cannotExtractFieldFor formats function name`() {
        val msg = ErrorMessages.cannotExtractFieldFor("myFunc")
        assertEquals(
            "Cannot extract field from 'myFunc()' receiver. Only KPojo property access is supported.",
            msg
        )
    }

    @Test
    fun `missingOperandFor formats operator`() {
        val msg = ErrorMessages.missingOperandFor("<")
        assertEquals("Missing operand for '<' comparison", msg)
    }

    @Test
    fun `unsupportedConditionExprType includes type and IR dump`() {
        val msg = ErrorMessages.unsupportedConditionExprType("IrCall", "CALL 'foo'")
        assertEquals(
            "Unsupported condition expression type: IrCall\n" +
                "IR dump: CALL 'foo'\n" +
                "Suggestion: Use supported condition operators (==, !=, &&, ||, isNull, between, like, etc.)",
            msg
        )
    }

    @Test
    fun `unsupportedConditionExprType handles null type`() {
        val msg = ErrorMessages.unsupportedConditionExprType(null, "dump")
        assertEquals(
            "Unsupported condition expression type: null\n" +
                "IR dump: dump\n" +
                "Suggestion: Use supported condition operators (==, !=, &&, ||, isNull, between, like, etc.)",
            msg
        )
    }

    @Test
    fun `debugAnalyzeCallCondition formats origin and funcName`() {
        val msg = ErrorMessages.debugAnalyzeCallCondition("ORIGIN", "myFunc")
        assertEquals("analyzeCallSqlExpr else: origin=ORIGIN, funcName=myFunc", msg)
    }

    @Test
    fun `unrecognizedConditionFunction includes suggestion`() {
        val msg = ErrorMessages.unrecognizedConditionFunction("fooBar")
        assertEquals(
            "Unrecognized condition function 'fooBar'. This call will be ignored in the generated SQL.\n" +
                "Suggestion: Use Kotlin comparison operators (==, !=, >, >=, <, <=), no-arg condition properties (eq, neq, lt, gt, le, ge), or supported condition functions (isNull, notNull, between, like, contains, asSql, etc.)",
            msg
        )
    }

    @Test
    fun `parameterizedConditionFunctionUnsupported recommends Kotlin comparisons`() {
        val msg = ErrorMessages.parameterizedConditionFunctionUnsupported("eq")
        assertEquals(
            "Parameterized condition function 'eq(...)' is not supported.\n" +
                "Suggestion: Use Kotlin comparison operators (==, !=, >, >=, <, <=) for explicit values, or '.eq' only when comparing with current KPojo values.",
            msg
        )
    }

    @Test
    fun `kpojoNoColumnProperties formats class name`() {
        val msg = ErrorMessages.kpojoNoColumnProperties("com.example.User")
        assertEquals("KPojo class 'com.example.User' has no column properties for equality comparison.", msg)
    }

    @Test
    fun `cannotFindClassForProperty formats property and receiver`() {
        val msg = ErrorMessages.cannotFindClassForProperty("username", "String")
        assertEquals(
            "Cannot find class for property access: username. Receiver type: String. This usually means the property is accessed on a non-KPojo type.",
            msg
        )
    }

    @Test
    fun `cannotFindClassForProperty handles null receiver`() {
        val msg = ErrorMessages.cannotFindClassForProperty("username", null)
        assertEquals(
            "Cannot find class for property access: username. Receiver type: unknown. This usually means the property is accessed on a non-KPojo type.",
            msg
        )
    }

    @Test
    fun `cannotFindPropertyInClass formats all parameters`() {
        val msg = ErrorMessages.cannotFindPropertyInClass("age", "User", "id, name")
        assertEquals(
            "Cannot find property 'age' in class User. Available properties: id, name. Make sure the property exists and is accessible.",
            msg
        )
    }

    @Test
    fun `cannotFindClassForMinus formats receiver type`() {
        val msg = ErrorMessages.cannotFindClassForMinus("Int")
        assertEquals(
            "Cannot find class for minus operation. Receiver type: Int. The minus operation (it - User::field) requires a KPojo instance on the left side.",
            msg
        )
    }

    @Test
    fun `cannotFindClassForMinus handles null receiver`() {
        val msg = ErrorMessages.cannotFindClassForMinus(null)
        assertEquals(
            "Cannot find class for minus operation. Receiver type: unknown. The minus operation (it - User::field) requires a KPojo instance on the left side.",
            msg
        )
    }

    @Test
    fun `noColumnPropertiesForMinus formats class name`() {
        val msg = ErrorMessages.noColumnPropertiesForMinus("User")
        assertEquals("No column properties found in class User for minus operation. Make sure the class has properties annotated as columns.", msg)
    }

    @Test
    fun `failedToTransformCondition formats message`() {
        val msg = ErrorMessages.failedToTransformCondition("some error")
        assertEquals("Failed to transform condition expression: some error", msg)
    }

    @Test
    fun `failedToTransformCondition handles null message`() {
        val msg = ErrorMessages.failedToTransformCondition(null)
        assertEquals("Failed to transform condition expression: null", msg)
    }

    @Test
    fun `kpojoNoNoArgConstructor formats fqName`() {
        val msg = ErrorMessages.kpojoNoNoArgConstructor("com.example.User")
        assertEquals(
            "KPojo class 'com.example.User' has no no-arg constructor and will be excluded from generated KPojo factories.",
            msg
        )
    }
}
