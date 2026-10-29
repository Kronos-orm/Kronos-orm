package com.kotlinorm.beans.logging

import com.kotlinorm.enums.KLogLevel
import com.kotlinorm.enums.KLoggerType
import com.kotlinorm.interfaces.KLogger
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KronosLoggingConcurrencyTest {
    @Test
    fun retainedLoggerObservesConfigurationChangedFromAnotherThread() {
        val config = KronosLoggingConfig()
        val first = AtomicInteger()
        val second = AtomicInteger()
        val firstLogger = countingLogger(first)
        val secondLogger = countingLogger(second)
        config.factory = { firstLogger }
        val retained = config.logger("query")
        val ready = CountDownLatch(1)
        val changed = CountDownLatch(1)
        val failure = AtomicReference<Throwable?>()
        val worker = thread(name = "kronos-logging-contract") {
            try {
                retained.info(arrayOf(KLogMessage("before")))
                ready.countDown()
                check(changed.await(10, TimeUnit.SECONDS))
                assertFalse(retained.isInfoEnabled())
                config.enabled = true
                assertFalse(retained.isInfoEnabled())
                assertTrue(retained.isErrorEnabled())
                retained.error(arrayOf(KLogMessage("after")))
                assertEquals(KLoggerType.SLF4J_LOGGER, config.type)
                assertContentEquals(arrayOf("console", "logs"), config.path)
            } catch (cause: Throwable) { failure.set(cause); ready.countDown() }
        }
        try {
            assertTrue(ready.await(10, TimeUnit.SECONDS))
            config.enabled = false
            config.minimumLevel = KLogLevel.ERROR
            config.type = KLoggerType.SLF4J_LOGGER
            config.path = arrayOf("console", "logs")
            config.factory = { secondLogger }
        } finally { changed.countDown(); worker.join(15_000) }
        assertFalse(worker.isAlive)
        assertNull(failure.get())
        assertEquals(1, first.get())
        assertEquals(1, second.get())
    }

    @Test
    fun factoryCallbacksCanWaitForAnotherThreadToReconfigureLogging() {
        val config = KronosLoggingConfig()
        val sink = countingLogger(AtomicInteger())
        config.factory = {
            val updated = CountDownLatch(1)
            val worker = thread(name = "kronos-logging-factory") {
                config.enabled = false
                updated.countDown()
            }
            try {
                assertTrue(updated.await(5, TimeUnit.SECONDS), "Factory callback must run outside the configuration lock")
                sink
            } finally { worker.join(5_000) }
        }
        assertSame(sink, config.resolve("query"))
        assertFalse(config.enabled)
    }

    @Test
    fun destinationArraysCannotMutateTheConfigurationByAlias() {
        val config = KronosLoggingConfig()
        val input = arrayOf("console")
        config.path = input
        input[0] = "mutated input"
        val output = config.path
        output[0] = "mutated output"
        assertContentEquals(arrayOf("console"), config.path)
    }

    private fun countingLogger(counter: AtomicInteger): KLogger = object : KLogger {
        override fun isTraceEnabled() = true
        override fun isDebugEnabled() = true
        override fun isInfoEnabled() = true
        override fun isWarnEnabled() = true
        override fun isErrorEnabled() = true
        override fun trace(messages: Array<KLogMessage>, e: Throwable?) { counter.incrementAndGet() }
        override fun debug(messages: Array<KLogMessage>, e: Throwable?) { counter.incrementAndGet() }
        override fun info(messages: Array<KLogMessage>, e: Throwable?) { counter.incrementAndGet() }
        override fun warn(messages: Array<KLogMessage>, e: Throwable?) { counter.incrementAndGet() }
        override fun error(messages: Array<KLogMessage>, e: Throwable?) { counter.incrementAndGet() }
    }
}
