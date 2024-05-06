package com.kotoframework.interfaces

import com.kotoframework.beans.logging.KLogMessage

public interface KLogger {

    /**
     * Check if the logger instance enabled for the [com.kotoframework.enums.KLogLevel.TRACE] level.
     */
    public fun isTraceEnabled(): Boolean

    /**
     * Log a message at the [com.kotoframework.enums.KLogLevel.TRACE] level.
     */
    public fun trace(messages: Array<KLogMessage>, e: Throwable? = null)

    /**
     * Check if the logger instance enabled for the [com.kotoframework.enums.KLogLevel.DEBUG] level.
     */
    public fun isDebugEnabled(): Boolean

    /**
     * Log a message at the [com.kotoframework.enums.KLogLevel.DEBUG] level.
     */
    public fun debug(messages: Array<KLogMessage>, e: Throwable? = null)

    /**
     * Check if the logger instance enabled for the [com.kotoframework.enums.KLogLevel.INFO] level.
     */
    public fun isInfoEnabled(): Boolean

    /**
     * Log a message at the [com.kotoframework.enums.KLogLevel.INFO] level.
     */
    public fun info(messages: Array<KLogMessage>, e: Throwable? = null)

    /**
     * Check if the logger instance enabled for the 'WARN' level.
     */
    public fun isWarnEnabled(): Boolean

    /**
     * Log a message at the [com.kotoframework.enums.KLogLevel.WARN] level.
     */
    public fun warn(messages: Array<KLogMessage>, e: Throwable? = null)

    /**
     * Check if the logger instance enabled for the [com.kotoframework.enums.KLogLevel.ERROR] level.
     */
    public fun isErrorEnabled(): Boolean

    /**
     * Log a message at the [com.kotoframework.enums.KLogLevel.ERROR] level.
     */
    public fun error(messages: Array<KLogMessage>, e: Throwable? = null)

}