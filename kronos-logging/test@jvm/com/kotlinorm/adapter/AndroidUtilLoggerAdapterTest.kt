package com.kotlinorm.adapter

import android.util.Log
import com.kotlinorm.Kronos
import com.kotlinorm.KronosLoggerApp
import com.kotlinorm.beans.logging.KLogMessage
import com.kotlinorm.enums.KLoggerType
import com.kotlinorm.interfaces.KLogger
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class AndroidUtilLoggerAdapterTest {
    private val logger = AndroidUtilLoggerAdapter("KronosTest")

    @BeforeTest
    fun resetLog() {
        Log.reset()
    }

    @Test
    fun `uses Android Log priorities for enabled checks`() {
        assertTrue(logger.isTraceEnabled())
        assertEquals(2, Log.lastPriority)
        assertTrue(logger.isDebugEnabled())
        assertEquals(3, Log.lastPriority)
        assertTrue(logger.isInfoEnabled())
        assertEquals(4, Log.lastPriority)
        assertTrue(logger.isWarnEnabled())
        assertEquals(5, Log.lastPriority)
        assertTrue(logger.isErrorEnabled())
        assertEquals(6, Log.lastPriority)
        assertEquals("KronosTest", Log.lastTag)
    }

    @Test
    fun `forwards each Kronos level to Android Log`() {
        val message = arrayOf(KLogMessage("database message"))
        val failure = IllegalStateException("database failure")

        logger.trace(message)
        assertEquals("v", Log.lastMethod)
        logger.debug(message)
        assertEquals("d", Log.lastMethod)
        logger.info(message)
        assertEquals("i", Log.lastMethod)
        logger.warn(message)
        assertEquals("w", Log.lastMethod)
        logger.error(message, failure)
        assertEquals("e", Log.lastMethod)
        assertEquals("KronosTest", Log.lastTag)
        assertEquals("database message", Log.lastMessage)
        assertSame(failure, Log.lastThrowable)
    }

    @Test
    fun `detecting logging installs the Android adapter`() {
        val originalLoggerType = Kronos.loggerType
        val originalLoggerFactory = Kronos.defaultLogger

        try {
            KronosLoggerApp.detectLoggerImplementation()
            val detectedLogger = Kronos.defaultLogger("KronosDetected")

            assertEquals(KLoggerType.ANDROID_LOGGER, Kronos.loggerType)
            assertTrue(detectedLogger is AndroidUtilLoggerAdapter)

            detectedLogger.info(arrayOf(KLogMessage("detected Android logger")))
            assertEquals("i", Log.lastMethod)
            assertEquals("KronosDetected", Log.lastTag)
            assertEquals("detected Android logger", Log.lastMessage)
        } finally {
            Kronos.defaultLogger = originalLoggerFactory
            Kronos.loggerType = originalLoggerType
        }
    }

    @Test
    fun `logger detection preserves an explicitly configured factory`() {
        val originalLoggerType = Kronos.loggerType
        val originalLoggerFactory = Kronos.defaultLogger
        val customLogger = DisabledLogger()
        val customFactory = { _: Any -> customLogger }

        try {
            Kronos.defaultLogger = customFactory

            KronosLoggerApp.detectLoggerImplementation()

            assertTrue(Kronos.defaultLogger === customFactory)
            assertEquals(KLoggerType.NONE, Kronos.loggerType)
        } finally {
            Kronos.defaultLogger = originalLoggerFactory
            Kronos.loggerType = originalLoggerType
        }
    }

    private class DisabledLogger : KLogger {
        override fun isTraceEnabled() = false
        override fun trace(messages: Array<KLogMessage>, e: Throwable?) = Unit
        override fun isDebugEnabled() = false
        override fun debug(messages: Array<KLogMessage>, e: Throwable?) = Unit
        override fun isInfoEnabled() = false
        override fun info(messages: Array<KLogMessage>, e: Throwable?) = Unit
        override fun isWarnEnabled() = false
        override fun warn(messages: Array<KLogMessage>, e: Throwable?) = Unit
        override fun isErrorEnabled() = false
        override fun error(messages: Array<KLogMessage>, e: Throwable?) = Unit
    }
}
