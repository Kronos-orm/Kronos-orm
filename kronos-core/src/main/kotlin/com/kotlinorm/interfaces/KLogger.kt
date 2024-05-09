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

package com.kotlinorm.interfaces

import com.kotlinorm.beans.logging.KLogMessage

/**
 * Kronos Logger Interface
 *
 * @author OUSC
 */
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