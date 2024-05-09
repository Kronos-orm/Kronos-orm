package com.kotlinorm.interfaces

import com.kotlinorm.beans.logging.KLogMessage

interface KLogger {

    /**
     * Check if the logger instance enabled for the [com.kotlinorm.enums.KLogLevel.TRACE] level.
     */
    fun isTraceEnabled(): Boolean

    /**
     * Log a message at the [com.kotlinorm.enums.KLogLevel.TRACE] level.
     *
     * @param messages messages to log
     * @param e exception
     */
    fun trace(messages: Array<KLogMessage>, e: Throwable? = null)

    /**
     * Check if the logger instance enabled for the [com.kotlinorm.enums.KLogLevel.DEBUG] level.
     */
    fun isDebugEnabled(): Boolean

    /**
     * Log a message at the [com.kotlinorm.enums.KLogLevel.DEBUG] level.
     *
     * @param messages messages to log
     * @param e exception
     */
    fun debug(messages: Array<KLogMessage>, e: Throwable? = null)

    /**
     * Check if the logger instance enabled for the [com.kotlinorm.enums.KLogLevel.INFO] level.
     */
    fun isInfoEnabled(): Boolean

    /**
     * Log a message at the [com.kotlinorm.enums.KLogLevel.INFO] level.
     *
     * @param messages messages to log
     * @param e exception
     */
    fun info(messages: Array<KLogMessage>, e: Throwable? = null)

    /**
     * Check if the logger instance enabled for the 'WARN' level.
     */
    fun isWarnEnabled(): Boolean

    /**
     * Log a message at the [com.kotlinorm.enums.KLogLevel.WARN] level.
     *
     * @param messages messages to log
     * @param e exception
     */
    fun warn(messages: Array<KLogMessage>, e: Throwable? = null)

    /**
     * Check if the logger instance enabled for the [com.kotlinorm.enums.KLogLevel.ERROR] level.
     */
    fun isErrorEnabled(): Boolean

    /**
     * Log a message at the [com.kotlinorm.enums.KLogLevel.ERROR] level.
     *
     * @param messages messages to log
     * @param e exception
     */
    fun error(messages: Array<KLogMessage>, e: Throwable? = null)

}