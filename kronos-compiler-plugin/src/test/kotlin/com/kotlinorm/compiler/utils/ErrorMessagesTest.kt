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
import kotlin.test.assertContains

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
        assertContains(ErrorMessages.CANNOT_EXTRACT_FIELD_ISNULL, "isNull()")
        assertContains(ErrorMessages.CANNOT_EXTRACT_FIELD_ISNULL, "KPojo")
    }

    @Test
    fun `CANNOT_EXTRACT_FIELD_NOTNULL mentions notNull`() {
        assertContains(ErrorMessages.CANNOT_EXTRACT_FIELD_NOTNULL, "notNull()")
        assertContains(ErrorMessages.CANNOT_EXTRACT_FIELD_NOTNULL, "KPojo")
    }

    @Test
    fun `CANNOT_BUILD_EQUALITY includes suggestion`() {
        assertContains(ErrorMessages.CANNOT_BUILD_EQUALITY, "Suggestion")
        assertContains(ErrorMessages.CANNOT_BUILD_EQUALITY, "KPojo property")
    }

    @Test
    fun `constant receiver messages contain function names`() {
        assertContains(ErrorMessages.MISSING_RECEIVER_NOT_CALL, "not()")
        assertContains(ErrorMessages.MISSING_RECEIVER_ISNULL, "isNull()")
        assertContains(ErrorMessages.MISSING_RECEIVER_NOTNULL, "notNull()")
        assertContains(ErrorMessages.MISSING_RECEIVER_EQ, "eq()")
        assertContains(ErrorMessages.MISSING_RECEIVER_NEQ, "neq()")
        assertContains(ErrorMessages.MISSING_RECEIVER_STARTSWITH, "startsWith()")
        assertContains(ErrorMessages.MISSING_RECEIVER_ENDSWITH, "endsWith()")
        assertContains(ErrorMessages.MISSING_RECEIVER_CONTAINS, "contains()")
        assertContains(ErrorMessages.MISSING_RECEIVER_ASSQL, "asSql()")
        assertContains(ErrorMessages.MISSING_RECEIVER_IFNOVALUE, "ifNoValue()")
        assertContains(ErrorMessages.MISSING_RECEIVER_TAKEIF, "takeIf()")
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
        assertContains(msg, "myFunc()")
        assertContains(msg, "KPojo property access")
    }

    @Test
    fun `missingOperandFor formats operator`() {
        val msg = ErrorMessages.missingOperandFor("<")
        assertEquals("Missing operand for '<' comparison", msg)
    }

    @Test
    fun `unsupportedConditionExprType includes type and IR dump`() {
        val msg = ErrorMessages.unsupportedConditionExprType("IrCall", "CALL 'foo'")
        assertContains(msg, "IrCall")
        assertContains(msg, "CALL 'foo'")
        assertContains(msg, "Suggestion")
    }

    @Test
    fun `unsupportedConditionExprType handles null type`() {
        val msg = ErrorMessages.unsupportedConditionExprType(null, "dump")
        assertContains(msg, "null")
        assertContains(msg, "dump")
    }

    @Test
    fun `debugAnalyzeCallCriteria formats origin and funcName`() {
        val msg = ErrorMessages.debugAnalyzeCallCriteria("ORIGIN", "myFunc")
        assertContains(msg, "ORIGIN")
        assertContains(msg, "myFunc")
    }

    @Test
    fun `debugAnalyzeWhenCriteria formats result and condition dumps`() {
        val msg = ErrorMessages.debugAnalyzeWhenCriteria("resultDump", "condDump")
        assertContains(msg, "resultDump")
        assertContains(msg, "condDump")
    }

    @Test
    fun `unrecognizedConditionFunction includes suggestion`() {
        val msg = ErrorMessages.unrecognizedConditionFunction("fooBar")
        assertContains(msg, "fooBar")
        assertContains(msg, "Suggestion")
        assertContains(msg, "ignored")
    }

    @Test
    fun `kpojoNoColumnProperties formats class name`() {
        val msg = ErrorMessages.kpojoNoColumnProperties("com.example.User")
        assertContains(msg, "com.example.User")
        assertContains(msg, "no column properties")
    }

    @Test
    fun `cannotFindClassForProperty formats property and receiver`() {
        val msg = ErrorMessages.cannotFindClassForProperty("username", "String")
        assertContains(msg, "username")
        assertContains(msg, "String")
    }

    @Test
    fun `cannotFindClassForProperty handles null receiver`() {
        val msg = ErrorMessages.cannotFindClassForProperty("username", null)
        assertContains(msg, "username")
        assertContains(msg, "unknown")
    }

    @Test
    fun `cannotFindPropertyInClass formats all parameters`() {
        val msg = ErrorMessages.cannotFindPropertyInClass("age", "User", "id, name")
        assertContains(msg, "age")
        assertContains(msg, "User")
        assertContains(msg, "id, name")
    }

    @Test
    fun `cannotFindClassForMinus formats receiver type`() {
        val msg = ErrorMessages.cannotFindClassForMinus("Int")
        assertContains(msg, "Int")
        assertContains(msg, "minus operation")
    }

    @Test
    fun `cannotFindClassForMinus handles null receiver`() {
        val msg = ErrorMessages.cannotFindClassForMinus(null)
        assertContains(msg, "unknown")
    }

    @Test
    fun `noColumnPropertiesForMinus formats class name`() {
        val msg = ErrorMessages.noColumnPropertiesForMinus("User")
        assertContains(msg, "User")
        assertContains(msg, "No column properties")
    }

    @Test
    fun `failedToTransformCondition formats message`() {
        val msg = ErrorMessages.failedToTransformCondition("some error")
        assertEquals("Failed to transform condition expression: some error", msg)
    }

    @Test
    fun `failedToTransformCondition handles null message`() {
        val msg = ErrorMessages.failedToTransformCondition(null)
        assertContains(msg, "null")
    }

    @Test
    fun `kpojoNoNoArgConstructor formats fqName`() {
        val msg = ErrorMessages.kpojoNoNoArgConstructor("com.example.User")
        assertContains(msg, "com.example.User")
        assertContains(msg, "no no-arg constructor")
    }
}
