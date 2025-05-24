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
import com.kotlinorm.beans.logging.KLogMessage.Companion.kMsgOf
import com.kotlinorm.enums.ColorPrintCode
import com.kotlinorm.interfaces.KronosDataSourceWrapper
import java.lang.reflect.Method
import java.util.*
import javax.sql.DataSource

fun createWrapper(className: String?, dataSource: DataSource): KronosDataSourceWrapper {
    val className = className ?: {
        Kronos.defaultLogger(dataSource).warn(
            kMsgOf(
                "wrapperClassName is not set, using default: com.kotlinorm.KronosBasicWrapper",
                ColorPrintCode.YELLOW
            ).endl().toArray()
        )
        "com.kotlinorm.KronosBasicWrapper"
    }()
    return Class.forName(className)
        .getDeclaredConstructor(dataSource::class.java)
        .newInstance(dataSource) as KronosDataSourceWrapper
}

fun initialDataSource(config: Map<String, Any?>): DataSource {
    val dataSource =
        Class.forName(
            config["dataSourceClassName"]?.toString() ?: {
                Kronos.defaultLogger(config).warn(
                    kMsgOf(
                        "dataSourceClassName is not set, using default: org.apache.commons.dbcp2.BasicDataSource",
                        ColorPrintCode.YELLOW
                    ).endl().toArray()
                )
                "org.apache.commons.dbcp2.BasicDataSource"
            }()
        )
            .getDeclaredConstructor()
            .newInstance() as DataSource

    dataSource.apply {
        config.entries.forEach { (key, value) ->
            if (key in arrayOf("dataSourceClassName", "wrapperClassName")) return@forEach
            try {
                // 生成可能的setter方法名（兼容不同命名风格）
                val methodNames = listOf(
                    "set${key.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }}",
                    "set${key.uppercase(Locale.ROOT)}",
                    "set${key.lowercase(Locale.ROOT)}"
                )

                val targetMethod = methodNames.firstNotNullOfOrNull { name ->
                    findCompatibleMethod(this.javaClass, name, value)
                }

                targetMethod?.apply {
                    invoke(dataSource, convertValue(value, targetMethod.parameterTypes[0]))
                }
                    ?: Kronos.defaultLogger(this).warn(
                        kMsgOf(
                            "Setter for '$key' not found in ${this::class.java.name}",
                            ColorPrintCode.YELLOW
                        ).endl().toArray()
                    )
            } catch (e: Exception) {
                Kronos.defaultLogger(this).warn(
                    kMsgOf(
                        "Error setting property '$key': ${e.message?.replace('\n', ' ')}",
                        ColorPrintCode.YELLOW
                    ).endl().toArray()
                )
            }
        }
    }
    return dataSource
}

private fun findCompatibleMethod(clazz: Class<*>, methodName: String, value: Any?): Method? {
    return try {
        clazz.methods.firstOrNull { method ->
            method.name == methodName &&
                    method.parameterCount == 1 &&
                    isTypeCompatible(method.parameterTypes[0], value)
        }
    } catch (_: Exception) {
        null
    }
}

private fun isTypeCompatible(targetType: Class<*>, value: Any?): Boolean {
    if (value == null) return !targetType.isPrimitive
    return when (targetType) {
        Int::class.java, Integer.TYPE -> value is Number
        Long::class.java, java.lang.Long.TYPE -> value is Number
        Boolean::class.java, java.lang.Boolean.TYPE -> value is Boolean
        String::class.java -> true
        else -> targetType.isAssignableFrom(value.javaClass)
    }
}

private fun convertValue(value: Any?, targetType: Class<*>): Any? {
    if (value == null) return null

    return when {
        targetType.isAssignableFrom(value.javaClass) -> value
        targetType == Int::class.java || targetType == Integer.TYPE -> (value as? Number)?.toInt()
        targetType == Long::class.java || targetType == java.lang.Long.TYPE -> (value as? Number)?.toLong()
        targetType == Boolean::class.java || targetType == java.lang.Boolean.TYPE -> value.toString().toBoolean()
        targetType == String::class.java -> value.toString()
        targetType.isEnum -> enumValueOfSafe(targetType, value.toString())
        else -> throw IllegalArgumentException("Unsupported type conversion: ${value.javaClass} to $targetType")
    } ?: throw TypeCastException("Cannot convert $value (${value.javaClass}) to $targetType")
}

private fun enumValueOfSafe(enumClass: Class<*>, value: String): Any {
    return enumClass.enumConstants?.firstOrNull {
        (it as Enum<*>).name.equals(value, ignoreCase = true)
    } ?: throw IllegalArgumentException("Invalid enum value '$value' for ${enumClass.simpleName}")
}