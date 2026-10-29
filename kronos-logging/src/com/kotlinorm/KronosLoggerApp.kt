/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm

/** Installs the best logging adapter available on the current platform. */
object KronosLoggerApp {
    fun detectLoggerImplementation() {
        installPlatformLogger()
    }
}

internal expect fun installPlatformLogger()
