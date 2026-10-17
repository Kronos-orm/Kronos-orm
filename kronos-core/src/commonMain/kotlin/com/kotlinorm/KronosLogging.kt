/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm

import com.kotlinorm.enums.KLoggerType
import com.kotlinorm.types.KLoggerFactory

/** The logging state shared by every runtime target. */
expect object Kronos {
    var defaultLogger: KLoggerFactory

    var loggerType: KLoggerType

    /** IANA time-zone id, or `null` to use the platform system default. */
    var timeZone: String?
}
