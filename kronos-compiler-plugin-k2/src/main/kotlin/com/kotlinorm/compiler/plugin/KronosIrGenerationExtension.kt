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

import com.kotlinorm.compiler.transformers.KronosParserTransformer
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.BodyPrintingStrategy
import org.jetbrains.kotlin.ir.util.CustomKotlinLikeDumpStrategy
import org.jetbrains.kotlin.ir.util.FakeOverridesStrategy
import org.jetbrains.kotlin.ir.util.KotlinLikeDumpOptions
import org.jetbrains.kotlin.ir.util.LabelPrintingStrategy
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.dumpKotlinLike
import java.io.File

/**
 * IR generation extension for the Kronos compiler plugin
 *
 * This is the main entry point for IR transformations
 *
 * @property messageCollector Message collector for compilation messages
 * @property dumpIr Whether IR dump is enabled
 * @property dumpIrPath Path to save IR dump (default: build/tmp/kronosDebug)
 * @property dumpIrMode IR dump mode: "kotlinLike" or "common"
 * @property dumpIrFiles Comma-separated file patterns to filter (null means dump all files)
 */
class KronosIrGenerationExtension(
    private val messageCollector: MessageCollector,
    private val dumpIr: Boolean,
    private val dumpIrPath: String,
    private val dumpIrMode: String,
    private val dumpIrFiles: String?
) : IrGenerationExtension {

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        println("[Kronos] Kronos compiler plugin K2 initialized")
        
        // Apply transformations
        val transformer = KronosParserTransformer(pluginContext, messageCollector)
        moduleFragment.transform(transformer, null)
        
        // Dump IR if enabled
        if (dumpIr) {
            println("[Kronos] IR dump enabled - mode: $dumpIrMode, path: $dumpIrPath")
            if (dumpIrFiles != null) {
                println("[Kronos] Filtering files: $dumpIrFiles")
            } else {
                println("[Kronos] Dumping all files")
            }
            dumpIR(moduleFragment)
        }
    }

    /**
     * Dumps the IR to files for debugging
     *
     * @param moduleFragment The module fragment to dump
     */
    private fun dumpIR(moduleFragment: IrModuleFragment) {
        val filePatterns = dumpIrFiles?.split(",")?.map { it.trim() }
        var dumpedFiles = 0
        
        moduleFragment.files.forEach { file ->
            try {
                val fullPath = file.fileEntry.name
                val fileName = File(fullPath).name
                
                // Check if this file matches the filter
                if (filePatterns != null && !matchesAnyPattern(fileName, filePatterns)) {
                    return@forEach
                }
                
                // Create IR dump directory if it doesn't exist
                val dumpDir = File(dumpIrPath)
                if (!dumpDir.exists()) {
                    dumpDir.mkdirs()
                }

                val fileExtension = if (dumpIrMode.lowercase() == "kotlinlike") ".kt" else ".txt"
                
                val outputFileName = fileName.removeSuffix(".kt") + fileExtension
                val outputFile = File(dumpDir, outputFileName)

                val irDump = when (dumpIrMode.lowercase()) {
                    "kotlinlike" -> {
                        // Kotlin-like format
                        val dumpOptions = KotlinLikeDumpOptions(
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
                        file.module.dumpKotlinLike(dumpOptions)
                    }
                    "common" -> {
                        // Plain IR text format
                        file.dump()
                    }
                    else -> {
                        println("[Kronos] Warning: Unknown dump mode '$dumpIrMode', using 'common' mode")
                        file.dump()
                    }
                }
                
                outputFile.writeText(irDump)
                dumpedFiles++
            } catch (e: Exception) {
                // Don't fail compilation if IR dump fails
                println("[Kronos] Warning: Failed to dump IR for ${file.fileEntry.name}: ${e.message}")
            }
        }
        
        println("[Kronos] IR dump complete: $dumpedFiles file(s) saved to $dumpIrPath")
    }
    
    /**
     * Checks if a filename matches any of the given patterns
     * Supports simple wildcard patterns with * (e.g., "*Service.kt", "User*.kt")
     */
    private fun matchesAnyPattern(fileName: String, patterns: List<String>): Boolean {
        return patterns.any { pattern ->
            val regex = pattern
                .replace(".", "\\.")
                .replace("*", ".*")
                .toRegex()
            regex.matches(fileName)
        }
    }
}
