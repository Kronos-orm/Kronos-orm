/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.beans.logging

import com.kotlinorm.beans.logging.BundledSimpleLoggerAdapter.Companion.format
import com.kotlinorm.enums.KLogLevel
import com.kotlinorm.interfaces.PrintCode
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.exists
import kotlin.io.path.writeText

actual class KLogMessage actual constructor(
    actual var text: String,
    internal actual var codes: Array<PrintCode>,
    actual var newLine: Boolean
) {
    fun print(level: KLogLevel) {
        val out = if (level > KLogLevel.WARN) System.out else System.err
        if (newLine) {
            out.println(format(text, codes))
        } else {
            out.print(format(text, codes))
        }
    }

    fun write(path: Path) {
        if (path.exists()) {
            path.writeText(
                text + ("\r\n".takeIf { newLine } ?: ""),
                Charsets.UTF_8,
                StandardOpenOption.APPEND
            )
        } else {
            path.writeText(text + ("\r\n".takeIf { newLine } ?: ""), Charsets.UTF_8)
        }
    }

    actual companion object {
        actual fun Array<KLogMessage>.formatted(): String = joinToString("") { message ->
            message.text + if (message.newLine) "\r\n" else ""
        }
    }
}
