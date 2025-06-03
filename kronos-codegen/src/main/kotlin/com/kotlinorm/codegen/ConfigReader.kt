/**
 * Copyright 2022-2025 kronos-orm
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

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.toml.TomlFactory
import com.kotlinorm.Kronos
import com.kotlinorm.Kronos.lineHumpNamingStrategy
import com.kotlinorm.Kronos.noneNamingStrategy
import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.logging.log
import com.kotlinorm.enums.PrimaryKeyType
import java.io.File


var mapper = ObjectMapper(TomlFactory())
var codeGenConfig: TemplateConfig? = null

@Suppress("UNCHECKED_CAST")
fun init(path: String) {
    val config = readConfig(path)
    val tables = try {
        config["table"] as? List<Map<String, String?>>
            ?: throw IllegalArgumentException("Table configuration is required in config: $path")
    } catch (_: TypeCastException) {
        throw IllegalArgumentException("Table configuration must be a list of table definitions in config: $path")
    }
    val output = config["output"] as? Map<String, Any?>
        ?: throw IllegalArgumentException("Output configuration is required in config: $path")
    val dataSource = config["dataSource"] as? Map<String, Any?>
        ?: throw IllegalArgumentException("DataSource configuration is required in config: $path")
    val strategies = config["strategy"] as? Map<String, Any?> ?: mapOf()
    codeGenConfig = TemplateConfig(
        table = tables.map {
            TableConfig(
                name = it["name"] ?: throw IllegalArgumentException("Table name is required in config: $path"),
                className = it["className"]
            )
        },
        strategy = StrategyConfig(
            tableNamingStrategy = when (strategies["tableNamingStrategy"]) {
                "lineHumpNamingStrategy" -> lineHumpNamingStrategy
                "noneNamingStrategy" -> noneNamingStrategy
                else -> null
            },
            fieldNamingStrategy = when (strategies["fieldNamingStrategy"]) {
                "lineHumpNamingStrategy" -> lineHumpNamingStrategy
                "noneNamingStrategy" -> noneNamingStrategy
                else -> null
            },
            createTimeStrategy = strategies["createTimeStrategy"]?.let {
                KronosCommonStrategy(true, Field(it as String))
            },
            updateTimeStrategy = strategies["updateTimeStrategy"]?.let {
                KronosCommonStrategy(true, Field(it as String))
            },
            logicDeleteStrategy = strategies["logicDeleteStrategy"]?.let {
                KronosCommonStrategy(true, Field(it as String))
            },
            optimisticLockStrategy = strategies["optimisticLockStrategy"]?.let {
                KronosCommonStrategy(true, Field(it as String))
            },
            primaryKeyStrategy = strategies["primaryKeyStrategy"]?.let {
                KronosCommonStrategy(true, Field(it as String, primaryKey = PrimaryKeyType.IDENTITY))
            }
        ),
        output = OutputConfig(
            targetDir = output["targetDir"] as? String
                ?: throw IllegalArgumentException("Target directory is required in output config: $path"),
            packageName = output["packageName"] as String?,
            tableCommentLineWords = (output["tableCommentLineWords"] as? Number)?.toInt()
        ),
        dataSource = createWrapper(
            dataSource["wrapperClassName"]?.toString(),
            initialDataSource(dataSource)
        )
    ).apply {
        Kronos.defaultLogger(this).info(
            log {
                +"Reading config file successfully: $path"[green]
            }
        )
    }
}

fun readConfig(path: String): Map<String, Any?> {
    var config = mapper.readValue(
        File(path),
        object : TypeReference<Map<String, Any?>?>() {}
    ) ?: throw IllegalArgumentException("Config file not found or is empty")
    while (config["extend"] != null) {
        val extendPath = config["extend"] as String
        Kronos.defaultLogger(extendPath).info(
            log {
                +"Config extension found: $extendPath"[blue, bold]
            }
        )
        val extendConfig = mapper.readValue(
            File(extendPath),
            object : TypeReference<Map<String, Any?>?>() {}
        ) ?: throw IllegalArgumentException("Extend config file not found or is empty: $extendPath"
        )
        config = extendConfig + config
    }
    return config
}