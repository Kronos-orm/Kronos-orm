/**
 * Copyright 2022-2025 kronos-orm
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

import com.kotlinorm.enums.PrintBackground
import com.kotlinorm.enums.PrintColor
import com.kotlinorm.enums.PrintStyle
import com.kotlinorm.interfaces.PrintCode

class KLogMessageBuilder {
    private val _lines = mutableListOf<KLogMessage>()
    val lines: List<KLogMessage> get() = _lines

    val black = PrintColor.BLACK
    val red = PrintColor.RED
    val green = PrintColor.GREEN
    val yellow = PrintColor.YELLOW
    val blue = PrintColor.BLUE
    val magenta = PrintColor.MAGENTA
    val cyan = PrintColor.CYAN
    val grey = PrintColor.GREY

    val bgBlack = PrintBackground.BLACK_BACKGROUND
    val bgRed = PrintBackground.RED_BACKGROUND
    val bgGreen = PrintBackground.GREEN_BACKGROUND
    val bgYellow = PrintBackground.YELLOW_BACKGROUND
    val bgBlue = PrintBackground.BLUE_BACKGROUND
    val bgMagenta = PrintBackground.MAGENTA_BACKGROUND
    val bgCyan = PrintBackground.CYAN_BACKGROUND
    val bgGrey = PrintBackground.GREY_BACKGROUND

    val bold = PrintStyle.BOLD
    val italic = PrintStyle.ITALIC
    val underline = PrintStyle.UNDERLINE
    val blink = PrintStyle.BLINK
    val reverse = PrintStyle.REVERSE
    val hidden = PrintStyle.HIDDEN

    fun add(element: KLogMessage) {
        _lines.add(element)
    }

    // 添加消息的核心方法
    private fun addElement(element: Any) {
        when (element) {
            is KLogMessage -> _lines.add(element)
            is Array<*> -> element.forEach {
                if (it is KLogMessage) _lines.add(it)
            }

            is List<*> -> element.forEach {
                if (it is KLogMessage) _lines.add(it)
            }
            else -> throw IllegalArgumentException("Unsupported type: ${element::class.java}")
        }
    }

    // 字符串消息
    operator fun String.unaryMinus(): KLogMessageBuilder {
        addElement(KLogMessage(this))
        return this@KLogMessageBuilder
    }

    operator fun String.unaryPlus(): KLogMessageBuilder {
        addElement(KLogMessage(this, newLine = true))
        return this@KLogMessageBuilder
    }

    // 日志消息对象
    operator fun KLogMessage.unaryMinus(): KLogMessageBuilder {
        addElement(this)
        return this@KLogMessageBuilder
    }

    operator fun KLogMessage.unaryPlus(): KLogMessageBuilder {
        addElement(apply { newLine = true })
        return this@KLogMessageBuilder
    }

    // 数组消息
    operator fun Array<KLogMessage>.unaryMinus(): KLogMessageBuilder {
        addElement(this)
        return this@KLogMessageBuilder
    }

    operator fun Array<KLogMessage>.unaryPlus(): KLogMessageBuilder {
        addElement(onEach { it.newLine = true })
        return this@KLogMessageBuilder
    }

    // 样式应用
    operator fun String.get(vararg codes: PrintCode) = KLogMessage(this, codes.toList().toTypedArray())
    operator fun KLogMessage.get(vararg codes: PrintCode) =
        KLogMessage(text, codes.toList().toTypedArray(), newLine = newLine)

    operator fun Array<KLogMessage>.get(vararg codes: PrintCode) = map {
        KLogMessage(it.text, codes.toList().toTypedArray(), newLine = it.newLine)
    }.toTypedArray()
}

// 修改后的log函数
fun log(buildLog: KLogMessageBuilder.() -> Unit): Array<KLogMessage> {
    val builder = KLogMessageBuilder()
    builder.buildLog()
    return builder.lines.toTypedArray()
}