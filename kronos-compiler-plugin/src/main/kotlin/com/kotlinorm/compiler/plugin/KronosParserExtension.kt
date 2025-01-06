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

package com.kotlinorm.compiler.plugin

import com.kotlinorm.compiler.plugin.transformer.KronosParserTransformer
import com.kotlinorm.compiler.plugin.utils.KClassCreatorUtil.buildKClassMapper
import com.kotlinorm.compiler.plugin.utils.KClassCreatorUtil.initFunctions
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.name
import org.jetbrains.kotlin.ir.util.BodyPrintingStrategy
import org.jetbrains.kotlin.ir.util.CustomKotlinLikeDumpStrategy
import org.jetbrains.kotlin.ir.util.FakeOverridesStrategy
import org.jetbrains.kotlin.ir.util.KotlinLikeDumpOptions
import org.jetbrains.kotlin.ir.util.LabelPrintingStrategy
import org.jetbrains.kotlin.ir.util.dumpKotlinLike
import java.io.File

open class KronosParserExtension(private val debug: Boolean, private val debugInfoPath: String) : IrGenerationExtension {

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        moduleFragment
            .transform(KronosParserTransformer(pluginContext), null)

        initFunctions.forEach { (context, function) ->
            with(context) {
                buildKClassMapper(function)
            }
        }

        if (debug) {
            moduleFragment.files.forEach {
                File(debugInfoPath).let { file ->
                    if (!file.exists()) file.mkdirs()
                }
                File(debugInfoPath, it.name).writeText(
                    it.module.dumpKotlinLike(
                        KotlinLikeDumpOptions(
                            CustomKotlinLikeDumpStrategy.Default,
                            printRegionsPerFile = true,
                            printFileName = true,
                            printFilePath = true,
                            useNamedArguments = true,
                            labelPrintingStrategy = LabelPrintingStrategy.ALWAYS,
                            printFakeOverridesStrategy = FakeOverridesStrategy.ALL,
                            bodyPrintingStrategy = BodyPrintingStrategy.PRINT_BODIES,
                            inferElseBranches = false,
                            printMemberDeclarations = true,
                            printUnitReturnType = true,
                            stableOrder = true
                        )
                    )
                )
                println("Debug info saved to $debugInfoPath/${it.name}")
            }
        }
    }
}