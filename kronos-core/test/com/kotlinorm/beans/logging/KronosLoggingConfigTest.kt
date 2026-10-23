@file:OptIn(com.kotlinorm.annotations.InternalKronosApi::class)

package com.kotlinorm.beans.logging

import com.kotlinorm.enums.KLogLevel
import com.kotlinorm.enums.KLoggerType
import com.kotlinorm.interfaces.KLogger
import com.kotlinorm.interfaces.infoIfEnabled
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class KronosLoggingConfigTest {
    @Test
    fun defaults_to_enabled_console_output() {
        val config = KronosLoggingConfig()
        assertTrue(config.enabled)
        assertContentEquals(arrayOf("console"), config.path)
        assertEquals(KLoggerType.DEFAULT_LOGGER, config.type)
        assertEquals(KLogLevel.TRACE, config.minimumLevel)
        assertEquals(null, config.factory)
    }

    @Test
    fun disabling_prevents_all_levels_factory_creation_and_message_building() {
        val config = KronosLoggingConfig()
        var factories = 0
        var messages = 0
        config.factory = { factories++; RecordingLogger() }
        val logger = config.logger("test")
        config.enabled = false

        assertEquals(List(5) { false }, checks(logger))
        emitAll(logger)
        logger.infoIfEnabled { messages++; arrayOf(KLogMessage("built")) }
        assertEquals(0, factories)
        assertEquals(0, messages)
    }

    @Test
    fun toggling_existing_logger_preserves_factory_type_paths_and_exceptions() {
        val config = KronosLoggingConfig()
        val sink = RecordingLogger()
        val factory = { _: Any -> sink }
        val error = IllegalStateException("database failed")
        config.factory = factory
        config.type = KLoggerType.SLF4J_LOGGER
        config.path = emptyArray()
        val logger = config.logger("database")

        config.enabled = false
        emitAll(logger, error)
        assertEquals(emptyList(), sink.levels)
        config.enabled = true
        assertEquals(List(5) { true }, checks(logger))
        emitAll(logger, error)
        assertEquals(listOf("TRACE", "DEBUG", "INFO", "WARN", "ERROR"), sink.levels)
        assertTrue(sink.errors.all { it === error })
        assertTrue(sink.messages.all { it.single().text == "message" })
        assertSame(factory, config.factory)
        assertEquals(KLoggerType.SLF4J_LOGGER, config.type)
        assertContentEquals(emptyArray(), config.path)
    }

    @Test
    fun minimum_level_and_backend_checks_both_filter_direct_and_lazy_calls() {
        val config = KronosLoggingConfig()
        val sink = RecordingLogger()
        config.factory = { sink }
        config.minimumLevel = KLogLevel.WARN
        val logger = config.logger("test")
        assertEquals(listOf(false, false, false, true, true), checks(logger))
        emitAll(logger)
        assertEquals(listOf("WARN", "ERROR"), sink.levels)

        config.minimumLevel = KLogLevel.TRACE
        sink.accepts = false
        assertEquals(List(5) { false }, checks(logger))
        emitAll(logger)
        logger.infoIfEnabled { error("Disabled backend must not build messages") }
        assertEquals(listOf("WARN", "ERROR"), sink.levels)
    }

    @Test
    fun custom_factory_takes_precedence_without_changing_adapter_selection() {
        val config = KronosLoggingConfig()
        val platform = RecordingLogger()
        val custom = RecordingLogger()
        var source: Any? = null
        config.installAdapterFactory { source = it; platform }
        val logger = config.logger("query")
        logger.info(arrayOf(KLogMessage("platform")))
        config.factory = { custom }
        logger.info(arrayOf(KLogMessage("custom")))
        config.factory = null
        logger.info(arrayOf(KLogMessage("platform again")))
        assertEquals("query", source)
        assertEquals(listOf("platform", "platform again"), platform.messages.map { it.single().text })
        assertEquals(listOf("custom"), custom.messages.map { it.single().text })
        assertEquals(KLoggerType.DEFAULT_LOGGER, config.type)
    }

    @Test
    fun non_bundled_adapter_requires_platform_installation_or_a_custom_factory() {
        val config = KronosLoggingConfig()
        config.type = KLoggerType.SLF4J_LOGGER
        val error = assertFailsWith<IllegalStateException> { config.logger("test").isInfoEnabled() }
        assertEquals("Logging adapter SLF4J_LOGGER is not installed. Initialize kronos-logging or configure logging.factory.", error.message)
        config.factory = { RecordingLogger() }
        assertTrue(config.logger("test").isInfoEnabled())
    }

    private fun checks(logger: KLogger) = listOf(logger.isTraceEnabled(), logger.isDebugEnabled(),
        logger.isInfoEnabled(), logger.isWarnEnabled(), logger.isErrorEnabled())

    private fun emitAll(logger: KLogger, error: Throwable? = null) {
        val messages = arrayOf(KLogMessage("message"))
        logger.trace(messages, error)
        logger.debug(messages, error)
        logger.info(messages, error)
        logger.warn(messages, error)
        logger.error(messages, error)
    }

    private class RecordingLogger : KLogger {
        var accepts = true
        val levels = mutableListOf<String>()
        val messages = mutableListOf<Array<KLogMessage>>()
        val errors = mutableListOf<Throwable?>()
        private fun record(level: String, message: Array<KLogMessage>, error: Throwable?) {
            levels += level; messages += message; errors += error
        }
        override fun isTraceEnabled() = accepts
        override fun isDebugEnabled() = accepts
        override fun isInfoEnabled() = accepts
        override fun isWarnEnabled() = accepts
        override fun isErrorEnabled() = accepts
        override fun trace(messages: Array<KLogMessage>, e: Throwable?) = record("TRACE", messages, e)
        override fun debug(messages: Array<KLogMessage>, e: Throwable?) = record("DEBUG", messages, e)
        override fun info(messages: Array<KLogMessage>, e: Throwable?) = record("INFO", messages, e)
        override fun warn(messages: Array<KLogMessage>, e: Throwable?) = record("WARN", messages, e)
        override fun error(messages: Array<KLogMessage>, e: Throwable?) = record("ERROR", messages, e)
    }
}
