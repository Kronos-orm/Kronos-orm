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
import com.kotlinorm.enums.KLogLevel
import com.kotlinorm.interfaces.KLogger
import com.kotlinorm.interfaces.PrintCode
import com.kotlinorm.utils.DateTimeUtil.currentDateTime
import java.nio.file.Files
import java.util.concurrent.*
import java.util.concurrent.locks.*
import kotlin.io.path.Path

/**
 * BundledSimpleLoggerAdapter
 *
 * Bundled Simple Logger Adapter by Kronos ORM, support color print on console and write to file.
 *
 * @property tagName the name of the logger
 */
class BundledSimpleLoggerAdapter(private val tagName: String) : KLogger {
    companion object {
        private val synchronized = ReentrantLock()
        private var logTaskList = ConcurrentLinkedQueue<LogTask>() // log task queue
        private const val SEMICOLON = ";"

        var logDateTimeFormat = "yyyy-MM-dd HH:mm:ss.SSS"
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
        internal fun format(txt: String, codes: Array<PrintCode>): String {
            val codeStr = codes.joinToString(SEMICOLON) { it.code.toString() }
            return 27.toChar().toString() + "[" + codeStr + "m" + txt + 27.toChar() + "[0m"
        }
    }

    private val current get() = currentDateTime(logDateTimeFormat)

    override fun isTraceEnabled(): Boolean {
        return traceEnabled
    }

    override fun trace(messages: Array<KLogMessage>, e: Throwable?) {
        logTaskList.add(
            LogTask(
                KLogLevel.TRACE,
                log {
                    -"[$current] [trace] [$tagName] "[blue] + messages
                }
            )
        )
        executeLogTask()
    }

    override fun isDebugEnabled(): Boolean {
        return debugEnabled
    }

    override fun debug(messages: Array<KLogMessage>, e: Throwable?) {
        logTaskList.add(
            LogTask(
                KLogLevel.DEBUG,
                log {
                    -"[$current] [debug] [$tagName] "[magenta] + messages
                }
            )
        )
       executeLogTask()
    }

    override fun isInfoEnabled(): Boolean {
        return infoEnabled
    }

    override fun info(messages: Array<KLogMessage>, e: Throwable?) {
        logTaskList.add(
            LogTask(
                KLogLevel.INFO,
                log {
                    -"[$current] [info] [$tagName] "[cyan] + messages
                }
            )
        )
        executeLogTask()
    }

    override fun isWarnEnabled(): Boolean {
        return warnEnabled
    }

    override fun warn(messages: Array<KLogMessage>, e: Throwable?) {
        logTaskList.add(
            LogTask(
                KLogLevel.WARN,
                log {
                    -"[$current] [warn] [$tagName] "[yellow] + messages
                }
            )
        )
        executeLogTask()
    }

    override fun isErrorEnabled(): Boolean {
        return errorEnabled
    }

    override fun error(messages: Array<KLogMessage>, e: Throwable?) {
        logTaskList.add(
            LogTask(
                KLogLevel.ERROR,
                log {
                    -"[$current] [error] [$tagName] "[red] + messages
                }
            )
        )
        executeLogTask()
    }

    /**
     * Executes the log task asynchronously.
     */
    private fun executeLogTask() {
        if(logPath.isEmpty()) return
        if (synchronized.tryLock()) { // 尝试获取锁
            Runnable {
                try {
                    while (logTaskList.isNotEmpty()) {
                        val logTask = logTaskList.poll()
                        logPath.forEach { path ->
                            if (path == "console") {
                                logTask.messages.forEach { message ->
                                    message.print(logTask.level)
                                }
                            } else {
                                val directory = Path(path)
                                if (!Files.exists(directory)) {
                                    Files.createDirectories(directory)
                                }
                                val logFileName = logFileNameRule()
                                logTask.messages.forEach { message ->
                                    message.write(Path(path, logFileName))
                                }
                            }
                        }
                    }
                } finally {
                    synchronized.unlock()
                }
            }.run()
        }
    }
}