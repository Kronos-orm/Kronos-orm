/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.beans.logging

import com.kotlinorm.interfaces.KLogger
import com.kotlinorm.beans.logging.KLogMessage.Companion.formatted

internal class NativeConsoleKLogger(private val tag: String) : KLogger {
    override fun isTraceEnabled(): Boolean = true

    override fun trace(messages: Array<KLogMessage>, e: Throwable?) = emit("TRACE", messages, e)

    override fun isDebugEnabled(): Boolean = true

    override fun debug(messages: Array<KLogMessage>, e: Throwable?) = emit("DEBUG", messages, e)

    override fun isInfoEnabled(): Boolean = true

    override fun info(messages: Array<KLogMessage>, e: Throwable?) = emit("INFO", messages, e)

    override fun isWarnEnabled(): Boolean = true

    override fun warn(messages: Array<KLogMessage>, e: Throwable?) = emit("WARN", messages, e)

    override fun isErrorEnabled(): Boolean = true

    override fun error(messages: Array<KLogMessage>, e: Throwable?) = emit("ERROR", messages, e)

    private fun emit(level: String, messages: Array<KLogMessage>, error: Throwable?) {
        val suffix = error?.message?.let { " ($it)" }.orEmpty()
        print("[$level] [$tag] ${messages.formatted()}$suffix")
    }
}
