/*
 * Copyright 2018-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kotoframework.adapter

import com.kotoframework.KotoLoggerApp.invoke0
import com.kotoframework.beans.logging.KLogMessage
import com.kotoframework.beans.logging.KLogMessage.Companion.formatted
import com.kotoframework.interfaces.KLogger
import java.lang.reflect.Method

/**
 * Adapter [KLogger] implementation integrating Slf4j with Kotoframework.
 */
class Slf4jLoggerAdapter(loggerName: String) : KLogger {
    // Access SLF4J API by reflection, because we haven't required it in module-info.java.
    private val loggerFactoryClass = Class.forName("org.slf4j.LoggerFactory")
    private val logClass = Class.forName("org.slf4j.KLogger")
    private val methodCache = mutableMapOf<String, Method>()
    private val getLoggerMethod = { name: String -> methodCache[name] ?: logClass.getMethod(name, String::class.java, Throwable::class.java).apply { methodCache[name] = this } }
    private val getLoggerEnabledMethod = { name: String -> methodCache[name] ?: logClass.getMethod(name).apply { methodCache[name] = this } }
    private val logger = loggerFactoryClass.getMethod("getLogger", String::class.java).invoke0(null, loggerName)

    override fun isTraceEnabled(): Boolean {
        return getLoggerEnabledMethod("isTraceEnabled").invoke0(logger) as Boolean
    }

    override fun trace(messages: Array<KLogMessage>, e: Throwable?) {
        getLoggerMethod("trace").invoke0(logger, messages.formatted(), e)
    }

    override fun isDebugEnabled(): Boolean {
        return getLoggerEnabledMethod("isDebugEnabled").invoke0(logger) as Boolean
    }

    override fun debug(messages: Array<KLogMessage>, e: Throwable?) {
        getLoggerMethod("debug").invoke0(logger, messages.formatted(), e)
    }

    override fun isInfoEnabled(): Boolean {
        return getLoggerEnabledMethod("isInfoEnabled").invoke0(logger) as Boolean
    }

    override fun info(messages: Array<KLogMessage>, e: Throwable?) {
        getLoggerMethod("info").invoke0(logger, messages.formatted(), e)
    }

    override fun isWarnEnabled(): Boolean {
        return getLoggerEnabledMethod("isWarnEnabled").invoke0(logger) as Boolean
    }

    override fun warn(messages: Array<KLogMessage>, e: Throwable?) {
        getLoggerMethod("warn").invoke0(logger, messages.formatted(), e)
    }

    override fun isErrorEnabled(): Boolean {
        return getLoggerEnabledMethod("isErrorEnabled").invoke0(logger) as Boolean
    }

    override fun error(messages: Array<KLogMessage>, e: Throwable?) {
        getLoggerMethod("error").invoke0(logger, messages.formatted(), e)
    }
}
