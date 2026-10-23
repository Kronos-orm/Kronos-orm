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

import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.cli.common.diagnosticsCollector
import org.jetbrains.kotlin.test.backend.handlers.JvmBinaryArtifactHandler
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureJvmArtifactsHandlersStep
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.CompilationStage
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

/** Checks normal callback execution and rejects every unexpected IR warning in these cases. */
class ConditionCallbackBoxTest : AbstractKronosJvmBoxSuite("condition") {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configureJvmArtifactsHandlersStep { useHandlers(::NoCallbackWarningsHandler) }
    }

    @Test
    fun unitCallbacksPreserveForwardedConditions() = box("unitCallbacksPreserveForwardedConditions")

    @Test
    fun nullConditionsRemainEmpty() = box("nullConditionsRemainEmpty")
}

private class NoCallbackWarningsHandler(testServices: TestServices) : JvmBinaryArtifactHandler(testServices) {
    override fun processModule(module: TestModule, info: BinaryArtifacts.Jvm) {
        checkArtifact(info)
        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module, CompilationStage.FIRST)
        val warnings = configuration.diagnosticsCollector.diagnostics.filter { it.severity == Severity.WARNING }
        assertTrue(warnings.isEmpty(), "Unexpected callback IR warnings: ${warnings.joinToString { it.factory.name }}")
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) = Unit
}
