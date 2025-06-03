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

package com.kotlinorm

import com.kotlinorm.adapter.AndroidUtilLoggerAdapter
import com.kotlinorm.adapter.ApacheCommonsLoggerAdapter
import com.kotlinorm.adapter.JavaUtilLoggerAdapter
import com.kotlinorm.adapter.Slf4jLoggerAdapter
import com.kotlinorm.beans.logging.BundledSimpleLoggerAdapter
import com.kotlinorm.enums.KLoggerType
import com.kotlinorm.exceptions.KotoNoLoggerException
import com.kotlinorm.i18n.Noun
import com.kotlinorm.interfaces.KLogger

object KronosLoggerApp {
    /**
     * Returns an instance of KLogger based on the provided logging class.
     *
     * @param loggingClazz the class for which the logger is being created
     * @return an instance of KLogger based on the logger type specified in Kronos
     * @throws KotoNoLoggerException if the logger type specified in Kronos is not supported
     */
    private fun getKotoLoggerInstance(loggingClazz: Any): KLogger {
        val tag = if (loggingClazz is String) {
            loggingClazz
        } else {
            loggingClazz::class.simpleName!!
        }

        return when (Kronos.loggerType) {
            KLoggerType.ANDROID_LOGGER -> AndroidUtilLoggerAdapter(tag)
            KLoggerType.COMMONS_LOGGER -> ApacheCommonsLoggerAdapter(tag)
            KLoggerType.JDK_LOGGER -> JavaUtilLoggerAdapter(tag)
            KLoggerType.SLF4J_LOGGER -> Slf4jLoggerAdapter(tag)
            KLoggerType.DEFAULT_LOGGER -> BundledSimpleLoggerAdapter(tag)
            else -> throw KotoNoLoggerException(Noun.noKLoggerMessage)
        }
    }

    /**
     * Detects and sets the logger implementation based on the available logger types.
     *
     * This function sets the default logger implementation using the `getKotoLoggerInstance` function.
     * It then iterates over each available logger type and attempts to initialize the logger using the `init` function.
     * If the logger initialization is successful, the logger type is set as the current logger type.
     * Any `ClassNotFoundException` or `NoClassDefFoundError` exceptions are caught and ignored.
     *
     * @throws KotoNoLoggerException if no logger implementation is found for the given logger type
     */
    @Suppress("unused")
    fun detectLoggerImplementation() {
        Kronos.defaultLogger = { getKotoLoggerInstance(it) }
        val tag = "com.kotlinorm.core"
        var result: KLogger? = null

        infix fun KLoggerType.detect(init: () -> KLogger) {
            if (result == null) {
                try {
                    result = init()
                    Kronos.loggerType = this
                } catch (_: ClassNotFoundException) {
                    // ignored
                } catch (_: NoClassDefFoundError) {
                    // ignored
                }
            }
        }

        KLoggerType.ANDROID_LOGGER detect { AndroidUtilLoggerAdapter(tag) }
        KLoggerType.COMMONS_LOGGER detect { ApacheCommonsLoggerAdapter(tag) }
        KLoggerType.JDK_LOGGER detect { JavaUtilLoggerAdapter(tag) }
        KLoggerType.SLF4J_LOGGER detect { Slf4jLoggerAdapter(tag) }
        KLoggerType.DEFAULT_LOGGER detect { BundledSimpleLoggerAdapter(tag) }
    }
}