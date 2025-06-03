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
import com.kotlinorm.enums.KLogLevel
import com.kotlinorm.interfaces.KLogger
import java.lang.reflect.Method

/**
 * Adapter [KLogger] implementation integrating
 */
class AndroidUtilLoggerAdapter(private val tag: String) : KLogger {
    // Access Android Log API by reflection, because Android SDK is not a JDK 9 module,
    // we are not able to require it in module-info.java.
    private val logClass = Class.forName("android.utils.Log")
    private val isLoggableMethod = logClass.getMethod("isLoggable", String::class.java, Int::class.javaPrimitiveType)
    private val methodCache = mutableMapOf<String, Method>()
    private val getLoggerMethod =
        { name: String ->
            methodCache[name] ?: logClass.getMethod(
                name,
                String::class.java,
                String::class.java,
                Throwable::class.java
            ).apply { methodCache[name] = this }
        }

    override fun isTraceEnabled(): Boolean {
        return isLoggableMethod.invoke(null, tag, KLogLevel.VERBOSE.ordinal) as Boolean
    }

    override fun trace(messages: Array<KLogMessage>, e: Throwable?) {
        getLoggerMethod("v").invoke(null, tag, messages.formatted(), e)
    }

    override fun isDebugEnabled(): Boolean {
        return isLoggableMethod.invoke(null, tag, KLogLevel.DEBUG.ordinal) as Boolean
    }

    override fun debug(messages: Array<KLogMessage>, e: Throwable?) {
        getLoggerMethod("d").invoke(null, tag, messages.formatted(), e)
    }

    override fun isInfoEnabled(): Boolean {
        return isLoggableMethod.invoke(null, tag, KLogLevel.INFO.ordinal) as Boolean
    }

    override fun info(messages: Array<KLogMessage>, e: Throwable?) {
        getLoggerMethod("i").invoke(null, tag, messages.formatted(), e)
    }

    override fun isWarnEnabled(): Boolean {
        return isLoggableMethod.invoke(null, tag, KLogLevel.WARN.ordinal) as Boolean
    }

    override fun warn(messages: Array<KLogMessage>, e: Throwable?) {
        getLoggerMethod("w").invoke(null, tag, messages.formatted(), e)
    }

    override fun isErrorEnabled(): Boolean {
        return isLoggableMethod.invoke(null, tag, KLogLevel.ERROR.ordinal) as Boolean
    }

    override fun error(messages: Array<KLogMessage>, e: Throwable?) {
        getLoggerMethod("e").invoke(null, tag, messages.formatted(), e)
    }
}
