/*
 * Copyright 2022-2026 kronos-orm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.kotlinorm.interfaces

import com.kotlinorm.beans.logging.KLogMessage

/**
 * Builds and sends a message only when the target logger accepts the level.
 * Keeping the builder lazy avoids work and prevents disabled output from
 * reaching adapters that use an eager implementation.
 */
inline fun KLogger.traceIfEnabled(e: Throwable? = null, messages: () -> Array<KLogMessage>) {
    if (isTraceEnabled()) trace(messages(), e)
}

inline fun KLogger.debugIfEnabled(e: Throwable? = null, messages: () -> Array<KLogMessage>) {
    if (isDebugEnabled()) debug(messages(), e)
}

inline fun KLogger.infoIfEnabled(e: Throwable? = null, messages: () -> Array<KLogMessage>) {
    if (isInfoEnabled()) info(messages(), e)
}

inline fun KLogger.warnIfEnabled(e: Throwable? = null, messages: () -> Array<KLogMessage>) {
    if (isWarnEnabled()) warn(messages(), e)
}

inline fun KLogger.errorIfEnabled(e: Throwable? = null, messages: () -> Array<KLogMessage>) {
    if (isErrorEnabled()) error(messages(), e)
}
