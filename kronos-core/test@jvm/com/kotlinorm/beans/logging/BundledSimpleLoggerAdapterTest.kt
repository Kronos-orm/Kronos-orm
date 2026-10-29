package com.kotlinorm.beans.logging

import com.kotlinorm.Kronos
import com.kotlinorm.beans.logging.BundledSimpleLoggerAdapter.Companion.logFileNameRule
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class BundledSimpleLoggerAdapterTest {
    private val logger = BundledSimpleLoggerAdapter(BundledSimpleLoggerAdapterTest::class.simpleName!!)
    private val originalPath = Kronos.logging.path
    lateinit var logPath: File
    lateinit var logFile: File

    @BeforeTest
    fun createTempDir() {
        logPath = createTempDirectory("bundledSimpleLoggerTest").toFile()
        logFile = File(logPath, logFileNameRule())
        Kronos.logging.path = [
            logPath.path
        ]
    }

    @AfterTest
    fun deleteTempDir() {
        Kronos.logging.path = originalPath
        logPath.deleteRecursively()
    }

    private fun assertLogLineEquals(expected: String, actual: String) {
        assertEquals(expected, actual.split("[BundledSimpleLoggerAdapterTest] ").getOrElse(1) { actual })
        logPath.deleteRecursively()
    }

    private fun assertLogLineEquals(expected: List<String>, actual: List<String>) {
        expected.forEachIndexed { index, message ->
            assertLogLineEquals(message, actual[index])
        }
        logPath.deleteRecursively()
    }


    @Test
    fun `global disable and empty paths discard messages before enabling again`() {
        val originalEnabled = Kronos.logging.enabled
        try {
            Kronos.logging.enabled = false
            logger.info(log { -"globally disabled" })
            assertFalse(logFile.exists())
            Kronos.logging.enabled = true
            Kronos.logging.path = emptyArray()
            logger.info(log { -"no destinations" })
            assertFalse(logFile.exists())
            Kronos.logging.path = arrayOf(logPath.path)
            logger.info(log { -"retained" })
            assertLogLineEquals("retained", logFile.readText())
        } finally {
            Kronos.logging.enabled = originalEnabled
        }
    }

    @Test
    fun `bundled output includes the original exception stack trace`() {
        val failure = IllegalStateException("database failure")
        logger.error(log { -"failed" }, failure)
        assertLogLineEquals("failed\r\n${failure.stackTraceToString()}\r\n", logFile.readText())
    }

    @Test
    fun testInfo() {
        val expected = "Hello, World!"
        logger.info(log { -expected })
        assertLogLineEquals(expected, logFile.readText())
    }

    @Test
    fun testInfoMultiple() {
        val expected = log {
            +"Hello, World!"
            +"Hello, World!"
        }
        logger.info(expected)
        val actual = logFile.readLines()
        assertLogLineEquals(expected.map { it.text }, actual)
    }

    @Test
    fun `disabled info level does not write a log task`() {
        val originalEnabled = BundledSimpleLoggerAdapter.infoEnabled
        try {
            BundledSimpleLoggerAdapter.infoEnabled = false
            logger.info(log { -"suppressed" })
            assertFalse(logFile.exists())
        } finally {
            BundledSimpleLoggerAdapter.infoEnabled = originalEnabled
        }
    }

    @Test
    fun testWarn() {
        val expected = "Hello, World!"
        logger.warn(log { -expected })
        assertLogLineEquals(expected, logFile.readText())
    }

    @Test
    fun testWarnMultiple() {
        val expected = log {
            +"Hello, World!"
            +"Hello, World!"
        }
        logger.warn(expected)
        val actual = logFile.readLines()
        assertLogLineEquals(expected.map { it.text }, actual)
    }

    @Test
    fun testError() {
        val expected = "Hello, World!"
        logger.error(log { -expected })
        assertLogLineEquals(expected, logFile.readText())
    }

    @Test
    fun testErrorMultiple() {
        val expected = log {
            +"Hello, World!"
            +"Hello, World!"
        }
        logger.error(expected)
        val actual = logFile.readLines()
        assertLogLineEquals(expected.map { it.text }, actual)
    }

    @Test
    fun testDebug() {
        val expected = "Hello, World!"
        logger.debug(log {
            -expected
        })
        assertLogLineEquals(expected, logFile.readText())
    }

    @Test
    fun testDebugMultiple() {
        val expected = log {
            +"Hello, World!"
            +"Hello, World!"
        }
        logger.debug(expected)
        val actual = logFile.readLines()
        assertLogLineEquals(expected.map { it.text }, actual)
    }

    @Test
    fun testTrace() {
        val expected = "Hello, World!"
        logger.trace(log {
            -expected
        })
        assertLogLineEquals(expected, logFile.readText())
    }

    @Test
    fun testTraceMultiple() {
        val expected = log {
            +"Hello, World!"
            +"Hello, World!"
        }
        logger.trace(expected)
        val actual = logFile.readLines()
        assertLogLineEquals(expected.map { it.text }, actual)
    }
}
