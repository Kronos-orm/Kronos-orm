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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class GradleGeneratedTypeProviderIdentityTest {
    @Test
    fun namingRuleUsesStableSha256Prefix() {
        val identity = gradleGeneratedTypeProviderIdentity("kronos:shared:coordinate")

        assertEquals("kronos:shared:coordinate#e3673cf3b65e57f4", identity.id)
        assertEquals(
            "com.kotlinorm.generated.factory.KronosGeneratedTypeProvider_e3673cf3b65e57f4",
            identity.fqName
        )
    }

    @Test
    fun compilerOptionsAndServiceUseTheSameIdentity() {
        val identity = gradleGeneratedTypeProviderIdentity("gradle:com.example:sample::main")

        assertEquals(identity.id, identity.compilerOptions[GeneratedProviderIdOptionName])
        assertEquals(identity.fqName, identity.compilerOptions[GeneratedProviderFqNameOptionName])
        assertEquals("${identity.fqName}\n", identity.serviceContent)
    }

    @Test
    fun twoModuleServiceEntriesRemainDistinctWhenMerged() {
        val first = gradleGeneratedTypeProviderIdentity("gradle:com.example:sample::first:main")
        val second = gradleGeneratedTypeProviderIdentity("gradle:com.example:sample::second:main")

        assertNotEquals(first.id, second.id)
        assertNotEquals(first.fqName, second.fqName)
        assertEquals(
            listOf(first.fqName, second.fqName),
            (first.serviceContent + second.serviceContent).lineSequence().filter(String::isNotBlank).toList()
        )
    }
}
