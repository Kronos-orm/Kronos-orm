/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm

import com.kotlinorm.beans.logging.KLogMessage
import com.kotlinorm.beans.logging.KLogMessage.Companion.formatted
import com.kotlinorm.beans.logging.log
import kotlin.test.Test
import kotlin.test.assertEquals

class KronosLoggingContractTest {
    @Test
    fun preserves_message_builder_and_transport_format() {
        val messages = log {
            -"one"
            +"two"
        }

        assertEquals("one", messages[0].text)
        assertEquals(false, messages[0].newLine)
        assertEquals("two", messages[1].text)
        assertEquals(true, messages[1].newLine)
        assertEquals("onetwo\r\n", messages.formatted())
    }

    @Test
    fun formats_direct_messages_without_platform_services() {
        assertEquals(
            "ready\r\n",
            arrayOf(KLogMessage("ready", newLine = true)).formatted()
        )
    }
}
