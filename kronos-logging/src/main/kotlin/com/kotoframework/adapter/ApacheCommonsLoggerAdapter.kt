package com.kotoframework.adapter

import com.kotoframework.KotoLoggerApp.invoke0
import com.kotoframework.beans.logging.KLogMessage
import com.kotoframework.beans.logging.KLogMessage.Companion.formatted
import com.kotoframework.interfaces.KLogger
import java.lang.reflect.Method

/**
 * Adapter [KLogger] implementation integrating Apache Commons Logging with Kotoframework.
 */
class ApacheCommonsLoggerAdapter(loggerName: String) : KLogger {
    // Access commons logging API by reflection, because it is not a JDK 9 module,
    // we are not able to require it in module-info.java.
    private val logFactoryClass = Class.forName("org.apache.commons.logging.LogFactory")
    private val logClass = Class.forName("org.apache.commons.logging.Log")
    private val logger = logFactoryClass.getMethod("getLog", String::class.java).invoke0(null, loggerName)
    private val methodCache = mutableMapOf<String, Method>()
    private val getLoggerMethod = { name: String -> methodCache[name] ?: logClass.getMethod(name, Any::class.java, Throwable::class.java).apply { methodCache[name] = this } }
    private val getLoggerEnabledMethod = { name: String -> methodCache[name] ?: logClass.getMethod(name).apply { methodCache[name] = this } }

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
