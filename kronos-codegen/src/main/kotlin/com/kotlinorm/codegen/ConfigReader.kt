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
package com.kotlinorm.codegen

import com.kotlinorm.Kronos
import com.kotlinorm.Kronos.lineHumpNamingStrategy
import com.kotlinorm.Kronos.noneNamingStrategy
import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.logging.KLogMessage.Companion.kMsgOf
import com.kotlinorm.enums.ColorPrintCode
import com.moandjiezana.toml.Toml
import kotlin.io.path.Path


@Suppress("UNCHECKED_CAST")
fun readConfig(path: String): TemplateConfig {
    val toml = Toml()
    val configMap = toml.read(Path(path).toFile()).toMap()
    val tableMap = configMap["table"] as Map<String, Any?>
    val outputMap = configMap["output"] as Map<String, Any?>
    return TemplateConfig(
        table = TableConfig(
            name = tableMap["name"] as String,
            tableNamingStrategy = when (tableMap["tableNamingStrategy"]) {
                "lineHumpNamingStrategy" -> lineHumpNamingStrategy
                "noneNamingStrategy" -> noneNamingStrategy
                else -> null
            },
            fieldNamingStrategy = when (tableMap["fieldNamingStrategy"]) {
                "lineHumpNamingStrategy" -> lineHumpNamingStrategy
                "noneNamingStrategy" -> noneNamingStrategy
                else -> null
            },
            createTimeStrategy = tableMap["createTimeStrategy"]?.let {
                KronosCommonStrategy(true, Field(it as String))
            },
            updateTimeStrategy = tableMap["updateTimeStrategy"]?.let {
                KronosCommonStrategy(true, Field(it as String))
            },
            logicDeleteStrategy = tableMap["logicDeleteStrategy"]?.let {
                KronosCommonStrategy(true, Field(it as String))
            },
            optimisticLockStrategy = tableMap["optimisticLockStrategy"]?.let {
                KronosCommonStrategy(true, Field(it as String))
            },
            className = tableMap["className"] as String?
        ),
        output = OutputConfig(
            targetDir = outputMap["targetDir"] as String,
            packageName = outputMap["packageName"] as String?,
            tableCommentLineWords = outputMap["tableCommentLineWords"] as Int?
        ),
        dataSource = initialDataSource(configMap["dataSource"] as Map<String, Any?>)
    ).also {
        Kronos.defaultLogger(toml).info(
            kMsgOf(
                "Reading config file successfully: $path",
                ColorPrintCode.GREEN
            ).endl().toArray()
        )
    }
}