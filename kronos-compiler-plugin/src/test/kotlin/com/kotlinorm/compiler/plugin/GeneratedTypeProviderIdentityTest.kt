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

package com.kotlinorm.compiler.plugin

import org.jetbrains.kotlin.name.FqName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals

class GeneratedTypeProviderIdentityTest {
    private val module = GeneratedTypeProviderConfiguration(
        "gradle:example:models:commonMain",
        FqName("com.kotlinorm.generated.factory.KronosGeneratedTypeProvider_example")
    )

    @Test
    fun repeatedSourceIdentityIsStableAndRenamingChangesBothPublicIdentities() {
        val original = module.fragmentFor("/workspace/models/Person.kt")
        assertEquals(original, module.fragmentFor("/workspace/models/Person.kt"))
        val renamed = module.fragmentFor("/workspace/models/Account.kt")
        assertNotEquals(original.id, renamed.id)
        assertNotEquals(original.fqName, renamed.fqName)
        assertFalse(original.id.contains("/workspace"))
        assertFalse(original.fqName.asString().contains("Person.kt"))
    }

    @Test
    fun sameSourceInAnotherModuleHasAnIndependentIdentity() {
        val other = module.copy(id = "gradle:example:other:commonMain",
            fqName = FqName("com.kotlinorm.generated.factory.KronosGeneratedTypeProvider_other"))
        assertNotEquals(module.fragmentFor("Person.kt"), other.fragmentFor("Person.kt"))
        assertFailsWith<IllegalArgumentException> { module.fragmentFor(" \n ") }
    }
}
