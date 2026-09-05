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

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConstantsTest {
    @Test
    fun `recognizes every supported join source arity and rejects nearby class ids`() {
        assertTrue(ClassId.topLevel(FqName("com.kotlinorm.orm.join.JoinSource")).isJoinSourceClassId())
        assertTrue(ClassId.topLevel(FqName("com.kotlinorm.orm.join.JoinSource2")).isJoinSourceClassId())
        assertTrue(ClassId.topLevel(FqName("com.kotlinorm.orm.join.JoinSource16")).isJoinSourceClassId())
        assertFalse(ClassId.topLevel(FqName("com.kotlinorm.orm.join.JoinSource1")).isJoinSourceClassId())
        assertFalse(ClassId.topLevel(FqName("com.kotlinorm.orm.join.JoinSource17")).isJoinSourceClassId())
        assertFalse(ClassId.topLevel(FqName("com.kotlinorm.orm.join.JoinSourceX")).isJoinSourceClassId())
        assertFalse(ClassId.topLevel(FqName("com.kotlinorm.other.JoinSource2")).isJoinSourceClassId())
    }
}
