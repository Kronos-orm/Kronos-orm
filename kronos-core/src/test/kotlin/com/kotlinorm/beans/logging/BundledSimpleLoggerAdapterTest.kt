package com.kotlinorm.beans.logging

import com.kotlinorm.Kronos
import com.kotlinorm.beans.logging.BundledSimpleLoggerAdapter.Companion.logFileNameRule
import com.kotlinorm.beans.logging.KLogMessage.Companion.kMsgOf
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.charset.Charset
import kotlin.test.assertEquals

class BundledSimpleLoggerAdapterTest {
    private val logger = BundledSimpleLoggerAdapter(BundledSimpleLoggerAdapterTest::class.java.simpleName)
    private val logFile = File("build/tmp/logs/${logFileNameRule()}")
    private val logPath = File("build/tmp/logs")

    init {
        Kronos.logPath = listOf(
            logPath.path
        )
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
        logger.info(kMsgOf(expected).toArray())
        assertLogLineEquals(expected, logFile.readText(Charset.forName("UTF-8")))
    }

    @Test
    fun testInfoMultiple() {
        val expected =
            arrayOf(
                kMsgOf("Hello, World!").endl(),
                kMsgOf("Hello, World!")
            )
        logger.info(expected)
        val actual = logFile.readLines(Charset.forName("UTF-8"))
        assertLogLineEquals(expected.map { it.text }, actual)
    }

    @Test
    fun testWarn() {
        val expected = "Hello, World!"
        logger.warn(kMsgOf(expected).toArray())
        assertLogLineEquals(expected, logFile.readText(Charset.forName("UTF-8")))
    }

    @Test
    fun testWarnMultiple() {
        val expected =
            arrayOf(
                kMsgOf("Hello, World!").endl(),
                kMsgOf("Hello, World!")
            )
        logger.warn(expected)
        val actual = logFile.readLines(Charset.forName("UTF-8"))
        assertLogLineEquals(expected.map { it.text }, actual)
    }

    @Test
    fun testError() {
        val expected = "Hello, World!"
        logger.error(kMsgOf(expected).toArray())
        assertLogLineEquals(expected, logFile.readText(Charset.forName("UTF-8")))
    }

    @Test
    fun testErrorMultiple() {
        val expected =
            arrayOf(
                kMsgOf("Hello, World!").endl(),
                kMsgOf("Hello, World!")
            )
        logger.error(expected)
        val actual = logFile.readLines(Charset.forName("UTF-8"))
        assertLogLineEquals(expected.map { it.text }, actual)
    }

    @Test
    fun testDebug() {
        val expected = "Hello, World!"
        logger.debug(kMsgOf(expected).toArray())
        assertLogLineEquals(expected, logFile.readText(Charset.forName("UTF-8")))
    }

    @Test
    fun testDebugMultiple() {
        val expected =
            arrayOf(
                kMsgOf("Hello, World!").endl(),
                kMsgOf("Hello, World!")
            )
        logger.debug(expected)
        val actual = logFile.readLines(Charset.forName("UTF-8"))
        assertLogLineEquals(expected.map { it.text }, actual)
    }

    @Test
    fun testTrace() {
        val expected = "Hello, World!"
        logger.trace(kMsgOf(expected).toArray())
        assertLogLineEquals(expected, logFile.readText(Charset.forName("UTF-8")))
    }

    @Test
    fun testTraceMultiple() {
        val expected =
            arrayOf(
                kMsgOf("Hello, World!").endl(),
                kMsgOf("Hello, World!")
            )
        logger.trace(expected)
        val actual = logFile.readLines(Charset.forName("UTF-8"))
        assertLogLineEquals(expected.map { it.text }, actual)
    }
}