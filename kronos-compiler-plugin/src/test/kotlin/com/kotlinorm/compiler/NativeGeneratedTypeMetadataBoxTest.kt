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

package com.kotlinorm.compiler

import org.junit.jupiter.api.Test
import kotlin.test.assertFails
import kotlin.test.assertTrue

/** Native registration policy with the official FIR/IR verifier; Native linking is covered by TestKit. */
class NativeGeneratedTypeMetadataBoxTest : AbstractKronosJvmBoxTest() {
    override val nativeMetadataEntry: String = "fixture.installNativeMetadata"

    @Test
    fun explicitEntryRegistersFactoriesBeforeFreeze() {
        runTest("$testDataDir/box/nativeMetadata/explicitEntryRegistersFactories.kt")
    }

    @Test
    fun sourceAnchorKeepsProjectionMetadata() {
        runTest("$testDataDir/box/nativeMetadata/sourceAnchorKeepsProjectionMetadata.kt")
    }

    @Test
    fun missingGradleSourceEntryIsRejectedBeforeNativeLinking() {
        assertGenerationRejected("missingSourceEntry", "Native generated metadata entry 'fixture.installNativeMetadata' was not found")
    }

    @Test
    fun missingSourceProviderIsRejectedBeforeNativeLinking() {
        assertGenerationRejected("missingSourceProvider", "Native source provider 'fixture.installNativeMetadataProvider' was not found")
    }

    @Test
    fun missingDependencyAggregateCannotSilentlyOmitInitialization() {
        assertGenerationRejected("missingDependencyAggregate", "Missing generated Native dependency entry 'fixture.installNativeMetadataDependencies'")
    }

    private fun assertGenerationRejected(name: String, expected: String) {
        val failure = assertFails { runTest("$testDataDir/box/nativeMetadata/$name.kt") }
        assertTrue(failure.stackTraceToString().contains(expected), failure.stackTraceToString())
    }
}
