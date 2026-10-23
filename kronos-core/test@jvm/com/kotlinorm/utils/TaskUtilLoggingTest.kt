@file:OptIn(com.kotlinorm.annotations.InternalKronosApi::class)

package com.kotlinorm.utils

import com.kotlinorm.Kronos
import com.kotlinorm.beans.logging.KLogMessage
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.interfaces.KLogger
import com.kotlinorm.enums.QueryType
import kotlin.reflect.typeOf
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class TaskUtilLoggingTest {
    private val originalLoggerFactory = Kronos.logging.factory
    private val originalLoggerType = Kronos.logging.type

    @AfterTest
    fun restoreLogger() {
        Kronos.logging.factory = originalLoggerFactory
        Kronos.logging.type = originalLoggerType
    }

    @Test
    fun `disabled info logger does not receive ORM execution messages`() {
        val logger = DisabledLogger()
        Kronos.logging.factory = { logger }

        handleLogResult(
            KronosAtomicQueryTask(
                sql = "SELECT 1",
                targetType = typeOf<List<Any?>>()
            ),
            listOf(1),
            QueryType.ToList
        )

        assertEquals(0, logger.infoCalls)
    }

    @Test
    fun `global switch skips SQL parameter formatting and logger creation`() {
        val originalEnabled = Kronos.logging.enabled
        var factories = 0
        Kronos.logging.factory = { factories++; DisabledLogger() }
        val unformattable = object {
            override fun toString(): String = error("Disabled SQL logging must not format parameters")
        }
        try {
            Kronos.logging.enabled = false
            handleLogResult(
                KronosAtomicQueryTask(
                    sql = "SELECT :value",
                    paramMap = mapOf("value" to unformattable),
                    targetType = typeOf<List<Any?>>()
                ),
                listOf(1),
                QueryType.ToList
            )
            assertEquals(0, factories)
        } finally {
            Kronos.logging.enabled = originalEnabled
        }
    }

    private class DisabledLogger : KLogger {
        var infoCalls = 0

        override fun isTraceEnabled() = false
        override fun trace(messages: Array<KLogMessage>, e: Throwable?) = Unit
        override fun isDebugEnabled() = false
        override fun debug(messages: Array<KLogMessage>, e: Throwable?) = Unit
        override fun isInfoEnabled() = false
        override fun info(messages: Array<KLogMessage>, e: Throwable?) {
            infoCalls++
        }
        override fun isWarnEnabled() = false
        override fun warn(messages: Array<KLogMessage>, e: Throwable?) = Unit
        override fun isErrorEnabled() = false
        override fun error(messages: Array<KLogMessage>, e: Throwable?) = Unit
    }
}
