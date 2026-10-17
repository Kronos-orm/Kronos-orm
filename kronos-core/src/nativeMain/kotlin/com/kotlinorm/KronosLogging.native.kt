/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm

import com.kotlinorm.beans.logging.NativeConsoleKLogger
import com.kotlinorm.enums.KLoggerType
import com.kotlinorm.types.KLoggerFactory

actual object Kronos {
    actual var defaultLogger: KLoggerFactory = { source ->
        NativeConsoleKLogger(source as? String ?: source::class.simpleName ?: "Kronos")
    }
        set(value) {
            field = value
            loggerType = KLoggerType.NONE
        }

    actual var loggerType: KLoggerType = KLoggerType.DEFAULT_LOGGER

    actual var timeZone: String? = null
}
