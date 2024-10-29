/**
 * Copyright 2022-2024 kronos-orm
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

package com.kotlinorm.compiler.fir

import com.kotlinorm.compiler.fir.transformer.KronosParserTransformer
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.*
import java.io.File

open class KronosParserExtension(val debug: Boolean, val debugInfoPath: String) : IrGenerationExtension {

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        moduleFragment.transform(KronosParserTransformer(pluginContext), null)
        if (debug) {
            if (!File(debugInfoPath).exists()) {
                File(debugInfoPath).mkdirs()
            }
            File(debugInfoPath, "DEBUG.kt").writeText(
                moduleFragment.dumpKotlinLike(
                    KotlinLikeDumpOptions(
                        CustomKotlinLikeDumpStrategy.Default,
                        printRegionsPerFile = true,
                        printFileName = true,
                        printFilePath = true,
                        useNamedArguments = true,
                        labelPrintingStrategy = LabelPrintingStrategy.ALWAYS,
                        printFakeOverridesStrategy = FakeOverridesStrategy.ALL,
                        bodyPrintingStrategy = BodyPrintingStrategy.PRINT_BODIES,
                        printElseAsTrue = false,
                        printUnitReturnType = true,
                        stableOrder = true
                    )
                )
            )
        }
    }
}