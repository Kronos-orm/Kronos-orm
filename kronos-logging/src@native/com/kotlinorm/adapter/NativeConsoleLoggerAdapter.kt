/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.adapter

import com.kotlinorm.beans.logging.KLogMessage
import com.kotlinorm.beans.logging.KLogMessage.Companion.formatted
import com.kotlinorm.Kronos
import com.kotlinorm.interfaces.KLogger

internal var nativeConsoleLogSink: (String) -> Unit = { message -> print(message) }

class NativeConsoleLoggerAdapter(private val tag: String) : KLogger {
    override fun isTraceEnabled(): Boolean = Kronos.logging.enabled && "console" in Kronos.logging.path

    override fun trace(messages: Array<KLogMessage>, e: Throwable?) = emit("TRACE", messages, e)

    override fun isDebugEnabled(): Boolean = Kronos.logging.enabled && "console" in Kronos.logging.path

    override fun debug(messages: Array<KLogMessage>, e: Throwable?) = emit("DEBUG", messages, e)

    override fun isInfoEnabled(): Boolean = Kronos.logging.enabled && "console" in Kronos.logging.path

    override fun info(messages: Array<KLogMessage>, e: Throwable?) = emit("INFO", messages, e)

    override fun isWarnEnabled(): Boolean = Kronos.logging.enabled && "console" in Kronos.logging.path

    override fun warn(messages: Array<KLogMessage>, e: Throwable?) = emit("WARN", messages, e)

    override fun isErrorEnabled(): Boolean = Kronos.logging.enabled && "console" in Kronos.logging.path

    override fun error(messages: Array<KLogMessage>, e: Throwable?) = emit("ERROR", messages, e)

    private fun emit(level: String, messages: Array<KLogMessage>, error: Throwable?) {
        if (!Kronos.logging.enabled || "console" !in Kronos.logging.path) return
        val suffix = error?.let { "\n${it.stackTraceToString()}" }.orEmpty()
        nativeConsoleLogSink("[$level] [$tag] ${messages.formatted()}$suffix")
    }
}
