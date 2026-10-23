/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm

import com.kotlinorm.adapter.NativeConsoleLoggerAdapter
import com.kotlinorm.enums.KLoggerType

internal actual fun installPlatformLogger() {
    if (Kronos.loggerType == KLoggerType.NONE) return

    Kronos.defaultLogger = { source ->
        NativeConsoleLoggerAdapter(source as? String ?: source::class.simpleName ?: "Kronos")
    }
    Kronos.loggerType = KLoggerType.DEFAULT_LOGGER
}
