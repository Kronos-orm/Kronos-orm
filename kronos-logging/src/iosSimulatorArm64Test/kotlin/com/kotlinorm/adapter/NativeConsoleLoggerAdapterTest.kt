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
    fun detection_installs_the_native_console_adapter_on_kronos() {
        val originalLoggerType = Kronos.loggerType
        val originalLoggerFactory = Kronos.defaultLogger
        val originalSink = nativeConsoleLogSink
        val output = mutableListOf<String>()

        try {
            nativeConsoleLogSink = { line -> output += line }
            KronosLoggerApp.detectLoggerImplementation()
            Kronos.defaultLogger("ios-test").info(arrayOf(KLogMessage("ready", newLine = true)))

            assertEquals(KLoggerType.DEFAULT_LOGGER, Kronos.loggerType)
            assertEquals("[INFO] [ios-test] ready\r\n", output.single())
        } finally {
            Kronos.defaultLogger = originalLoggerFactory
            Kronos.loggerType = originalLoggerType
            nativeConsoleLogSink = originalSink
        }
    }
}
