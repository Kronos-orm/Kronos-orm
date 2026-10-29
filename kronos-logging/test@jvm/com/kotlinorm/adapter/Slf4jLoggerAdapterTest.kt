/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.adapter

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.ThrowableProxy
import ch.qos.logback.core.read.ListAppender
import com.kotlinorm.beans.logging.KLogMessage
import com.kotlinorm.beans.logging.KronosLoggingConfig
import com.kotlinorm.enums.KLoggerType
import org.slf4j.LoggerFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame

class Slf4jLoggerAdapterTest {
    @Test
    fun forwards_real_facade_levels_messages_and_exceptions_with_shared_controls() {
        val backend = LoggerFactory.getLogger("KronosSlf4jContract") as Logger
        val originalLevel = backend.level
        val originalAdditive = backend.isAdditive
        val output = ListAppender<ILoggingEvent>().apply {
            context = backend.loggerContext
            start()
        }
        backend.addAppender(output)
        backend.isAdditive = false
        backend.level = Level.TRACE
        try {
            val adapter = Slf4jLoggerAdapter(backend.name)
            val config = KronosLoggingConfig().apply {
                type = KLoggerType.SLF4J_LOGGER
                factory = { adapter }
            }
            val logger = config.logger("query")
            val messages = arrayOf(KLogMessage("query", newLine = true), KLogMessage("done"))
            val failure = IllegalStateException("database failed")

            assertEquals(List(5) { true }, checks(logger))
            logger.trace(messages, failure)
            logger.debug(messages, failure)
            logger.info(messages, failure)
            logger.warn(messages, failure)
            logger.error(messages, failure)
            assertEquals(listOf(Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR), output.list.map { it.level })
            assertEquals(List(5) { "query\r\ndone" }, output.list.map { it.formattedMessage })
            output.list.forEach { assertSame(failure, (it.throwableProxy as ThrowableProxy).throwable) }

            config.enabled = false
            assertFalse(logger.isInfoEnabled())
            logger.info(messages)
            assertEquals(5, output.list.size)
            config.enabled = true
            logger.info(messages)
            assertEquals(6, output.list.size)
            assertEquals(null, output.list.last().throwableProxy)
            assertEquals(KLoggerType.SLF4J_LOGGER, config.type)

            backend.level = Level.OFF
            assertEquals(List(5) { false }, checks(logger))
            logger.error(messages, failure)
            assertEquals(6, output.list.size)
        } finally {
            backend.detachAppender(output)
            output.stop()
            backend.level = originalLevel
            backend.isAdditive = originalAdditive
        }
    }

    private fun checks(logger: com.kotlinorm.interfaces.KLogger): List<Boolean> = listOf(
        logger.isTraceEnabled(), logger.isDebugEnabled(), logger.isInfoEnabled(),
        logger.isWarnEnabled(), logger.isErrorEnabled()
    )
}
