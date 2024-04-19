package com.kotoframework.beans.logging

import com.kotoframework.beans.logging.BundledSimpleLoggerAdapter.Companion.format
import com.kotoframework.enums.ColorPrintCode
import com.kotoframework.enums.KLogLevel
import java.io.File
import java.io.FileWriter

/**
 * Created by sundaiyue on 2022/11/12 14:21
 */

/**
 * Log line
 * @property text
 * @property codes
 * @property endLine
 * @constructor Create empty Log line
 * @author ousc
 */
class KLogMessage(
    private val text: String,
    private val codes: Array<ColorPrintCode> = arrayOf(),
    private var endLine: Boolean = false,
) {

    /**
     * Endl log line
     *
     * @return Log line
     */
    fun endl(): KLogMessage {
        endLine = true
        return this
    }

    /**
     * Print
     *
     * @return Log line
     */
    fun print(level: KLogLevel) {
        val out = if(level > KLogLevel.WARN) System.out else System.err
        if (endLine) {
            out.println(format(text, codes))
        } else {
            out.print(format(text, codes))
        }
    }

    /**
     * Write
     *
     * @param path
     */
    fun write(path: String, fileName: String) {
        val file = File(path, fileName)
        if (!file.exists()) {
            file.createNewFile()
        }
        val writer = FileWriter(file, true)
        if (endLine) {
            writer.write(text + "\r")
        } else {
            writer.write(text)
        }
        writer.flush()
        writer.close()
    }

    fun toArray(): Array<KLogMessage> {
        return arrayOf(this)
    }

    companion object {
        fun logMessageOf(text: String, codes: Array<ColorPrintCode> = arrayOf()): KLogMessage {
            return KLogMessage(text, codes)
        }

        fun Array<KLogMessage>.formatted(): String {
            val sb = StringBuilder()
            this.forEach {
                sb.append(it.text)
                if(it.endLine) {
                    sb.append("\r\n")
                }
            }
            return sb.toString()
        }
    }
}
