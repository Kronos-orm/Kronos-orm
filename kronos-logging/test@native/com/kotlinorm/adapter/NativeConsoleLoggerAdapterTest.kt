/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.adapter

import com.kotlinorm.Kronos
import com.kotlinorm.KronosLoggerApp
import com.kotlinorm.beans.logging.KLogMessage
import com.kotlinorm.enums.KLoggerType
import kotlin.test.Test
import kotlin.test.assertEquals

class NativeConsoleLoggerAdapterTest {
    @Test
    fun global_switch_and_empty_console_destinations_suppress_native_output() {
        val originalEnabled = Kronos.logging.enabled
        val originalPath = Kronos.logging.path
        val originalSink = nativeConsoleLogSink
        val output = mutableListOf<String>()
        val logger = NativeConsoleLoggerAdapter("native-test")
        val message = arrayOf(KLogMessage("ready", newLine = true))
        try {
            nativeConsoleLogSink = { output += it }
            Kronos.logging.path = arrayOf("console")
            Kronos.logging.enabled = false
            logger.info(message)
            assertEquals(false, logger.isInfoEnabled())
            Kronos.logging.enabled = true
            Kronos.logging.path = emptyArray()
            logger.info(message)
            assertEquals(false, logger.isInfoEnabled())
            assertEquals(emptyList(), output)
            Kronos.logging.path = arrayOf("console")
            logger.info(message)
            assertEquals(listOf("[INFO] [native-test] ready\r\n"), output)
        } finally {
            Kronos.logging.enabled = originalEnabled
            Kronos.logging.path = originalPath
            nativeConsoleLogSink = originalSink
        }
    }

    @Test
    fun native_output_preserves_exception_details() {
        val originalSink = nativeConsoleLogSink
        val output = mutableListOf<String>()
        val error = IllegalStateException("database failed")
        try {
            nativeConsoleLogSink = { output += it }
            NativeConsoleLoggerAdapter("native-error").error(arrayOf(KLogMessage("failed")), error)
            assertEquals(listOf("[ERROR] [native-error] failed\n${error.stackTraceToString()}"), output)
        } finally {
            nativeConsoleLogSink = originalSink
        }
    }

    @Test
    fun detection_installs_the_native_console_adapter_on_kronos() {
        val originalLoggerType = Kronos.logging.type
        val originalLoggerFactory = Kronos.logging.factory
        val originalSink = nativeConsoleLogSink
        val output = mutableListOf<String>()

        try {
            nativeConsoleLogSink = { line -> output += line }
            KronosLoggerApp.detectLoggerImplementation()
            Kronos.logging.logger("ios-test").info(arrayOf(KLogMessage("ready", newLine = true)))

            assertEquals(KLoggerType.DEFAULT_LOGGER, Kronos.logging.type)
            assertEquals("[INFO] [ios-test] ready\r\n", output.single())
        } finally {
            Kronos.logging.factory = originalLoggerFactory
            Kronos.logging.type = originalLoggerType
            nativeConsoleLogSink = originalSink
        }
    }
}
