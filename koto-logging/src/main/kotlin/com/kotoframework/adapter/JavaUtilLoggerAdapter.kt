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

import com.kotoframework.beans.logging.KLogMessage
import com.kotoframework.beans.logging.KLogMessage.Companion.formatted
import com.kotoframework.interfaces.KLogger
import java.util.logging.Level

/**
 * Adapter [KLogger] implementation integrating [java.util.logging] with Kotoframework.
 */
class JavaUtilLoggerAdapter(loggerName: String) : KLogger {
    private val logger = java.util.logging.Logger.getLogger(loggerName)

    override fun isTraceEnabled(): Boolean {
        return logger.isLoggable(Level.FINEST)
    }

    override fun trace(messages: Array<KLogMessage>, e: Throwable?) {
        logger.log(Level.FINEST, messages.formatted(), e)
    }

    override fun isDebugEnabled(): Boolean {
        return logger.isLoggable(Level.FINE)
    }

    override fun debug(messages: Array<KLogMessage>, e: Throwable?) {
        logger.log(Level.FINE, messages.formatted(), e)
    }

    override fun isInfoEnabled(): Boolean {
        return logger.isLoggable(Level.INFO)
    }

    override fun info(messages: Array<KLogMessage>, e: Throwable?) {
        logger.log(Level.INFO, messages.formatted(), e)
    }

    override fun isWarnEnabled(): Boolean {
        return logger.isLoggable(Level.WARNING)
    }

    override fun warn(messages: Array<KLogMessage>, e: Throwable?) {
        logger.log(Level.WARNING, messages.formatted(), e)
    }

    override fun isErrorEnabled(): Boolean {
        return logger.isLoggable(Level.SEVERE)
    }

    override fun error(messages: Array<KLogMessage>, e: Throwable?) {
        logger.log(Level.SEVERE, messages.formatted(), e)
    }
}
