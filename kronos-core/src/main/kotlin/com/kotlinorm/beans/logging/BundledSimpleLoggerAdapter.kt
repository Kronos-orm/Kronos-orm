/**
 * Copyright 2022-2024 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kotlinorm.beans.logging

import com.kotlinorm.Kronos.logPath
import com.kotlinorm.enums.ColorPrintCode
import com.kotlinorm.enums.KLogLevel
import com.kotlinorm.interfaces.KLogger
import com.kotlinorm.utils.DateTimeUtil.currentDateTime
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

/**
 * BundledSimpleLoggerAdapter
 *
 * Bundled Simple Logger Adapter by Kronos ORM, support color print on console and write to file.
 *
 * @property tagName the name of the logger
 */
class BundledSimpleLoggerAdapter(private val tagName: String) : KLogger {
    companion object {
        private var logLock = false
        private var logTaskList = mutableListOf<LogTask>() // log task queue
        private const val SEMICOLON = ";"

        var logDateTimeFormat = "yyyy-MM-dd HH:mm:ss"
        var traceEnabled = true
        var debugEnabled = true
        var infoEnabled = true
        var warnEnabled = true
        var errorEnabled = true
        var logFileNameRule = { "kronos-log-${currentDateTime("yyyy-MM-dd")}.log" }

        /**
         * 格式化信息
         *
         * @param txt  the text to format
         * @param codes the style codes
         * @return the formatted text
         */
        internal fun format(txt: String, codes: Array<ColorPrintCode>): String {
            val codeStr = codes.joinToString(SEMICOLON) { it.code.toString() }
            return 27.toChar().toString() + "[" + codeStr + "m" + txt + 27.toChar() + "[0m"
        }
    }

    /**
     * Log Task
     *
     * @property level the log level
     * @property messages the log messages
     */
    data class LogTask(val level: KLogLevel, val messages: List<KLogMessage>)

    private fun Array<KLogMessage>.attachCodes(codes: Array<ColorPrintCode>): MutableList<KLogMessage> {
        return this.onEach { if (it.codes.isEmpty()) it.codes = codes }.toMutableList()
    }

    override fun isTraceEnabled(): Boolean {
        return traceEnabled
    }

    override fun trace(messages: Array<KLogMessage>, e: Throwable?) {
        logTaskList.add(
            LogTask(
                KLogLevel.TRACE,
                messages.attachCodes(ColorPrintCode.BLUE.toArray())
                    .apply {
                        add(
                            0,
                            KLogMessage(
                                "[${currentDateTime(logDateTimeFormat)}] [trace] [$tagName] ",
                                ColorPrintCode.BLUE.toArray()
                            )
                        )
                    }
            )
        )
        if (!logLock) executeLogTask()
    }

    override fun isDebugEnabled(): Boolean {
        return debugEnabled
    }

    override fun debug(messages: Array<KLogMessage>, e: Throwable?) {
        logTaskList.add(
            LogTask(
                KLogLevel.DEBUG,
                messages.attachCodes(ColorPrintCode.MAGENTA.toArray())
                    .apply {
                        add(
                            0,
                            KLogMessage(
                                "[${currentDateTime(logDateTimeFormat)}] [debug] [$tagName] ",
                                ColorPrintCode.MAGENTA.toArray()
                            )
                        )
                    }
            )
        )
        if (!logLock) executeLogTask()
    }

    override fun isInfoEnabled(): Boolean {
        return infoEnabled
    }

    override fun info(messages: Array<KLogMessage>, e: Throwable?) {
        logTaskList.add(
            LogTask(
                KLogLevel.INFO,
                messages.attachCodes(ColorPrintCode.CYAN.toArray())
                    .apply {
                        add(
                            0,
                            KLogMessage(
                                "[${currentDateTime(logDateTimeFormat)}] [info] [$tagName] ",
                                ColorPrintCode.CYAN.toArray()
                            )
                        )
                    }
            )
        )
        if (!logLock) executeLogTask()
    }

    override fun isWarnEnabled(): Boolean {
        return warnEnabled
    }

    override fun warn(messages: Array<KLogMessage>, e: Throwable?) {
        logTaskList.add(
            LogTask(
                KLogLevel.WARN,
                messages.attachCodes(ColorPrintCode.YELLOW.toArray())
                    .apply {
                        add(
                            0,
                            KLogMessage(
                                "[${currentDateTime(logDateTimeFormat)}] [warn] [$tagName] ",
                                ColorPrintCode.YELLOW.toArray()
                            )
                        )
                    }
            )
        )
        if (!logLock) executeLogTask()
    }

    override fun isErrorEnabled(): Boolean {
        return errorEnabled
    }

    override fun error(messages: Array<KLogMessage>, e: Throwable?) {
        logTaskList.add(
            LogTask(
                KLogLevel.ERROR,
                messages.attachCodes(ColorPrintCode.RED.toArray())
                    .apply {
                        add(
                            0,
                            KLogMessage(
                                "[${currentDateTime(logDateTimeFormat)}] [error] [$tagName] ",
                                ColorPrintCode.RED.toArray()
                            )
                        )
                    }
            )
        )
        if (!logLock) executeLogTask()
    }

    /**
     * Executes the log task asynchronously.
     */
    private fun executeLogTask() {
        Runnable {
            logLock = true
            while (logTaskList.isNotEmpty()) {
                val logTask = logTaskList.first()
                logPath.forEach { path ->
                    if (path == "console") {
                        logTask.messages.forEach { message ->
                            message.print(logTask.level)
                        }
                    } else {
                        val directory = Path(path)
                        if (!SystemFileSystem.exists(directory)) {
                            SystemFileSystem.createDirectories(directory)
                        }
                        val logFileName = logFileNameRule()
                        logTask.messages.forEach { message ->
                            message.write(Path(path, logFileName))
                        }
                    }
                }
                logTaskList.removeFirst()
            }
            logLock = false
        }.run()
    }
}