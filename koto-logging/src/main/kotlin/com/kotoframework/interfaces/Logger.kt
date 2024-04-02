package com.kotoframework.interfaces

import com.kotoframework.KotoApp
import com.kotoframework.adapter.*
import com.kotoframework.enums.LoggerType
import com.kotoframework.utils.BundledSimpleLogger
import jdk.jfr.internal.LogLevel
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

public interface Logger {

    /**
     * Check if the logger instance enabled for the [TRACE] level.
     */
    public fun isTraceEnabled(): Boolean

    /**
     * Log a message at the [TRACE] level.
     */
    public fun trace(msg: String, e: Throwable? = null)

    /**
     * Check if the logger instance enabled for the [DEBUG] level.
     */
    public fun isDebugEnabled(): Boolean

    /**
     * Log a message at the [DEBUG] level.
     */
    public fun debug(msg: String, e: Throwable? = null)

    /**
     * Check if the logger instance enabled for the [INFO] level.
     */
    public fun isInfoEnabled(): Boolean

    /**
     * Log a message at the [INFO] level.
     */
    public fun info(msg: String, e: Throwable? = null)

    /**
     * Check if the logger instance enabled for the [WARN] level.
     */
    public fun isWarnEnabled(): Boolean

    /**
     * Log a message at the [WARN] level.
     */
    public fun warn(msg: String, e: Throwable? = null)

    /**
     * Check if the logger instance enabled for the [ERROR] level.
     */
    public fun isErrorEnabled(): Boolean

    /**
     * Log a message at the [ERROR] level.
     */
    public fun error(msg: String, e: Throwable? = null)

}

/**
 * Call the [Method.invoke] method, catching [InvocationTargetException], and rethrowing the target exception.
 */
@Suppress("SwallowedException")
internal fun Method.invoke0(obj: Any?, vararg args: Any?): Any? {
    try {
        return this.invoke(obj, *args)
    } catch (e: InvocationTargetException) {
        throw e.targetException
    }
}

public fun detectLoggerImplementation(tag: String): Logger {

    var result: Logger? = null

    fun loggerImplement(init: () -> Logger) {
        if (result == null) {
            try {
                result = init()
            } catch (_: ClassNotFoundException) {
            } catch (_: NoClassDefFoundError) {
            }
        }
    }

    when(KotoApp.defaultLogger) {
        LoggerType.ANDROID_LOGGER -> loggerImplement { AndroidLoggerAdapter(tag) }
        LoggerType.COMMONS_LOGGER -> loggerImplement { CommonsLoggerAdapter(tag) }
        LoggerType.CONSOLE_LOGGER -> loggerImplement { ConsoleLoggerAdapter(threshold = LogLevel.INFO) }
        LoggerType.JDK_LOGGER -> loggerImplement { JdkLoggerAdapter(tag) }
        LoggerType.SLF4J_LOGGER -> loggerImplement { Slf4jLoggerAdapter(tag) }
        else -> loggerImplement { BundledSimpleLogger(tag) }
    }

    return result ?: throw Exception("No logger implementation found")
}
