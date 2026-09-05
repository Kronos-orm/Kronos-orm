package com.kotlinorm.beans.logging

import com.kotlinorm.enums.KLogLevel
import com.kotlinorm.enums.PrintColor
import com.kotlinorm.enums.PrintStyle
import java.lang.reflect.InvocationTargetException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class KLogMessageBuilderTest {

    @Test
    fun `log builder accepts messages arrays lists styles and rejects unsupported values`() {
        val first = KLogMessage("first")
        val second = KLogMessage("second")
        val styled = KLogMessage("styled")
        val builder = KLogMessageBuilder()

        with(builder) {
            add(first)
            -arrayOf(second)
            invokeAddElement(this, listOf(KLogMessage("third")))
            +styled[bold, underline]
            -arrayOf(KLogMessage("fourth"))[red]
        }
        val messages = builder.lines

        assertEquals(
            listOf(
                LogShape("first", false, emptyList()),
                LogShape("second", false, emptyList()),
                LogShape("third", false, emptyList()),
                LogShape("styled", true, listOf(PrintStyle.BOLD.code, PrintStyle.UNDERLINE.code)),
                LogShape("fourth", false, listOf(PrintColor.RED.code))
            ),
            messages.map { LogShape(it.text, it.newLine, it.codes.map { code -> code.code }) }
        )
        assertEquals(
            "Unsupported type: class java.lang.Integer",
            assertFailsWith<IllegalArgumentException> {
                invokeAddElement(KLogMessageBuilder(), 1)
            }.message
        )
    }

    @Test
    fun `log task compares message arrays by content`() {
        val firstMessage = KLogMessage("first")
        val secondMessage = KLogMessage("second")
        val first = LogTask(KLogLevel.INFO, arrayOf(firstMessage, secondMessage))
        val same = LogTask(KLogLevel.INFO, arrayOf(firstMessage, secondMessage))
        val differentLevel = LogTask(KLogLevel.ERROR, arrayOf(firstMessage, secondMessage))
        val differentMessages = LogTask(KLogLevel.INFO, arrayOf(firstMessage))

        assertEquals(first, first)
        assertEquals(first, same)
        assertEquals(first.hashCode(), same.hashCode())
        assertEquals(false, first == differentLevel)
        assertEquals(false, first == differentMessages)
        assertEquals(false, first.equals("not a log task"))
    }

    private fun invokeAddElement(builder: KLogMessageBuilder, element: Any) {
        val method = KLogMessageBuilder::class.java.getDeclaredMethod("addElement", Any::class.java)
        method.isAccessible = true
        try {
            method.invoke(builder, element)
        } catch (e: InvocationTargetException) {
            throw e.targetException
        }
    }

    private data class LogShape(val text: String, val newLine: Boolean, val codes: List<Int>)
}
