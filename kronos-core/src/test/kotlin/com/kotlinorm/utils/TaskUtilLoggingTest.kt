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
    private val originalLoggerFactory = Kronos.defaultLogger
    private val originalLoggerType = Kronos.loggerType

    @AfterTest
    fun restoreLogger() {
        Kronos.defaultLogger = originalLoggerFactory
        Kronos.loggerType = originalLoggerType
    }

    @Test
    fun `disabled info logger does not receive ORM execution messages`() {
        val logger = DisabledLogger()
        Kronos.defaultLogger = { logger }

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
