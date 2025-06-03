/**
 * Copyright 2022-2025 kronos-orm
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

package com.kotlinorm.enums

/**
 * Condition types.
 *
 * @author ousc
 */
enum class ConditionType(val value: String) {
    LIKE("like"),
    EQUAL("="),
    IN("in"),
    ISNULL("is null"),
    SQL(""),
    GT(">"),
    GE(">="),
    LT("<"),
    LE("<="),
    BETWEEN("between"),
    REGEXP("regexp"),
    AND("and"),
    OR("or"),
    ROOT("");

    companion object {
        val Root = ROOT
        val Like = LIKE
        val Equal = EQUAL
        val In = IN
        val IsNull = ISNULL
        val Sql = SQL
        val Gt = GT
        val Ge = GE
        val Lt = LT
        val Le = LE
        val Regexp = REGEXP
        val Between = BETWEEN
        val And = AND
        val Or = OR
    }
}