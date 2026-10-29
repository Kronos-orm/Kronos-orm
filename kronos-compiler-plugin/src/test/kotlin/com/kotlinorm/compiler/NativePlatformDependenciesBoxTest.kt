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

/** Verifies target-only aggregate entries are injected without common-source references or duplicate calls. */
class NativePlatformDependenciesBoxTest : AbstractKronosJvmBoxTest() {
    override val nativeMetadataEntry = "fixture.installNativeMetadata"
    override val nativeMetadataDependencyEntries = listOf(
        "fixture.installCommonDependency", "fixture.installTargetDependency",
        "fixture.installTargetDependency", "fixture.installNativeMetadataDependencies"
    )

    @Test
    fun targetDependencyEntriesJoinTheApplicationAggregate() {
        runTest("$testDataDir/box/nativeMetadata/targetDependencyEntries.kt")
    }

    @Test
    fun binaryTargetDependencyEntriesJoinTheApplicationAggregate() {
        runTest("$testDataDir/box/nativeMetadata/binaryTargetDependencyEntries.kt")
    }

    @Test
    fun missingTargetDependencyEntryIsRejectedBeforeNativeLinking() {
        assertGenerationRejected("missingTargetDependencyEntry",
            "Native dependency metadata entry 'fixture.installTargetDependency' was not found in this target compilation")
    }

    @Test
    fun missingTargetDependencyAggregateIsRejectedBeforeNativeLinking() {
        assertGenerationRejected("missingDependencyAggregate",
            "Missing generated Native dependency entry 'fixture.installNativeMetadataDependencies'")
    }

    private fun assertGenerationRejected(name: String, expected: String) {
        val failure = assertFails { runTest("$testDataDir/box/nativeMetadata/$name.kt") }
        assertTrue(failure.stackTraceToString().contains(expected), failure.stackTraceToString())
    }
}
