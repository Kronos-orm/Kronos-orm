/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

@file:OptIn(com.kotlinorm.annotations.InternalKronosApi::class)

package com.kotlinorm

import com.kotlinorm.adapter.NativeConsoleLoggerAdapter
import com.kotlinorm.enums.KLoggerType

internal actual fun installPlatformLogger() {
    if (Kronos.logging.factory != null) return

    Kronos.logging.installAdapterFactory { source ->
        NativeConsoleLoggerAdapter(source as? String ?: source::class.simpleName ?: "Kronos")
    }
    Kronos.logging.type = KLoggerType.DEFAULT_LOGGER
}
