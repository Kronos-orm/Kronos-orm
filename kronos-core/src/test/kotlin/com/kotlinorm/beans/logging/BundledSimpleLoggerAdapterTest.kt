package com.kotlinorm.beans.logging

import com.kotlinorm.Kronos
import com.kotlinorm.beans.logging.BundledSimpleLoggerAdapter.Companion.logFileNameRule
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class BundledSimpleLoggerAdapterTest {
    private val logger = BundledSimpleLoggerAdapter(BundledSimpleLoggerAdapterTest::class.simpleName!!)
    lateinit var logPath: File
    lateinit var logFile: File

    @BeforeTest
    fun createTempDir() {
        logPath = createTempDirectory("bundledSimpleLoggerTest").toFile()
        logFile = File(logPath, logFileNameRule())
        Kronos.logPath = listOf(
            logPath.path
        )
    }

    @AfterTest
    fun deleteTempDir() {
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