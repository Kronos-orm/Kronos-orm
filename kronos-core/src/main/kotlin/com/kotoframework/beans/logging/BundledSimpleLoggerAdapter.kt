package com.kotoframework.beans.logging

import com.kotoframework.enums.ColorPrintCode
import com.kotoframework.enums.KLogLevel
import com.kotoframework.interfaces.KLogger
import com.kotoframework.utils.DateTimeUtil.currentDateTime

/**
 * Bundled Simple Logger Adapter by Kronos ORM
 */
class BundledSimpleLoggerAdapter(private val tagName: String) : KLogger {
    companion object {
        private var logLock = false
        private var logTaskList = mutableListOf<LogTask>() // log task queue
        private const val SEMICOLON = ";"


        var logPath = mutableListOf("console")
        var logDateTimeFormat = "yyyy-MM-dd HH:mm:ss"
        var colorPrintEnabled = true
        var traceEnabled = true
        var debugEnabled = true
        var infoEnabled = true
        var warnEnabled = true
        var errorEnabled = true
        var logFileNameRule = { "kronos-log-${currentDateTime("yyyy-MM-dd")}.log" }

        /**
         * 格式化信息
         *
         * @param txt   信息
         * @param codes 参数集合
         * @return 格式化后的信息
         */
        internal fun format(txt: String, codes: Array<ColorPrintCode>): String {
            if (!colorPrintEnabled) {
                return txt
            }
            val codeStr = java.lang.String.join(
                SEMICOLON,
                codes.map { code -> code.code.toString() })
            return 27.toChar().toString() + "[" + codeStr + "m" + txt + 27.toChar() + "[0m"
        }
    }

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
                        logTask.messages.forEach { message ->
                            message.write(path, logFileNameRule())
                        }
                    }
                }
                logTaskList.removeFirst()
            }
            logLock = false
        }.run()
    }
}