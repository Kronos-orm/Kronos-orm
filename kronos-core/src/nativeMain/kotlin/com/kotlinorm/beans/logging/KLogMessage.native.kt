/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.beans.logging

import com.kotlinorm.interfaces.PrintCode

actual class KLogMessage actual constructor(
    actual var text: String,
    internal actual var codes: Array<PrintCode>,
    actual var newLine: Boolean
) {
    actual companion object {
        actual fun Array<KLogMessage>.formatted(): String = joinToString("") { message ->
            message.text + if (message.newLine) "\r\n" else ""
        }
    }
}
