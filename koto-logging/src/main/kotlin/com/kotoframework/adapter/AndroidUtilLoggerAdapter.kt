package com.kotoframework.adapter

import com.kotoframework.KotoLoggerApp.invoke0
import com.kotoframework.beans.logging.KLogMessage
import com.kotoframework.beans.logging.KLogMessage.Companion.formatted
import com.kotoframework.enums.KLogLevel
import com.kotoframework.interfaces.KLogger
import java.lang.reflect.Method

/**
 * Adapter [KLogger] implementation integrating
 */
class AndroidUtilLoggerAdapter(private val tag: String) : KLogger {
    // Access Android Log API by reflection, because Android SDK is not a JDK 9 module,
    // we are not able to require it in module-info.java.
    private val logClass = Class.forName("android.util.Log")
    private val isLoggableMethod = logClass.getMethod("isLoggable", String::class.java, Int::class.javaPrimitiveType)
    private val methodCache = mutableMapOf<String, Method>()
    private val getLoggerMethod =
        { name: String -> methodCache[name] ?: logClass.getMethod(name, String::class.java, String::class.java, Throwable::class.java).apply { methodCache[name] = this } }

    override fun isTraceEnabled(): Boolean {
        return isLoggableMethod.invoke0(null, tag, KLogLevel.VERBOSE.ordinal) as Boolean
    }

    override fun trace(messages: Array<KLogMessage>, e: Throwable?) {
        getLoggerMethod("v").invoke0(null, tag, messages.formatted(), e)
    }

    override fun isDebugEnabled(): Boolean {
        return isLoggableMethod.invoke0(null, tag, KLogLevel.DEBUG.ordinal) as Boolean
    }

    override fun debug(messages: Array<KLogMessage>, e: Throwable?) {
        getLoggerMethod("d").invoke0(null, tag, messages.formatted(), e)
    }

    override fun isInfoEnabled(): Boolean {
        return isLoggableMethod.invoke0(null, tag, KLogLevel.INFO.ordinal) as Boolean
    }

    override fun info(messages: Array<KLogMessage>, e: Throwable?) {
        getLoggerMethod("i").invoke0(null, tag, messages.formatted(), e)
    }

    override fun isWarnEnabled(): Boolean {
        return isLoggableMethod.invoke0(null, tag, KLogLevel.WARN.ordinal) as Boolean
    }

    override fun warn(messages: Array<KLogMessage>, e: Throwable?) {
        getLoggerMethod("w").invoke0(null, tag, messages.formatted(), e)
    }

    override fun isErrorEnabled(): Boolean {
        return isLoggableMethod.invoke0(null, tag, KLogLevel.ERROR.ordinal) as Boolean
    }

    override fun error(messages: Array<KLogMessage>, e: Throwable?) {
        getLoggerMethod("e").invoke0(null, tag, messages.formatted(), e)
    }
}
