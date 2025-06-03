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

package com.kotlinorm.adapter

import com.kotlinorm.beans.logging.KLogMessage
import com.kotlinorm.beans.logging.KLogMessage.Companion.formatted
import com.kotlinorm.interfaces.KLogger
import java.lang.reflect.Method

/**
 * Adapter [KLogger] implementation integrating Apache Commons Logging with kotlinorm.
 */
class ApacheCommonsLoggerAdapter(loggerName: String) : KLogger {
    // Access commons logging API by reflection, because it is not a JDK 9 module,
    // we are not able to require it in module-info.java.
    private val logFactoryClass = Class.forName("org.apache.commons.logging.LogFactory")
    private val logClass = Class.forName("org.apache.commons.logging.Log")
    private val logger = logFactoryClass.getMethod("getLog", String::class.java).invoke(null, loggerName)
    private val methodCache = mutableMapOf<String, Method>()
    private val getLoggerMethod = { name: String ->
        methodCache[name] ?: logClass.getMethod(name, Any::class.java, Throwable::class.java)
            .apply { methodCache[name] = this }
    }
    private val getLoggerEnabledMethod =
        { name: String -> methodCache[name] ?: logClass.getMethod(name).apply { methodCache[name] = this } }

    override fun isTraceEnabled(): Boolean {
        return getLoggerEnabledMethod("isTraceEnabled").invoke(logger) as Boolean
    }

    override fun trace(messages: Array<KLogMessage>, e: Throwable?) {
        getLoggerMethod("trace").invoke(logger, messages.formatted(), e)
    }

    override fun isDebugEnabled(): Boolean {
        return getLoggerEnabledMethod("isDebugEnabled").invoke(logger) as Boolean
    }

    override fun debug(messages: Array<KLogMessage>, e: Throwable?) {
        getLoggerMethod("debug").invoke(logger, messages.formatted(), e)
    }

    override fun isInfoEnabled(): Boolean {
        return getLoggerEnabledMethod("isInfoEnabled").invoke(logger) as Boolean
    }

    override fun info(messages: Array<KLogMessage>, e: Throwable?) {
        getLoggerMethod("info").invoke(logger, messages.formatted(), e)
    }

    override fun isWarnEnabled(): Boolean {
        return getLoggerEnabledMethod("isWarnEnabled").invoke(logger) as Boolean
    }

    override fun warn(messages: Array<KLogMessage>, e: Throwable?) {
        getLoggerMethod("warn").invoke(logger, messages.formatted(), e)
    }

    override fun isErrorEnabled(): Boolean {
        return getLoggerEnabledMethod("isErrorEnabled").invoke(logger) as Boolean
    }

    override fun error(messages: Array<KLogMessage>, e: Throwable?) {
        getLoggerMethod("error").invoke(logger, messages.formatted(), e)
    }
}
