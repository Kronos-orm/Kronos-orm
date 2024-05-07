package com.kotlinorm.plugins

import com.google.auto.service.AutoService
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
@OptIn(ExperimentalCompilerApi::class)
@AutoService(CommandLineProcessor::class)
class KronosCommandLineProcessor : CommandLineProcessor {
    companion object {
        private const val OPTION_IGNORE_WARNING = "ignoreWarning"

        val ARG_OPTION_IGNORE_WARNING = CompilerConfigurationKey<Boolean>(OPTION_IGNORE_WARNING)
    }

    override val pluginId: String = "kronos-compiler-plugin"

    override val pluginOptions: Collection<AbstractCliOption> = listOf(
        CliOption(
            optionName = OPTION_IGNORE_WARNING,
            valueDescription = "true or false",
            description = "ignore warning",
            required = false,
        )
    )

    override fun processOption(
        option: AbstractCliOption,
        value: String,
        configuration: CompilerConfiguration
    ) {
        println("processOption:: option=$option value=$value")
        return when (option.optionName) {
            OPTION_IGNORE_WARNING -> configuration.put(ARG_OPTION_IGNORE_WARNING, value.lowercase() == "true")
            else -> throw IllegalArgumentException("Unexpected config option ${option.optionName}")
        }
    }

}